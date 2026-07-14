package com.observance.watcher.finale;

import java.util.HashSet;
import java.util.Set;

/** Dependency-free executable guard for all six choice combinations and durable phase rules. */
public final class FinaleStateMachineSelfTest {
    public static void main(String[] args) {
        Set<String> goodbyes = new HashSet<>();
        Set<String> codas = new HashSet<>();
        for (FinaleStateMachine.WrenOutcome wren : FinaleStateMachine.WrenOutcome.values()) {
            for (FinaleStateMachine.NameTreatment name : FinaleStateMachine.NameTreatment.values()) {
                FinaleStateMachine machine = new FinaleStateMachine();
                FinaleStateMachine.Snapshot armed = machine.arm(wren, name,
                        FinaleStateMachine.ConductVerdict.UNANIMOUS,
                        "self-test", 1_000L, 15_000L);
                expect(armed.phase() == FinaleStateMachine.Phase.ARMED, "arm phase");
                expect(armed.cancelCutoffAtEpochMs() == 16_000L, "arm cutoff");
                FinaleStateMachine.Snapshot committed = machine.commit(4_000L);
                expect(committed.phase() == FinaleStateMachine.Phase.COMMITTED, "commit phase");
                expect(committed.wrenOutcome() == wren && committed.nameTreatment() == name,
                        "commit preserves both dimensions");
                expect(machine.enterCoda().phase() == FinaleStateMachine.Phase.CODA, "coda phase");
                try {
                    machine.arm(wren, name, FinaleStateMachine.ConductVerdict.SOLO,
                            "again", 8_000L, 15_000L);
                    throw new AssertionError("CODA allowed a second ending");
                } catch (IllegalStateException expected) { }

                String goodbye = FinaleStateMachine.goodbye(wren, name,
                        FinaleStateMachine.ConductVerdict.UNANIMOUS);
                String coda = FinaleStateMachine.coda(wren, name,
                        FinaleStateMachine.ConductVerdict.UNANIMOUS);
                expect(goodbye.contains("the record is closed."), "universal close is present");
                expect(goodbye.endsWith("— averyn"), "Averyn signs every close");
                expect(goodbyes.add(goodbye), "six-way goodbye must be unique");
                expect(codas.add(coda), "six-way coda must be unique");
            }
        }
        expect(goodbyes.size() == 6 && codas.size() == 6, "exactly six ending combinations");

        FinaleStateMachine expired = new FinaleStateMachine();
        expired.arm(FinaleStateMachine.WrenOutcome.FREE,
                FinaleStateMachine.NameTreatment.PUBLISH,
                FinaleStateMachine.ConductVerdict.PERSISTENT, "self-test", 1_000L, 15_000L);
        try {
            expired.commit(16_000L);
            throw new AssertionError("expired arm committed");
        } catch (IllegalStateException expected) { }

        FinaleStateMachine beforeRestart = new FinaleStateMachine();
        beforeRestart.arm(FinaleStateMachine.WrenOutcome.CONDEMN,
                FinaleStateMachine.NameTreatment.RELEASE_UNNAMED,
                FinaleStateMachine.ConductVerdict.SOLO, "restart-test", 2_000L, 20_000L);
        FinaleStateMachine.Snapshot durableCommit = beforeRestart.commit(4_000L);
        FinaleStateMachine afterRestart = new FinaleStateMachine(durableCommit);
        expect(afterRestart.snapshot().phase() == FinaleStateMachine.Phase.COMMITTED,
                "restart preserves COMMITTED without replay choice mutation");
        FinaleStateMachine.Snapshot restartCoda = afterRestart.enterCoda();
        expect(restartCoda.wrenOutcome() == FinaleStateMachine.WrenOutcome.CONDEMN
                        && restartCoda.nameTreatment() == FinaleStateMachine.NameTreatment.RELEASE_UNNAMED
                        && restartCoda.conductVerdict() == FinaleStateMachine.ConductVerdict.SOLO,
                "restart/CODA preserves all exact dimensions");

        FinaleStateMachine cancellable = new FinaleStateMachine();
        cancellable.arm(FinaleStateMachine.WrenOutcome.FREE,
                FinaleStateMachine.NameTreatment.RELEASE_UNNAMED,
                FinaleStateMachine.ConductVerdict.DIVIDED, "self-test", 1L, 15_000L);
        expect(cancellable.cancel().phase() == FinaleStateMachine.Phase.IDLE, "cancel before commit");

        expect(FinaleStateMachine.parseWrenOutcome("condemn")
                == FinaleStateMachine.WrenOutcome.CONDEMN, "condemn parse");
        expect(FinaleStateMachine.parseNameTreatment("release unnamed")
                == FinaleStateMachine.NameTreatment.RELEASE_UNNAMED, "unnamed parse");
        expect(FinaleStateMachine.parseConductVerdict("persistent")
                == FinaleStateMachine.ConductVerdict.PERSISTENT, "conduct parse");
        expect(FinaleStateMachine.parseConductVerdict("kept") == null,
                "retired V4 conduct must not parse");

        for (FinaleStateMachine.ConductVerdict conduct : FinaleStateMachine.ConductVerdict.values()) {
            expect(!FinaleStateMachine.conductClause(conduct).isBlank(), conduct + " clause exists");
        }
        expect(FinaleStateMachine.conductClause(FinaleStateMachine.ConductVerdict.SOLO)
                        .equals("you carried every name alone. they still arrived together."),
                "solo clause parity");
        expect(FinaleStateMachine.conductClause(FinaleStateMachine.ConductVerdict.UNANIMOUS)
                        .equals("you came to one answer without becoming one voice."),
                "unanimous clause parity");
        expect(FinaleStateMachine.conductClause(FinaleStateMachine.ConductVerdict.DIVIDED)
                        .equals("you disagreed and continued together. the record never knew how to write that."),
                "divided clause parity");
        expect(FinaleStateMachine.conductClause(FinaleStateMachine.ConductVerdict.PERSISTENT)
                        .equals("you did not agree quickly. you stayed until the evidence did."),
                "persistent clause parity");

        FinaleStateMachine faulted = new FinaleStateMachine(new FinaleStateMachine.Snapshot(
                FinaleStateMachine.Phase.FAULT, null, null, null,
                "corrupt-state", 0L, 0L, 0L));
        try {
            faulted.arm(FinaleStateMachine.WrenOutcome.FREE,
                    FinaleStateMachine.NameTreatment.PUBLISH,
                    FinaleStateMachine.ConductVerdict.SOLO, "unsafe", 1L, 15_000L);
            throw new AssertionError("FAULT allowed a finale arm");
        } catch (IllegalStateException expected) { }

        System.out.println("FinaleStateMachineSelfTest: OK - schema-2, six endings, four conduct clauses, expiry/CODA.");
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
