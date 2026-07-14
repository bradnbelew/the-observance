package com.observance.watcher.v5runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Thread-safe, local-primary durable progress store.
 *
 * <p>Every mutation is serialized, written to a forced same-directory temporary file, and
 * atomically replaced before the in-memory snapshot changes. This class has deliberately no
 * remote-read or remote-write path; remote mirrors cannot authorize or roll back local state.</p>
 */
public final class V5ProgressStore {
    public static final int SCHEMA_VERSION = 1;
    public static final String AUTHORITY_MODE = "local_plugin_data";

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .setPrettyPrinting()
            .create();
    private static final Pattern STATE_KEY = Pattern.compile("v5_[a-z0-9_]+");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Set<String> ROOT_KEYS = Set.of(
            "schema_version", "campaign_version", "manifest_sha256", "revision",
            "updated_at_epoch_ms", "booleans", "players", "branches", "ballots", "escrow",
            "conduct_verdict");
    private static final Set<String> PLAYER_KEYS =
            Set.of("inspections", "topics", "routes", "session_bits");
    private static final Set<String> BALLOT_KEYS = Set.of(
            "initial_roster_count", "maximum_visible_roster_count",
            "first_ballot_eligible_count", "first_ballot_cast_count",
            "first_ballot_distinct_choices", "first_ballot_tied", "resolution_rounds",
            "disconnect_resnap_count");
    private static final Set<String> ESCROW_KEYS = Set.of(
            "escrow_id", "artifact_id", "intended_player_id", "source_site_id", "source_slot",
            "item_fingerprint_sha256", "amount", "created_at_epoch_ms", "updated_at_epoch_ms",
            "status", "metadata");

    private final Path path;
    private final String manifestSha256;
    private final Set<String> physicalCompletionFlags;
    private final ReentrantLock writeLock = new ReentrantLock(true);
    private volatile ProgressSnapshot snapshot;

    private V5ProgressStore(
            Path path,
            String manifestSha256,
            Set<String> physicalCompletionFlags,
            ProgressSnapshot snapshot) {
        this.path = path;
        this.manifestSha256 = manifestSha256;
        this.physicalCompletionFlags = Set.copyOf(physicalCompletionFlags);
        this.snapshot = snapshot;
    }

    public static V5ProgressStore open(Path path, PhysicalPredicateAuthority authority)
            throws IOException {
        Objects.requireNonNull(authority, "authority");
        Set<String> flags = new HashSet<>();
        for (PhysicalPredicateAuthority.Node node : authority.nodes()) {
            flags.add(node.completionFlag());
        }
        return open(path, authority.sha256(), flags);
    }

    static V5ProgressStore open(Path path, String manifestSha256, Set<String> completionFlags)
            throws IOException {
        Objects.requireNonNull(path, "path");
        requireSha256(manifestSha256, "manifestSha256");
        if (completionFlags.size() != PhysicalPredicateAuthority.REQUIRED_NODE_COUNT) {
            throw new IllegalArgumentException("completionFlags must contain all 60 physical flags");
        }
        for (String flag : completionFlags) {
            requireStateKey(flag, "completion flag");
        }

        Path absolute = path.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("progress path must have a parent directory");
        }
        Files.createDirectories(parent);

        ProgressSnapshot loaded;
        if (Files.exists(absolute)) {
            byte[] bytes = Files.readAllBytes(absolute);
            try {
                loaded = decode(bytes, manifestSha256);
            } catch (RuntimeException exception) {
                Path recoveryCopy = null;
                IOException preservationFailure = null;
                try {
                    recoveryCopy = preserveCorruptRecord(absolute);
                } catch (IOException copyException) {
                    preservationFailure = copyException;
                }
                CorruptProgressException corrupt = new CorruptProgressException(
                        "V5 local progress is corrupt or incompatible; refusing to start with blank state",
                        exception,
                        absolute,
                        recoveryCopy);
                if (preservationFailure != null) {
                    corrupt.addSuppressed(preservationFailure);
                }
                throw corrupt;
            }
        } else {
            loaded = new ProgressSnapshot(
                    SCHEMA_VERSION,
                    PhysicalPredicateAuthority.CAMPAIGN_VERSION,
                    manifestSha256,
                    0,
                    System.currentTimeMillis(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Optional.empty());
            validateSnapshot(loaded, manifestSha256);
            writeAtomic(absolute, loaded);
        }
        return new V5ProgressStore(absolute, manifestSha256, completionFlags, loaded);
    }

