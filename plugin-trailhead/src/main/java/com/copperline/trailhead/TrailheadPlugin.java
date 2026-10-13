package com.copperline.trailhead;

import com.copperline.mrrw.Corpus;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.google.gson.JsonParser;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * mrrw-01, the Act 0 trailhead. Rides along on another server (Marked game night) and does
 * nothing at all until an operator arms it. It never touches inventories, the scoreboard,
 * the action bar, titles, boss bars or the world, which are all Marked's.
 *
 * Operator commands (console or op):
 *   /trailhead start           arm: the server-list hover starts listing 2011 residents
 *   /trailhead knock [name]    a resident "joins the game" and leaves a few seconds later
 *   /trailhead preview         show only you a candidate Morrow line (run again to reroll)
 *   /trailhead speak           post the previewed line (or a fresh one) as [Recovery]
 *   /trailhead key all         every online player gets their sector of the key, as a book
 *                              (operators, i.e. anyone with mrrw.trailhead, are left out so the
 *                              owner never holds a piece; add --include-ops to include them)
 *   /trailhead key NAME        resend (or late-assign) one player's sector
 *   /trailhead key reset       forget who got which sector
 *   /trailhead stop            disarm; the server list goes back to normal
 *   /trailhead status
 */
public final class TrailheadPlugin extends JavaPlugin implements Listener {
    private static final int HEARD_LINES = 30;

