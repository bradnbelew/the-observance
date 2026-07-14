package com.observance.watcher.v5runtime.ritual;

import com.observance.watcher.v5runtime.LeaseBook;
import com.observance.watcher.v5runtime.PlayerBitDomain;
import com.observance.watcher.v5runtime.PlayerProgress;
import com.observance.watcher.v5runtime.SiteMutexes;
import com.observance.watcher.v5runtime.V5ProgressStore;
import com.observance.watcher.v5runtime.ritual.RitualChoices.ClosingChoice;
import com.observance.watcher.v5runtime.ritual.RitualChoices.ClosingReply;
import com.observance.watcher.v5runtime.ritual.RitualChoices.WrenTopic;
import java.io.IOException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Durable per-player WR03 conversation; beginning a reply can never complete it. */
public final class WrenDialogueRite {
    public enum TurnKind {
        TOPIC,
        CLOSING
    }

    public enum BeginStatus {
        STARTED,
        BUSY,
        ALREADY_HEARD,
        MISSING_TOPICS,
        PREREQUISITE_MISSING,
        ALREADY_COMPLETE
    }

    public enum CompleteStatus {
        COMPLETED_REPLY,
        COMPLETED_WR03,
        TOO_EARLY,
        STALE_TURN,
        PREREQUISITE_MISSING,
        ALREADY_COMPLETE
    }

    public record DialogueTurn(
            UUID id,
            UUID playerId,
            TurnKind kind,
            WrenTopic topic,
            List<String> canonicalLines,
            long notBeforeTick) {
        public DialogueTurn {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(kind, "kind");
            canonicalLines = List.copyOf(canonicalLines);
            if (canonicalLines.isEmpty()) {
                throw new IllegalArgumentException("dialogue turn requires canonical lines");
            }
            if (kind == TurnKind.TOPIC && topic == null) {
                throw new IllegalArgumentException("topic turn requires a topic");
            }
            if (kind == TurnKind.CLOSING && topic != null) {
                throw new IllegalArgumentException("closing turn cannot carry a topic");
            }
        }
    }

    public record BeginResult(BeginStatus status, Optional<DialogueTurn> turn,
                              Set<WrenTopic> stillMissing) {
        public BeginResult {
            Objects.requireNonNull(status, "status");
            turn = Objects.requireNonNull(turn, "turn");
            stillMissing = Set.copyOf(stillMissing);
        }
    }

    private record ActiveTurn(DialogueTurn turn, LeaseBook.Token lease) {
    }

    private static final String PREREQUISITE = "v5_wr02_index";
    private static final String TOPIC_PREFIX = "WR03:TOPIC:";
    private static final String CHOICE_BIT = "WR03:CHOICE:" + ClosingChoice.YOU_CHOSE_TO_SEND_THEM;
    private static final String REPLY_BIT = "WR03:REPLY:"
            + ClosingReply.ADMISSION_AND_FEAR_WITHOUT_COERCION_EXCUSE;
    private static final Duration SESSION_LEASE = Duration.ofMinutes(3);
    private static final Duration MUTEX_TIMEOUT = Duration.ofSeconds(2);
    private static final long TICKS_PER_LINE = 20L;

    private final RitualAuthorityContract authority;
    private final CanonicalRitualText text;
    private final V5ProgressStore progress;
    private final LeaseBook leases;
    private final SiteMutexes mutexes;
    private final RitualClock clock;
    private final Map<UUID, ActiveTurn> activeTurns = new ConcurrentHashMap<>();

    public WrenDialogueRite(
            RitualAuthorityContract authority,
            CanonicalRitualText text,
            V5ProgressStore progress,
            LeaseBook leases,
            SiteMutexes mutexes,
            RitualClock clock) {
        this.authority = Objects.requireNonNull(authority, "authority");
        this.text = Objects.requireNonNull(text, "text");
        this.progress = Objects.requireNonNull(progress, "progress");
        this.leases = Objects.requireNonNull(leases, "leases");
        this.mutexes = Objects.requireNonNull(mutexes, "mutexes");
        this.clock = Objects.requireNonNull(clock, "clock");
        authority.node("WR03");
    }

