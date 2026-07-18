package com.observance.watcher.v5runtime.ritual;

import com.observance.watcher.v5runtime.LeaseBook;
import com.observance.watcher.v5runtime.SiteMutexes;
import com.observance.watcher.v5runtime.V5ProgressStore;
import com.observance.watcher.v5runtime.ritual.RitualChoices.WrenOutcome;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** RP04 voluntary any-subset confirmation with branch-specific Bridge handling. */
public final class CollectivePresenceRite {
    public enum Status {
        STARTED,
        UPDATED,
        CONFIRMED,
        COMPLETED,
        BUSY,
        INVALID,
        NOT_ELIGIBLE,
        PREREQUISITE_MISSING,
        ALREADY_COMPLETE,
        ABORTED,
        NO_ACTIVE_WINDOW
    }

    public interface AuditSink {
        void accessibilitySectorReplacement(
                String operator, int unreachableSector, int replacementSector, String reason);
    }

    public record Result(Status status, Optional<View> view) {
        public Result {
            Objects.requireNonNull(status, "status");
            view = Objects.requireNonNull(view, "view");
        }
    }

    public record View(
            List<UUID> visibleRoster,
            Map<UUID, Integer> occupiedSectors,
            Set<UUID> confirmedPlayers,
            boolean bridgeOperationComplete,
            long closesAtTick,
            int disconnectResnaps) {
        public View {
            visibleRoster = List.copyOf(visibleRoster);
            occupiedSectors = Map.copyOf(occupiedSectors);
            confirmedPlayers = Set.copyOf(confirmedPlayers);
        }
    }

    private record Presence(int sector, boolean lit) {
    }

    private static final long DISCONNECT_GRACE_TICKS = 20L * 20L;
    private static final Duration LEASE_DURATION = Duration.ofMinutes(5);
    private static final Duration MUTEX_TIMEOUT = Duration.ofSeconds(2);

    private static final class Window {
        private final LinkedHashSet<UUID> roster;
        private final ProtocolBridge bridge;
        private final WrenOutcome outcome;
        private final LeaseBook.Token lease;
        private final long closesAtTick;
        private final Map<UUID, Presence> presence = new LinkedHashMap<>();
        private final Map<UUID, Integer> confirmations = new LinkedHashMap<>();
        private final Map<UUID, Long> disconnectedAt = new LinkedHashMap<>();
        private final Map<Integer, Integer> sectorReplacements = new LinkedHashMap<>();
        private boolean bridgeOperationComplete;
        private int resnaps;

        private Window(Set<UUID> roster, ProtocolBridge bridge, WrenOutcome outcome,
                       LeaseBook.Token lease, long closesAtTick) {
            this.roster = sorted(roster);
            this.bridge = bridge;
            this.outcome = outcome;
            this.lease = lease;
            this.closesAtTick = closesAtTick;
        }
    }

    private final RitualAuthorityContract authority;
    private final V5ProgressStore progress;
    private final LeaseBook leases;
    private final SiteMutexes mutexes;
    private final RitualClock clock;
    private final AuditSink audit;
    private Window window;

    public CollectivePresenceRite(
            RitualAuthorityContract authority,
            V5ProgressStore progress,
            LeaseBook leases,
            SiteMutexes mutexes,
            RitualClock clock,
            AuditSink audit) {
        this.authority = Objects.requireNonNull(authority, "authority");
        this.progress = Objects.requireNonNull(progress, "progress");
        this.leases = Objects.requireNonNull(leases, "leases");
        this.mutexes = Objects.requireNonNull(mutexes, "mutexes");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.audit = Objects.requireNonNull(audit, "audit");
        authority.node("RP04");
    }

    public synchronized Result start(Set<UUID> visibleRoster, ProtocolBridge bridge) {
        if (isComplete()) {
            return result(Status.ALREADY_COMPLETE);
        }
        if (!progress.snapshot().isComplete("v5_rp03_name_choice")) {
            return result(Status.PREREQUISITE_MISSING);
        }
        if (window != null) {
            return result(Status.BUSY);
        }
        if (visibleRoster == null || visibleRoster.isEmpty()
                || visibleRoster.stream().anyMatch(Objects::isNull)
                || bridge == null || bridge.outcome().isEmpty()) {
            return result(Status.INVALID);
        }
        WrenOutcome committed;
        try {
            committed = WrenOutcome.fromWireValue(
                    progress.snapshot().branches().get("v5_wren_outcome"));
        } catch (RuntimeException failure) {
            return result(Status.INVALID);
        }
        if (bridge.outcome().orElseThrow() != committed) {
            return result(Status.INVALID);
        }
        LeaseBook.Token lease = leases.tryAcquire(
                "ritual-presence:RP04", authority.node("RP04").siteId(), LEASE_DURATION)
                .orElse(null);
        if (lease == null) {
            return result(Status.BUSY);
        }
        window = new Window(visibleRoster, bridge, committed, lease, Long.MAX_VALUE);
        return result(Status.STARTED);
    }

