package com.observance.watcher.command;

import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.data.rows.IdentityLinkChallengeRow;
import com.observance.watcher.v5runtime.IdentityLinkCode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Player-only, non-admin command that proves control of the exact online Minecraft UUID. */
public final class IdentityLinkCodeCommand implements CommandExecutor {
    private static final long LOCAL_COOLDOWN_MILLIS = 5_000L;
    private final ObservancePlugin plugin;
    private final Map<UUID, Long> lastAttempt = new ConcurrentHashMap<>();

    public IdentityLinkCodeCommand(ObservancePlugin plugin) {
        this.plugin = java.util.Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This proof can only be requested by the player it names.");
            return true;
        }
        if (!player.hasPermission("observance.link")) {
            player.sendMessage("The record does not accept a hand from here.");
            return true;
        }
        if (args.length != 0) {
            player.sendMessage("Usage: /" + label);
            return true;
        }
        if (!IdentityLinkCode.canIssueForAuthenticatedUuid(plugin.getServer().getOnlineMode())) {
            player.sendMessage("Identity proof is unavailable because this server is not authenticating Minecraft accounts. No code was issued.");
            return true;
        }
        if (plugin.supabase() == null || !plugin.supabase().isConfigured()) {
            player.sendMessage("The handoff ledger is quiet. No code was issued.");
            return true;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long previous = lastAttempt.put(playerId, now);
        if (previous != null && now - previous < LOCAL_COOLDOWN_MILLIS) {
            player.sendMessage("The handoff ledger is still turning that page. Wait a moment.");
            return true;
        }

        String displayCode = IdentityLinkCode.generateDisplayCode();
        String codeHash = IdentityLinkCode.sha256(displayCode);
        plugin.scheduler().runAsyncSafe("identity-link.issue", () -> {
            var result = plugin.supabase().issueIdentityLinkChallenge(playerId.toString(), codeHash);
            IdentityLinkChallengeRow row = result != null && result.ok() ? result.value() : null;
            plugin.scheduler().runMainSafe("identity-link.issue.reply", () -> {
                Player online = plugin.getServer().getPlayer(playerId);
                if (online == null || !online.isOnline()) return;
                if (row == null || row.issueState == null) {
                    online.sendMessage("The handoff ledger did not answer. No code was issued.");
                    return;
                }
                if ("rate_limited".equals(row.issueState)) {
                    online.sendMessage("A handoff code was already issued moments ago. Use that code or wait 30 seconds.");
                    return;
                }
                if (!"issued".equals(row.issueState)) {
                    online.sendMessage("The handoff ledger does not know this exact world identity yet. Rejoin once, then try again.");
                    return;
                }
                online.sendMessage("Your one-time Discord handoff code is: " + displayCode);
                online.sendMessage("It expires in 5 minutes. In Discord use /link with your exact name,"
                        + " the recovered callback, and this code.");
                online.sendMessage("The code is single-use. Do not post it in public chat.");
            });
        });
        return true;
    }
}
