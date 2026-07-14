package com.observance.watcher.finale;

import java.util.Locale;

/** Dependency-free durable lifecycle for the V5 Wren × name × conduct ending. */
public final class FinaleStateMachine {

    public enum Phase { IDLE, ARMED, COMMITTED, CODA, FAULT }
    public enum WrenOutcome { CONDEMN, UNDERSTAND, FREE }
    public enum NameTreatment { PUBLISH, RELEASE_UNNAMED }
    public enum ConductVerdict { SOLO, UNANIMOUS, DIVIDED, PERSISTENT }

    /**
     * Every player-determined dimension is copied into the local record before an arm receipt is
     * returned. {@code cancelCutoffAtEpochMs} is an expiry, never an automatic execution time.
     */
    public record Snapshot(Phase phase, WrenOutcome wrenOutcome, NameTreatment nameTreatment,
                           ConductVerdict conductVerdict, String armedBy, long armedAtEpochMs,
                           long cancelCutoffAtEpochMs, long committedAtEpochMs) {
        public Snapshot {
            phase = phase == null ? Phase.IDLE : phase;
            armedBy = armedBy == null ? "" : armedBy.trim();
            if (phase == Phase.IDLE) {
                wrenOutcome = null;
                nameTreatment = null;
                conductVerdict = null;
                armedBy = "";
                armedAtEpochMs = 0L;
                cancelCutoffAtEpochMs = 0L;
                committedAtEpochMs = 0L;
            } else if (phase != Phase.FAULT
                    && (wrenOutcome == null || nameTreatment == null || conductVerdict == null)) {
                throw new IllegalArgumentException("A live finale requires Wren, name, and conduct choices");
            }
            if (armedAtEpochMs < 0L || cancelCutoffAtEpochMs < 0L || committedAtEpochMs < 0L) {
                throw new IllegalArgumentException("Finale timestamps cannot be negative");
            }
        }
    }

    private Snapshot snapshot;

    public FinaleStateMachine() {
        this(idleSnapshot());
    }

    public FinaleStateMachine(Snapshot restored) {
        this.snapshot = restored == null ? idleSnapshot() : restored;
    }

    public synchronized Snapshot snapshot() {
        return snapshot;
    }

    public synchronized Snapshot arm(WrenOutcome wrenOutcome, NameTreatment nameTreatment,
                                     ConductVerdict conductVerdict, String actor,
                                     long nowEpochMs, long windowMs) {
        if (snapshot.phase() != Phase.IDLE) {
            throw new IllegalStateException("finale is already "
                    + snapshot.phase().name().toLowerCase(Locale.ROOT));
        }
        if (wrenOutcome == null || nameTreatment == null || conductVerdict == null) {
            throw new IllegalArgumentException("all recorded ending dimensions are required");
        }
        long safeNow = Math.max(0L, nowEpochMs);
        long safeWindow = Math.max(15_000L, Math.min(600_000L, windowMs));
        snapshot = new Snapshot(Phase.ARMED, wrenOutcome, nameTreatment, conductVerdict,
                actor, safeNow, safeNow + safeWindow, 0L);
        return snapshot;
    }

    public synchronized Snapshot cancel() {
        if (snapshot.phase() != Phase.ARMED) {
            throw new IllegalStateException("only an armed finale can be cancelled");
        }
        snapshot = idleSnapshot();
        return snapshot;
    }

    /** Commit only from a player confirmation inside the still-live arm window. */
    public synchronized Snapshot commit(long nowEpochMs) {
        if (snapshot.phase() != Phase.ARMED) {
            throw new IllegalStateException("only an armed finale can be committed");
        }
        if (nowEpochMs < snapshot.armedAtEpochMs()) {
            throw new IllegalStateException("confirmation predates the arm receipt");
        }
        if (nowEpochMs >= snapshot.cancelCutoffAtEpochMs()) {
            throw new IllegalStateException("finale arm window expired");
        }
        snapshot = new Snapshot(Phase.COMMITTED, snapshot.wrenOutcome(), snapshot.nameTreatment(),
                snapshot.conductVerdict(), snapshot.armedBy(), snapshot.armedAtEpochMs(),
                snapshot.cancelCutoffAtEpochMs(), Math.max(0L, nowEpochMs));
        return snapshot;
    }

