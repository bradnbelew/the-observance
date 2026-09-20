package com.observance.watcher.morrow.mossfield;

import java.util.List;
import java.util.Objects;

/** Pure authority for Cairn's ordinary storehouse routine. */
public final class MossfieldArrivalAuthority {
    public static final String FIRST_CONNECTION = "morrow.gate.g04_first_connection";
    public static final String STOREHOUSE_TRAIL = "morrow.gate.g05_storehouse_trail";
    public static final String MAINTENANCE_CACHE = "morrow.gate.g06_book_coordinates";
    public static final List<String> ROUTINE = List.of("survey", "mend", "mark", "light");

    private MossfieldArrivalAuthority() { }

    public static Result observe(State state, String action) {
        Objects.requireNonNull(state, "state");
        if (!ROUTINE.contains(action)) throw new IllegalArgumentException("unknown storehouse action");
        if (state.completed()) return new Result(Status.ALREADY_COMPLETE, state);
        int expected = state.nextIndex();
        if (ROUTINE.get(expected).equals(action)) {
            int next = expected + 1;
            State changed = new State(next, next == ROUTINE.size());
            return new Result(changed.completed() ? Status.COMPLETE : Status.CONTINUE, changed);
        }
        int restart = ROUTINE.get(0).equals(action) ? 1 : 0;
        return new Result(Status.RESET, new State(restart, false));
    }

    public record State(int nextIndex, boolean completed) {
        public State {
            if (nextIndex < 0 || nextIndex > ROUTINE.size()) {
                throw new IllegalArgumentException("storehouse index is outside the routine");
            }
            if (completed != (nextIndex == ROUTINE.size())) {
                throw new IllegalArgumentException("completion must match the final routine index");
            }
        }

        public static State initial(boolean completed) {
            return new State(completed ? ROUTINE.size() : 0, completed);
        }
    }

    public enum Status { CONTINUE, RESET, COMPLETE, ALREADY_COMPLETE }

    public record Result(Status status, State state) {
        public Result {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(state, "state");
        }
    }
}
