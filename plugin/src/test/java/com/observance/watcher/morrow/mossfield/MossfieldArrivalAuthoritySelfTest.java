package com.observance.watcher.morrow.mossfield;

import com.observance.watcher.morrow.mossfield.MossfieldArrivalAuthority.Result;
import com.observance.watcher.morrow.mossfield.MossfieldArrivalAuthority.State;
import com.observance.watcher.morrow.mossfield.MossfieldArrivalAuthority.Status;

public final class MossfieldArrivalAuthoritySelfTest {
    public static void main(String[] args) {
        State state = State.initial(false);
        state = require(MossfieldArrivalAuthority.observe(state, "survey"), Status.CONTINUE, 1);
        state = require(MossfieldArrivalAuthority.observe(state, "light"), Status.RESET, 0);
        state = require(MossfieldArrivalAuthority.observe(state, "survey"), Status.CONTINUE, 1);
        state = require(MossfieldArrivalAuthority.observe(state, "mend"), Status.CONTINUE, 2);
        state = require(MossfieldArrivalAuthority.observe(state, "mark"), Status.CONTINUE, 3);
        state = require(MossfieldArrivalAuthority.observe(state, "light"), Status.COMPLETE, 4);
        require(MossfieldArrivalAuthority.observe(state, "survey"), Status.ALREADY_COMPLETE, 4);
        if (!state.completed()) throw new AssertionError("completed route was not retained");
        if (!"morrow.gate.g06_book_coordinates".equals(MossfieldArrivalAuthority.MAINTENANCE_CACHE)) {
            throw new AssertionError("G06 cache event key drifted");
        }
        System.out.println("MORROW MOSSFIELD ARRIVAL: PASS route=survey/mend/mark/light");
    }

    private static State require(Result result, Status status, int next) {
        if (result.status() != status || result.state().nextIndex() != next) {
            throw new AssertionError("expected " + status + "/" + next + " but got " + result);
        }
        return result.state();
    }
}