    private final List<String> residents = new ArrayList<>();
    private final Deque<String> heard = new ArrayDeque<>();
    private Voice voice;
    private Corpus.Reply pending;
    private File stateFile;
    private YamlConfiguration state;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try (InputStream in = getResource("canon/canon.json")) {
            JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonArray("listed_players_early").forEach(e -> residents.add(e.getAsString()));
        } catch (IOException e) {
            throw new IllegalStateException("canon.json missing from the jar", e);
        }
        try (InputStream in = getResource("canon/corpus.json")) {
            voice = new Voice(in, getConfig().getStringList("avoid-words"));
        } catch (IOException e) {
            throw new IllegalStateException("corpus.json missing from the jar", e);
        }
        stateFile = new File(getDataFolder(), "state.yml");
        state = YamlConfiguration.loadConfiguration(stateFile);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("mrrw-01 " + (armed() ? "ARMED" : "dormant") + ", corpus " + voice.size() + " lines");
    }

    private boolean armed() {
        return state.getBoolean("armed");
    }

    private void save() {
        try {
            state.save(stateFile);
        } catch (IOException e) {
            getLogger().warning("could not save state.yml: " + e.getMessage());
        }
    }

    // ---- server list: the only always-on effect once armed ----

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPing(PaperServerListPingEvent event) {
        if (!armed()) return;
        var listed = event.getListedPlayers();
        for (String name : residents) {
            if (listed.stream().noneMatch(p -> p.name().equalsIgnoreCase(name))) {
                UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
                listed.add(new PaperServerListPingEvent.ListedPlayerInfo(name, id));
            }
        }
        event.setNumPlayers(Math.max(event.getNumPlayers(), Bukkit.getOnlinePlayers().size() + residents.size()));
    }

    // ---- what the table is saying, so Morrow's line can echo it (memory only, armed only) ----

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!armed()) return;
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        synchronized (heard) {
            heard.addLast(text);
            while (heard.size() > HEARD_LINES) heard.removeFirst();
        }
    }

    private String recentChat() {
        synchronized (heard) {
            return String.join(" ", heard);
        }
    }

    // ---- commands ----

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        String sub = args[0].toLowerCase();
        if (!sub.equals("start") && !sub.equals("status") && !armed()) {
            say(sender, "dormant. Run /trailhead start first.");
            return true;
        }
        switch (sub) {
            case "start" -> {
                state.set("armed", true);
                save();
                say(sender, "armed. The server list now shows " + String.join(", ", residents) + ". Nothing in-game changes until you use another command.");
            }
            case "stop" -> {
                state.set("armed", false);
                save();
                pending = null;
                synchronized (heard) {
                    heard.clear();
                }
                say(sender, "dormant again. Key assignments are kept (key reset clears them).");
            }
            case "knock" -> knock(sender, args.length > 1 ? args[1] : residents.get(residents.size() - 1));
            case "preview" -> {
                pending = voice.line(recentChat(), System.nanoTime());
                if (pending == null) {
                    say(sender, "no line found, try again.");
                } else {
                    say(sender, "preview (only you see this; /trailhead speak to post it, preview again to reroll):");
                    sender.sendMessage(recoveryLine(pending.text()));
                    pending.fragments().forEach(f -> sender.sendMessage(Component.text("   " + f.words() + "  <-  " + f.source(), NamedTextColor.DARK_GRAY)));
                }
            }
            case "speak" -> {
                Corpus.Reply line = pending != null ? pending : voice.line(recentChat(), System.nanoTime());
                pending = null;
                if (line == null) {
                    say(sender, "no line found, try again.");
                    return true;
                }
                Bukkit.broadcast(recoveryLine(line.text()));
                getLogger().info("[Recovery] said: " + line.text());
            }
            case "key" -> key(sender, args.length > 1 ? args[1] : "all",
                    java.util.Arrays.stream(args).anyMatch(a -> a.equalsIgnoreCase("--include-ops")));
            case "status" -> say(sender, (armed() ? "ARMED" : "dormant") + ", sectors " + state.getInt("key.n", 0)
                    + ", assigned " + assigned().size() + (pending != null ? ", a previewed line is waiting" : ""));
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void say(CommandSender sender, String text) {
        sender.sendMessage(Component.text("mrrw-01: " + text, NamedTextColor.GRAY));
    }

    /** Same look as Morrow's chat in Mossfield, so players recognise it later. */
    private static Component recoveryLine(String text) {
        return Component.text("[Recovery] ", NamedTextColor.DARK_GRAY).append(Component.text(text, NamedTextColor.GRAY));
    }

    private void knock(CommandSender sender, String name) {
        Bukkit.broadcast(Component.translatable("multiplayer.player.joined", Component.text(name)).color(NamedTextColor.YELLOW));
        Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.broadcast(
                Component.translatable("multiplayer.player.left", Component.text(name)).color(NamedTextColor.YELLOW)), 20L * 4);
        say(sender, name + " knocked.");
    }

    // ---- the key ----

    private List<String> assigned() {
        var section = state.getConfigurationSection("key.assigned");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    private void key(CommandSender sender, String target, boolean includeOps) {
        if (target.equalsIgnoreCase("reset")) {
            state.set("key", null);
            save();
            say(sender, "key assignments cleared.");
            return;
        }
        List<Player> who = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        if (target.equalsIgnoreCase("all") || target.equalsIgnoreCase("--include-ops")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!includeOps && p.hasPermission("mrrw.trailhead")) skipped.add(p.getName());
                else who.add(p);
            }
            who.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        } else {
            Player p = Bukkit.getPlayerExact(target);
            if (p == null) {
                say(sender, target + " is not online.");
                return;
            }
            who.add(p);
        }
        if (who.isEmpty()) {
            say(sender, skipped.isEmpty() ? "nobody online." : "nobody online but operators " + skipped + " (add --include-ops to include them).");
            return;
        }
        int n = state.getInt("key.n", 0);
        if (n == 0) {  // first hand-out decides the split
            n = Math.max(2, who.size());
            state.set("key.n", n);
        }
        for (Player p : who) {
            String path = "key.assigned." + p.getName().toLowerCase();
            if (!state.contains(path)) state.set(path, Math.floorMod(assigned().size(), n));
            give(p, state.getInt(path), n);
        }
        save();
        say(sender, "sectors (" + n + " total) sent to " + who.stream().map(Player::getName).toList()
                + (skipped.isEmpty() ? "" : "; operators left out: " + skipped));
    }

    private void give(Player p, int i, int n) {
        List<String> lines = KeyFragments.sector(getConfig().getStringList("key.lines"), n, i);
        String header = "recovered sector\n" + (i + 1) + " of " + n + "\n\n";
        Book book = Book.book(Component.text("sector " + (i + 1)), Component.text("mrrw-01"),
                Component.text(header + String.join("\n", lines)));
        p.openBook(book);
        p.sendMessage(Component.text("[Recovery] ", NamedTextColor.DARK_GRAY)
                .append(Component.text("recovered sector " + (i + 1) + " of " + n, NamedTextColor.GRAY)));
        for (String line : lines) p.sendMessage(Component.text("  " + line, NamedTextColor.GRAY));
    }
}
