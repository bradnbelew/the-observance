package com.observance.watcher.command;

import com.google.gson.JsonObject;
import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.util.Safety;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /observance} admin command. Read-only status + safe controls (reload, local sleep
 * toggle). The body is wrapped in Safety so a bad command never propagates an exception.
 */
public final class ObservanceCommand implements CommandExecutor, TabCompleter {

    private final ObservancePlugin plugin;
    private final Safety safety;

    public ObservanceCommand(ObservancePlugin plugin, Safety safety) {
        this.plugin = plugin;
        this.safety = safety;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        safety.run("command.observance", () -> handle(sender, args));
        return true;
    }

    private void handle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("observance.admin")) {
            sender.sendMessage("You do not have permission.");
            return;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "status";
        switch (sub) {
            case "status" -> sendStatus(sender);
            case "reload" -> {
                boolean ok = plugin.reloadAll();
                sender.sendMessage(ok ? "Observance: config + sites reloaded."
                        : "Observance: reload hit an error (see console/event_log).");
            }
            case "sleep" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /observance sleep <on|off>  (currently "
                            + (plugin.isLocallyAsleep() ? "on" : "off") + ")");
                    return;
                }
                String v = args[1].toLowerCase(Locale.ROOT);
                boolean on = v.equals("on") || v.equals("true") || v.equals("1");
                plugin.setLocallyAsleep(on);
                sender.sendMessage("Observance: local watcher-sleep " + (on ? "ENABLED (muted)" : "disabled"));
            }
            case "flag" -> handleFlag(sender, args);
            default -> sender.sendMessage("Unknown subcommand. Use: status | reload | sleep <on|off> | flag <set|clear|list>");
        }
    }

    /**
     * {@code /observance flag <set|clear|list> [key] [true|false]} — the storylet-gate admin control
     * (OVERHAUL.md §6 Phase 1). Sets/clears a key in {@code arc_state.flags} via the atomic merge RPC
     * so a tester can OPEN a gated branch (e.g. {@code iss_caught}) to prove gating in-world before the
     * flag producers are all built, or read the current flags. The blocking Supabase I/O runs async;
     * the reply is scheduled back on the main thread.
     */
    private void handleFlag(CommandSender sender, String[] args) {
        var sb = plugin.supabase();
        if (sb == null) {
            sender.sendMessage("Observance: supabase unavailable.");
            return;
        }
        String op = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        switch (op) {
            case "set", "clear" -> {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /observance flag " + op + " <key>"
                            + (op.equals("set") ? " [true|false]" : ""));
                    return;
                }
                String key = args[2];
                // clear → false; set → true unless an explicit false/0 is given.
                boolean val = op.equals("set")
                        && !(args.length >= 4 && (args[3].equalsIgnoreCase("false") || args[3].equals("0")));
                JsonObject flags = new JsonObject();
                flags.addProperty(key, val);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    sb.mergeArcFlags(flags);
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage("Observance: flag '" + key + "' = " + val + " (merged into arc_state.flags)."));
                });
            }
            case "list" -> Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                var r = sb.fetchArcState();
                Map<String, Object> map = (r.ok() && r.value() != null)
                        ? r.value().flagsMap() : Collections.emptyMap();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("== arc_state.flags ==");
                    if (map.isEmpty()) {
                        sender.sendMessage(" (none set)");
                    } else {
                        map.forEach((k, v) -> sender.sendMessage(" " + k + " = " + v));
                    }
                });
            });
            default -> sender.sendMessage("Usage: /observance flag <set|clear|list> [key] [true|false]");
        }
    }

    private void sendStatus(CommandSender sender) {
        var sb = plugin.supabase();
        boolean configured = sb != null && sb.isConfigured();
        sender.sendMessage("== The Observance ==");
        sender.sendMessage(" supabase configured: " + configured);
        if (sb != null) {
            sender.sendMessage(" last db call ok:    " + sb.lastCallSucceeded());
            sender.sendMessage(" queued writes:      " + sb.queuedWriteCount());
        }
        sender.sendMessage(" local sleep:        " + plugin.isLocallyAsleep());
        sender.sendMessage(" sites placed:       " + plugin.placedSiteCount());
        sender.sendMessage(" drama enabled:      " + (plugin.config() != null && plugin.config().dramaEnabled()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : new String[]{"status", "reload", "sleep", "flag"}) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("sleep")) {
            out.add("on");
            out.add("off");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("flag")) {
            for (String s : new String[]{"set", "clear", "list"}) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        }
        return out;
    }
}