    public synchronized Result updatePresence(UUID playerId, int sector, boolean lit)
            throws IOException {
        boolean knownSector = window != null && (sector >= 0 && sector < window.roster.size()
                || window.sectorReplacements.containsValue(sector));
        if (!eligible(playerId) || !knownSector) {
            return result(window == null ? Status.NO_ACTIVE_WINDOW : Status.NOT_ELIGIBLE);
        }
        int effectiveSector = window.sectorReplacements.getOrDefault(sector, sector);
        boolean occupiedByAnother = window.presence.entrySet().stream()
                .anyMatch(entry -> !entry.getKey().equals(playerId)
                        && entry.getValue().sector() == effectiveSector);
        if (occupiedByAnother) {
            return result(Status.INVALID);
        }
        Presence previous = window.presence.put(playerId, new Presence(effectiveSector, lit));
        if (previous == null || previous.sector() != effectiveSector || !lit) {
            window.confirmations.remove(playerId);
        }
        return maybeComplete(Status.UPDATED);
    }

    public synchronized Result leaveSector(UUID playerId) {
        if (window == null) {
            return result(Status.NO_ACTIVE_WINDOW);
        }
        window.presence.remove(playerId);
        window.confirmations.remove(playerId);
        return result(Status.UPDATED);
    }

    public synchronized Result confirmOwnSector(UUID playerId, int handleSector)
            throws IOException {
        if (!eligible(playerId)) {
            return result(window == null ? Status.NO_ACTIVE_WINDOW : Status.NOT_ELIGIBLE);
        }
        Presence current = window.presence.get(playerId);
        if (current == null || !current.lit() || current.sector() != handleSector) {
            return result(Status.INVALID);
        }
        window.confirmations.put(playerId, handleSector);
        return maybeComplete(Status.CONFIRMED);
    }

    public synchronized Result confirmCondemnBlackHousing(UUID actor) throws IOException {
        if (!eligible(actor) || window.outcome != WrenOutcome.CONDEMN) {
            return result(Status.INVALID);
        }
        window.bridgeOperationComplete = true;
        return maybeComplete(Status.UPDATED);
    }

    public synchronized Result confirmUnderstandPass(
            UUID firstCarrier, UUID secondCarrier, boolean amberHousingReached,
            long soloSelfHoldTicks) throws IOException {
        if (window == null || window.outcome != WrenOutcome.UNDERSTAND
                || !eligible(firstCarrier) || !eligible(secondCarrier) || !amberHousingReached) {
            return result(Status.INVALID);
        }
        boolean valid = window.roster.size() == 1
                ? firstCarrier.equals(secondCarrier) && soloSelfHoldTicks >= 5L * 20L
                : !firstCarrier.equals(secondCarrier);
        if (!valid) {
            return result(Status.INVALID);
        }
        window.bridgeOperationComplete = true;
        return maybeComplete(Status.UPDATED);
    }

    public synchronized Result confirmFreeCenterToWhiteTrough(
            UUID carrier, boolean pickedUpAtCenter, boolean reachedWhiteTrough) throws IOException {
        if (window == null || window.outcome != WrenOutcome.FREE || !eligible(carrier)
                || !pickedUpAtCenter || !reachedWhiteTrough) {
            return result(Status.INVALID);
        }
        window.bridgeOperationComplete = true;
        return maybeComplete(Status.UPDATED);
    }