    public Path path() {
        return path;
    }

    public String manifestSha256() {
        return manifestSha256;
    }

    public ProgressSnapshot snapshot() {
        return snapshot;
    }

    public boolean completeIfAbsent(String completionFlag) throws IOException {
        return compareAndSetCompletion(completionFlag, false, true);
    }

    /** Monotonic compare-and-set: V5 physical completion flags can move only false to true. */
    public boolean compareAndSetCompletion(
            String completionFlag, boolean expected, boolean update) throws IOException {
        if (expected || !update) {
            throw new IllegalArgumentException(
                    "physical completion is monotonic; the only permitted CAS is false -> true");
        }
        return transact(editor -> editor.compareAndSetCompletion(completionFlag, false, true));
    }

    /** Runs one local transaction. The result becomes visible only after its atomic file commit. */
    public <T> T transact(Function<Editor, T> mutation) throws IOException {
        Objects.requireNonNull(mutation, "mutation");
        writeLock.lock();
        try {
            ProgressSnapshot before = snapshot;
            Editor editor = new Editor(before, physicalCompletionFlags);
            T result;
            try {
                result = mutation.apply(editor);
            } finally {
                editor.deactivate();
            }
            if (!editor.dirty()) {
                return result;
            }
            ProgressSnapshot after = editor.build(
                    before.revision() + 1,
                    Math.max(System.currentTimeMillis(), before.updatedAtEpochMillis()));
            validateSnapshot(after, manifestSha256);
            writeAtomic(path, after);
            snapshot = after;
            return result;
        } finally {
            writeLock.unlock();
        }
    }

    public static final class Editor {
        private final int schemaVersion;
        private final String campaignVersion;
        private final String manifestSha256;
        private final Set<String> completionFlags;
        private final Map<String, Boolean> booleans;
        private final Map<String, MutablePlayer> players;
        private final Map<String, String> branches;
        private final Map<String, BallotTelemetry> ballots;
        private final Map<String, EscrowEntry> escrow;
        private ConductVerdict conductVerdict;
        private boolean active = true;
        private boolean dirty;

        private Editor(ProgressSnapshot source, Set<String> completionFlags) {
            schemaVersion = source.schemaVersion();
            campaignVersion = source.campaignVersion();
            manifestSha256 = source.manifestSha256();
            this.completionFlags = completionFlags;
            booleans = new HashMap<>(source.booleans());
            players = new HashMap<>();
            source.players().forEach((playerId, progress) ->
                    players.put(playerId, new MutablePlayer(progress)));
            branches = new HashMap<>(source.branches());
            ballots = new HashMap<>(source.ballots());
            escrow = new HashMap<>(source.escrow());
            conductVerdict = source.conductVerdict().orElse(null);
        }

        public boolean compareAndSetCompletion(
                String completionFlag, boolean expected, boolean update) {
            requireActive();
            if (!completionFlags.contains(completionFlag)) {
                throw new IllegalArgumentException(
                        "not a registered V5 physical completion flag: " + completionFlag);
            }
            if (expected || !update) {
                throw new IllegalArgumentException(
                        "physical completion is monotonic; only false -> true is permitted");
            }
            if (Boolean.TRUE.equals(booleans.get(completionFlag))) {
                return false;
            }
            booleans.put(completionFlag, true);
            dirty = true;
            return true;
        }

        /** Adds a monotonic local mirror bit, including non-physical prerequisite flags. */
        public boolean setBooleanTrue(String key) {
            requireActive();
            requireStateKey(key, "boolean key");
            if (completionFlags.contains(key)) {
                throw new IllegalArgumentException(
                        "physical completion flags must use compareAndSetCompletion: " + key);
            }
            if (Boolean.TRUE.equals(booleans.get(key))) {
                return false;
            }
            booleans.put(key, true);
            dirty = true;
            return true;
        }

