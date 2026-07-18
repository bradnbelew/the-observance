package com.observance.watcher.v5runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.ArcStateRow;
import com.observance.watcher.data.rows.PlayerLookupRow;
import com.observance.watcher.data.rows.SettingsRow;
import com.observance.watcher.v5runtime.container.ContainerRuntimePorts;
import com.observance.watcher.v5runtime.mechanics.MechanicPorts;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Validated, monotonic cache for cross-surface V5 facts.
 *
 * <p>False progression values and outages never revoke a fact that was previously observed under
 * the exact campaign/authority metadata. Identity bindings are the exception: a successful player
 * row read with a blank {@code discord_id} authoritatively revokes the old hand after an atomic
 * recovery, while an outage or missing row preserves last-known-good identity state. Plugin-owned
 * physical completion flags are never hydrated from the network; their local CAS file is the only
 * authority.</p>
 */
public final class V5RemoteStateCache implements
        MechanicPorts.ExternalFlagSnapshot,
        ContainerRuntimePorts.ExternalPrerequisites,
        ContainerRuntimePorts.ActorFacts,
        AutoCloseable {
    public static final String CAMPAIGN_SETTING = "v5_campaign_version";
    public static final String AUTHORITY_SETTING = "v5_physical_authority_sha256";

    private final ObservancePlugin plugin;
    private final PhysicalPredicateAuthority authority;
    private final V5ProgressStore progress;
    private final Set<String> physicalFlags;
    private final Set<String> trueRemoteFlags = ConcurrentHashMap.newKeySet();
    private final IdentityLinkRefreshState identities = new IdentityLinkRefreshState();
    private final AtomicBoolean refreshInFlight = new AtomicBoolean();
    private volatile boolean metadataValidated;
    private BukkitTask refreshTask;
    private BukkitTask identityRefreshTask;

    public V5RemoteStateCache(
            ObservancePlugin plugin,
            PhysicalPredicateAuthority authority,
            V5ProgressStore progress) {
        this.plugin = java.util.Objects.requireNonNull(plugin, "plugin");
        this.authority = java.util.Objects.requireNonNull(authority, "authority");
        this.progress = java.util.Objects.requireNonNull(progress, "progress");
        this.physicalFlags = authority.nodes().stream()
                .map(PhysicalPredicateAuthority.Node::completionFlag)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        progress.snapshot().booleans().forEach((flag, value) -> {
            if (Boolean.TRUE.equals(value) && !physicalFlags.contains(flag)) {
                trueRemoteFlags.add(flag);
            }
        });
    }

    public void start() {
        if (refreshTask != null) return;
        refreshAsync();
        refreshTask = plugin.scheduler().runAsyncTimerSafe(
                "v5.remote.refresh", 20L * 10L, 20L * 10L, this::refreshBlocking);
        // Bukkit's online-player collection is main-thread-only. Snapshot it on a synchronous timer,
        // then let hydratePlayerAsync perform only the blocking Supabase read off-thread. This makes
        // a Discord link created after join visible without forcing a reconnect.
        refreshOnlineIdentities();
        identityRefreshTask = plugin.scheduler().runTimerSafe(
                "v5.remote.player-links", 20L, 20L * 5L, this::refreshOnlineIdentities);
    }

    public void refreshAsync() {
        plugin.scheduler().runAsyncSafe("v5.remote.prime", this::refreshBlocking);
    }

    /** Network thread only. */
    private void refreshBlocking() {
        if (!refreshInFlight.compareAndSet(false, true)) return;
        try {
            SupabaseClient client = plugin.supabase();
            if (client == null || !client.isConfigured()) return;
            var campaignResult = client.fetchSetting(CAMPAIGN_SETTING);
            var hashResult = client.fetchSetting(AUTHORITY_SETTING);
            if (campaignResult == null || !campaignResult.ok()
                    || hashResult == null || !hashResult.ok()) {
                return; // an outage never revokes the last validated monotonic image
            }
            SettingsRow campaign = campaignResult.value();
            SettingsRow hash = hashResult.value();
            if (!PhysicalPredicateAuthority.CAMPAIGN_VERSION.equals(text(campaign))
                    || !authority.sha256().equals(text(hash))) {
                metadataValidated = false;
                plugin.getLogger().warning("V5 remote flags ignored: campaign/authority metadata "
                        + "does not match the packaged runtime");
                return;
            }
            metadataValidated = true;
            var result = client.fetchArcState();
            ArcStateRow row = result != null && result.ok() ? result.value() : null;
            if (row == null) return;
            Map<String, Object> newlyTrue = new LinkedHashMap<>();
            row.flagsMap().forEach((flag, raw) -> {
                if (truthy(raw) && flag != null && isAllowedRemoteFact(flag)
                        && !physicalFlags.contains(flag)) {
                    trueRemoteFlags.add(flag);
                    newlyTrue.put(flag, Boolean.TRUE);
                }
            });
            if (!newlyTrue.isEmpty()) {
                try {
                    progress.transact(editor -> {
                        newlyTrue.keySet().forEach(editor::setBooleanTrue);
                        return null;
                    });
                    plugin.scheduler().runMainSafe(
                            "v5.remote.project", () -> {
                                V5RuntimeCoordinator runtime = plugin.v5Runtime();
                                if (runtime != null) runtime.projectLocalState();
                            });
                } catch (IOException | RuntimeException failure) {
                    plugin.getLogger().severe("Validated V5 remote facts could not be mirrored locally: "
                            + failure.getMessage());
                }
            }
            mirrorLocalSnapshotAsync();
        } finally {
            refreshInFlight.set(false);
        }
    }

    public void hydratePlayerAsync(UUID playerId) {
        if (playerId == null) return;
        if (!identities.begin(playerId, metadataValidated)) return;
        try {
            plugin.scheduler().runAsyncSafe("v5.remote.player-link", () -> {
                IdentityLinkRefreshState.Observation observation =
                        IdentityLinkRefreshState.Observation.INDETERMINATE;
                try {
                    if (!metadataValidated) return;
                    SupabaseClient client = plugin.supabase();
                    if (client == null || !client.isConfigured()) return;
                    var result = client.fetchPlayerByUuid(playerId.toString());
                    if (result == null || !result.ok() || !metadataValidated) return;
                    PlayerLookupRow row = result.value();
                    // A missing row is not authoritative enough to revoke: joins/upserts may still
                    // be converging. Only an exact successful row with blank discord_id proves an
                    // atomic recovery removed this Minecraft identity's binding.
                    if (row == null || row.mcUuid == null
                            || !playerId.toString().equalsIgnoreCase(row.mcUuid.trim())) return;
                    observation = row.discordId != null && !row.discordId.isBlank()
                            ? IdentityLinkRefreshState.Observation.LINKED
                            : IdentityLinkRefreshState.Observation.UNLINKED;
                } finally {
                    identities.finish(playerId, observation);
                }
            });
        } catch (RuntimeException schedulingFailure) {
            identities.finish(playerId, IdentityLinkRefreshState.Observation.INDETERMINATE);
            throw schedulingFailure;
        }
    }

    /** Main thread only: snapshot online UUIDs, scheduling (not performing) their network reads. */
    private void refreshOnlineIdentities() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            hydratePlayerAsync(player.getUniqueId());
        }
    }

    @Override
    public boolean isTrue(String flag) {
        return flag != null && (progress.snapshot().isComplete(flag)
                || trueRemoteFlags.contains(flag));
    }

    @Override
    public Optional<ContainerRuntimePorts.ValidatedSnapshot> current() {
        if (!metadataValidated) return Optional.empty();
        Map<String, Boolean> flags = new LinkedHashMap<>();
        trueRemoteFlags.forEach(flag -> flags.put(flag, true));
        return Optional.of(new ContainerRuntimePorts.ValidatedSnapshot(
                PhysicalPredicateAuthority.CAMPAIGN_VERSION, authority.sha256(), flags));
    }

    @Override
    public boolean linked(UUID actor) {
        // Outages preserve last-known-good state. The five-second online poll still revalidates
        // linked actors so an authoritative atomic recovery revokes the old hand promptly.
        boolean known = identities.linked(actor);
        if (!known) hydratePlayerAsync(actor); // non-blocking on-demand retry at the interaction edge
        return known;
    }

    @Override
    public boolean matchesHandoff(UUID actor, String sourceFlag) {
        return linked(actor) && isTrue(sourceFlag);
    }

    public Set<UUID> linkedPlayers() {
        return identities.snapshot();
    }

    public boolean metadataValidated() {
        return metadataValidated;
    }

    private static boolean isAllowedRemoteFact(String flag) {
        return flag.startsWith("v5_")
                || flag.matches("p(?:1[0-2]|[1-9])\\.[a-z0-9_]+");
    }

    /** Local completion mirror; enqueue-only and idempotent. */
    public void mirrorAsync(PhysicalPredicateAuthority.Node node, long localRevision) {
        if (node == null) return;
        mirrorLocalSnapshotAsync();
    }

    /** Mirrors the complete monotonic local image; skipped until remote metadata is exact. */
    public void mirrorLocalSnapshotAsync() {
        plugin.scheduler().runAsyncSafe("v5.remote.mirror.local", () -> {
            if (!metadataValidated) return;
            SupabaseClient client = plugin.supabase();
            if (client == null || !client.isConfigured()) return;
            ProgressSnapshot snapshot = progress.snapshot();
            JsonObject flags = new JsonObject();
            snapshot.booleans().forEach((key, value) -> {
                if (Boolean.TRUE.equals(value)) flags.addProperty(key, true);
            });
            snapshot.branches().forEach(flags::addProperty);
            snapshot.conductVerdict().ifPresent(
                    verdict -> flags.addProperty("v5_conduct_verdict", verdict.wireValue()));
            flags.addProperty("v5_minecraft_local_revision", snapshot.revision());
            client.mergeArcFlags(flags);
        });
    }

    @Override
    public void close() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        if (identityRefreshTask != null) {
            identityRefreshTask.cancel();
            identityRefreshTask = null;
        }
    }

    private static SettingsRow value(com.observance.watcher.data.SupabaseResult<SettingsRow> result) {
        return result != null && result.ok() ? result.value() : null;
    }

    private static String text(SettingsRow row) {
        JsonElement value = row == null ? null : row.value;
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsString().trim() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.doubleValue() != 0.0;
        if (value instanceof String text) {
            return Set.of("true", "1", "yes", "on").contains(
                    text.trim().toLowerCase(java.util.Locale.ROOT));
        }
        return false;
    }
}
