package com.copperline.mrrw;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Acts 7-9: Morrow at night.
 *  Act 7  per-player signs at lowpath's place: each player sees a different front (C, D Act 7).
 *  Act 8  the sound replay in patchcord's hut (D11): sounds, subtitles and real vibrations, in order;
 *         cairn's closing routine opens his sealed room (C16); the presence register (D12) opens the
 *         doors of whoever Morrow thinks is home; unwaxed MAINT signs are reverted at dawn.
 *  Act 9  players count as residents: their doors open, their recovery compass points to where they
 *         first logged in, Morrow labels their spots with pots, and the meadow is redrawn with them.
 */
public final class Nights implements Listener {
    private static final Map<Integer, Sound> SOUND_FOR_FREQUENCY = Map.ofEntries(
            Map.entry(1, Sound.BLOCK_STONE_STEP), Map.entry(2, Sound.ENTITY_GENERIC_SMALL_FALL),
            Map.entry(3, Sound.ITEM_BUCKET_FILL), Map.entry(4, Sound.ITEM_ARMOR_EQUIP_GENERIC),
            Map.entry(5, Sound.ITEM_ARMOR_EQUIP_LEATHER), Map.entry(6, Sound.ENTITY_SHEEP_SHEAR),
            Map.entry(7, Sound.ENTITY_PLAYER_HURT), Map.entry(8, Sound.ENTITY_GENERIC_EAT),
            Map.entry(9, Sound.BLOCK_CHEST_CLOSE), Map.entry(10, Sound.BLOCK_CHEST_OPEN),
            Map.entry(11, Sound.BLOCK_LEVER_CLICK), Map.entry(12, Sound.BLOCK_STONE_BREAK),
            Map.entry(13, Sound.BLOCK_STONE_PLACE), Map.entry(14, Sound.ENTITY_ITEM_FRAME_PLACE),
            Map.entry(15, Sound.ENTITY_PLAYER_DEATH));
    private static final Map<Integer, GameEvent> EVENT_FOR_FREQUENCY = Map.ofEntries(
            Map.entry(1, GameEvent.STEP), Map.entry(2, GameEvent.HIT_GROUND), Map.entry(3, GameEvent.ITEM_INTERACT_FINISH),
            Map.entry(4, GameEvent.UNEQUIP), Map.entry(5, GameEvent.EQUIP), Map.entry(6, GameEvent.SHEAR),
            Map.entry(7, GameEvent.ENTITY_DAMAGE), Map.entry(8, GameEvent.EAT), Map.entry(9, GameEvent.CONTAINER_CLOSE),
            Map.entry(10, GameEvent.CONTAINER_OPEN), Map.entry(11, GameEvent.BLOCK_CHANGE), Map.entry(12, GameEvent.BLOCK_DESTROY),
            Map.entry(13, GameEvent.BLOCK_PLACE), Map.entry(14, GameEvent.ENTITY_PLACE), Map.entry(15, GameEvent.ENTITY_DIE));

    private final Plugin plugin;
    private final Canon canon;
    private final State state;
    private final Journal journal;
    private final PathRecorder recorder;
    private final ModernLayer modern;
    private final Map<UUID, List<String>> routine = new HashMap<>();
    private final List<Location> playerDoors = new ArrayList<>();
    private long lastSoundReplay;
    private long lastRegisterDay = -1;