        public boolean addPlayerBit(UUID playerId, PlayerBitDomain domain, String bit) {
            requireActive();
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(domain, "domain");
            requireText(bit, "player bit");
            MutablePlayer player = players.computeIfAbsent(
                    playerId.toString(), ignored -> new MutablePlayer(PlayerProgress.empty()));
            boolean changed = player.bits(domain).add(bit);
            dirty |= changed;
            return changed;
        }

        public boolean clearSessionBit(UUID playerId, String bit) {
            requireActive();
            Objects.requireNonNull(playerId, "playerId");
            requireText(bit, "session bit");
            MutablePlayer player = players.get(playerId.toString());
            boolean changed = player != null && player.sessionBits.remove(bit);
            dirty |= changed;
            return changed;
        }

        public boolean putBranchOnce(String key, String value) {
            requireActive();
            requireStateKey(key, "branch key");
            requireText(value, "branch value");
            validateKnownBranch(key, value);
            String current = branches.get(key);
            if (current != null) {
                if (!current.equals(value)) {
                    throw new IllegalStateException("immutable branch " + key + " is already " + current);
                }
                return false;
            }
            branches.put(key, value);
            dirty = true;
            return true;
        }

        public boolean putBallotOnce(String voteNodeId, BallotTelemetry telemetry) {
            requireActive();
            if (!Set.of("WR05", "RP03").contains(voteNodeId)) {
                throw new IllegalArgumentException("only WR05 and RP03 ballot telemetry is valid");
            }
            Objects.requireNonNull(telemetry, "telemetry");
            BallotTelemetry current = ballots.get(voteNodeId);
            if (current != null) {
                if (!current.equals(telemetry)) {
                    throw new IllegalStateException(
                            "immutable first-ballot telemetry already exists for " + voteNodeId);
                }
                return false;
            }
            ballots.put(voteNodeId, telemetry);
            dirty = true;
            return true;
        }

        public ConductVerdict deriveAndSetConductVerdict() {
            requireActive();
            BallotTelemetry wr05 = ballots.get("WR05");
            BallotTelemetry rp03 = ballots.get("RP03");
            ConductVerdict derived = ConductVerdictDeriver.derive(
                    wr05,
                    rp03,
                    branches.get("v5_wren_outcome"),
                    branches.get("v5_name_treatment"));
            if (conductVerdict != null && conductVerdict != derived) {
                throw new IllegalStateException("persisted conduct verdict cannot be changed");
            }
            if (conductVerdict == null) {
                conductVerdict = derived;
                dirty = true;
            }
            return derived;
        }

        public boolean putEscrowOnce(EscrowEntry entry) {
            requireActive();
            Objects.requireNonNull(entry, "entry");
            EscrowEntry current = escrow.get(entry.escrowId());
            if (current != null) {
                if (!current.equals(entry)) {
                    throw new IllegalStateException("escrow id already exists: " + entry.escrowId());
                }
                return false;
            }
            escrow.put(entry.escrowId(), entry);
            dirty = true;
            return true;
        }

        public boolean transitionEscrow(
                String escrowId, EscrowStatus expectedStatus, EscrowEntry replacement) {
            requireActive();
            requireText(escrowId, "escrowId");
            Objects.requireNonNull(expectedStatus, "expectedStatus");
            Objects.requireNonNull(replacement, "replacement");
            EscrowEntry current = escrow.get(escrowId);
            if (current == null || current.status() != expectedStatus) {
                return false;
            }
            if (!sameEscrowIdentity(current, replacement)) {
                throw new IllegalArgumentException("escrow transition cannot change protected item identity");
            }
            if (replacement.updatedAtEpochMillis() < current.updatedAtEpochMillis()) {
                throw new IllegalArgumentException("escrow transition cannot move its timestamp backward");
            }
            if (current.equals(replacement)) {
                return false;
            }
            if (!allowedEscrowTransition(current.status(), replacement.status())) {
                throw new IllegalArgumentException("invalid escrow status transition " + current.status()
                        + " -> " + replacement.status());
            }
            escrow.put(escrowId, replacement);
            dirty = true;
            return true;
        }

        private boolean dirty() {
            return dirty;
        }

        private void deactivate() {
            active = false;
        }

