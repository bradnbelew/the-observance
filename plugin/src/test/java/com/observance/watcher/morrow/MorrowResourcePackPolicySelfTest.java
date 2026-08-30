package com.observance.watcher.morrow;

/** Dependency-free policy proof for optional pack/fallback reachability. */
public final class MorrowResourcePackPolicySelfTest {
    private MorrowResourcePackPolicySelfTest() { }

    public static void main(String[] args) {
        MorrowResourcePackPolicy.Plan disabled = MorrowResourcePackPolicy.create(
                false, true, "", "", "", 20L);
        check(!disabled.enabled() && !disabled.required(), "disabled policy must be inert");

        MorrowResourcePackPolicy.Plan hosted = MorrowResourcePackPolicy.create(
                true, false, "https://assets.example.test/morrow.zip",
                "bae2623687b8d9e68eb4910807ed87d03671ca3c", "Optional authored models and audio", 20L);
        check(hosted.enabled() && !hosted.required() && hosted.delayTicks() == 20L,
                "hosted pack must remain optional");

        MorrowResourcePackPolicy.Plan loopback = MorrowResourcePackPolicy.create(
                true, false, "http://127.0.0.1:8765/morrow.zip",
                "bae2623687b8d9e68eb4910807ed87d03671ca3c", "Local rehearsal", 2_000L);
        check(!loopback.required() && loopback.delayTicks() == 1_200L,
                "loopback rehearsal must clamp delay and remain optional");

        expectFailure(() -> MorrowResourcePackPolicy.create(
                true, true, hosted.url(), hosted.sha1(), hosted.prompt(), 20L));
        expectFailure(() -> MorrowResourcePackPolicy.create(
                true, false, "http://example.test/morrow.zip", hosted.sha1(), hosted.prompt(), 20L));
        expectFailure(() -> MorrowResourcePackPolicy.create(
                true, false, hosted.url(), "deadbeef", hosted.prompt(), 20L));
        expectFailure(() -> MorrowResourcePackPolicy.create(
                true, false, "https://user:secret@example.test/morrow.zip",
                hosted.sha1(), hosted.prompt(), 20L));
        expectFailure(() -> MorrowResourcePackPolicy.create(
                true, false, hosted.url(), hosted.sha1(), "two\nlines", 20L));
        System.out.println("MORROW RESOURCE PACK POLICY: PASS optional=1 required=refused loopback=bounded");
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected resource-pack policy refusal");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
