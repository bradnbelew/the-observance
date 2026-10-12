package com.copperline.mrrw;

import com.google.gson.JsonObject;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

/**
 * Act 10: Back By Tomorrow.
 *  Ready = the MAINT sign at spawn reads "MAINT ON" AND is waxed, AND lowpath's session has ended
 *  (someone gave it a bed). While ready, the server-list MOTD carries "maint=on sessions=0", which
 *  the website's stop console reads. The console then issues a code; a player types it in chat at
 *  the maintenance annex. The code is an HMAC shared with the website (config stop-secret =
 *  MORROW_STOP_SECRET). Then the stop log plays, south_rail is skipped, and Morrow is silent.
 *  Until then, bounded hostile beats: unwaxed signs flip, a false restart notice, the route
 *  returns you somewhere wrong once. Nothing destroys evidence; nothing traps anyone.
 */
public final class Finale implements Listener {
    private final Plugin plugin;
    private final Canon canon;
    private final State state;
    private final Journal journal;
    private final Ghosts ghosts;
    private final Random rng = new Random();
    private boolean ready;
    private long lastBeat;

    public Finale(Plugin plugin, Canon canon, State state, Journal journal, Ghosts ghosts) {
        this.plugin = plugin;
        this.canon = canon;
        this.state = state;
        this.journal = journal;
        this.ghosts = ghosts;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 100L, 100L);
    }

    public boolean ready() {
        return ready && !state.done("stopped");
    }

    public boolean stopped() {
        return state.done("stopped");
    }

    private void tick() {
        if (state.act() < 10 || stopped()) return;
        World w = Bukkit.getWorlds().get(0);
        boolean wasReady = ready;
        ready = maintOn(w) && state.done("lowpath.session_ended");
        if (ready && !wasReady) journal.write("morrow.ready", new JsonObject());
        long t = w.getTime();
        boolean night = t >= 13000 && t < 23000;
        if (night && !ready && System.currentTimeMillis() - lastBeat > 90_000 && !w.getPlayers().isEmpty()) {
            lastBeat = System.currentTimeMillis();
            hostileBeat(w);
        }
    }

    boolean maintOn(World w) {
        int[] a = canon.anchor("maint_sign");
        if (!(w.getBlockAt(a[0], a[1], a[2]).getState() instanceof Sign s) || !s.isWaxed()) return false;
        String text = PlainTextComponentSerializer.plainText().serialize(s.getSide(Side.FRONT).line(0)).trim().toUpperCase();
        return text.equals(canon.string("maint_sign_text"));
    }

    private void hostileBeat(World w) {
        switch (rng.nextInt(3)) {
            case 0 -> Bukkit.broadcast(Component.text("[Server] Restarting in 10s", NamedTextColor.RED));  // diegetic, and false
            case 1 -> {  // unwaxed signs near spawn flip back to the old text
                int[] a = canon.anchor("maint_sign");
                if (w.getBlockAt(a[0], a[1], a[2]).getState() instanceof Sign s && !s.isWaxed()) {
                    s.getSide(Side.FRONT).line(0, Component.text("MAINT OFF"));
                    s.update(true, false);
                }
            }
            default -> {  // the low path returns one player somewhere wrong, once each
                int[] lp = canon.anchor("lowpath_place");
                for (Player p : w.getPlayers()) {
                    String flag = "act10.returned." + p.getUniqueId();
                    if (state.done(flag) || p.getLocation().distanceSquared(new Location(w, lp[0], lp[1], lp[2])) > 40 * 40) continue;
                    int[] sp = canon.anchor("spawn");
                    p.teleport(new Location(w, sp[0] + .5, sp[1] + 1, sp[2] + .5));
                    p.sendActionBar(Component.text("the low path brings you home", NamedTextColor.GRAY));
                    state.markDone(flag);
                    break;
                }
            }
        }
        JsonObject d = new JsonObject();
        d.addProperty("beat", "hostile");
        journal.write("morrow.hostile", d);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (state.act() < 10 || stopped()) return;
        String msg = PlainTextComponentSerializer.plainText().serialize(e.message()).trim().toUpperCase();
        if (!msg.matches("[CRW]-[0-9A-F]{4}-[0-9A-F]{4}")) return;
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> attempt(p, msg));
    }

    private void attempt(Player p, String code) {
        int[] annex = canon.anchor("maintenance_annex");
        if (p.getLocation().distanceSquared(new Location(p.getWorld(), annex[0], annex[1], annex[2])) > 24 * 24) {
            p.sendActionBar(Component.text("mrrw: console not found here", NamedTextColor.GRAY));
            return;
        }
        if (!ready()) {
            p.sendMessage(Component.text("[mrrw] stop refused: maint=" + (maintOn(p.getWorld()) ? "on" : "off")
                    + " sessions=" + (state.done("lowpath.session_ended") ? "0" : "1"), NamedTextColor.GRAY));
            return;
        }
        if (!valid(code)) {
            p.sendMessage(Component.text("[mrrw] key mismatch", NamedTextColor.GRAY));
            return;
        }
        stop(p, code.charAt(0));
    }

    boolean valid(String code) {
        long bucket = System.currentTimeMillis() / 1_800_000L;
        for (long b = bucket; b >= bucket - 1; b--) if (code.equals(StopCode.of(secret(), code.charAt(0), b))) return true;
        return false;
    }

    private String secret() {
        return plugin.getConfig().getString("stop-secret", "mrrw-dev-stop-secret");
    }

    /** Operator fallback (/mrrw stop CODE): the same readiness and code checks, without a player at the annex. */
    public String operatorStop(String code) {
        code = code.trim().toUpperCase();
        if (state.act() < 10) return "not act 10";
        if (stopped()) return "already stopped";
        if (!ready()) return "not ready: maint=" + (maintOn(Bukkit.getWorlds().get(0)) ? "on" : "off") + " sessions=" + (state.done("lowpath.session_ended") ? "0" : "1");
        if (!code.matches("[CRW]-[0-9A-F]{4}-[0-9A-F]{4}") || !valid(code)) return "key mismatch";
        stop("operator", code.charAt(0));
        return "stopping";
    }

    private void stop(Player by, char choice) {
        stop(by.getName(), choice);
    }

    private void stop(String byName, char choice) {
        List<String> log = Canon.strings(canon.array("stop_log"));
        for (int i = 0; i < log.size(); i++) {
            String line = log.get(i);
            Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcast(Component.text(line, NamedTextColor.DARK_GRAY)), 40L * (i + 1));
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            state.markDone("stopped");
            ghosts.clearAll();
            JsonObject d = new JsonObject();
            d.addProperty("by", byName);
            d.addProperty("retained", switch (choice) { case 'C' -> "clean"; case 'R' -> "recovered"; default -> "corpus"; });
            journal.write("morrow.stopped", d);
            try {  // the Discord bot watches for this file and cuts the bridge
                Files.writeString(new File(plugin.getDataFolder(), "stopped.flag").toPath(), d.toString(), StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                // the journal already records the stop
            }
        }, 40L * (log.size() + 1));
    }

    /** Never used after the stop; kept for the operator's /mrrw status. */
    public String status() {
        return stopped() ? "stopped" : ready ? "ready" : "running";
    }

    static final class StopCode {
        static String of(String secret, char choice, long bucket) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                String hex = HexFormat.of().withUpperCase().formatHex(mac.doFinal(("stop|" + choice + "|" + bucket).getBytes(StandardCharsets.UTF_8)));
                return choice + "-" + hex.substring(0, 4) + "-" + hex.substring(4, 8);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
