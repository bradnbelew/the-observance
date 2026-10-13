package com.copperline.trailhead;

import java.util.ArrayList;
import java.util.List;

/**
 * The key, split so nobody can read it alone. Sector i of n shows every letter whose index
 * (counting only non-space characters, across all lines) is i mod n; the rest are '_'.
 * Spaces always show, so word shapes line up when the group compares notes.
 * Pure logic, no Bukkit types: tested by {@code SelfTest}.
 */
public final class KeyFragments {
    public static final char HIDDEN = '_';

    private KeyFragments() { }

    public static List<String> sector(List<String> lines, int n, int i) {
        List<String> out = new ArrayList<>();
        int k = 0;
        for (String line : lines) {
            StringBuilder sb = new StringBuilder();
            for (char c : line.toCharArray()) {
                if (c == ' ') {
                    sb.append(c);
                    continue;
                }
                sb.append(Math.floorMod(k, n) == i ? c : HIDDEN);
                k++;
            }
            out.add(sb.toString());
        }
        return out;
    }

    /** What the group gets by laying every sector on top of each other. */
    public static List<String> overlay(List<List<String>> sectors) {
        List<String> out = new ArrayList<>();
        for (int l = 0; l < sectors.get(0).size(); l++) {
            char[] merged = sectors.get(0).get(l).toCharArray();
            for (List<String> s : sectors) {
                char[] c = s.get(l).toCharArray();
                for (int j = 0; j < merged.length; j++) if (merged[j] == HIDDEN) merged[j] = c[j];
            }
            out.add(new String(merged));
        }
        return out;
    }
}