        private void requireActive() {
            if (!active) {
                throw new IllegalStateException("transaction editor is no longer active");
            }
        }

        private ProgressSnapshot build(long revision, long updatedAtEpochMillis) {
            Map<String, PlayerProgress> immutablePlayers = new HashMap<>();
            players.forEach((playerId, player) -> immutablePlayers.put(playerId, player.toImmutable()));
            return new ProgressSnapshot(
                    schemaVersion,
                    campaignVersion,
                    manifestSha256,
                    revision,
                    updatedAtEpochMillis,
                    booleans,
                    immutablePlayers,
                    branches,
                    ballots,
                    escrow,
                    Optional.ofNullable(conductVerdict));
        }
    }

    private static final class MutablePlayer {
        private final Set<String> inspections;
        private final Set<String> topics;
        private final Set<String> routes;
        private final Set<String> sessionBits;

        private MutablePlayer(PlayerProgress source) {
            inspections = new HashSet<>(source.inspections());
            topics = new HashSet<>(source.topics());
            routes = new HashSet<>(source.routes());
            sessionBits = new HashSet<>(source.sessionBits());
        }

        private Set<String> bits(PlayerBitDomain domain) {
            return switch (domain) {
                case INSPECTION -> inspections;
                case TOPIC -> topics;
                case ROUTE -> routes;
                case SESSION -> sessionBits;
            };
        }

        private PlayerProgress toImmutable() {
            return new PlayerProgress(inspections, topics, routes, sessionBits);
        }
    }

    private static boolean sameEscrowIdentity(EscrowEntry left, EscrowEntry right) {
        return left.escrowId().equals(right.escrowId())
                && left.artifactId().equals(right.artifactId())
                && left.itemFingerprintSha256().equals(right.itemFingerprintSha256())
                && left.amount() == right.amount()
                && left.createdAtEpochMillis() == right.createdAtEpochMillis();
    }

    private static boolean allowedEscrowTransition(EscrowStatus from, EscrowStatus to) {
        if (from == to) {
            return from != EscrowStatus.DELIVERED;
        }
        return switch (from) {
            case HELD -> EnumSet.of(EscrowStatus.DELIVERY_PENDING, EscrowStatus.RETURN_PENDING)
                    .contains(to);
            case DELIVERY_PENDING, RETURN_PENDING ->
                    EnumSet.of(EscrowStatus.HELD, EscrowStatus.DELIVERED).contains(to);
            case DELIVERED -> false;
        };
    }

    private static void validateKnownBranch(String key, String value) {
        if ("v5_wren_outcome".equals(key)
                && !Set.of("condemn", "understand", "free").contains(value)) {
            throw new IllegalArgumentException("invalid v5_wren_outcome " + value);
        }
        if ("v5_name_treatment".equals(key)
                && !Set.of("publish", "release_unnamed").contains(value)) {
            throw new IllegalArgumentException("invalid v5_name_treatment " + value);
        }
    }

