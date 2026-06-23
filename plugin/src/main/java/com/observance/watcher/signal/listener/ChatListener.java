package com.observance.watcher.signal.listener;

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

/**
 * Tracks chat signals (DESIGN §2.1, §2.4):
 * <ul>
 *   <li>deterministic sentiment (a tiny lexicon scan — Phase 0 has NO AI), folded into a per-player
 *       running mean;</li>
 *   <li>The Unspoken custom: a forbidden word in chat is a tracked VIOLATION + a hit counter.</li>
 * </ul>
 *
 * <p><b>Threading.</b> {@link AsyncChatEvent} fires OFF the main thread. We therefore touch NO
 * Bukkit world objects here — only the player's UUID/name (immutable identity) and the message
 * text via Adventure's plain-text serializer. The tracker's in-memory accumulators are thread-safe,
 * so updating them off-thread is safe. We never mutate the event (no censoring) — this is pure
 * tracking; any response is a downstream engine's job. Body fully wrapped in Safety.
 */
public final class ChatListener implements Listener {

    private final SignalTracker tracker;
    private final Safety safety;

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    // Tiny deterministic sentiment lexicons (Phase 0). Intentionally small + neutral; this is a
    // coarse mood signal, not NLP. Lore-agnostic; tuning could move these to config later.
    private static final Set<String> POSITIVE = Set.of(
            "good", "great", "nice", "love", "awesome", "cool", "happy", "thanks", "thank",
            "yes", "win", "won", "lol", "haha", "gg", "epic", "amazing", "yay", "fun", "best");
    private static final Set<String> NEGATIVE = Set.of(
            "bad", "hate", "ugh", "no", "lost", "lose", "dead", "die", "died", "scared",
            "afraid", "creepy", "wtf", "angry", "mad", "fear", "worst", "broken", "lag", "help");

    public ChatListener(SignalTracker tracker, Safety safety) {
        this.tracker = tracker;
        this.safety = safety;
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
        });
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
