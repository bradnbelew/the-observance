package com.observance.watcher.morrow;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free validation receipt for the fail-closed reboot configuration boundary. */
public final class MorrowRuntimeSettingsSelfTest {
    private static final String RELEASE = "morrow.rehearsal.001";
    private static final String CAMPAIGN = "8e0b1a62-d2dd-4e86-91f1-4b07af5e2922";
    private static final String SECRET = "selftest-secret-with-at-least-32-bytes";

    private MorrowRuntimeSettingsSelfTest() { }

    public static void main(String[] args) throws Exception {
        MorrowRuntimeSettings valid = create(
                RELEASE, CAMPAIGN, "morrow_rehearsal", "https://example.test/api/morrow", SECRET);
        check(RELEASE.equals(valid.releaseId()), "release retained");
        check("https".equals(valid.ingestUri().getScheme()), "HTTPS retained");

        expectIllegal(() -> create("", CAMPAIGN, "morrow_rehearsal", "https://example.test/api", SECRET));
        expectIllegal(() -> create("bad", CAMPAIGN, "morrow_rehearsal", "https://example.test/api", SECRET));
        expectIllegal(() -> create(RELEASE, "not-a-uuid", "morrow_rehearsal", "https://example.test/api", SECRET));
        expectIllegal(() -> create(RELEASE, CAMPAIGN, "../world", "https://example.test/api", SECRET));
        expectIllegal(() -> create(RELEASE, CAMPAIGN, "morrow_rehearsal", "http://example.test/api", SECRET));
        expectIllegal(() -> create(RELEASE, CAMPAIGN, "morrow_rehearsal", "https://user@example.test/api", SECRET));
        expectIllegal(() -> create(RELEASE, CAMPAIGN, "morrow_rehearsal", "https://example.test/api", "short"));
        expectIllegal(() -> MorrowRuntimeSettings.create(
                RELEASE, CAMPAIGN, "morrow_rehearsal", "https://example.test/api", SECRET,
                249, 10_000, 500, 30_000));
        expectIllegal(() -> MorrowRuntimeSettings.create(
                RELEASE, CAMPAIGN, "morrow_rehearsal", "https://example.test/api", SECRET,
                5_000, 10_000, 1_000, 500));

        String shipped = Files.readString(Path.of("src/main/resources/config.yml"), StandardCharsets.UTF_8);
        int section = shipped.indexOf("morrow-reboot:");
        check(section >= 0, "shipped Morrow section exists");
        String reboot = shipped.substring(section);
        check(reboot.matches("(?s).*morrow-reboot:\\s*\\R\\s+enabled: false(?:\\R|$).*"),
                "shipped Morrow gate remains disabled");
        check(reboot.contains("ingest-secret-env: \"MORROW_MINECRAFT_INGEST_SECRET\""),
                "dedicated secret environment binding remains present");
        System.out.println("MORROW RUNTIME SETTINGS: PASS");
    }

    private static MorrowRuntimeSettings create(
            String release, String campaign, String world, String url, String secret) {
        return MorrowRuntimeSettings.create(
                release, campaign, world, url, secret,
                5_000, 10_000, 500, 30_000);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectIllegal(Throwing action) throws Exception {
        try {
            action.run();
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @FunctionalInterface
    private interface Throwing { void run() throws Exception; }
}
