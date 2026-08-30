package com.observance.watcher.morrow;

import java.net.URI;
import java.util.Locale;

/** Fail-closed policy for Morrow's optional resource-pack handshake. */
public final class MorrowResourcePackPolicy {
    public record Plan(
            boolean enabled,
            boolean required,
            String url,
            String sha1,
            String prompt,
            long delayTicks) { }

    private MorrowResourcePackPolicy() { }

    public static Plan create(
            boolean enabled,
            boolean configuredRequired,
            String url,
            String sha1,
            String prompt,
            long delayTicks) {
        if (!enabled) return new Plan(false, false, "", "", "", 0L);
        if (configuredRequired) {
            throw new IllegalArgumentException(
                    "Morrow resource pack must remain optional so the vanilla fallback is reachable");
        }
        String normalizedUrl = url == null ? "" : url.trim();
        String normalizedSha1 = sha1 == null ? "" : sha1.trim().toLowerCase(Locale.ROOT);
        String normalizedPrompt = prompt == null ? "" : prompt.trim();
        if (!normalizedSha1.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("Morrow resource pack requires an exact SHA-1");
        }
        URI uri;
        try {
            uri = URI.create(normalizedUrl);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("Morrow resource pack URL is invalid", invalid);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean loopbackHttp = "http".equals(scheme)
                && ("localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host));
        if (!("https".equals(scheme) || loopbackHttp) || host.isBlank()
                || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Morrow resource pack URL must be credential-free HTTPS or loopback HTTP");
        }
        if (normalizedPrompt.contains("\n") || normalizedPrompt.contains("\r")
                || normalizedPrompt.length() > 256) {
            throw new IllegalArgumentException("Morrow resource pack prompt must be one bounded line");
        }
        return new Plan(true, false, normalizedUrl, normalizedSha1, normalizedPrompt,
                Math.max(0L, Math.min(1_200L, delayTicks)));
    }
}
