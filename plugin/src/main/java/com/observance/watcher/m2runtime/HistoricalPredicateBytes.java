package com.observance.watcher.m2runtime;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

/** Exact reconstruction of the historical 37020... mixed-EOL predicate receipt. */
public final class HistoricalPredicateBytes {
    public static final String HISTORICAL_RAW_SHA256 =
            "37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b";
    public static final String CANONICAL_LF_SHA256 =
            "16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a";
    public static final String SEMANTIC_SHA256 =
            "d2eec35f58cf79a30f2255f429cb0d19a5c1e8b5bd7942604b3bef724272cbf6";

    private static final Set<Integer> LF_ONLY_LINES = Set.of(
            52, 55, 60, 66, 67, 68, 71, 72, 906, 914, 932, 940, 958, 966, 982, 990,
            1005, 1013, 1030, 1038, 1105, 1106, 1107, 1108, 1109, 1110, 1111, 1112,
            1113, 1114, 1115, 1116, 1117, 1118, 1119, 1120, 1121, 1122, 1123, 1124,
            1125, 1126, 1127, 1133, 1136, 1137, 1138, 1139, 1140, 1141, 1142, 1143,
            1144, 1145, 1146, 1147, 1148, 1323);

    private HistoricalPredicateBytes() {
    }

    public static byte[] reconstruct(byte[] canonicalLfBytes) {
        if (!PredicateAuthorityVersion.rawSha256(canonicalLfBytes).equals(CANONICAL_LF_SHA256)) {
            throw new IllegalArgumentException("Canonical LF predicate bytes do not match 16de receipt");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream(canonicalLfBytes.length + 1490);
        int line = 1;
        for (byte value : canonicalLfBytes) {
            if (value == '\r') {
                throw new IllegalArgumentException("Canonical predicate contains CR bytes");
            }
            if (value == '\n') {
                if (!LF_ONLY_LINES.contains(line)) output.write('\r');
                output.write('\n');
                line++;
            } else {
                output.write(value);
            }
        }
        byte[] result = output.toByteArray();
        if (result.length != 138349
                || !PredicateAuthorityVersion.rawSha256(result).equals(HISTORICAL_RAW_SHA256)) {
            throw new IllegalStateException("Historical predicate reconstruction did not match 37020 receipt");
        }
        return result;
    }

    public static Set<Integer> lfOnlyLines() {
        return Set.copyOf(new HashSet<>(LF_ONLY_LINES));
    }
}
