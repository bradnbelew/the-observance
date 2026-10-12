package com.copperline.mrrw;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/**
 * Morrow's chat. Act 1-2: only answers !status with Theo's macro-flavoured progress lines.
 * Act 3 onward: occasionally answers ordinary chat with a word-chain collage of the corpus.
 * From Act 7 the corpus includes the players' own lines.
 */
public final class ChatListener implements Listener {
    private static final long COOLDOWN_MS = 45_000;
    private final Plugin plugin;
    private final Canon canon;
    private final State state;
    private final PlayerRoster roster;
    private final Journal journal;
    private long lastReply;
    private int playerLinesLoaded;
    private boolean kitLoaded;

    public ChatListener(Plugin plugin, Canon canon, State state, PlayerRoster roster, Journal journal) {
        this.plugin = plugin;
        this.canon = canon;
        this.state = state;
        this.roster = roster;
        this.journal = journal;
        syncPlayerLines();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        roster.join(event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String name = event.getPlayer().getName();
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        Bukkit.getScheduler().runTask(plugin, () -> handle(name, text));
    }

    private void handle(String name, String text) {
        if (state.done("stopped")) return;  // silent after shutdown
        if (state.act() >= 8 && !kitLoaded) {  // Theo fed it his inbox; Kit's line is in there
            canon.corpus.add(canon.string("kit_line"), "theo's inbox, 2019-03-02", false);
            kitLoaded = true;
        }
        roster.said(name, text);
        syncPlayerLines();
        int act = state.act();
        String reply;
        if (text.trim().equalsIgnoreCase("!status")) {
            reply = canon.statusLines.get(Math.min(canon.statusLines.size() - 1, act / 4));
        } else if (act >= 3 && System.currentTimeMillis() - lastReply > COOLDOWN_MS && Math.floorMod(text.hashCode(), 3) == 0) {
            Corpus.Reply r = canon.corpus.reply(text, act, text.hashCode() * 31L + act);
            if (r.text().isBlank()) return;
            reply = r.text();
            JsonObject d = new JsonObject();
            d.addProperty("heard", text);
            d.addProperty("said", reply);
            d.addProperty("obeys_corpus_law", canon.corpus.obeysLaw(r, act));
            JsonArray src = new JsonArray();
            r.fragments().forEach(f -> src.add(f.words() + "  <-  " + f.source()));
            d.add("sources", src);
            journal.write("morrow.chat", d);
        } else {
            return;
        }
        lastReply = System.currentTimeMillis();
        long delay = 30 + (Math.floorMod(text.hashCode(), 50));
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcast(
                Component.text("[Recovery] ", NamedTextColor.DARK_GRAY).append(Component.text(reply, NamedTextColor.GRAY))), delay);
    }

    private void syncPlayerLines() {
        var lines = roster.lines();
        for (int i = playerLinesLoaded; i < lines.size(); i++) {
            String[] parts = lines.get(i).split("\u0000", 2);
            if (parts.length == 2) canon.corpus.add(parts[1], parts[0] + ", in Mossfield", true);
        }
        playerLinesLoaded = lines.size();
    }
}
