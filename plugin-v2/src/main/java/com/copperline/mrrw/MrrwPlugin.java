package com.copperline.mrrw;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * mrrw: the Mossfield recovery process, as Iona wrote it in 2011 and Theo kept patching.
 *
 * Operator commands (console or op; /mrrw help lists them all, OPERATOR-RUNBOOK.md has the table).
 * The site's operator page runs the same text over the bridge (POST /event {"kind":"op","cmd":...}).
 * To a non-op the command does not exist: hidden from tab-complete, and "Unknown command" if typed.
 *
 * The stop is refused while stop-secret is the dev default. /mrrw status prints its fingerprint,
 * the first 8 hex of HMAC-SHA256(key = stop-secret, msg = "mrrw-fingerprint"); the website's
 * stop console shows the same fingerprint for MORROW_STOP_SECRET, so a mismatch is visible.
 *
 * Acts advance by themselves (config auto_advance, see {@link Campaign}); the website and the bot
 * read the act, readiness and the USB vote from the signed bridge (see {@link Bridge}).
 */
public final class MrrwPlugin extends JavaPlugin {
    private Canon canon;
    private State state;
    private Journal journal;
    private PlayerRoster roster;
    private ActDirector director;
    private RecoveryService recovery;
    private Ghosts ghosts;
    private Replays replays;
    private ModernLayer modern;
    private Corrections corrections;
    private Finale finale;
    private Nights nights;
    private Fixtures fixtures;
    private Campaign campaign;
    private Bridge bridge;
    private String pauseWarning;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        canon = Canon.load(getClassLoader());
        state = new State(getDataFolder());
        journal = new Journal(getDataFolder());
        roster = new PlayerRoster(getDataFolder());
        if (canon.provisionalLayout) getLogger().warning("canon layout is PROVISIONAL; anchors are placeholders until the Beta world is laid out");
        warnDevSecret();
        hideFromPlayers();
        checkPauseWhenEmpty();

