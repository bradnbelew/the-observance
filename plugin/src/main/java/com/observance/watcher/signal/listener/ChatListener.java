package com.observance.watcher.signal.listener;

import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.ObservationRow;
import com.observance.watcher.signal.PlayerSignals;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.signal.TrackerConfig;
import com.observance.watcher.util.Safety;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Tracks chat signals (DESIGN §2.1, §2.4):
 * <ul>
 *   <li>deterministic sentiment (a tiny lexicon scan — Phase 0 has NO AI), folded into a per-player
 *       running mean;</li>
 *   <li>The Unspoken custom: a forbidden word in chat is a tracked VIOLATION + a hit counter.</li>
 *   <li>OBSERVER TIER-1 capture: when (and ONLY when) the operator has enabled it, the verbatim
 *       message is stored as an {@code observations} row so it can later be quoted back GROUNDED.</li>
 * </ul>
 *
 * <p><b>Threading.</b> {@link AsyncChatEvent} fires OFF the main thread. We therefore touch NO
 * Bukkit world objects here — only the player's UUID/name (immutable identity) and the message
 * text via Adventure's plain-text serializer. The tracker's in-memory accumulators are thread-safe,
 * so updating them off-thread is safe. We never mutate the event (no censoring) — this is pure
 * tracking; any response is a downstream engine's job. Body fully wrapped in Safety.
 *
 * <p><b>Consent (privacy-sensitive).</b> Capture is GROUNDED (the utterance is stored verbatim, never
 * altered) and DOUBLE consent-gated: gate 1 is the GLOBAL {@code observer_capture} switch, read here
 * cheaply via {@link #captureEnabled} (a cached flag refreshed off the chat thread) — when it is false
 * (the default) NOTHING is stored. Gate 2 is the per-player {@code players.observer_opt_out}, deferred
 * DOWNSTREAM to the showrunner's weaponizer (a per-message players lookup on the chat thread would be
 * too expensive) — it skips opted-out players before ever quoting them. A capture failure is silent: a
 * missing observation is always safer than a crash.
 */
public final class ChatListener implements Listener {

    private final SignalTracker tracker;
    private final Safety safety;
    private final SupabaseClient supabase;
    private final BooleanSupplier captureEnabled;

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    // Cap captured text so a pasted wall never becomes a stored observation (privacy + storage hygiene);
    // empty messages are skipped entirely. Longer messages are dropped, not truncated (a partial quote
    // could misrepresent what was actually said — grounding means the whole utterance or none).
    private static final int MAX_CAPTURE_CHARS = 512;

    // Tiny deterministic sentiment lexicons (Phase 0). Intentionally small + neutral; this is a
    // coarse mood signal, not NLP. Lore-agnostic; tuning could move these to config later.
    private static final Set<String> POSITIVE = Set.of(
            "good", "great", "nice", "love", "awesome", "cool", "happy", "thanks", "thank",
            "yes", "win", "won", "lol", "haha", "gg", "epic", "amazing", "yay", "fun", "best");
    private static final Set<String> NEGATIVE = Set.of(
            "bad", "hate", "ugh", "no", "lost", "lose", "dead", "die", "died", "scared",
            "afraid", "creepy", "wtf", "angry", "mad", "fear", "worst", "broken", "lag", "help");

    public ChatListener(SignalTracker tracker, Safety safety,
                        SupabaseClient supabase, BooleanSupplier captureEnabled) {
        this.tracker = tracker;
        this.safety = safety;
        this.supabase = supabase;
        this.captureEnabled = captureEnabled;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        safety.run("signal.AsyncChat", () -> {
            Player p = event.getPlayer();
            if (p == null) return;
            TrackerConfig cfg = tracker.config();
            if (!cfg.enabled()) return;

            String message = plain(event.message());
            if (message == null || message.isEmpty()) return;

            PlayerSignals ps = tracker.signals(p.getUniqueId(), p.getName());

            // The Unspoken — forbidden word scan.
            if (cfg.containsForbidden(message)) {
                ps.addForbiddenWordHit();
                ps.violate(TrackerConfig.CUSTOM_UNSPOKEN, System.currentTimeMillis());
                safety.info("signal.unspoken", p.getName() + " wrote the unspoken");
            }

            // Sentiment — deterministic lexicon mean, folded in.
            ps.addChatSentiment(scoreSentiment(message));

            // OBSERVER TIER-1 — verbatim capture, GATE 1 (global switch). Only when the operator has
            // enabled it; the default is off, so zero chat is stored until then. The message is captured
            // EXACTLY as sent (never censored — the event is untouched above and here). Gate 2 (per-player
            // opt-out) is enforced downstream by the weaponizer. Blocking I/O is queued by the client, so
            // this stays cheap on the chat thread; the whole body is already Safety-wrapped.
            if (supabase != null && captureEnabled != null && captureEnabled.getAsBoolean()) {
                captureObservation(p, message);
            }
        });
    }

    /**
     * Store one verbatim utterance as an {@code observations} row (source='chat'). Bounds: {@code message}
     * is already non-empty here (the tracking path returned early on empty); we DROP anything longer than
     * {@link #MAX_CAPTURE_CHARS} rather than truncate, so a stored quote is always the whole thing said.
     * {@code player_id}/{@code weaponized_at} are left null (resolved/set downstream). Never throws.
     */
    private void captureObservation(Player p, String message) {
        if (message.length() > MAX_CAPTURE_CHARS) return; // skip pastes; grounding = whole utterance or none
        // Provenance is the CONSTANT "in-game" — we deliberately do NOT read p.getWorld() here: this runs
        // OFF the main thread, and the class invariant is to touch NO Bukkit world objects (only the
        // immutable uuid). A world name would be a main-thread read; the fixed source is enough provenance.
        ObservationRow row = new ObservationRow(
                p.getUniqueId().toString(), "chat", message, "in-game");
        supabase.insertObservation(row); // client queues on failure; a missing observation beats a crash
    }

    private String plain(Component component) {
        if (component == null) return "";
        try {
            return PLAIN.serialize(component);
        } catch (Throwable t) {
            return "";
        }
    }

    /** Deterministic sentiment in [-1, +1]: (pos - neg) / max(1, pos + neg). */
    private double scoreSentiment(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        int n = lower.length();
        int pos = 0, neg = 0;
        StringBuilder tok = new StringBuilder(16);
        for (int i = 0; i <= n; i++) {
            char ch = i < n ? lower.charAt(i) : ' ';
            if (Character.isLetter(ch)) {
                tok.append(ch);
            } else if (tok.length() > 0) {
                String w = tok.toString();
                if (POSITIVE.contains(w)) pos++;
                else if (NEGATIVE.contains(w)) neg++;
                tok.setLength(0);
            }
        }
        int total = pos + neg;
        if (total == 0) return 0.0;
        return (double) (pos - neg) / (double) total;
    }
}
