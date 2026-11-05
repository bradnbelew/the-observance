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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZoneId;
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
 *
 *  The stop is staged as a lights-out: every replayed figure freezes mid-step and is gone 10 s later,
 *  the copper golems stop (no AI, for good), and June's lamp flashes once more and goes dark as
 *  "exit 0" prints. If a resident ever typed a line asking to leave something unfinished, the skip
 *  line quotes it. The USB group vote's winner (usb-votes.json) is recorded with the stop.
 */
public final class Finale implements Listener {
    private final Plugin plugin;
    private final Canon canon;
    private final State state;
    private final Journal journal;
    private final Ghosts ghosts;
    private final PlayerRoster roster;
    private final PathRecorder recorder;
    private final Random rng = new Random();
    private ActDirector director;
    private boolean ready;
    private boolean stopping;
    private List<String> finalLog;
    private long lastBeat;

    public Finale(Plugin plugin, Canon canon, State state, Journal journal, Ghosts ghosts, PlayerRoster roster, PathRecorder recorder) {
        this.plugin = plugin;
        this.canon = canon;
        this.state = state;
        this.journal = journal;
        this.ghosts = ghosts;
        this.roster = roster;
        this.recorder = recorder;
        if (state.done("stop_pending") && !state.done("stopped")) {  // a restart mid-scene: the stop stands, the scene is skipped
            state.markDone("stopped");
            state.setStamp("stopped_at", System.currentTimeMillis());
            JsonObject d = new JsonObject();
            d.addProperty("by", "resumed after a restart mid-scene");
            journal.write("morrow.stopped", d);
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 100L, 100L);
    }

    private String engine(String key) {
        return canon.object("engine_lines").get(key).getAsString();
    }

    private void announce(String text) {
        Bukkit.broadcast(Component.text("[Recovery] ", NamedTextColor.DARK_GRAY).append(Component.text(text, NamedTextColor.GRAY)));
        journal.recovery(text);
    }

    /** June's lamp lives in the ActDirector (it runs the flasher); set once both exist. */
    public void director(ActDirector director) {
        this.director = director;
    }

    public boolean ready() {
        return ready && !state.done("stopped");
    }

    public boolean stopped() {
        return state.done("stopped");
    }

    private void tick() {
        if (state.act() < 10 || stopped() || stopping) return;
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
        if (stopping || stopped()) return;
        int[] annex = canon.anchor("maintenance_annex");
        Location console = new Location(p.getWorld(), annex[0], annex[1], annex[2]);
        if (p.getWorld() != console.getWorld() || p.getLocation().distanceSquared(console) > 24 * 24) {
            // a chat line, not a passing action bar: say where the console is
            p.sendMessage(Component.text("[mrrw] no console here. the console's at the maintenance annex", NamedTextColor.GRAY));
            return;
        }
        if (devSecret()) {
            p.sendMessage(Component.text("[mrrw] stop refused: no key on file", NamedTextColor.GRAY));
            plugin.getLogger().warning("Act 10 stop refused: stop-secret is still the dev default (set MORROW_STOP_SECRET, then /mrrw reload)");
            return;
        }
        if (!ready()) {
            p.sendMessage(Component.text("[mrrw] stop refused: maint=" + (maintOn(p.getWorld()) ? "on" : "off")
                    + " sessions=" + (state.done("lowpath.session_ended") ? "0" : "1"), NamedTextColor.GRAY));
            return;
        }
        switch (check(code)) {
            case OK -> {
                lastCode = code;
                stop(p, code.charAt(0));
            }
            case EXPIRED -> p.sendMessage(Component.text("[mrrw] key expired. get a new code from the stop console", NamedTextColor.GRAY));
            default -> p.sendMessage(Component.text("[mrrw] key mismatch", NamedTextColor.GRAY));
        }
    }

    enum Check { OK, EXPIRED, MISMATCH }

