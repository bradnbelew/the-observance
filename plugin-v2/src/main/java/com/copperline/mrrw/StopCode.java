package com.copperline.mrrw;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * The Act 10 stop code and the stop-secret fingerprint, shared with the website (same secret =
 * MORROW_STOP_SECRET). Pure logic, no Bukkit types: checked by {@code SelfTest}.
 */
final class StopCode {
    private StopCode() { }

    /** choice-XXXX-XXXX: the first 8 uppercase hex of HMAC-SHA256(secret, "stop|choice|bucket"). */
    static String of(String secret, char choice, long bucket) {
        String hex = HexFormat.of().withUpperCase().formatHex(hmac(secret, "stop|" + choice + "|" + bucket));
        return choice + "-" + hex.substring(0, 4) + "-" + hex.substring(4, 8);
    }

    /** First 8 hex (lowercase) of HMAC-SHA256(key = secret, msg = "mrrw-fingerprint"). */
    static String fingerprint(String secret) {
        return HexFormat.of().formatHex(hmac(secret, "mrrw-fingerprint")).substring(0, 8);
    }

    private static byte[] hmac(String secret, String msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
