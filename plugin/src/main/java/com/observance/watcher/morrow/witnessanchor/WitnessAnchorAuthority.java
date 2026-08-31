package com.observance.watcher.morrow.witnessanchor;

import com.observance.watcher.morrow.MorrowRelationshipSnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Pure M07 authority: arbitrary six-cell construction, immutable version, and mismatch proof. */
public final class WitnessAnchorAuthority {
    public static final String PREREQUISITE = "morrow.act3.incomplete_consensus_proven";
    public static final String EVENT = "morrow.act4.witness_anchor_registered";

    private WitnessAnchorAuthority() { }

    public static Result edit(Progress progress, MorrowRelationshipSnapshot snapshot,
                              CellId cell, MaterialChoice material, UUID player) {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(player, "player");
        if (!snapshot.committedEvents().contains(PREREQUISITE)) return Result.readOnly(Status.LOCKED,
                progress, "The witness table is sealed until M06 files incomplete consensus.");
        if (progress.anchor() != null) return Result.readOnly(Status.IMMUTABLE, progress,
                "Anchor version 1 is committed and cannot be overwritten. Compare the reconstruction instead.");
        Progress next = progress.edit(cell, material);
        String value = material == null ? "cleared" : material.displayName();
        return Result.readOnly(Status.EDITED, next,
                cell.label() + " " + value + ". Uncommitted cells remain freely editable.");
    }

    public static Result commit(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player) {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(player, "player");
        if (!snapshot.committedEvents().contains(PREREQUISITE)) return Result.readOnly(Status.LOCKED,
                progress, "M07 cannot commit before M06 preserves the dissenting votes.");
        if (progress.anchor() != null) return Result.readOnly(Status.IMMUTABLE, progress,
                "Anchor version 1 already exists. It remains visible and unchanged.");
        if (progress.draft().size() != CellId.values().length) return Result.readOnly(Status.INCOMPLETE,
                progress, "Fill all six labeled cells before committing an authentication anchor.");
        CommittedAnchor anchor = createAnchor(progress.draft());
        return Result.readOnly(Status.COMMITTED_LOCAL, progress.commit(anchor),
                "Anchor version 1 committed as " + anchor.originalSha256().substring(0, 12)
                        + ". Morrow reconstructed it beside the original; identify the one incorrect cell.");
    }

    public static Result identify(Progress progress, MorrowRelationshipSnapshot snapshot,
                                  CellId reconstructionCell, UUID player) {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(reconstructionCell, "reconstructionCell");
        Objects.requireNonNull(player, "player");
        if (progress.anchor() == null) return Result.readOnly(Status.INCOMPLETE, progress,
                "Commit a complete original anchor before auditing a reconstruction.");
        if (snapshot.committedEvents().contains(EVENT)) return Result.readOnly(Status.DUPLICATE, progress,
                "Witness anchor version 1 is already authenticated. Both versions remain visible.");
        if (progress.identified()) return recover(progress, snapshot);
        if (reconstructionCell != progress.anchor().mismatch()) {
            return Result.readOnly(Status.WRONG_CELL, progress,
                    reconstructionCell.label() + " matches the committed original. Compare the named material at both coordinates; nothing changed.");
        }
        Progress identified = progress.identify();
        return new Result(Status.READY_TO_COMMIT, identified, EVENT,
                "paper:m07:witness-anchor:v1",
                "Mismatch authenticated at " + reconstructionCell.label()
                        + ". Arbitrary human choice now has a durable versioned witness.");
    }