    /** Accessibility replacement preserves roster size and still requires that player's handle. */
    public synchronized Result replaceUnreachableSector(
            String operator, int unreachable, int replacement, String reason) {
        if (window == null || operator == null || operator.isBlank() || reason == null
                || reason.isBlank() || unreachable < 0 || unreachable >= window.roster.size()
                || replacement < window.roster.size()
                || window.sectorReplacements.containsValue(replacement)) {
            return result(Status.INVALID);
        }
        window.sectorReplacements.put(unreachable, replacement);
        audit.accessibilitySectorReplacement(operator, unreachable, replacement, reason);
        return result(Status.UPDATED);
    }

    public synchronized void disconnected(UUID playerId) {
        if (window != null && window.roster.contains(playerId)) {
            window.disconnectedAt.putIfAbsent(playerId, clock.tick());
        }
    }

    public synchronized void reconnected(UUID playerId) {
        if (window != null) {
            window.disconnectedAt.remove(playerId);
        }
    }

    public synchronized Result tick() throws IOException {
        if (window == null) {
            return result(Status.NO_ACTIVE_WINDOW);
        }
        long now = clock.tick();
        List<UUID> expired = window.disconnectedAt.entrySet().stream()
                .filter(entry -> now - entry.getValue() >= DISCONNECT_GRACE_TICKS)
                .map(Map.Entry::getKey).sorted().toList();
        if (!expired.isEmpty()) {
            expired.forEach(player -> {
                window.roster.remove(player);
                window.presence.remove(player);
                window.confirmations.remove(player);
                window.disconnectedAt.remove(player);
            });
            window.resnaps++;
            if (window.roster.isEmpty()) {
                return abort();
            }
        }
        return maybeComplete(Status.UPDATED);
    }

    public synchronized Result abort() {
        if (window == null) {
            return result(Status.NO_ACTIVE_WINDOW);
        }
        window.lease.close();
        window = null;
        return result(Status.ABORTED);
    }

    public boolean isComplete() {
        return progress.snapshot().isComplete(authority.completionFlag("RP04"));
    }

    private Result maybeComplete(Status otherwise) throws IOException {
        if (window == null || !window.bridgeOperationComplete
                || window.confirmations.size() != window.roster.size()
                || window.presence.size() != window.roster.size()) {
            return result(otherwise);
        }
        Set<Integer> sectors = new LinkedHashSet<>();
        for (UUID player : window.roster) {
            Presence presence = window.presence.get(player);
            Integer confirmation = window.confirmations.get(player);
            if (presence == null || !presence.lit() || confirmation == null
                    || confirmation != presence.sector() || !sectors.add(presence.sector())) {
                return result(otherwise);
            }
        }
        try (SiteMutexes.Guard ignored = acquireMutex()) {
            String participantWire = window.roster.stream().map(UUID::toString)
                    .sorted().reduce((left, right) -> left + "," + right).orElseThrow();
            String bridgeId = window.bridge.instanceId().toString();
            progress.transact(editor -> {
                editor.putBranchOnce("v5_rp04_participants", participantWire);
                editor.putBranchOnce("v5_rp04_bridge_instance", bridgeId);
                editor.compareAndSetCompletion(authority.completionFlag("RP04"), false, true);
                return null;
            });
        }
        View completed = viewOf(window);
        window.lease.close();
        window = null;
        return new Result(Status.COMPLETED, Optional.of(completed));
    }

    private boolean eligible(UUID playerId) {
        return window != null && playerId != null && window.roster.contains(playerId)
                && !window.disconnectedAt.containsKey(playerId);
    }

    private SiteMutexes.Guard acquireMutex() {
        try {
            return mutexes.tryAcquire(authority.node("RP04").siteId(), MUTEX_TIMEOUT)
                    .orElseThrow(() -> new IllegalStateException("RP04 site is busy"));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted acquiring RP04 mutex", interrupted);
        }
    }

    private Result result(Status status) {
        return new Result(status, Optional.ofNullable(window).map(CollectivePresenceRite::viewOf));
    }

    private static View viewOf(Window source) {
        Map<UUID, Integer> sectors = new LinkedHashMap<>();
        source.presence.forEach((player, presence) -> sectors.put(player, presence.sector()));
        return new View(new ArrayList<>(source.roster), sectors,
                source.confirmations.keySet(), source.bridgeOperationComplete,
                source.closesAtTick, source.resnaps);
    }

    private static LinkedHashSet<UUID> sorted(Set<UUID> source) {
        LinkedHashSet<UUID> result = new LinkedHashSet<>();
        source.stream().sorted(Comparator.naturalOrder()).forEach(result::add);
        return result;
    }
}