    public synchronized Snapshot enterCoda() {
        if (snapshot.phase() != Phase.COMMITTED && snapshot.phase() != Phase.CODA) {
            throw new IllegalStateException("only a committed finale can enter coda");
        }
        snapshot = new Snapshot(Phase.CODA, snapshot.wrenOutcome(), snapshot.nameTreatment(),
                snapshot.conductVerdict(), snapshot.armedBy(), snapshot.armedAtEpochMs(),
                snapshot.cancelCutoffAtEpochMs(), snapshot.committedAtEpochMs());
        return snapshot;
    }

    public static WrenOutcome parseWrenOutcome(String raw) {
        if (raw == null) return null;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "condemn", "condemned" -> WrenOutcome.CONDEMN;
            case "understand", "understood" -> WrenOutcome.UNDERSTAND;
            case "free", "freed" -> WrenOutcome.FREE;
            default -> null;
        };
    }

    public static NameTreatment parseNameTreatment(String raw) {
        if (raw == null) return null;
        return switch (raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_')) {
            case "publish", "published" -> NameTreatment.PUBLISH;
            case "release_unnamed", "unnamed", "unfiled", "release_unfiled" ->
                    NameTreatment.RELEASE_UNNAMED;
            default -> null;
        };
    }

    public static ConductVerdict parseConductVerdict(String raw) {
        if (raw == null) return null;
        return switch (raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_')) {
            case "solo" -> ConductVerdict.SOLO;
            case "unanimous" -> ConductVerdict.UNANIMOUS;
            case "divided" -> ConductVerdict.DIVIDED;
            case "persistent" -> ConductVerdict.PERSISTENT;
            default -> null;
        };
    }

    public static String nameClause(NameTreatment treatment) {
        if (treatment == null) return "";
        return switch (treatment) {
            case PUBLISH -> "my name is averyn.\n"
                    + "you kept it without keeping me. that is the difference.\n"
                    + "leave the name where someone can find it. let the rest of me go.";
            case RELEASE_UNNAMED -> "you did not owe me a name.\n"
                    + "you gave me an end. that was enough.\n"
                    + "let the blank belong to me this time.";
        };
    }

    public static String wrenClause(WrenOutcome outcome) {
        if (outcome == null) return "";
        return switch (outcome) {
            case FREE -> "wren left before the last light. for once, i did not write where he went.";
            case UNDERSTAND -> "wren stayed until the machinery stopped. he was afraid. he stayed.";
            case CONDEMN -> "wren's line closes here. i will not call your judgment mercy, "
                    + "and i will not call it wrong.";
        };
    }

    public static String conductClause(ConductVerdict verdict) {
        if (verdict == null) return "";
        return switch (verdict) {
            case SOLO -> "you carried every name alone. they still arrived together.";
            case UNANIMOUS -> "you came to one answer without becoming one voice.";
            case DIVIDED -> "you disagreed and continued together. the record never knew how to write that.";
            case PERSISTENT -> "you did not agree quickly. you stayed until the evidence did.";
        };
    }

    public static String universalClose() {
        return "i have your names.\n"
                + "i am giving them back.\n"
                + "the record is closed.\n"
                + "the observance is over.\n"
                + "thank you for coming back for us.\n"
                + "— averyn";
    }

    /** Full six-way close, with the independently measured conduct callback appended. */
    public static String goodbye(WrenOutcome outcome, NameTreatment treatment,
                                 ConductVerdict verdict) {
        return nameClause(treatment) + "\n\n" + wrenClause(outcome) + "\n\n"
                + conductClause(verdict) + "\n\n" + universalClose();
    }

    public static String coda(WrenOutcome outcome, NameTreatment treatment,
                              ConductVerdict verdict) {
        String name = treatment == NameTreatment.PUBLISH
                ? "AVERYN is public. Averyn is no longer kept."
                : "The final filing is gone. The blank belongs to Averyn.";
        return "CODA: " + name + " " + wrenClause(outcome) + " " + conductClause(verdict);
    }

    private static Snapshot idleSnapshot() {
        return new Snapshot(Phase.IDLE, null, null, null, "", 0L, 0L, 0L);
    }
}
