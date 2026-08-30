package com.observance.watcher.morrow;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Immutable, Bukkit-free configuration validated before the Morrow journal is opened. */
public record MorrowRuntimeSettings(
        String releaseId,
        UUID campaignId,
        String worldName,
        URI ingestUri,
        String ingestSecret,
        Duration connectTimeout,
        Duration requestTimeout,
        Duration initialRetry,
        Duration maximumRetry) {
    private static final Pattern RELEASE = Pattern.compile("[a-z0-9][a-z0-9._-]{6,79}");
    private static final Pattern UUID_TEXT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");
    private static final Pattern WORLD = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    public MorrowRuntimeSettings {
        releaseId = required(releaseId, "release-id");
        worldName = required(worldName, "world");
        ingestSecret = required(ingestSecret, "ingest secret");
        Objects.requireNonNull(campaignId, "campaignId");
        Objects.requireNonNull(ingestUri, "ingestUri");
        connectTimeout = bounded(connectTimeout, "connect timeout", 250, 30_000);
        requestTimeout = bounded(requestTimeout, "request timeout", 500, 60_000);
        initialRetry = bounded(initialRetry, "initial retry", 10, 60_000);
        maximumRetry = bounded(maximumRetry, "maximum retry", 10, 300_000);

        if (!RELEASE.matcher(releaseId).matches()) {
            throw new IllegalArgumentException("morrow-reboot.release-id has invalid grammar");
        }
        if (!WORLD.matcher(worldName).matches() || ".".equals(worldName) || "..".equals(worldName)) {
            throw new IllegalArgumentException("morrow-reboot.world has invalid grammar");
        }
        if (!"https".equalsIgnoreCase(ingestUri.getScheme()) || ingestUri.getHost() == null
                || ingestUri.getUserInfo() != null || ingestUri.getFragment() != null) {
            throw new IllegalArgumentException("morrow-reboot.ingest-url must be an absolute HTTPS URL");
        }
        int secretBytes = ingestSecret.getBytes(StandardCharsets.UTF_8).length;
        if (secretBytes < 32 || secretBytes > 1024) {
            throw new IllegalArgumentException("Morrow ingest secret must contain 32-1024 UTF-8 bytes");
        }
        if (maximumRetry.compareTo(initialRetry) < 0) {
            throw new IllegalArgumentException("Morrow maximum retry must not be shorter than initial retry");
        }
    }

    public static MorrowRuntimeSettings create(
            String releaseId,
            String campaignId,
            String worldName,
            String ingestUrl,
            String ingestSecret,
            long connectTimeoutMillis,
            long requestTimeoutMillis,
            long initialRetryMillis,
            long maximumRetryMillis) {
        String campaignText = required(campaignId, "campaign-id");
        if (!UUID_TEXT.matcher(campaignText).matches()) {
            throw new IllegalArgumentException("morrow-reboot.campaign-id must be a canonical UUID");
        }
        URI uri;
        try {
            uri = URI.create(required(ingestUrl, "ingest-url"));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("morrow-reboot.ingest-url must be a valid URI", failure);
        }
        return new MorrowRuntimeSettings(
                releaseId,
                UUID.fromString(campaignText),
                worldName,
                uri,
                ingestSecret,
                Duration.ofMillis(connectTimeoutMillis),
                Duration.ofMillis(requestTimeoutMillis),
                Duration.ofMillis(initialRetryMillis),
                Duration.ofMillis(maximumRetryMillis));
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Morrow " + name + " is required");
        return value.trim();
    }

    private static Duration bounded(Duration value, String name, long minimumMillis, long maximumMillis) {
        Objects.requireNonNull(value, name);
        long millis;
        try {
            millis = value.toMillis();
        } catch (ArithmeticException failure) {
            throw new IllegalArgumentException("Morrow " + name + " is out of range", failure);
        }
        if (millis < minimumMillis || millis > maximumMillis) {
            throw new IllegalArgumentException("Morrow " + name + " is out of range");
        }
        return value;
    }
}
