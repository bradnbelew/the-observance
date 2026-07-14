package com.observance.watcher.v5runtime;

import java.util.HashSet;
import java.util.Set;

/** Cross-surface token grammar, entropy-shape, and SHA-256 regression test. */
public final class IdentityLinkCodeSelfTest {
    private IdentityLinkCodeSelfTest() { }

    public static void main(String[] args) {
        check(IdentityLinkCode.canIssueForAuthenticatedUuid(true),
                "online-mode server may issue an authenticated identity proof");
        check(!IdentityLinkCode.canIssueForAuthenticatedUuid(false),
                "offline-mode server must fail closed before proof issuance");
        String canonical = "ABCDEFGHJKMP";
        check(canonical.equals(IdentityLinkCode.normalize("abcd-efgh-jkmp")),
                "case and display hyphens normalize");
        check(canonical.equals(IdentityLinkCode.normalize("ＡＢＣＤ ＥＦＧＨ ＪＫＭＰ")),
                "Unicode width and spaces normalize");
        check(IdentityLinkCode.normalize("ABCD-EFGH-IJKL").isEmpty(),
                "ambiguous I/L symbols are rejected");
        check(IdentityLinkCode.normalize("ABCD-EFGH-JKM").isEmpty(), "short code rejected");
        check(IdentityLinkCode.normalize("ABCD-EFGH-JKMPX").isEmpty(), "long code rejected");
        check("58b71f1d45d2b6d407ec0262d2b9f5b85f56cf4605f596bf977c85bc2e01397c"
                        .equals(IdentityLinkCode.sha256(canonical)),
                "SHA-256 vector must match the Discord implementation");

        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 256; i++) {
            String display = IdentityLinkCode.generateDisplayCode();
            String normalized = IdentityLinkCode.normalize(display);
            check(display.length() == 14 && display.charAt(4) == '-' && display.charAt(9) == '-',
                    "generated code uses 4-4-4 display groups");
            check(normalized.length() == IdentityLinkCode.SYMBOL_COUNT, "generated code has 60 bits");
            check(IdentityLinkCode.sha256(display).matches("[0-9a-f]{64}"),
                    "generated code hashes to lowercase SHA-256");
            generated.add(normalized);
        }
        check(generated.size() == 256, "sampled generated codes must not collide");
        System.out.println("Identity link code selftest OK: online-mode gate + 60-bit token + strict grammar + shared SHA-256");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
