package com.copperline.mrrw;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * mrrw: the Mossfield recovery process, as Iona wrote it in 2011 and Theo kept patching.
 * The operator advances acts with /mrrw act N (console or op). Everything else is automatic.
 */
public final class MrrwPlugin extends JavaPlugin {
    private Canon canon;
    private State state;
    private Journal journal;
    private ActDirector director;
    private RecoveryService recovery;

    @Override
    public void onEnable() {
        canon = Canon.load(getClassLoader());
        state = new State(getDataFolder());
        journal = new Journal(getDataFolder());
        PlayerRoster roster = new PlayerRoster(getDataFolder());
        if (canon.provisionalLayout) getLogger().warning("canon layout is PROVISIONAL; anchors are placeholders until the Beta world is laid out");
        getServer().getPluginManager().registerEvents(new PingListener(canon, state, roster), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, canon, state, roster, journal), this);
        recovery = new RecoveryService(this, canon, state, journal);
        getServer().getPluginManager().registerEvents(recovery, this);
        director = new ActDirector(this, canon, state, journal);
        getServer().getScheduler().runTask(this, director::apply);
        getLogger().info("mrrw ready: act " + state.act() + ", corpus " + canon.corpus.size() + " lines");
    }

    @Override
    public void onDisable() {
        if (director != null) director.stop();
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
            director.apply();
            sender.sendMessage(Component.text("mrrw: act " + state.act() + " (remember to set MORROW_ACT on the website too)"));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(Component.text("mrrw: act " + state.act() + ", corpus " + canon.corpus.size()
                    + " lines, layout " + (canon.provisionalLayout ? "PROVISIONAL" : "final")));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("dawn")) {
            recovery.dawn(getServer().getWorlds().get(0));
            sender.sendMessage(Component.text("mrrw: dawn jobs run"));
            return true;
        }
        return false;
    }
}