    /** Codes live for the current and previous 30-minute bucket; a real code up to a day older is "expired". */
    Check check(String code) {
        long bucket = System.currentTimeMillis() / 1_800_000L;
        for (long b = bucket; b >= bucket - 1; b--) if (code.equals(StopCode.of(secret(), code.charAt(0), b))) return Check.OK;
        for (long b = bucket - 2; b >= bucket - 48; b--) if (code.equals(StopCode.of(secret(), code.charAt(0), b))) return Check.EXPIRED;
        return Check.MISMATCH;
    }

    boolean valid(String code) {
        return check(code) == Check.OK;
    }

    static final String DEV_SECRET = "mrrw-dev-stop-secret";

    private String secret() {
        return plugin.getConfig().getString("stop-secret", DEV_SECRET);
    }

    boolean devSecret() {
        String s = secret();
        return s == null || s.isBlank() || DEV_SECRET.equals(s);
    }

    /** First 8 hex of HMAC-SHA256(key = stop secret, msg = "mrrw-fingerprint"); the website shows the same. */
    public String fingerprint() {
        return StopCode.fingerprint(secret());
    }


    /** Operator fallback (/mrrw stop CODE): the same readiness and code checks, without a player at the annex. */
    public String operatorStop(String code) {
        code = code.trim().toUpperCase();
        if (state.act() < 10) return "not act 10";
        if (stopped() || stopping) return "already stopped";
        if (devSecret()) return "refused: stop-secret is the dev default; set it to the website's MORROW_STOP_SECRET in config.yml, then /mrrw reload";
        if (!ready()) return "not ready: maint=" + (maintOn(Bukkit.getWorlds().get(0)) ? "on" : "off") + " sessions=" + (state.done("lowpath.session_ended") ? "0" : "1");
        if (!code.matches("[CRW]-[0-9A-F]{4}-[0-9A-F]{4}")) return "key mismatch";
        Check c = check(code);
        if (c == Check.EXPIRED) return "key expired: get a new code from the website's stop console";
        if (c != Check.OK) return "key mismatch (fingerprint " + fingerprint() + "; compare with the website's)";
        lastCode = code;
        stop("operator", code.charAt(0));
        return "stopping";
    }

    private void stop(Player by, char choice) {
        stop(by.getName(), choice);
    }

    /** The going-home phase lasts this long before the log prints. */
    private static final long HOME_TICKS = 20L * 90;

    /** The free block outside a home's nearest wooden door (June's is on her east wall), else the anchor's north side. */
    private static Location doorstep(World w, int[] a) {
        for (int dx = -4; dx <= 4; dx++) for (int dy = -1; dy <= 2; dy++) for (int dz = -4; dz <= 4; dz++) {
            org.bukkit.block.Block b = w.getBlockAt(a[0] + dx, a[1] + dy, a[2] + dz);
            if (!b.getType().name().endsWith("_DOOR") || b.getType().name().startsWith("IRON")) continue;
            if (!(b.getBlockData() instanceof org.bukkit.block.data.type.Door d) || d.getHalf() != org.bukkit.block.data.Bisected.Half.BOTTOM) continue;
            for (org.bukkit.block.BlockFace f : List.of(d.getFacing(), d.getFacing().getOppositeFace())) {
                org.bukkit.block.Block out = b.getRelative(f);
                if (out.getType().isAir() && out.getRelative(0, 1, 0).getType().isAir()) return out.getLocation().add(0.5, 0, 0.5);
            }
        }
        return new Location(w, a[0] + 0.5, a[1], a[2] - 1.5);
    }