    private static void validateSnapshot(ProgressSnapshot value, String expectedManifestSha256) {
        if (value.schemaVersion() != SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported progress schema " + value.schemaVersion());
        }
        if (!PhysicalPredicateAuthority.CAMPAIGN_VERSION.equals(value.campaignVersion())) {
            throw new IllegalArgumentException("progress campaign is not v5");
        }
        if (!expectedManifestSha256.equals(value.manifestSha256())) {
            throw new IllegalArgumentException("progress manifest hash does not match packaged authority");
        }
        value.booleans().forEach((key, state) -> {
            requireStateKey(key, "boolean key");
            if (state == null || !state) {
                throw new IllegalArgumentException(
                        "V5 booleans are monotonic true-only facts; invalid value for " + key);
            }
        });
        value.players().forEach((playerId, progress) -> {
            UUID.fromString(playerId);
            Objects.requireNonNull(progress, "player progress");
        });
        value.branches().forEach((key, branch) -> {
            requireStateKey(key, "branch key");
            requireText(branch, "branch value");
            validateKnownBranch(key, branch);
        });
        value.ballots().forEach((voteId, telemetry) -> {
            if (!Set.of("WR05", "RP03").contains(voteId)) {
                throw new IllegalArgumentException("unknown ballot telemetry owner " + voteId);
            }
            Objects.requireNonNull(telemetry, "ballot telemetry");
        });
        value.escrow().forEach((escrowId, entry) -> {
            if (!escrowId.equals(entry.escrowId())) {
                throw new IllegalArgumentException("escrow map key does not match entry id");
            }
        });

        boolean wr05Committed = value.isComplete("v5_case_c08_complete");
        if (wr05Committed
                && (!value.ballots().containsKey("WR05")
                        || !value.branches().containsKey("v5_wren_outcome"))) {
            throw new IllegalArgumentException("WR05 completion requires branch and first-ballot telemetry");
        }
        boolean rp03Committed = value.isComplete("v5_rp03_name_choice");
        if (rp03Committed && (value.conductVerdict().isEmpty()
                || !value.ballots().keySet().containsAll(Set.of("WR05", "RP03"))
                || !value.branches().keySet().containsAll(
                        Set.of("v5_wren_outcome", "v5_name_treatment")))) {
            throw new IllegalArgumentException(
                    "RP03 completion requires both ballots, both resolved branches, and conduct verdict");
        }
        value.conductVerdict().ifPresent(verdict -> {
            ConductVerdict derived = ConductVerdictDeriver.derive(
                    value.ballots().get("WR05"),
                    value.ballots().get("RP03"),
                    value.branches().get("v5_wren_outcome"),
                    value.branches().get("v5_name_treatment"));
            if (verdict != derived) {
                throw new IllegalArgumentException("stored conduct verdict does not match durable ballots");
            }
        });
    }

