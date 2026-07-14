package com.observance.watcher.v5runtime;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Implements the answer-sign normalization contract in the V5 predicate authority. */
public final class AnswerNormalizer {
    private static final Pattern LEGACY_COLOR = Pattern.compile("(?i)\u00a7[0-9A-FK-ORX]");

    private AnswerNormalizer() {
    }

    public static String normalize(String input) {
        Objects.requireNonNull(input, "input");
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC);
        normalized = LEGACY_COLOR.matcher(normalized).replaceAll("");
        normalized = stripControls(normalized).toUpperCase(Locale.ROOT);

        StringBuilder output = new StringBuilder(normalized.length());
        boolean pendingSpace = false;
        for (int index = 0; index < normalized.length();) {
            int codePoint = normalized.codePointAt(index);
            index += Character.charCount(codePoint);
            if (isPunctuation(codePoint) || Character.isWhitespace(codePoint)
                    || Character.isSpaceChar(codePoint)) {
                pendingSpace = output.length() > 0;
                continue;
            }
            if (pendingSpace) {
                output.append(' ');
                pendingSpace = false;
            }
            output.appendCodePoint(codePoint);
        }
        return output.toString();
    }

    private static String stripControls(String value) {
        StringBuilder output = new StringBuilder(value.length());
        for (int index = 0; index < value.length();) {
            int codePoint = value.codePointAt(index);
            index += Character.charCount(codePoint);
            int type = Character.getType(codePoint);
            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                output.append(' ');
            } else if (type != Character.CONTROL && type != Character.FORMAT) {
                output.appendCodePoint(codePoint);
            }
        }
        return output.toString();
    }

    private static boolean isPunctuation(int codePoint) {
        return switch (Character.getType(codePoint)) {
            case Character.CONNECTOR_PUNCTUATION,
                    Character.DASH_PUNCTUATION,
                    Character.START_PUNCTUATION,
                    Character.END_PUNCTUATION,
                    Character.INITIAL_QUOTE_PUNCTUATION,
                    Character.FINAL_QUOTE_PUNCTUATION,
                    Character.OTHER_PUNCTUATION -> true;
            default -> false;
        };
    }
}