    public BeginResult beginTopic(UUID playerId, WrenTopic topic) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(topic, "topic");
        if (isComplete()) {
            return beginResult(BeginStatus.ALREADY_COMPLETE, playerId, Optional.empty());
        }
        if (!progress.snapshot().isComplete(PREREQUISITE)) {
            return beginResult(BeginStatus.PREREQUISITE_MISSING, playerId, Optional.empty());
        }
        if (heardTopics(playerId).contains(topic)) {
            return beginResult(BeginStatus.ALREADY_HEARD, playerId, Optional.empty());
        }
        Optional<LeaseBook.Token> acquired = leases.tryAcquire(
                leaseScope(playerId), playerId.toString(), SESSION_LEASE);
        if (acquired.isEmpty()) {
            return beginResult(BeginStatus.BUSY, playerId, Optional.empty());
        }
        List<String> lines = text.wrenTopicReply(topic);
        DialogueTurn turn = new DialogueTurn(
                UUID.randomUUID(), playerId, TurnKind.TOPIC, topic, lines,
                clock.tick() + Math.multiplyExact(lines.size(), TICKS_PER_LINE));
        ActiveTurn active = new ActiveTurn(turn, acquired.orElseThrow());
        ActiveTurn previous = activeTurns.putIfAbsent(playerId, active);
        if (previous != null) {
            active.lease().close();
            return beginResult(BeginStatus.BUSY, playerId, Optional.empty());
        }
        return beginResult(BeginStatus.STARTED, playerId, Optional.of(turn));
    }

    public BeginResult beginClosing(UUID playerId, ClosingChoice choice) throws IOException {
        Objects.requireNonNull(playerId, "playerId");
        if (choice != ClosingChoice.YOU_CHOSE_TO_SEND_THEM) {
            throw new IllegalArgumentException("WR03 accepts only the canonical closing choice");
        }
        if (isComplete()) {
            return beginResult(BeginStatus.ALREADY_COMPLETE, playerId, Optional.empty());
        }
        if (!progress.snapshot().isComplete(PREREQUISITE)) {
            return beginResult(BeginStatus.PREREQUISITE_MISSING, playerId, Optional.empty());
        }
        if (!missingTopics(playerId).isEmpty()) {
            return beginResult(BeginStatus.MISSING_TOPICS, playerId, Optional.empty());
        }
        Optional<LeaseBook.Token> acquired = leases.tryAcquire(
                leaseScope(playerId), playerId.toString(), SESSION_LEASE);
        if (acquired.isEmpty()) {
            return beginResult(BeginStatus.BUSY, playerId, Optional.empty());
        }
        try {
            progress.transact(editor -> {
                editor.addPlayerBit(playerId, PlayerBitDomain.TOPIC, CHOICE_BIT);
                return null;
            });
        } catch (IOException | RuntimeException failure) {
            acquired.orElseThrow().close();
            throw failure;
        }
        List<String> lines = text.wrenClosingReply();
        DialogueTurn turn = new DialogueTurn(
                UUID.randomUUID(), playerId, TurnKind.CLOSING, null, lines,
                clock.tick() + Math.multiplyExact(lines.size(), TICKS_PER_LINE));
        ActiveTurn active = new ActiveTurn(turn, acquired.orElseThrow());
        ActiveTurn previous = activeTurns.putIfAbsent(playerId, active);
        if (previous != null) {
            active.lease().close();
            return beginResult(BeginStatus.BUSY, playerId, Optional.empty());
        }
        return beginResult(BeginStatus.STARTED, playerId, Optional.of(turn));
    }

    public CompleteStatus completeReply(UUID playerId, UUID turnId) throws IOException {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(turnId, "turnId");
        if (isComplete()) {
            closeTurn(playerId);
            return CompleteStatus.ALREADY_COMPLETE;
        }
        if (!progress.snapshot().isComplete(PREREQUISITE)) {
            closeTurn(playerId);
            return CompleteStatus.PREREQUISITE_MISSING;
        }
        ActiveTurn active = activeTurns.get(playerId);
        if (active == null || !active.turn().id().equals(turnId)) {
            return CompleteStatus.STALE_TURN;
        }
        if (clock.tick() < active.turn().notBeforeTick()) {
            return CompleteStatus.TOO_EARLY;
        }
        try (SiteMutexes.Guard ignored = acquireMutex()) {
            if (active.turn().kind() == TurnKind.TOPIC) {
                progress.transact(editor -> {
                    editor.addPlayerBit(playerId, PlayerBitDomain.TOPIC,
                            TOPIC_PREFIX + active.turn().topic().name());
                    return null;
                });
                return CompleteStatus.COMPLETED_REPLY;
            }
            final boolean[] won = {false};
            progress.transact(editor -> {
                editor.addPlayerBit(playerId, PlayerBitDomain.TOPIC, REPLY_BIT);
                won[0] = editor.compareAndSetCompletion(
                        authority.completionFlag("WR03"), false, true);
                return null;
            });
            return won[0] ? CompleteStatus.COMPLETED_WR03 : CompleteStatus.ALREADY_COMPLETE;
        } finally {
            closeTurn(playerId);
        }
    }

    public void disconnect(UUID playerId) {
        closeTurn(Objects.requireNonNull(playerId, "playerId"));
    }

    public Set<WrenTopic> heardTopics(UUID playerId) {
        PlayerProgress player = progress.snapshot().players().get(playerId.toString());
        if (player == null) {
            return Set.of();
        }
        EnumSet<WrenTopic> result = EnumSet.noneOf(WrenTopic.class);
        for (WrenTopic topic : WrenTopic.values()) {
            if (player.topics().contains(TOPIC_PREFIX + topic.name())) {
                result.add(topic);
            }
        }
        return Set.copyOf(result);
    }

    public Set<WrenTopic> missingTopics(UUID playerId) {
        EnumSet<WrenTopic> missing = EnumSet.allOf(WrenTopic.class);
        missing.removeAll(heardTopics(playerId));
        return Set.copyOf(missing);
    }

    public boolean isComplete() {
        return progress.snapshot().isComplete(authority.completionFlag("WR03"));
    }

    private BeginResult beginResult(
            BeginStatus status, UUID playerId, Optional<DialogueTurn> turn) {
        return new BeginResult(status, turn, missingTopics(playerId));
    }

    private SiteMutexes.Guard acquireMutex() {
        try {
            return mutexes.tryAcquire(authority.node("WR03").siteId(), MUTEX_TIMEOUT)
                    .orElseThrow(() -> new IllegalStateException("WR03 site is busy"));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while acquiring WR03 site mutex", interrupted);
        }
    }

    private void closeTurn(UUID playerId) {
        ActiveTurn removed = activeTurns.remove(playerId);
        if (removed != null) {
            removed.lease().close();
        }
    }

    private static String leaseScope(UUID playerId) {
        return "wr03-dialogue:" + playerId;
    }
}
