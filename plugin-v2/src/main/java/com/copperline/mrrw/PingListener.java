package com.copperline.mrrw;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * C14, the server-list channel. The MOTD's colour codes are hex nibbles of Morrow's
 * restore queue; the hover list shows residents "online" who are not, and from Act 9 the
 * players themselves, even when they are offline.
 */
public final class PingListener implements Listener {
    private final Canon canon;
    private final State state;
    private final PlayerRoster roster;
    private final Finale finale;

    public PingListener(Canon canon, State state, PlayerRoster roster, Finale finale) {
        this.canon = canon;
        this.state = state;
        this.roster = roster;
        this.finale = finale;
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        int act = state.act();
        event.getListedPlayers().clear();
        if (finale.stopped()) {  // silent: no band, nobody listed
            event.motd(Component.text("Mossfield"));
            event.setNumPlayers((int) Bukkit.getOnlinePlayers().stream().count());
            return;
        }
        String raw = finale.ready() ? canon.string("motd_ready") : canon.motdByAct.getOrDefault(act, canon.motdByAct.get(1));
        Component band = LegacyComponentSerializer.legacySection().deserialize(raw);
        event.motd(Component.text("Mossfield ").append(band));
        for (String name : canon.listedPlayersEarly) add(event, name);
        if (act >= 9) for (String name : roster.names()) add(event, name);
        int online = (int) Bukkit.getOnlinePlayers().stream().filter(Player::isOnline).count();
        event.setNumPlayers(Math.max(online, event.getListedPlayers().size()));
    }

    private static void add(PaperServerListPingEvent event, String name) {
        UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        event.getListedPlayers().add(new PaperServerListPingEvent.ListedPlayerInfo(name, id));
    }
}
