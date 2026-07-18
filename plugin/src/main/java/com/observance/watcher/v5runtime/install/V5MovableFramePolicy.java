package com.observance.watcher.v5runtime.install;

/** Safety policy for movable item-frame sets whose items intentionally occupy shuffled targets. */
final class V5MovableFramePolicy {
    private V5MovableFramePolicy() { }

    /**
     * Item identity can locate an ordinary displaced frame, but it cannot identify the target of a
     * movable set: before solve, each item is deliberately mounted on a different target address.
     * Treating an adjacent shuffled piece as displaced steals that piece and makes the set incomplete.
     */
    static boolean mayInferDisplacementFromItemIdentity(boolean reorderable) {
        return !reorderable;
    }
}