    public Nights(Plugin plugin, Canon canon, State state, Journal journal, PathRecorder recorder, ModernLayer modern) {
        this.plugin = plugin;
        this.canon = canon;
        this.state = state;
        this.journal = journal;
        this.recorder = recorder;
        this.modern = modern;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 60L, 40L);
    }

    private boolean silent() {
        return state.done("stopped");
    }

    private void tick() {
        if (silent()) return;
        World w = Bukkit.getWorlds().get(0);
        int act = state.act();
        if (act >= 7 && act <= 9) perPlayerSigns(w);
        if (act >= 8) soundReplayIfOccupied(w);
        long time = w.getTime(), day = w.getFullTime() / 24000L;
        if (act >= 3 && time >= 18000 && time < 18200 && day != lastRegisterDay) {
            lastRegisterDay = day;
            presenceRegister(w);
        }
        if (act >= 8 && time < 200) revertUnwaxedMaint(w);
        if (act >= 9) once("act9.meadow_redraw", () -> modern.meadow(w, new ArrayList<>(recorder.firstJoins().values())));
        if (act >= 9) once("act9.pots", () -> recorder.firstJoins().values().forEach(p ->
                Corrections.placePot(w.getBlockAt((int) p[0] + 2, (int) p[1], (int) p[2] + 2), Canon.strings(canon.array("pot_for_players")), null)));
    }

    private void once(String flag, Runnable r) {
        if (state.done(flag)) return;
        r.run();
        state.markDone(flag);
        JsonObject d = new JsonObject();
        d.addProperty("change", flag);
        journal.write("morrow.world_change", d);
    }

    // ---- Act 7: one sign, a different front for each player -----------------------------------

    private void perPlayerSigns(World w) {
        int[] a = canon.anchor("lowpath_place");
        Location signLoc = new Location(w, a[0] + 1, a[1] + 1, a[2] + 3);
        if (!(signLoc.getBlock().getState() instanceof Sign base)) return;
        JsonArray variants = canon.array("per_player_signs");
        List<String> roster = new ArrayList<>(recorder.firstJoins().keySet());
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(signLoc) > 24 * 24) continue;
            int idx = Math.max(0, roster.indexOf(p.getName())) % variants.size();
            List<String> lines = Canon.strings(variants.get(idx).getAsJsonArray());
            Sign view = (Sign) base.copy();
            for (int i = 0; i < 4; i++) view.getSide(Side.FRONT).line(i, Component.text(lines.get(i)));
            p.sendBlockUpdate(signLoc, view);
        }
    }

    // ---- Act 8: the sound of patchcord's last session (D11) -----------------------------------

    private void soundReplayIfOccupied(World w) {
        if (System.currentTimeMillis() - lastSoundReplay < 180_000) return;
        long time = w.getTime();
        if (time < 13000 || time > 23000) return;
        int[] hut = canon.anchor("patchcord_relay_hut");
        Location c = new Location(w, hut[0] + 2, hut[1] + 1, hut[2] + 1);
        if (w.getPlayers().stream().noneMatch(p -> p.getLocation().distanceSquared(c) < 36)) return;
        lastSoundReplay = System.currentTimeMillis();
        JsonArray events = canon.array("sound_replay");
        for (int i = 0; i < events.size(); i++) {
            int f = events.get(i).getAsJsonObject().get("frequency").getAsInt();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                w.playSound(c, SOUND_FOR_FREQUENCY.get(f), 1f, 1f);
                w.sendGameEvent(null, EVENT_FOR_FREQUENCY.get(f), c.toVector());
            }, 40L + i * 30L);
        }
        journal.write("morrow.sound_replay", new JsonObject());
    }

    // ---- Act 8: cairn's closing routine opens his room (C16) ---------------------------------

    private static final List<String> ROUTINE = List.of("lot", "hinge_open", "hinge_close", "chalk", "lamp_off");

    private Block fixture(World w, int dx, int dy, int dz) {
        int[] a = canon.anchor("listening_room");
        return w.getBlockAt(a[0] + dx, a[1] + dy, a[2] + dz);
    }

    private void step(Player p, String what) {
        if (state.act() < 8 || silent() || state.done("act8.listening_room_open")) return;
        List<String> done = routine.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>());
        if (!what.equals(ROUTINE.get(done.size()))) {
            done.clear();
            if (what.equals(ROUTINE.get(0))) done.add(what);
            return;
        }
        done.add(what);
        if (done.size() == ROUTINE.size()) {
            Block door = fixture(p.getWorld(), 2, 1, 0);
            if (door.getBlockData() instanceof Openable o) {
                o.setOpen(true);
                door.setBlockData(o, false);
                if (door.getRelative(0, 1, 0).getBlockData() instanceof Openable top) {
                    top.setOpen(true);
                    door.getRelative(0, 1, 0).setBlockData(top, false);
                }
            }
            p.sendMessage(Component.text("[Recovery] ", NamedTextColor.DARK_GRAY).append(Component.text("welcome back, cairn", NamedTextColor.GRAY)));
            state.markDone("act8.listening_room_open");
            JsonObject d = new JsonObject();
            d.addProperty("by", p.getName());
            journal.write("morrow.listening_room_open", d);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null || e.getHand() == EquipmentSlot.OFF_HAND) return;
        Block b = e.getClickedBlock();
        World w = b.getWorld();
        if (e.getAction() == Action.PHYSICAL && b.getLocation().equals(fixture(w, -4, 0, -2).getLocation())) step(e.getPlayer(), "lot");
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && b.getLocation().equals(fixture(w, 0, 0, -2).getLocation())
                && e.getPlayer().getInventory().getItemInMainHand().getType() == Material.WHITE_DYE) {
            e.setCancelled(true);
            step(e.getPlayer(), "chalk");
        }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && b.getLocation().equals(fixture(w, 1, 1, -2).getLocation())
                && b.getBlockData() instanceof Switch sw && sw.isPowered()) {
            step(e.getPlayer(), "lamp_off");
        }
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p && e.getInventory().getLocation() != null
                && e.getInventory().getLocation().getBlock().getLocation().equals(fixture(p.getWorld(), -2, 0, -2).getLocation())) step(p, "hinge_open");
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p && e.getInventory().getLocation() != null
                && e.getInventory().getLocation().getBlock().getLocation().equals(fixture(p.getWorld(), -2, 0, -2).getLocation())) step(p, "hinge_close");
    }

    // ---- D12: the presence register ----------------------------------------------------------

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (e.getBlock().getType().name().endsWith("_DOOR")) playerDoors.add(e.getBlock().getLocation());
    }

    private void presenceRegister(World w) {
        JsonObject reg = canon.object("door_register").getAsJsonObject(String.valueOf(state.act()));
        List<String> order = Canon.strings(reg.getAsJsonArray("order"));
        JsonArray open = reg.getAsJsonArray("open");
        String[] houseOf = {"rookery_house", "cairn_storehouse_door_2011", "finch_hub", "june_house", "patchcord_relay_hut", "kit_fossil_base", "lowpath_place"};
        for (int i = 0; i < order.size() && i < houseOf.length; i++) {
            boolean isOpen = open.get(i).getAsBoolean() || (order.get(i).equals("lowpath") && state.done("lowpath.session_ended"));
            setDoorNear(w, canon.anchor(houseOf[i]), isOpen);
        }
        if (reg.get("players_doors_open").getAsBoolean()) for (Location l : playerDoors) setDoor(l.getBlock(), true);
    }

    private static void setDoorNear(World w, int[] a, boolean open) {
        for (int dx = -4; dx <= 4; dx++) for (int dy = -1; dy <= 3; dy++) for (int dz = -4; dz <= 4; dz++) {
            Block b = w.getBlockAt(a[0] + dx, a[1] + dy, a[2] + dz);
            if (b.getType().name().endsWith("_DOOR") && !b.getType().name().startsWith("IRON")) {
                setDoor(b, open);
                return;
            }
        }
    }

    private static void setDoor(Block b, boolean open) {
        if (b.getBlockData() instanceof Openable o && o.isOpen() != open) {
            o.setOpen(open);
            b.setBlockData(o, false);
        }
    }

    // ---- Act 8+: Morrow puts the MAINT sign back unless it's waxed -----------------------------

    private void revertUnwaxedMaint(World w) {
        int[] a = canon.anchor("maint_sign");
        if (w.getBlockAt(a[0], a[1], a[2]).getState() instanceof Sign s && !s.isWaxed()) {
            String now = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(s.getSide(Side.FRONT).line(0));
            if (!now.equals("MAINT OFF")) {
                s.getSide(Side.FRONT).line(0, Component.text("MAINT OFF"));
                for (int i = 1; i < 4; i++) s.getSide(Side.FRONT).line(i, Component.empty());
                s.update(true, false);
                journal.write("morrow.maint_reverted", new JsonObject());
            }
        }
    }

    // ---- Act 9: the recovery compass points to where you first came home ----------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (state.act() < 9 || silent()) return;
        double[] fj = recorder.firstJoins().get(e.getPlayer().getName());
        if (fj != null) e.getPlayer().setLastDeathLocation(new Location(e.getPlayer().getWorld(), fj[0], fj[1], fj[2]));
    }
}
