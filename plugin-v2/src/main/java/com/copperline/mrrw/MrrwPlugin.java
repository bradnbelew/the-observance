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
 *   /mrrw act N          set the act (also set MORROW_ACT on the website)
 *   /mrrw status         act, corpus size, layout, finale state
 *   /mrrw dawn           run the dawn jobs now
 *   /mrrw replay         play the 18 May 2011 incident replay now (Act 5+)
 *   /mrrw stop CODE      Act 10 fallback: enter the console's stop code for the players
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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        canon = Canon.load(getClassLoader());
        state = new State(getDataFolder());
        journal = new Journal(getDataFolder());
        PlayerRoster roster = new PlayerRoster(getDataFolder());
        if (canon.provisionalLayout) getLogger().warning("canon layout is PROVISIONAL; anchors are placeholders until the Beta world is laid out");
        if ("mrrw-dev-stop-secret".equals(getConfig().getString("stop-secret"))) getLogger().warning("stop-secret is the dev default; set it to the website's MORROW_STOP_SECRET before Act 10");

        ghosts = new Ghosts(this);
        PathRecorder recorder = new PathRecorder(this, state);
        finale = new Finale(this, canon, state, journal, ghosts);
        modern = new ModernLayer(this, canon, state, journal);
        recovery = new RecoveryService(this, canon, state, journal);
        corrections = new Corrections(this, canon, state, journal);
        replays = new Replays(this, canon, state, journal, ghosts, recorder);
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PingListener(canon, state, roster, finale), this);
        pm.registerEvents(new ChatListener(this, canon, state, roster, journal), this);
        pm.registerEvents(recovery, this);
        pm.registerEvents(recorder, this);
        pm.registerEvents(new Nights(this, canon, state, journal, recorder, modern), this);
        pm.registerEvents(finale, this);
        pm.registerEvents(new FinchLock(canon), this);
        director = new ActDirector(this, canon, state, journal);
        getServer().getScheduler().runTask(this, () -> {
            modern.reattachMaps();
            applyAct();
        });
        getLogger().info("mrrw ready: act " + state.act() + ", corpus " + canon.corpus.size() + " lines, " + finale.status());
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
        if (director != null) director.stop();
        if (ghosts != null) ghosts.clearAll();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("act")) {
            int act;
            try {
                act = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                return false;
            }
            state.setAct(act);
            JsonObject d = new JsonObject();
            d.addProperty("act", state.act());
            d.addProperty("by", sender.getName());
            journal.write("operator.act", d);
            applyAct();
            sender.sendMessage(Component.text("mrrw: act " + state.act() + " (set MORROW_ACT and MORROW_ACT_STARTED on the website too)"));
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
                        + ", lowpath " + (state.done("lowpath.session_ended") ? "home" : "walking")));
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