    /** Crash recovery after local identified=true persisted but before the journal append. */
    public static Result recover(Progress progress, MorrowRelationshipSnapshot snapshot) {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.committedEvents().contains(EVENT)) return Result.readOnly(Status.DUPLICATE, progress,
                "Witness anchor version 1 is already authenticated.");
        if (progress.anchor() == null || !progress.identified()) return Result.readOnly(Status.INCOMPLETE,
                progress, "No identified M07 reconstruction is waiting for recovery.");
        return new Result(Status.READY_TO_COMMIT, progress, EVENT,
                "paper:m07:witness-anchor:v1",
                "Recovered the identified M07 mismatch without replaying a player interaction.");
    }

    public static byte[] payload(Result result) {
        if (!EVENT.equals(result.eventKey()) || result.progress().anchor() == null) {
            throw new IllegalArgumentException("M07 result is not committable");
        }
        CommittedAnchor anchor = result.progress().anchor();
        StringBuilder cells = new StringBuilder();
        for (CellId cell : CellId.values()) {
            if (!cells.isEmpty()) cells.append(',');
            cells.append('"').append(cell.label()).append("\":\"")
                    .append(anchor.original().get(cell).minecraftKey()).append('"');
        }
        return ("{\"anchor_version\":1,\"cells\":{" + cells + "},\"identified_mismatch\":\""
                + anchor.mismatch().label() + "\",\"investigation\":\"M07\",\"original_sha256\":\""
                + anchor.originalSha256() + "\",\"reconstruction_sha256\":\""
                + anchor.reconstructionSha256() + "\",\"source_overwritten\":false}")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static CommittedAnchor createAnchor(Map<CellId, MaterialChoice> draft) {
        EnumMap<CellId, MaterialChoice> original = new EnumMap<>(CellId.class);
        original.putAll(draft);
        String originalHash = hash(original);
        CellId mismatch = CellId.values()[Integer.parseInt(originalHash.substring(0, 2), 16)
                % CellId.values().length];
        EnumMap<CellId, MaterialChoice> reconstruction = new EnumMap<>(original);
        reconstruction.put(mismatch, original.get(mismatch).next());
        return new CommittedAnchor(original, reconstruction, mismatch, originalHash,
                hash(reconstruction), 1);
    }

    static String hash(Map<CellId, MaterialChoice> cells) {
        StringBuilder canonical = new StringBuilder();
        for (CellId cell : CellId.values()) {
            MaterialChoice material = cells.get(cell);
            if (material == null) throw new IllegalArgumentException("M07 hash requires all six cells");
            canonical.append(cell.label()).append('=').append(material.minecraftKey()).append('\n');
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public enum CellId {
        A1("A1"), A2("A2"), A3("A3"), B1("B1"), B2("B2"), B3("B3");
        private final String label;
        CellId(String label) { this.label = label; }
        public String label() { return label; }
    }

    public enum MaterialChoice {
        COPPER_SQUARE("minecraft:copper_block", "copper square"),
        CARVED_SHELF("minecraft:chiseled_bookshelf", "carved shelf"),
        TARGET_RING("minecraft:target", "target ring"),
        TUFF_BRICKS("minecraft:tuff_bricks", "tuff bricks"),
        HONEYCOMB("minecraft:honeycomb_block", "honeycomb"),
        WEATHERED_CUT("minecraft:oxidized_cut_copper", "weathered cut copper");
        private final String minecraftKey;
        private final String displayName;
        MaterialChoice(String minecraftKey, String displayName) {
            this.minecraftKey = minecraftKey;
            this.displayName = displayName;
        }
        public String minecraftKey() { return minecraftKey; }
        public String displayName() { return displayName; }
        public MaterialChoice next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public enum Status { LOCKED, EDITED, INCOMPLETE, COMMITTED_LOCAL, IMMUTABLE,
        WRONG_CELL, READY_TO_COMMIT, DUPLICATE }

    public record CommittedAnchor(Map<CellId, MaterialChoice> original,
                                  Map<CellId, MaterialChoice> reconstruction,
                                  CellId mismatch, String originalSha256,
                                  String reconstructionSha256, int version) {
        public CommittedAnchor {
            EnumMap<CellId, MaterialChoice> originalCopy = new EnumMap<>(CellId.class);
            originalCopy.putAll(Objects.requireNonNull(original, "original"));
            EnumMap<CellId, MaterialChoice> reconstructionCopy = new EnumMap<>(CellId.class);
            reconstructionCopy.putAll(Objects.requireNonNull(reconstruction, "reconstruction"));
            Map<CellId, MaterialChoice> immutableOriginal = Map.copyOf(originalCopy);
            Map<CellId, MaterialChoice> immutableReconstruction = Map.copyOf(reconstructionCopy);
            Objects.requireNonNull(mismatch, "mismatch");
            if (version != 1 || immutableOriginal.size() != CellId.values().length
                    || immutableReconstruction.size() != CellId.values().length
                    || !originalSha256.matches("[0-9a-f]{64}")
                    || !reconstructionSha256.matches("[0-9a-f]{64}")
                    || !originalSha256.equals(hash(immutableOriginal))
                    || !reconstructionSha256.equals(hash(immutableReconstruction))
                    || immutableOriginal.get(mismatch) == immutableReconstruction.get(mismatch)
                    || java.util.Arrays.stream(CellId.values())
                    .filter(cell -> cell != mismatch)
                    .anyMatch(cell -> immutableOriginal.get(cell) != immutableReconstruction.get(cell))) {
                throw new IllegalArgumentException("invalid M07 committed anchor");
            }
            original = immutableOriginal;
            reconstruction = immutableReconstruction;
        }
    }

    public record Progress(Map<CellId, MaterialChoice> draft, CommittedAnchor anchor,
                           boolean identified, long revision) {
        public Progress {
            EnumMap<CellId, MaterialChoice> copy = new EnumMap<>(CellId.class);
            copy.putAll(Objects.requireNonNull(draft, "draft"));
            if (revision < 0 || copy.values().stream().anyMatch(Objects::isNull)
                    || (anchor != null && !copy.equals(anchor.original()))
                    || (identified && anchor == null)) {
                throw new IllegalArgumentException("invalid M07 progress");
            }
            draft = Map.copyOf(copy);
        }
        public static Progress initial() { return new Progress(Map.of(), null, false, 0); }
        Progress edit(CellId cell, MaterialChoice material) {
            EnumMap<CellId, MaterialChoice> next = new EnumMap<>(CellId.class);
            next.putAll(draft);
            if (material == null) next.remove(cell); else next.put(cell, material);
            return new Progress(next, null, false, revision + 1);
        }
        Progress commit(CommittedAnchor committed) {
            return new Progress(committed.original(), committed, false, revision + 1);
        }
        Progress identify() { return new Progress(draft, anchor, true, revision + 1); }
    }

    public record Result(Status status, Progress progress, String eventKey,
                         String idempotencyKey, String feedback) {
        public Result {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(progress, "progress");
            Objects.requireNonNull(feedback, "feedback");
            if ((eventKey == null) != (idempotencyKey == null)) {
                throw new IllegalArgumentException("M07 event and idempotency key must be paired");
            }
        }
        static Result readOnly(Status status, Progress progress, String feedback) {
            return new Result(status, progress, null, null, feedback);
        }
        public boolean commitsEvent() { return eventKey != null; }
    }
}