    private static ProgressSnapshot decode(byte[] bytes, String expectedManifestSha256) {
        final JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("progress root must be an object");
            }
            root = parsed.getAsJsonObject();
            validateJsonShape(root);
        } catch (JsonParseException exception) {
            throw new IllegalArgumentException("malformed progress JSON", exception);
        }

        StoredState stored = GSON.fromJson(root, StoredState.class);
        ProgressSnapshot decoded = fromStored(stored);
        validateSnapshot(decoded, expectedManifestSha256);
        return decoded;
    }

    private static ProgressSnapshot fromStored(StoredState stored) {
        Objects.requireNonNull(stored, "stored");
        Map<String, PlayerProgress> players = new HashMap<>();
        requireMap(stored.players, "players").forEach((playerId, player) -> {
            Objects.requireNonNull(player, "stored player");
            players.put(playerId, new PlayerProgress(
                    new HashSet<>(requireList(player.inspections, "inspections")),
                    new HashSet<>(requireList(player.topics, "topics")),
                    new HashSet<>(requireList(player.routes, "routes")),
                    new HashSet<>(requireList(player.session_bits, "session_bits"))));
        });

        Map<String, BallotTelemetry> ballots = new HashMap<>();
        requireMap(stored.ballots, "ballots").forEach((voteId, ballot) -> {
            Objects.requireNonNull(ballot, "stored ballot");
            ballots.put(voteId, new BallotTelemetry(
                    ballot.initial_roster_count,
                    ballot.maximum_visible_roster_count,
                    ballot.first_ballot_eligible_count,
                    ballot.first_ballot_cast_count,
                    ballot.first_ballot_distinct_choices,
                    ballot.first_ballot_tied,
                    ballot.resolution_rounds,
                    ballot.disconnect_resnap_count));
        });

        Map<String, EscrowEntry> escrow = new HashMap<>();
        requireMap(stored.escrow, "escrow").forEach((escrowId, item) -> {
            Objects.requireNonNull(item, "stored escrow");
            Optional<UUID> player = item.intended_player_id == null
                    ? Optional.empty() : Optional.of(UUID.fromString(item.intended_player_id));
            EscrowStatus status = EscrowStatus.valueOf(requireText(item.status, "escrow status"));
            escrow.put(escrowId, new EscrowEntry(
                    item.escrow_id,
                    item.artifact_id,
                    player,
                    item.source_site_id,
                    item.source_slot,
                    item.item_fingerprint_sha256,
                    item.amount,
                    item.created_at_epoch_ms,
                    item.updated_at_epoch_ms,
                    status,
                    requireMap(item.metadata, "escrow metadata")));
        });

        Optional<ConductVerdict> conduct = stored.conduct_verdict == null
                ? Optional.empty()
                : Optional.of(ConductVerdict.fromWireValue(stored.conduct_verdict));
        return new ProgressSnapshot(
                stored.schema_version,
                stored.campaign_version,
                stored.manifest_sha256,
                stored.revision,
                stored.updated_at_epoch_ms,
                requireMap(stored.booleans, "booleans"),
                players,
                requireMap(stored.branches, "branches"),
                ballots,
                escrow,
                conduct);
    }

    private static void validateJsonShape(JsonObject root) {
        exactKeys(root, ROOT_KEYS, "root");
        requireJsonInteger(root.get("schema_version"), "schema_version");
        requireJsonString(root.get("campaign_version"), "campaign_version", false);
        requireJsonString(root.get("manifest_sha256"), "manifest_sha256", false);
        requireJsonInteger(root.get("revision"), "revision");
        requireJsonInteger(root.get("updated_at_epoch_ms"), "updated_at_epoch_ms");
        JsonObject players = requireObject(root, "players", "root");
        players.entrySet().forEach(entry -> {
            if (!entry.getValue().isJsonObject()) {
                throw new IllegalArgumentException("players." + entry.getKey() + " must be an object");
            }
            JsonObject player = entry.getValue().getAsJsonObject();
            exactKeys(player, PLAYER_KEYS, "players." + entry.getKey());
            for (String key : PLAYER_KEYS) {
                requireStringArray(player, key, "players." + entry.getKey());
            }
        });
        JsonObject booleans = requireObject(root, "booleans", "root");
        booleans.entrySet().forEach(entry -> {
            requireJsonBoolean(entry.getValue(), "booleans." + entry.getKey());
            if (!entry.getValue().getAsBoolean()) {
                throw new IllegalArgumentException(
                        "booleans." + entry.getKey() + " must be monotonic true");
            }
        });
        JsonObject branches = requireObject(root, "branches", "root");
        branches.entrySet().forEach(entry ->
                requireJsonString(entry.getValue(), "branches." + entry.getKey(), false));
        JsonObject ballots = requireObject(root, "ballots", "root");
        ballots.entrySet().forEach(entry -> {
            if (!entry.getValue().isJsonObject()) {
                throw new IllegalArgumentException("ballots." + entry.getKey() + " must be an object");
            }
            JsonObject ballot = entry.getValue().getAsJsonObject();
            String context = "ballots." + entry.getKey();
            exactKeys(ballot, BALLOT_KEYS, context);
            for (String key : BALLOT_KEYS) {
                if ("first_ballot_tied".equals(key)) {
                    requireJsonBoolean(ballot.get(key), context + "." + key);
                } else {
                    requireJsonInteger(ballot.get(key), context + "." + key);
                }
            }
        });
        JsonObject escrow = requireObject(root, "escrow", "root");
        escrow.entrySet().forEach(entry -> {
            if (!entry.getValue().isJsonObject()) {
                throw new IllegalArgumentException("escrow." + entry.getKey() + " must be an object");
            }
            JsonObject item = entry.getValue().getAsJsonObject();
            String context = "escrow." + entry.getKey();
            exactKeys(item, ESCROW_KEYS, context);
            for (String key : Set.of(
                    "escrow_id", "artifact_id", "source_site_id", "item_fingerprint_sha256",
                    "status")) {
                requireJsonString(item.get(key), context + "." + key, false);
            }
            JsonElement intendedPlayer = item.get("intended_player_id");
            if (intendedPlayer == null || (!intendedPlayer.isJsonNull()
                    && !isJsonString(intendedPlayer))) {
                throw new IllegalArgumentException(
                        context + ".intended_player_id must be a string or null");
            }
            for (String key : Set.of(
                    "source_slot", "amount", "created_at_epoch_ms", "updated_at_epoch_ms")) {
                requireJsonInteger(item.get(key), context + "." + key);
            }
            JsonObject metadata = requireObject(item, "metadata", context);
            metadata.entrySet().forEach(metadataEntry -> requireJsonString(
                    metadataEntry.getValue(), context + ".metadata." + metadataEntry.getKey(), false));
        });
        JsonElement conduct = root.get("conduct_verdict");
        if (conduct == null || (!conduct.isJsonNull()
                && (!conduct.isJsonPrimitive() || !conduct.getAsJsonPrimitive().isString()))) {
            throw new IllegalArgumentException("conduct_verdict must be a string or null");
        }
    }

    private static void requireStringArray(JsonObject parent, String key, String context) {
        JsonElement element = parent.get(key);
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException(context + "." + key + " must be an array");
        }
        JsonArray values = element.getAsJsonArray();
        Set<String> unique = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            JsonElement value = values.get(index);
            String parsed = requireJsonString(
                    value, context + "." + key + "[" + index + "]", false);
            if (!unique.add(parsed)) {
                throw new IllegalArgumentException(context + "." + key
                        + " cannot contain duplicate bit " + parsed);
            }
        }
    }

    private static String requireJsonString(JsonElement value, String context, boolean allowBlank) {
        if (!isJsonString(value)) {
            throw new IllegalArgumentException(context + " must be a JSON string");
        }
        String parsed = value.getAsString();
        if (!allowBlank && parsed.isBlank()) {
            throw new IllegalArgumentException(context + " cannot be blank");
        }
        return parsed;
    }

    private static boolean isJsonString(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
    }

    private static void requireJsonBoolean(JsonElement value, String context) {
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException(context + " must be a JSON boolean");
        }
    }

    private static void requireJsonInteger(JsonElement value, String context) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(context + " must be a JSON integer");
        }
        try {
            value.getAsBigDecimal().toBigIntegerExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(context + " must be an exact JSON integer", exception);
        }
    }

    private static JsonObject requireObject(JsonObject parent, String key, String context) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalArgumentException(context + "." + key + " must be an object");
        }
        return value.getAsJsonObject();
    }

    private static void exactKeys(JsonObject object, Set<String> expected, String context) {
        Set<String> actual = object.keySet();
        if (!actual.equals(expected)) {
            Set<String> missing = new HashSet<>(expected);
            missing.removeAll(actual);
            Set<String> unknown = new HashSet<>(actual);
            unknown.removeAll(expected);
            throw new IllegalArgumentException(context + " keys mismatch; missing=" + missing
                    + ", unknown=" + unknown);
        }
    }

    private static void writeAtomic(Path destination, ProgressSnapshot value) throws IOException {
        byte[] bytes = (GSON.toJson(toStored(value)) + System.lineSeparator())
                .getBytes(StandardCharsets.UTF_8);
        Path parent = destination.getParent();
        Path temporary = parent.resolve(destination.getFileName() + ".tmp-" + UUID.randomUUID());
        boolean moved = false;
        try {
            try (FileChannel channel = FileChannel.open(
                    temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            try {
                Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException(
                        "filesystem cannot atomically replace V5 progress; refusing unsafe commit",
                        exception);
            }
            moved = true;
            forceDirectoryBestEffort(parent);
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static Path preserveCorruptRecord(Path original) throws IOException {
        Path recovery = original.resolveSibling(original.getFileName()
                + ".corrupt-" + System.currentTimeMillis() + "-" + UUID.randomUUID() + ".recovery");
        Files.copy(original, recovery, StandardCopyOption.COPY_ATTRIBUTES);
        try (FileChannel channel = FileChannel.open(recovery, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
        forceDirectoryBestEffort(recovery.getParent());
        return recovery;
    }

    private static void forceDirectoryBestEffort(Path directory) {
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (IOException | UnsupportedOperationException ignored) {
            // The file itself was forced before rename/copy. Some Windows providers cannot open dirs.
        }
    }

    private static StoredState toStored(ProgressSnapshot source) {
        StoredState stored = new StoredState();
        stored.schema_version = source.schemaVersion();
        stored.campaign_version = source.campaignVersion();
        stored.manifest_sha256 = source.manifestSha256();
        stored.revision = source.revision();
        stored.updated_at_epoch_ms = source.updatedAtEpochMillis();
        stored.booleans = new TreeMap<>(source.booleans());
        stored.players = new TreeMap<>();
        source.players().forEach((playerId, progress) -> {
            StoredPlayer player = new StoredPlayer();
            player.inspections = new ArrayList<>(new TreeSet<>(progress.inspections()));
            player.topics = new ArrayList<>(new TreeSet<>(progress.topics()));
            player.routes = new ArrayList<>(new TreeSet<>(progress.routes()));
            player.session_bits = new ArrayList<>(new TreeSet<>(progress.sessionBits()));
            stored.players.put(playerId, player);
        });
        stored.branches = new TreeMap<>(source.branches());
        stored.ballots = new TreeMap<>();
        source.ballots().forEach((voteId, telemetry) -> {
            StoredBallot ballot = new StoredBallot();
            ballot.initial_roster_count = telemetry.initialRosterCount();
            ballot.maximum_visible_roster_count = telemetry.maximumVisibleRosterCount();
            ballot.first_ballot_eligible_count = telemetry.firstBallotEligibleCount();
            ballot.first_ballot_cast_count = telemetry.firstBallotCastCount();
            ballot.first_ballot_distinct_choices = telemetry.firstBallotDistinctChoices();
            ballot.first_ballot_tied = telemetry.firstBallotTied();
            ballot.resolution_rounds = telemetry.resolutionRounds();
            ballot.disconnect_resnap_count = telemetry.disconnectResnapCount();
            stored.ballots.put(voteId, ballot);
        });
        stored.escrow = new TreeMap<>();
        source.escrow().forEach((escrowId, entry) -> {
            StoredEscrow item = new StoredEscrow();
            item.escrow_id = entry.escrowId();
            item.artifact_id = entry.artifactId();
            item.intended_player_id = entry.intendedPlayer().map(UUID::toString).orElse(null);
            item.source_site_id = entry.sourceSiteId();
            item.source_slot = entry.sourceSlot();
            item.item_fingerprint_sha256 = entry.itemFingerprintSha256();
            item.amount = entry.amount();
            item.created_at_epoch_ms = entry.createdAtEpochMillis();
            item.updated_at_epoch_ms = entry.updatedAtEpochMillis();
            item.status = entry.status().name();
            item.metadata = new TreeMap<>(entry.metadata());
            stored.escrow.put(escrowId, item);
        });
        stored.conduct_verdict = source.conductVerdict()
                .map(ConductVerdict::wireValue)
                .orElse(null);
        return stored;
    }

    private static void requireStateKey(String key, String label) {
        if (key == null || !STATE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException(label + " must be a v5_ lower-snake key");
        }
    }

    private static void requireSha256(String value, String label) {
        if (value == null || !SHA256.matcher(value).matches()) {
            throw new IllegalArgumentException(label + " must be lowercase SHA-256");
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }

    private static <K, V> Map<K, V> requireMap(Map<K, V> value, String label) {
        return Objects.requireNonNull(value, label);
    }

    private static <T> List<T> requireList(List<T> value, String label) {
        return Objects.requireNonNull(value, label);
    }

    private static final class StoredState {
        private int schema_version;
        private String campaign_version;
        private String manifest_sha256;
        private long revision;
        private long updated_at_epoch_ms;
        private Map<String, Boolean> booleans;
        private Map<String, StoredPlayer> players;
        private Map<String, String> branches;
        private Map<String, StoredBallot> ballots;
        private Map<String, StoredEscrow> escrow;
        private String conduct_verdict;
    }

    private static final class StoredPlayer {
        private List<String> inspections;
        private List<String> topics;
        private List<String> routes;
        private List<String> session_bits;
    }

    private static final class StoredBallot {
        private int initial_roster_count;
        private int maximum_visible_roster_count;
        private int first_ballot_eligible_count;
        private int first_ballot_cast_count;
        private int first_ballot_distinct_choices;
        private boolean first_ballot_tied;
        private int resolution_rounds;
        private int disconnect_resnap_count;
    }

    private static final class StoredEscrow {
        private String escrow_id;
        private String artifact_id;
        private String intended_player_id;
        private String source_site_id;
        private int source_slot;
        private String item_fingerprint_sha256;
        private int amount;
        private long created_at_epoch_ms;
        private long updated_at_epoch_ms;
        private String status;
        private Map<String, String> metadata;
    }
}
