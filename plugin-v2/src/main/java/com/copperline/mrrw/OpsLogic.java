package com.copperline.mrrw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * The operator's tools as pure logic (no Bukkit types): checked by {@code SelfTest}.
 *  - snapshots of the plugin's state files (plugins/mrrw/snapshots/NAME/), and rollback to one;
 *  - the known one-shot flag names, so /mrrw flag can warn about a typo;
 *  - the USB quorum from the residents actually seen lately;
 *  - the bridge's {"kind":"op","cmd":...} text split into command arguments;
 *  - the pause-when-empty check on server.properties.
 */
final class OpsLogic {
    private OpsLogic() { }

    // ---- snapshots ------------------------------------------------------------------------------

    /**
     * What a snapshot holds: everything the campaign's progress lives in. Not the journal or the
     * bot's feed (append-only history: a rollback is one more line in it, never a rewrite), not
     * config.yml (the operator's settings stay as they are).
     */
    static final List<String> STATE_FILES = List.of("state.yml", "bridge-state.json", "roster.yml", "recovery.yml",
            "paths.yml", "fixtures.yml", "usb-votes.json", "stopped.flag", "stop-pending.json");

    static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9._-]{1,64}") && !name.startsWith(".");
    }

    /** Copies the state files into snapshots/NAME (replacing a snapshot of that name). Returns the files copied. */
    static List<String> snapshot(File dataFolder, String name) throws IOException {
        if (!validName(name)) throw new IllegalArgumentException("snapshot names are letters, digits, . _ - (max 64)");
        File dir = new File(new File(dataFolder, "snapshots"), name);
        Files.createDirectories(dir.toPath());
        List<String> copied = new ArrayList<>();
        for (String f : STATE_FILES) {
            File src = new File(dataFolder, f), dst = new File(dir, f);
            if (src.isFile()) {
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                copied.add(f);
            } else {
                Files.deleteIfExists(dst.toPath());
            }
        }
        return copied;
    }

    /**
     * Puts snapshots/NAME back: every state file it holds is copied over, and a state file it does
     * not hold is removed (a snapshot from before the stop has no stopped.flag). Returns the files restored.
     */
    static List<String> rollback(File dataFolder, String name) throws IOException {
        if (!validName(name)) throw new IllegalArgumentException("bad snapshot name");
        File dir = new File(new File(dataFolder, "snapshots"), name);
        if (!new File(dir, "state.yml").isFile()) throw new IllegalArgumentException("no snapshot " + name);
        List<String> restored = new ArrayList<>();
        for (String f : STATE_FILES) {
            File src = new File(dir, f), dst = new File(dataFolder, f);
            if (src.isFile()) {
                File tmp = new File(dataFolder, f + ".rollback");
                Files.copy(src.toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.move(tmp.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                restored.add(f);
            } else {
                Files.deleteIfExists(dst.toPath());
            }
        }
        return restored;
    }

    /** Snapshot names, oldest first. */
    static List<String> snapshots(File dataFolder) {
        File[] dirs = new File(dataFolder, "snapshots").listFiles(f -> f.isDirectory() && new File(f, "state.yml").isFile());
        if (dirs == null) return List.of();
        Arrays.sort(dirs, (a, b) -> Long.compare(new File(a, "state.yml").lastModified(), new File(b, "state.yml").lastModified()));
        List<String> out = new ArrayList<>();
        for (File d : dirs) out.add(d.getName());
        return out;
    }

    // ---- flags ----------------------------------------------------------------------------------

    /** Every one-shot flag the plugin writes (exact names). */
    static final List<String> FLAGS = List.of(
            "act2.sign_backs", "act3.rules_edit", "act3.tally_book", "act3.golems", "act3.creeper", "act3.tower_lamp",
            "act5.maint_on", "act5.maint_reverted", "act5.floor_tidy",
            "act6.sign_rewrites", "act6.book", "act6.rune_signs", "act6.pots", "act6.sheep",
            "act7.flags_tidied", "act8.flags_tidied", "act8.listening_room_open", "act8.kit_line.1", "act8.kit_line.2", "act9.meadow_redraw", "act9.pots", "act10.lamp_dark",
            "iona_chest.opened", "iona_chest.tidied", "lowpath.bed_kept", "lowpath.session_ended",
            "stop_pending", "stopped", "theo.timed.ask");

    /** Flag families with a variable tail (an act number, a voice, a player id, an install part). */
    static final List<String> FLAG_PREFIXES = List.of("install.", "rail.act", "act6.rewrites.night.", "act10.returned.", "theo.timed.", "tidy.tally.");

    static boolean knownFlag(String flag) {
        if (flag == null) return false;
        if (FLAGS.contains(flag)) return true;
        for (String p : FLAG_PREFIXES) if (flag.startsWith(p) && flag.length() > p.length()) return true;
        return false;
    }

    // ---- residents and the quorum ---------------------------------------------------------------

    /** Residents who have not been on the server for this long no longer count toward the USB quorum. */
    static final long QUORUM_WINDOW_MS = 14L * 24 * 3_600_000;

    /**
     * The residents the USB vote counts: the operator's number when set (> 0), else the residents
     * seen in the last 14 days (a name with no recorded visit, from before visits were kept, counts),
     * never fewer than 1 so a group of one can always vote.
     */
    static int quorum(Set<String> residents, Map<String, Long> lastSeen, long now, int override) {
        if (override > 0) return override;
        int n = 0;
        for (String r : residents) {
            Long seen = lastSeen.get(r);
            if (seen == null || now - seen <= QUORUM_WINDOW_MS) n++;
        }
        return Math.max(1, n);
    }

    // ---- the bridge's op command ----------------------------------------------------------------

    /** "act 5  2026-10-17T20:00:00-04:00" -> [act, 5, 2026-...]; a leading "/mrrw" or "mrrw" is dropped. */
    static String[] opArgs(String cmd) {
        if (cmd == null) return new String[0];
        String t = cmd.trim();
        if (t.startsWith("/")) t = t.substring(1);
        String[] parts = t.isEmpty() ? new String[0] : t.split("\\s+");
        if (parts.length > 0 && parts[0].equalsIgnoreCase("mrrw")) parts = Arrays.copyOfRange(parts, 1, parts.length);
        return parts;
    }

    // ---- pause-when-empty -----------------------------------------------------------------------

    /**
     * Null when server.properties keeps the world ticking with nobody online; else why not. Paper's
     * default (60) stops dawns, Theo's timed notes and the bridge between sessions.
     */
    static String pauseWarning(Properties serverProperties) {
        if (serverProperties == null) return null;  // no file found: nothing to say
        String v = serverProperties.getProperty("pause-when-empty-seconds");
        if (v == null) return "server.properties has no pause-when-empty-seconds (Paper pauses after 60 s): set pause-when-empty-seconds=-1";
        try {
            if (Integer.parseInt(v.trim()) < 0) return null;
        } catch (NumberFormatException e) {
            // fall through
        }
        return "server.properties pause-when-empty-seconds=" + v.trim()
                + ": the world stops with nobody online (no dawns, no Theo notes, the bridge times out). Set it to -1 and restart";
    }

    static String lower(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
