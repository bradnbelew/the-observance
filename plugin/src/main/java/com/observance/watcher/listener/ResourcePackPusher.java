package com.observance.watcher.listener;

import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Locale;

/**
 * THE ONE-CLICK INSTALL (Path A, MF-11 push half). On join, this hands the player the single
 * client-facing asset the Deep Hold needs — the resource pack carrying the keepers' rune alphabet
 * and the dark's voice (see {@code resourcepack/README.md}). The friend group installs nothing else;
 * accepting this one prompt is the whole of their setup friction.
 *
 * <p>This is the SEND half only. The {@link com.observance.watcher.signal.ResourcePackTracker}
 * (already registered) is the RECEIVE half: it records each player's
 * {@link org.bukkit.event.player.PlayerResourcePackStatusEvent} outcome and forwards it to the
 * director's dashboard so a human can see who is pack-ready before a rune-bearing beat is dispatched.
 * Keeping the two halves separate mirrors the existing tracker's single-responsibility shape — this
 * class never inspects status, never gates a beat, never mutates the world.
 *
 * <p><b>Anti-jank.</b> The pack is pushed on a short delayed MAIN-thread task (not inside the join
 * event itself), so a slow client handshake never stalls login and the player has finished spawning
 * before the prompt arrives. The push is wrapped in {@link Safety}; a failure to push degrades to
 * "no pack this session" (the player sees ASCII fallbacks via the tracker's gate, never a crash).
 * It is idempotent per join by construction (one push per {@link PlayerJoinEvent}); Paper itself
 * suppresses a re-prompt when the same URL+hash is already applied.
 *
 * <p><b>Reveal discipline N/A.</b> This touches no world block or entity — it is a per-player client
 * handshake — so the reveal contract does not apply. It honors the precision law trivially: the pack
 * is the same for everyone; nothing here is personalized.
 *
 * <p><b>Config-driven (lore-agnostic).</b> The URL, the 40-char hex SHA-1, the required flag, the
 * one-line prompt, and the push delay are all injected by the owner from {@code config.yml} — this
 * class hardcodes none of them. When the URL is blank (pack not yet hosted, the go-live residue),
 * the pusher is inert: it logs once and pushes nothing, so a pre-go-live server runs clean.
 */
public final class ResourcePackPusher implements Listener {

    /** A 40-char lowercase hex SHA-1 is the only shape Paper accepts for the integrity hash. */
    private static final int SHA1_HEX_LEN = 40;

    private final Scheduler scheduler;
    private final Safety safety;

    private final String url;          // the hosted pack URL; blank → inert (go-live residue)
    private final byte[] sha1;         // 20-byte SHA-1, or null when unparseable/absent
    private final java.util.UUID packId; // stable pack-stack id, derived from URL
    private final boolean required;    // Paper "required" flag (kick-on-decline is a SERVER policy, not ours)
    private final String prompt;       // one-line prompt text shown in the accept dialog (nullable)
    private final long delayTicks;     // delay after join before pushing (let the client finish spawning)

    private volatile boolean warnedBlank = false;

    public ResourcePackPusher(Scheduler scheduler, Safety safety,
                              String url, String sha1Hex, boolean required,
                              String prompt, long delayTicks) {
        this.scheduler = scheduler;
        this.safety = safety;
        this.url = url == null ? "" : url.trim();
        this.sha1 = parseSha1(sha1Hex);
        this.packId = this.url.isBlank()
                ? null
                : java.util.UUID.nameUUIDFromBytes(("observance-resource-pack:" + this.url)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.required = required;
        this.prompt = (prompt == null || prompt.isBlank()) ? null : prompt;
        this.delayTicks = Math.max(0L, delayTicks);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        safety.run("listener.ResourcePackPush", () -> {
            final Player p = event.getPlayer();
            if (p == null) return;

            if (url.isBlank()) {
                // Not hosted yet — the one go-live step. Log ONCE so the console isn't spammed per join.
                if (!warnedBlank) {
                    warnedBlank = true;
                    safety.info("pack.push", "resource-pack URL unset — skipping push (go-live residue). "
                            + "Set resource-pack.url + sha1 in config.yml to enable the one-click install.");
                }
                return;
            }

            // Push on a short delayed MAIN task so login never blocks on the handshake. Re-check the
            // player is still online at fire time (they may have bounced during the delay).
            final java.util.UUID id = p.getUniqueId();
            scheduler.runLaterSafe("pack.push.send", delayTicks, () -> {
                Player still = org.bukkit.Bukkit.getPlayer(id);
                if (still == null || !still.isOnline()) return;
                push(still);
            });
        });
    }

    /** Hand the pack to one player. MAIN thread. Uses Paper's modern pack-stack API when a hash exists,
     *  with a URL-only legacy fallback for intentionally hashless local testing. Never throws. */
    private void push(Player p) {
        try {
            if (sha1 != null && packId != null) {
                p.addResourcePack(packId, url, sha1, prompt, required);
                return;
            }
        } catch (Throwable t) {
            safety.warn("pack.push", "modern resource-pack push failed for " + p.getName()
                    + " (" + t.getClass().getSimpleName() + "); trying URL-only fallback.");
        }
        // Last resort: URL only. The integrity hash is strongly preferred (without it the client
        // re-downloads every join and can't verify the bytes), so warn if we land here.
        safety.warn("pack.push", "pushing pack to " + p.getName() + " WITHOUT a sha1 hash "
                + "(set resource-pack.sha1 to a 40-char hex digest for cache + integrity).");
        // safety.run reports any throwable via its swallow path — a failed push degrades to no pack.
        safety.run("pack.push.urlOnly", () -> p.addResourcePack(packId, url, null, prompt, required));
    }

    /* ------------------------------------------------------------------ */
    /*  SHA-1 parsing (40-char hex → 20 bytes)                             */
    /* ------------------------------------------------------------------ */

    /** Parse a 40-char hex SHA-1 into 20 bytes, or null when absent/malformed (push then goes hashless). */
    static byte[] parseSha1(String hex) {
        if (hex == null) return null;
        String h = hex.trim().toLowerCase(Locale.ROOT);
        if (h.length() != SHA1_HEX_LEN) return null;
        byte[] out = new byte[SHA1_HEX_LEN / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(h.charAt(i * 2), 16);
            int lo = Character.digit(h.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) return null;   // not hex → treat as absent, never throw
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (mirrors the repo's selftest idiom).    */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the SHA-1 parse: a known digest must round-trip to 20 bytes, and malformed inputs must
     * yield null (hashless push) rather than throw. Run from a test harness / on boot in debug; a
     * regression here would silently push an un-verifiable pack (every-join re-download).
     */
    static boolean sha1ParseSelfTest() {
        // 40 hex chars → 20 bytes, first byte 0xDA, last byte 0x39.
        byte[] ok = parseSha1("DA39A3EE5E6B4B0D3255BFEF95601890AFD80709");
        if (ok == null || ok.length != 20) return false;
        if ((ok[0] & 0xFF) != 0xDA || (ok[19] & 0xFF) != 0x09) return false;
        // Wrong length, non-hex, null → all null, none throw.
        if (parseSha1("DEAD") != null) return false;
        if (parseSha1("zz39a3ee5e6b4b0d3255bfef95601890afd80709") != null) return false;
        if (parseSha1(null) != null) return false;
        // Case-insensitive: lower and upper agree byte-for-byte.
        byte[] lo = parseSha1("da39a3ee5e6b4b0d3255bfef95601890afd80709");
        return lo != null && java.util.Arrays.equals(ok, lo);
    }
}
