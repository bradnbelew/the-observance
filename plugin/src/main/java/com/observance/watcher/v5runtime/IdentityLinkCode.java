package com.observance.watcher.v5runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

/** Generates and hashes the one-time code proving control of an online Minecraft identity. */
public final class IdentityLinkCode {
    public static final int SYMBOL_COUNT = 12;
    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdentityLinkCode() { }

    /** Identity proofs are meaningful only when Paper authenticated the UUID with Mojang. */
    public static boolean canIssueForAuthenticatedUuid(boolean serverOnlineMode) {
        return serverOnlineMode;
    }

    /** Twelve Crockford-base32 symbols (60 bits), grouped for transcription. */
    public static String generateDisplayCode() {
        StringBuilder compact = new StringBuilder(SYMBOL_COUNT);
        for (int i = 0; i < SYMBOL_COUNT; i++) {
            compact.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return display(compact.toString());
    }

    /** Accept only harmless case/width/space/hyphen changes; ambiguous I/L/O/U never normalize. */
    public static String normalize(String raw) {
        if (raw == null) return "";
        String text = Normalizer.normalize(raw, Normalizer.Form.NFKC).toUpperCase(Locale.ROOT);
        StringBuilder compact = new StringBuilder(SYMBOL_COUNT);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '-' || Character.isWhitespace(c)) continue;
            if (ALPHABET.indexOf(c) < 0) return "";
            compact.append(c);
            if (compact.length() > SYMBOL_COUNT) return "";
        }
        return compact.length() == SYMBOL_COUNT ? compact.toString() : "";
    }

    /** Lowercase SHA-256 of the canonical 60-bit token. Plaintext is never sent to storage. */
    public static String sha256(String raw) {
        String normalized = normalize(raw);
        if (normalized.isEmpty()) return "";
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public static String display(String raw) {
        String normalized = normalize(raw);
        if (normalized.isEmpty()) return "";
        return normalized.substring(0, 4) + "-" + normalized.substring(4, 8)
                + "-" + normalized.substring(8, 12);
    }
}