        ghosts = new Ghosts(this);
        PathRecorder recorder = new PathRecorder(this, state, roster);
        finale = new Finale(this, canon, state, journal, ghosts, roster, recorder);
        modern = new ModernLayer(this, canon, state, journal);
        recovery = new RecoveryService(this, canon, state, journal);
        corrections = new Corrections(this, canon, state, journal);
        replays = new Replays(this, canon, state, journal, ghosts, recorder);
        fixtures = new Fixtures(this, canon, state, journal);
        recovery.onDawn(fixtures::dawn);
        recovery.onDawn(w -> {
            int put = modern.refillMapCopies(w);
            if (put > 0) {
                JsonObject d = new JsonObject();
                d.addProperty("map_copies_put_back", put);
                journal.write("morrow.map_copies", d);
            }
        });
        nights = new Nights(this, canon, state, journal, recorder, modern, roster);
        campaign = new Campaign(this, canon, state, journal, roster, this::applyAct);
        recovery.onDawn(w -> campaign.onDawn());
        nights.onMaintReverted(() -> campaign.advance(6, "MAINT ON set by the players and reverted at dawn"));
        ChatListener chat = new ChatListener(this, canon, state, roster, journal);
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PingListener(canon, state, roster, finale), this);
        pm.registerEvents(chat, this);
        pm.registerEvents(campaign, this);
        pm.registerEvents(recovery, this);
        pm.registerEvents(recorder, this);
        pm.registerEvents(nights, this);
        pm.registerEvents(replays, this);
        pm.registerEvents(fixtures, this);
        pm.registerEvents(finale, this);
        pm.registerEvents(new FinchLock(canon), this);
        director = new ActDirector(this, canon, state, journal);
        finale.director(director);
        bridge = new Bridge(this, state, canon, roster, finale, campaign, chat, journal, recorder, this::opFromBridge);
        bridge.start();
        getServer().getScheduler().runTask(this, () -> {
            modern.reattachMaps();
            applyAct();
        });
        getLogger().info("mrrw ready: act " + state.act() + ", corpus " + canon.corpus.size() + " lines, " + finale.status()
                + ", stop-secret fingerprint " + finale.fingerprint());
    }

    private void warnDevSecret() {
        String s = getConfig().getString("stop-secret");
        if (s == null || s.isBlank() || "mrrw-dev-stop-secret".equals(s)) {
            getLogger().warning("stop-secret is the dev default: the Act 10 stop is REFUSED until it is set to the website's MORROW_STOP_SECRET (then /mrrw reload)");
        }
    }

    /** Players never see a backend: no /plugins, /version or /mrrw for non-ops (permissions.yml can still grant them). */
    private void hideFromPlayers() {
        for (String perm : List.of("bukkit.command.plugins", "bukkit.command.version")) {
            var p = getServer().getPluginManager().getPermission(perm);
            if (p != null) p.setDefault(PermissionDefault.OP);
        }
        var cmd = getCommand("mrrw");
        if (cmd != null) cmd.permissionMessage(Component.text("Unknown or incomplete command, see below for error", NamedTextColor.RED)
                .appendNewline().append(Component.text("mrrw", NamedTextColor.GRAY)).append(Component.text("<--[HERE]", NamedTextColor.RED)));
    }

    /** pause-when-empty-seconds must be -1, or nothing happens between sessions. Warned at start, in /mrrw status, in the journal. */
    private void checkPauseWhenEmpty() {
        Properties props = null;
        for (File f : List.of(new File("server.properties"), new File(getServer().getWorldContainer(), "server.properties"))) {
            if (!f.isFile()) continue;
            props = new Properties();
            try (FileReader r = new FileReader(f, StandardCharsets.UTF_8)) {
                props.load(r);
            } catch (Exception e) {
                props = null;
            }
            break;
        }
        pauseWarning = OpsLogic.pauseWarning(props);
        if (pauseWarning != null) {
            getLogger().severe(pauseWarning);
            JsonObject d = new JsonObject();
            d.addProperty("warning", pauseWarning);
            journal.write("warning.pause_when_empty", d);
        }
    }

    private void applyAct() {
        if (state.done("stopped")) return;
        director.apply();
        corrections.apply(world());
    }

    /** Everything held in memory goes to disk (before a snapshot). */
    void flushForSnapshot() {
        if (recovery != null) recovery.save();
    }

    private World world() {
        return getServer().getWorlds().get(0);
    }

    @Override
    public void onDisable() {
        if (bridge != null) bridge.stop();
        if (director != null) director.stop();
        if (recovery != null) recovery.save();
        if (ghosts != null) ghosts.clearAll();
    }

    // ---- operator commands ------------------------------------------------------------------------

    static final List<String> HELP = List.of(
            "status                       act, readiness, secret fingerprint, warnings",
            "help                         this list",
            "act <1-10> [ISO start]       set the act (always wins); same act again re-applies it",
            "auto on|off                  auto-advance (on applies anything held meanwhile)",
            "desk                         desk refs: received, reply time, released, applied",
            "handin <kind>                a hand-in as if the players did it: " + String.join(" ", handinKinds()),
            "hint <n>                     show this act's first n hints on the desk",
            "flags | flag <name> [on|off] the persisted one-shot flags",
            "journal [n]                  the last n journal lines (default 10)",
            "residents [add|remove <name>|quorum <n>]  who counts, and the USB quorum",
            "snapshot [name] | snapshots | rollback <name>  save / list / restore the plugin's state",
            "repair [part|all]            re-place install parts without touching progress",
            "restore [signs]              the dawn's build restore now (signs: edited clue signs back)",
            "dawn                         run every dawn job now",
            "tp <place> [player]          go to a canon anchor",
            "figures clear|respawn        remove every figure / start the replays now",
            "say <text>                   speak as Morrow ([Recovery] in chat and Discord)",
            "replay [sound]               the incident replay / patchcord's sound replay now",
            "install                      first install of the modern layer (idempotent)",
            "reload                       re-read config.yml",
            "stop <code>                  Act 10 fallback: enter the console's stop code");

    static List<String> handinKinds() {
        List<String> k = new ArrayList<>(BridgeLogic.DESK_ADVANCES.keySet());
        k.add("booking");
        k.addAll(List.of("maint", "lowpath", "cairn_room"));
        return k;
    }

    private static final List<String> SUBCOMMANDS = List.of("help", "status", "act", "auto", "desk", "handin", "hint", "flags", "flag",
            "journal", "residents", "snapshot", "snapshots", "rollback", "repair", "restore", "dawn", "tp", "figures", "say",
            "replay", "install", "reload", "stop");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        run(sender, sender.getName(), args);
        return true;
    }

    /** The site's operator page (bridge thread -> main thread): the same command, its replies collected. */
    private String opFromBridge(String cmd) {
        String[] args = OpsLogic.opArgs(cmd);
        if (args.length > 0 && args[0].equalsIgnoreCase("rollback")) {  // it restarts the plugin, the bridge with it: answer first
            if (args.length < 2 || !OpsLogic.snapshots(getDataFolder()).contains(args[1])) return Bridge.OP_FAILED + "mrrw: no snapshot " + (args.length < 2 ? "given" : args[1]);
            getServer().getScheduler().runTask(this, () -> run(getServer().getConsoleSender(), "op page", args));
            return "mrrw: rolling back to " + args[1] + " now; the bridge answers again in a few seconds";
        }
        StringBuilder out = new StringBuilder();
        CommandSender sender = getServer().createCommandSender(c -> out.append(PlainTextComponentSerializer.plainText().serialize(c)).append('\n'));
        boolean ok = run(sender, "op page", args);
        return (ok ? "" : Bridge.OP_FAILED) + out.toString().trim();
    }

    private static void say(CommandSender to, String text) {
        to.sendMessage(Component.text("mrrw: " + text));
    }

    private void opJournal(String kind, String by, String what, String value) {
        JsonObject d = new JsonObject();
        d.addProperty("by", by);
        if (what != null) d.addProperty(what, value);
        journal.write(kind, d);
    }

    /** Runs one /mrrw command. False when it failed (bad arguments, unknown command, an error); read-only ones never journal. */
    private boolean run(CommandSender s, String by, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "help" -> HELP.forEach(l -> s.sendMessage(Component.text("/mrrw " + l)));
                case "status" -> status(s);
                case "act" -> act(s, by, args);
                case "auto" -> {
                    if (args.length != 2 || !List.of("on", "off").contains(args[1].toLowerCase(Locale.ROOT))) {
                        say(s, "auto-advance is " + (campaign.autoAdvance() ? "on" : "off") + "; usage /mrrw auto on|off");
                        return args.length == 1;
                    }
                    int before = state.act();
                    campaign.setAutoAdvance(args[1].equalsIgnoreCase("on"), by);
                    say(s, "auto-advance " + (campaign.autoAdvance() ? "on" : "off") + (state.act() != before ? "; held moves applied: act " + before + " -> " + state.act() : ""));
                }
                case "desk" -> {
                    List<String> lines = campaign.deskLines();
                    if (lines.isEmpty()) say(s, "no desk refs yet");
                    lines.forEach(l -> s.sendMessage(Component.text(l)));
                }
                case "handin" -> handin(s, by, args);
                case "hint" -> {
                    int n = Integer.parseInt(arg(args, 1, "usage /mrrw hint <0-4>"));
                    n = campaign.releaseHints(n, by);
                    say(s, "act " + state.act() + " shows its first " + n + " hint(s) on the desk now");
                }
                case "flags" -> {
                    List<String> f = state.flags();
                    say(s, f.size() + " flag(s) set");
                    for (int i = 0; i < f.size(); i += 6) s.sendMessage(Component.text(String.join("  ", f.subList(i, Math.min(f.size(), i + 6)))));
                }
                case "flag" -> flag(s, by, args);
                case "journal" -> {
                    int n = args.length > 1 ? Math.max(1, Math.min(200, Integer.parseInt(args[1]))) : 10;
                    for (JsonObject e : journal.tail(n)) {
                        s.sendMessage(Component.text(e.get("at").getAsString().substring(0, Math.min(19, e.get("at").getAsString().length()))
                                + " " + e.get("kind").getAsString() + " " + e.get("detail"), NamedTextColor.GRAY));
                    }
                }
                case "residents" -> residents(s, by, args);
                case "snapshot" -> {
                    String name = args.length > 1 ? args[1] : "op-" + System.currentTimeMillis() / 1000;
                    flushForSnapshot();
                    List<String> files = OpsLogic.snapshot(getDataFolder(), name);
                    opJournal("operator.snapshot", by, "name", name);
                    say(s, "snapshot " + name + " (" + String.join(", ", files) + ")");
                }
                case "snapshots" -> {
                    List<String> names = OpsLogic.snapshots(getDataFolder());
                    say(s, names.isEmpty() ? "no snapshots" : String.join(", ", names));
                }
                case "rollback" -> rollback(s, by, args);
                case "repair" -> {
                    String which = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "all";
                    List<String> ran = modern.repair(world(), which);
                    if (ran.contains("listening_room") && state.done("act8.listening_room_open")) nights.openListeningRoom(world(), by);
                    opJournal("operator.repair", by, "which", which);
                    say(s, "re-placed " + String.join(", ", ran) + (ran.contains("maint_sign") ? " (the MAINT sign reads MAINT OFF, unwaxed)" : ""));
                }
                case "restore" -> {
                    String r = recovery.restoreNow(args.length > 1 && args[1].equalsIgnoreCase("signs"));
                    opJournal("operator.restore", by, "result", r);
                    say(s, r);
                }
                case "dawn" -> {
                    recovery.dawn(world());
                    opJournal("operator.dawn", by, null, null);
                    say(s, "dawn jobs run");
                }
                case "tp" -> tp(s, by, args);
                case "figures" -> figures(s, by, args);
                case "say" -> {
                    if (args.length < 2) {
                        say(s, "usage /mrrw say <text>");
                        return false;
                    }
                    String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    Bukkit.broadcast(Component.text("[Recovery] ", NamedTextColor.DARK_GRAY).append(Component.text(text, NamedTextColor.GRAY)));
                    journal.recovery(text);  // the bot relays it like any of Morrow's lines
                    opJournal("operator.say", by, "text", text);
                    say(s, "said");
                }
                case "replay" -> {
                    if (args.length > 1 && args[1].equalsIgnoreCase("sound")) {
                        var played = nights.soundReplay(world());
                        say(s, "sound replay started, " + played.size() + " events " + Vibrations.SPACING_TICKS + " ticks apart, decodes to " + Vibrations.decode(played));
                    } else {
                        replays.incident(world());
                        say(s, "incident replay started");
                    }
                }
                case "install" -> say(s, "installed " + modern.install(world()));
                case "reload" -> {
                    reloadConfig();
                    warnDevSecret();
                    opJournal("operator.reload", by, "fingerprint", finale.fingerprint());
                    say(s, "config reloaded, stop-secret fingerprint " + finale.fingerprint()
                            + (finale.devSecret() ? " (DEV DEFAULT: stop refused)" : "") + "; skins apply to the next figures spawned");
                }
                case "stop" -> say(s, finale.operatorStop(arg(args, 1, "usage /mrrw stop <code>")));
                default -> {
                    say(s, "no such command " + sub + "; /mrrw help");
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException e) {  // includes NumberFormatException
            say(s, e.getMessage() == null ? "bad argument; /mrrw help" : e.getMessage());
        } catch (Exception e) {
            getLogger().warning("/mrrw " + String.join(" ", args) + " failed: " + e);
            say(s, "failed: " + e);
        }
        return false;
    }

    private static String arg(String[] args, int i, String usage) {
        if (args.length <= i) throw new IllegalArgumentException(usage);
        return args[i];
    }

    private void status(CommandSender s) {
        say(s, "act " + state.act() + ", corpus " + canon.corpus.size()
                + " lines, layout " + (canon.provisionalLayout ? "PROVISIONAL" : "final") + ", " + finale.status()
                + ", lowpath " + (state.done("lowpath.session_ended") ? "home" : "walking")
                + ", stop-secret fingerprint " + finale.fingerprint() + (finale.devSecret() ? " (DEV DEFAULT: stop and bridge refused)" : "")
                + ", act started " + Campaign.iso(campaign.actStarted()) + ", auto-advance " + (campaign.autoAdvance() ? "on" : "off")
                + ", residents " + roster.names().size() + " (quorum " + Bridge.quorumPlayers(this, roster) + ")"
                + ", usb " + bridge.usb().json());
        if (pauseWarning != null) s.sendMessage(Component.text("WARNING: " + pauseWarning, NamedTextColor.RED));
    }

    private void act(CommandSender s, String by, String[] args) {
        int act;
        long started = System.currentTimeMillis();
        try {
            act = Integer.parseInt(args[1]);
            if (act < 1 || act > 10) throw new NumberFormatException();
            if (args.length == 3) started = Campaign.parseIso(args[2]);
            else if (act == state.act()) started = campaign.actStarted();  // re-applying this act keeps its clock (Theo's timed notes)
        } catch (RuntimeException e) {
            say(s, "usage /mrrw act <1-10> [started, ISO-8601 e.g. 2026-10-17T20:00:00-04:00]");
            return;
        }
        campaign.operatorSet(act, by, started);
        JsonObject d = new JsonObject();
        d.addProperty("act", state.act());
        d.addProperty("by", by);
        journal.write("operator.act", d);
        applyAct();
        campaign.retryEarly();  // a hand-in the players already made for the next act applies now
        say(s, "act " + state.act() + " since " + Campaign.iso(campaign.actStarted())
                + (state.act() != act ? " (an early desk ref moved it on from " + act + ")" : "")
                + " (the website and bot follow it over the bridge; auto-advance " + (campaign.autoAdvance() ? "on" : "off") + ")");
    }

    private void handin(CommandSender s, String by, String[] args) {
        String kind = arg(args, 1, "usage /mrrw handin <" + String.join("|", handinKinds()) + ">").toLowerCase(Locale.ROOT);
        switch (kind) {
            case "maint" -> {
                opJournal("operator.handin", by, "ref", kind);
                say(s, nights.operatorMaint(world()));
            }
            case "lowpath" -> {
                state.markDone("lowpath.bed_kept");
                state.markDone("lowpath.session_ended");
                opJournal("operator.handin", by, "ref", kind);
                say(s, "lowpath's session ended (lowpath.bed_kept, lowpath.session_ended)");
            }
            case "cairn_room" -> {
                nights.openListeningRoom(world(), by);
                say(s, "cairn's room is open");
            }
            default -> {
                if (!handinKinds().contains(kind)) throw new IllegalArgumentException("no hand-in " + kind + "; kinds: " + String.join(" ", handinKinds()));
                say(s, kind + ": " + campaign.handIn(kind, by));
            }
        }
    }

    private void flag(CommandSender s, String by, String[] args) {
        String name = arg(args, 1, "usage /mrrw flag <name> [on|off]");
        String warn = OpsLogic.knownFlag(name) ? "" : " (warning: not a flag the plugin knows; typo? see /mrrw flags)";
        if (args.length < 3) {
            say(s, name + " is " + (state.done(name) ? "on" : "off") + warn);
            return;
        }
        boolean on = switch (args[2].toLowerCase(Locale.ROOT)) {
            case "on", "true", "1" -> true;
            case "off", "false", "0" -> false;
            default -> throw new IllegalArgumentException("usage /mrrw flag <name> on|off");
        };
        if (on) state.markDone(name);
        else state.unmark(name);
        if (!on && name.equals("stopped")) state.unmark("stop_pending");  // else the next restart stops it again
        JsonObject d = new JsonObject();
        d.addProperty("by", by);
        d.addProperty("flag", name);
        d.addProperty("on", on);
        journal.write("operator.flag", d);
        say(s, name + " " + (on ? "on" : "off") + warn + (on ? "" : "; its one-shot runs again at the next act change or /mrrw act " + state.act()));
    }

    private void residents(CommandSender s, String by, String[] args) {
        String op = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        switch (op) {
            case "list" -> {}
            case "add" -> {
                roster.add(arg(args, 2, "usage /mrrw residents add <name>"));
                opJournal("operator.residents", by, "added", args[2]);
            }
            case "remove" -> {
                boolean had = roster.remove(arg(args, 2, "usage /mrrw residents remove <name>"));
                opJournal("operator.residents", by, "removed", args[2]);
                if (!had) say(s, args[2] + " was not on the roster (excluded from now on anyway)");
            }
            case "quorum" -> {
                int q = Math.max(0, Integer.parseInt(arg(args, 2, "usage /mrrw residents quorum <n> (0 = residents seen in 14 days)")));
                getConfig().set("usb_quorum", q);
                saveConfig();
                opJournal("operator.quorum", by, "usb_quorum", String.valueOf(q));
            }
            default -> throw new IllegalArgumentException("usage /mrrw residents [add|remove <name>|quorum <n>]");
        }
        long now = System.currentTimeMillis();
        List<String> lines = new ArrayList<>();
        for (String n : roster.names()) {
            Long seen = roster.lastSeen().get(n);
            lines.add(n + (seen == null ? "" : " (" + ((now - seen) / 3_600_000) + "h ago)"));
        }
        int q = getConfig().getInt("usb_quorum", 0);
        say(s, roster.names().size() + " resident(s): " + String.join(", ", lines)
                + (roster.excluded().isEmpty() ? "" : "; excluded: " + String.join(", ", roster.excluded())));
        say(s, "USB quorum " + Bridge.quorumPlayers(this, roster) + (q > 0 ? " (set by usb_quorum)" : " (residents seen in 14 days)")
                + ": a winner needs ballots from at least half of that");
    }

    private void rollback(CommandSender s, String by, String[] args) throws Exception {
        String name = arg(args, 1, "usage /mrrw rollback <name> (/mrrw snapshots lists them)");
        if (!OpsLogic.snapshots(getDataFolder()).contains(name)) throw new IllegalArgumentException("no snapshot " + name + "; /mrrw snapshots");
        flushForSnapshot();
        String undo = "before-rollback-" + System.currentTimeMillis() / 1000;
        OpsLogic.snapshot(getDataFolder(), undo);
        int from = state.act();
        // the services restart on the restored files, so each reloads them and the act is re-applied cleanly (in place,
        // not through the plugin manager: Paper closes a disabled plugin's class loader)
        onDisable();
        org.bukkit.event.HandlerList.unregisterAll(this);
        getServer().getScheduler().cancelTasks(this);
        List<String> files = OpsLogic.rollback(getDataFolder(), name);
        onEnable();
        List<String> consumed = campaign.consumeAfterRollback();
        JsonObject d = new JsonObject();
        d.addProperty("by", by);
        d.addProperty("snapshot", name);
        d.addProperty("from_act", from);
        d.addProperty("to_act", state.act());
        d.addProperty("undo_with", undo);
        d.addProperty("refs_consumed", String.join(",", consumed));
        journal.write("operator.rollback", d);
        say(s, "rolled back to " + name + " (" + String.join(", ", files) + "): act " + from + " -> " + state.act()
                + (consumed.isEmpty() ? "" : "; desk ref(s) " + String.join(", ", consumed) + " marked used (redo with /mrrw handin)")
                + ". World blocks Morrow already changed stay; undo this with /mrrw rollback " + undo);
    }

    private void tp(CommandSender s, String by, String[] args) {
        String place = arg(args, 1, "usage /mrrw tp <place> [player]; places: " + String.join(" ", canon.anchors.keySet()));
        if (!canon.anchors.containsKey(place)) throw new IllegalArgumentException("no place " + place + "; places: " + String.join(" ", canon.anchors.keySet()));
        Player p = args.length > 2 ? Bukkit.getPlayerExact(args[2]) : s instanceof Player self ? self : null;
        if (p == null) throw new IllegalArgumentException(args.length > 2 ? "no player " + args[2] + " online" : "from the console: /mrrw tp <place> <player>");
        World w = place.equals("nether_hub") ? getServer().getWorlds().stream().filter(x -> x.getEnvironment() == World.Environment.NETHER).findFirst().orElse(world()) : world();
        int[] a = canon.anchor(place);
        int y = a[1];
        for (int dy = 0; dy < 12; dy++) {  // the first spot at or above the anchor with room to stand
            if (w.getBlockAt(a[0], a[1] + dy, a[2]).isPassable() && w.getBlockAt(a[0], a[1] + dy + 1, a[2]).isPassable()) {
                y = a[1] + dy;
                break;
            }
        }
        p.teleport(new Location(w, a[0] + 0.5, y, a[2] + 0.5));
        opJournal("operator.tp", by, "place", place + " (" + p.getName() + ")");
        say(s, p.getName() + " at " + place);
    }

    private void figures(CommandSender s, String by, String[] args) {
        String op = arg(args, 1, "usage /mrrw figures clear|respawn");
        int had = ghosts.running().size();
        ghosts.clearAll();
        if (op.equalsIgnoreCase("clear")) {
            opJournal("operator.figures", by, "op", "clear");
            say(s, had + " figure run(s) cleared; the replays start again at the next dusk / night");
            return;
        }
        if (!op.equalsIgnoreCase("respawn")) throw new IllegalArgumentException("usage /mrrw figures clear|respawn");
        List<String> started = new ArrayList<>();
        if (state.act() >= 5 && !finale.stopped()) {
            replays.incident(world());
            started.add("incident");
        }
        if (replays.lowpathNow(world())) started.add("lowpath");
        opJournal("operator.figures", by, "op", "respawn");
        say(s, had + " cleared; started " + (started.isEmpty() ? "nothing (the replays begin at Act 5, lowpath at Act 8)" : String.join(" + ", started)));
    }

    // ---- tab completion -----------------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("mrrw.operator")) return List.of();
        List<String> opts = switch (args.length) {
            case 1 -> SUBCOMMANDS;
            case 2 -> switch (args[0].toLowerCase(Locale.ROOT)) {
                case "act" -> List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
                case "hint" -> List.of("0", "1", "2", "3", "4");
                case "auto" -> List.of("on", "off");
                case "handin" -> handinKinds();
                case "flag" -> {
                    List<String> f = new ArrayList<>(OpsLogic.FLAGS);
                    for (String x : state.flags()) if (!f.contains(x)) f.add(x);
                    yield f;
                }
                case "journal" -> List.of("10", "20", "50");
                case "residents" -> List.of("list", "add", "remove", "quorum");
                case "rollback" -> OpsLogic.snapshots(getDataFolder());
                case "repair" -> {
                    List<String> p = new ArrayList<>(modern.parts(world()).keySet());
                    p.remove("map_wall");
                    p.add(0, "all");
                    yield p;
                }
                case "restore" -> List.of("signs");
                case "tp" -> new ArrayList<>(canon.anchors.keySet());
                case "figures" -> List.of("clear", "respawn");
                case "replay" -> List.of("sound");
                default -> List.<String>of();
            };
            case 3 -> switch (args[0].toLowerCase(Locale.ROOT)) {
                case "flag" -> List.of("on", "off");
                case "residents" -> args[1].equalsIgnoreCase("remove") ? new ArrayList<>(roster.names())
                        : args[1].equalsIgnoreCase("add") ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.<String>of();
                case "tp" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                default -> List.<String>of();
            };
            default -> List.<String>of();
        };
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        return opts.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(last)).toList();
    }
}