    private void stop(String byName, char choice) {
        if (stopping || stopped()) return;
        stopping = true;
        state.markDone("stop_pending");  // on disk at once: a crash in the next three minutes must not un-stop the game
        // the south branch is the one thing Morrow leaves unfinished; the log must always say so,
        // and if a resident ever asked for it, the reason is theirs (else finch's own 2011 sign)
        BridgeLogic.Said asked = roster.firstUnfinished();
        String skip = BridgeLogic.skipLine(asked, ZoneId.systemDefault(), canon.raw.has("skip_fallback") ? canon.string("skip_fallback") : null);
        List<String> log = BridgeLogic.stopLog(Canon.strings(canon.array("stop_log")), skip);
        finalLog = List.copyOf(log);
        BridgeLogic.Usb usb = BridgeLogic.readUsb(new File(plugin.getDataFolder(), "usb-votes.json"), Bridge.quorumPlayers(plugin, roster));
        World w = Bukkit.getWorlds().get(0);
        // Morrow's last words: the code echoed, then the one fresh sentence it has that matches "stop"
        announce(engine("stop_requested").replace("{code}", byName.equals("operator") ? "console" : lastCode == null ? "" : lastCode));
        Bukkit.getScheduler().runTaskLater(plugin, () -> announce(canon.string("kit_line")), 40L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> goingHome(w), 80L);
        long logStart = 80L + HOME_TICKS;
        for (int i = 0; i < log.size(); i++) {
            String line = log.get(i);
            Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcast(Component.text(line, NamedTextColor.DARK_GRAY)), logStart + 40L * (i + 1));
        }
        // June's lamp: one last flash, dark as the last line ("exit 0") prints
        if (director != null) Bukkit.getScheduler().runTaskLater(plugin, director::lastFlash, logStart + Math.max(1L, 40L * log.size() - 30L));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            state.markDone("stopped");
            state.setStamp("stopped_at", System.currentTimeMillis());
            ghosts.clearAll();
            JsonObject d = new JsonObject();
            d.addProperty("by", byName);
            d.addProperty("retained", switch (choice) { case 'C' -> "clean"; case 'R' -> "recovered"; default -> "corpus"; });
            if (usb.winner() == null) d.add("usb_winner", com.google.gson.JsonNull.INSTANCE);
            else d.addProperty("usb_winner", usb.winner());
            d.add("usb_votes", usb.json().get("votes"));
            d.addProperty("skipped", "mossfield:south_rail");
            d.addProperty("skip_line", skip);
            if (asked != null) {
                d.addProperty("skip_reason_by", asked.name());
                d.addProperty("skip_reason", asked.text());
            }
            com.google.gson.JsonArray lines = new com.google.gson.JsonArray();
            log.forEach(lines::add);
            d.add("stop_log", lines);
            journal.write("morrow.stopped", d);
            try {  // the Discord bot watches for this file and cuts the bridge
                Files.writeString(new File(plugin.getDataFolder(), "stopped.flag").toPath(), d.toString(), StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                // the journal already records the stop
            }
        }, logStart + 40L * (log.size() + 1));
    }

    private String lastCode;

    /**
     * Finalising, as a scene: every replayed figure stops where it stands, then each resident's figure
     * appears where its replay last stood and walks home; as each reaches its door the engine prints
     * "session X: end" (join order) and the door closes; the golems stop for good; the players' own
     * figures walk to where they first logged in and stand. The lamp waits for the log's "exit 0".
     */
    private void goingHome(World w) {
        int figures = ghosts.freezeAll();
        ghosts.clearAll();
        ghosts.thaw();
        if (director != null) director.quietLamp();  // dark until its one last flash at "exit 0"
        int golems = 0;
        for (LivingEntity e : w.getLivingEntities()) {
            if (isGolem(e) && e.hasAI()) {
                e.setAI(false);
                golems++;
            }
        }
        String[][] homes = {{"rookery", "rookery_house"}, {"cairn", "cairn_storehouse_door_2011"}, {"finchline", "finch_hub"},
                {"juni_lantern", "june_house"}, {"patchcord", "patchcord_relay_hut"}, {"kitbag", "kit_genuine_door"}};
        List<String> order = Canon.strings(canon.array("residents_join_order"));
        // each walks at most 60 blocks (Kit's hut is 800 away: his figure starts on its last approach), and the
        // starts are staggered so the arrivals, and the "session X: end" lines, come in join order
        double arrival = 2.0;
        for (String who : order) {
            String anchor = null;
            for (String[] h : homes) if (h[0].equals(who)) anchor = h[1];
            if (anchor == null) continue;
            final String home = anchor;
            int[] a = canon.anchor(home);
            Location door = doorstep(w, a);
            Location from = lastStood(w, who);
            double dist = Math.hypot(from.getX() - door.getX(), from.getZ() - door.getZ());
            if (dist > 60) {
                double f = 60 / dist;
                from = new Location(w, door.getX() + (from.getX() - door.getX()) * f, door.getY(), door.getZ() + (from.getZ() - door.getZ()) * f);
                dist = 60;
            }
            double walk = Math.max(0.5, dist / 4.0);
            arrival = Math.max(arrival + 4.0, walk + 1.0);
            long startTicks = (long) ((arrival - walk) * 20);
            final Location start = from;
            Bukkit.getScheduler().runTaskLater(plugin, () -> ghosts.play(w, who, ghosts.profileFor(who), Ghosts.walk(start, door, 4.0), List.of(), 0, 0, false, () -> {
                announce(engine("session_end").replace("{who}", who));
                Nights.setDoorNear(w, canon.anchor(home), false);
            }), startTicks);
        }
        // the players' figures, to where each first came home
        recorder.firstJoins().forEach((name, p) -> {
            Player online = Bukkit.getPlayerExact(name);
            Location from = online != null ? online.getLocation() : spawn(w);
            Location to = new Location(w, p[0], p[1], p[2]);
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(name);
            var profile = io.papermc.paper.datacomponent.item.ResolvableProfile.resolvableProfile(op.getPlayerProfile());
            List<Ghosts.Point> walk = new ArrayList<>(Ghosts.walk(from, to, 4.0));
            Ghosts.Point end = walk.get(walk.size() - 1);
            walk.add(new Ghosts.Point(end.t() + 600, end.x(), end.y(), end.z()));  // then it stands
            Bukkit.getScheduler().runTaskLater(plugin, () -> ghosts.play(w, name, profile, walk, List.of(), 0, 0, false, null), 40L);
        });
        JsonObject d = new JsonObject();
        d.addProperty("figures_frozen", figures);
        d.addProperty("golems_stopped", golems);
        d.addProperty("residents_walking_home", order.size());
        journal.write("morrow.lights_out", d);
    }

    private Location spawn(World w) {
        int[] s = canon.anchor("spawn");
        return new Location(w, s[0] + 0.5, s[1] + 1, s[2] + 0.5);
    }

    /** Where a resident's figure last stood: the end of its incident path (its habit), else spawn. */
    private Location lastStood(World w, String who) {
        for (com.google.gson.JsonElement e : canon.array("incident_replay")) {
            JsonObject f = e.getAsJsonObject();
            if (!f.get("who").getAsString().equals(who)) continue;
            com.google.gson.JsonArray path = f.getAsJsonArray("path");
            com.google.gson.JsonArray p = path.get(path.size() - 1).getAsJsonArray();
            return new Location(w, p.get(1).getAsDouble(), p.get(2).getAsDouble(), p.get(3).getAsDouble());
        }
        return spawn(w);
    }

    private static boolean isGolem(Entity e) {
        return e.getType().name().equals("COPPER_GOLEM");
    }

    /** Golems in chunks that were unloaded at the stop stop too, the moment they load. */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        if (!stopping && !stopped()) return;
        for (Entity en : e.getEntities()) if (isGolem(en) && en instanceof LivingEntity le && le.hasAI()) le.setAI(false);
    }

    /** The stop log as it played (null before the stop); read back from stopped.flag after a restart. */
    public List<String> finalLog() {
        if (finalLog == null && stopped()) {
            try {
                JsonObject d = com.google.gson.JsonParser.parseString(Files.readString(
                        new File(plugin.getDataFolder(), "stopped.flag").toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                if (d.has("stop_log")) finalLog = Canon.strings(d.getAsJsonArray("stop_log"));
            } catch (Exception ignored) {
                // an older flag without the log
            }
        }
        return stopped() ? finalLog : null;
    }

    /** Never used after the stop; kept for the operator's /mrrw status. */
    public String status() {
        return stopped() ? "stopped" : ready ? "ready" : "running";
    }
}
