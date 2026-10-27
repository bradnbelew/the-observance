package com.copperline.mrrw;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * mrrw: the Mossfield recovery process, as Iona wrote it in 2011 and Theo kept patching.
 *
 * Operator commands (console or op):
 *   /mrrw install        build the Theo/Morrow-era layer on the upgraded world (once, idempotent)
 *   /mrrw act N [ISO]    set the act (the operator always wins over auto-advance); optional ISO-8601
 *                        start time for it (default now). The website and bot follow over the bridge.
 *   /mrrw status         act, corpus size, layout, finale state, stop-secret fingerprint
 *   /mrrw dawn           run the dawn jobs now
 *   /mrrw replay         play the 18 May 2011 incident replay now (Act 5+)
 *   /mrrw replay sound   play patchcord's sound replay (D11) in his hut now (rehearsal / checks)
 *   /mrrw reload         re-read config.yml (stop-secret, skins) without a restart
 *   /mrrw stop CODE      Act 10 fallback: enter the console's stop code for the players
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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        canon = Canon.load(getClassLoader());
        state = new State(getDataFolder());
        journal = new Journal(getDataFolder());
        PlayerRoster roster = new PlayerRoster(getDataFolder());
        if (canon.provisionalLayout) getLogger().warning("canon layout is PROVISIONAL; anchors are placeholders until the Beta world is laid out");
        warnDevSecret();

        ghosts = new Ghosts(this);
        PathRecorder recorder = new PathRecorder(this, state);
        finale = new Finale(this, canon, state, journal, ghosts, roster);
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
        nights = new Nights(this, canon, state, journal, recorder, modern);
        campaign = new Campaign(this, state, journal, this::applyAct);
        recovery.onDawn(w -> campaign.onDawn());
        ChatListener chat = new ChatListener(this, canon, state, roster, journal);
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PingListener(canon, state, roster, finale), this);
        pm.registerEvents(chat, this);
        pm.registerEvents(campaign, this);
        pm.registerEvents(recovery, this);
        pm.registerEvents(recorder, this);
        pm.registerEvents(nights, this);
        pm.registerEvents(fixtures, this);
        pm.registerEvents(finale, this);
        pm.registerEvents(new FinchLock(canon), this);
        director = new ActDirector(this, canon, state, journal);
        finale.director(director);
        bridge = new Bridge(this, state, canon, roster, finale, campaign, chat, journal);
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

    private void applyAct() {
        if (state.done("stopped")) return;
        director.apply();
        corrections.apply(world());
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ((args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("act")) {
            int act;
            long started = System.currentTimeMillis();
            try {
                act = Integer.parseInt(args[1]);
                if (args.length == 3) started = Campaign.parseIso(args[2]);
            } catch (RuntimeException e) {
                sender.sendMessage(Component.text("mrrw: usage /mrrw act <1-10> [started, ISO-8601 e.g. 2026-10-17T20:00:00-04:00]"));
                return true;
            }
            campaign.operatorSet(act, sender.getName(), started);
            JsonObject d = new JsonObject();
            d.addProperty("act", state.act());
            d.addProperty("by", sender.getName());
            journal.write("operator.act", d);
            applyAct();
            sender.sendMessage(Component.text("mrrw: act " + state.act() + " since " + Campaign.iso(started)
                    + " (the website and bot follow it over the bridge; auto-advance " + (campaign.autoAdvance() ? "on" : "off") + ")"));
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("replay") && args[1].equalsIgnoreCase("sound")) {
            var played = nights.soundReplay(world());
            sender.sendMessage(Component.text("mrrw: sound replay started, " + played.size() + " events "
                    + Vibrations.SPACING_TICKS + " ticks apart, decodes to " + Vibrations.decode(played)));
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stop")) {
            sender.sendMessage(Component.text("mrrw: " + finale.operatorStop(args[1])));
            return true;
        }
        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "status" -> sender.sendMessage(Component.text("mrrw: act " + state.act() + ", corpus " + canon.corpus.size()
                        + " lines, layout " + (canon.provisionalLayout ? "PROVISIONAL" : "final") + ", " + finale.status()
                        + ", lowpath " + (state.done("lowpath.session_ended") ? "home" : "walking")
                        + ", stop-secret fingerprint " + finale.fingerprint() + (finale.devSecret() ? " (DEV DEFAULT: stop and bridge refused)" : "")
                        + ", act started " + Campaign.iso(campaign.actStarted()) + ", auto-advance " + (campaign.autoAdvance() ? "on" : "off")
                        + ", usb " + bridge.usb().json()));
                case "reload" -> {
                    reloadConfig();
                    warnDevSecret();
                    JsonObject d = new JsonObject();
                    d.addProperty("by", sender.getName());
                    d.addProperty("fingerprint", finale.fingerprint());
                    journal.write("operator.reload", d);
                    sender.sendMessage(Component.text("mrrw: config reloaded, stop-secret fingerprint " + finale.fingerprint()
                            + (finale.devSecret() ? " (DEV DEFAULT: stop refused)" : "") + "; skins apply to the next figures spawned"));
                }
                case "dawn" -> {
                    recovery.dawn(world());
                    sender.sendMessage(Component.text("mrrw: dawn jobs run"));
                }
                case "install" -> sender.sendMessage(Component.text("mrrw: installed " + modern.install(world())));
                case "replay" -> {
                    replays.incident(world());
                    sender.sendMessage(Component.text("mrrw: incident replay started"));
                }
                default -> {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
