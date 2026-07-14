package com.observance.watcher.v5runtime.mechanics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.observance.watcher.v5runtime.FixtureTransform;
import com.observance.watcher.v5runtime.FixtureTransform.BlockPos;
import com.observance.watcher.v5runtime.FixtureTransform.LocalOffset;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex.Binding;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex.SitePose;
import com.observance.watcher.v5runtime.mechanics.MechanicPorts.Trigger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/** Tick-accurate, per-player route, crouch-hold, and sightline state machine. */
public final class BukkitRouteController {
    private static final List<String> ROUTE_NODES = List.of("BI07", "HS06", "KM03");
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private final PhysicalPredicateAuthority authority;
    private final BukkitFixtureIndex fixtures;
    private final BukkitMechanicState live;
    private final V5MechanicsEngine engine;
    private final Map<Key, Cursor> cursors = new HashMap<>();
    private final Map<Key, Integer> sightlineTicks = new HashMap<>();
    private final Map<Key, Integer> crouchTicks = new HashMap<>();

    public BukkitRouteController(
            PhysicalPredicateAuthority authority,
            BukkitFixtureIndex fixtures,
            BukkitMechanicState live,
            V5MechanicsEngine engine) {
        this.authority = Objects.requireNonNull(authority, "authority");
        this.fixtures = Objects.requireNonNull(fixtures, "fixtures");
        this.live = Objects.requireNonNull(live, "live");
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (String nodeId : ROUTE_NODES) {
                tickRoute(player, authority.requireNode(nodeId));
            }
            tickSightline(player);
            tickAltarCrouch(player);
        }
    }

    public boolean operate(Player player, Binding binding) {
        if ("BI07".equals(binding.nodeId())) {
            Key key = new Key(player.getUniqueId(), "BI07");
            Cursor cursor = cursors.get(key);
            if ("outer_lever".equals(binding.componentId())) {
                reset(player.getUniqueId(), "BI07");
                return true;
            }
            if ("inner_lever".equals(binding.componentId())
                    && cursor != null && cursor.awaitingOperation) {
                live.booleanFact(player.getUniqueId(), "BI07",
                        PhysicalPredicateEvaluator.factKey("route_sequence_exact", "route"), true);
                live.operated(player.getUniqueId(), "BI07", "inner_lever");
                live.didNotOperate(player.getUniqueId(), "BI07", "outer_lever");
                engine.evaluate("BI07", player.getUniqueId(), Trigger.ROUTE_COMPLETE);
                reset(player.getUniqueId(), "BI07");
                return true;
            }
        }
        return false;
    }

    public void recordReverseFrameView(Player player, Binding binding) {
        if (!"WR04".equals(binding.nodeId()) || !"rook_marks".equals(binding.componentId())) {
            return;
        }
        Optional<SitePose> pose = fixtures.site(binding.siteId());
        if (pose.isEmpty() || !player.getWorld().getUID().equals(pose.orElseThrow().worldId())) {
            return;
        }
        SitePose site = pose.orElseThrow();
        int deltaX = player.getLocation().getBlockX() - site.origin().x();
        int deltaZ = player.getLocation().getBlockZ() - site.origin().z();
        int forward = deltaX * site.expectedFront().x() + deltaZ * site.expectedFront().z();
        if (forward < 0) {
            live.viewSide(player.getUniqueId(), "WR04", "reverse_face");
        }
    }

    public void resetPlayer(UUID actor) {
        cursors.keySet().removeIf(key -> key.actor.equals(actor));
        sightlineTicks.keySet().removeIf(key -> key.actor.equals(actor));
        crouchTicks.keySet().removeIf(key -> key.actor.equals(actor));
        live.clearPlayer(actor);
    }

    private void tickRoute(Player player, PhysicalPredicateAuthority.Node node) {
        RouteSpec route = route(node);
        Optional<SitePose> poseOptional = fixtures.site(node.siteId());
        if (poseOptional.isEmpty()) {
            return;
        }
        SitePose pose = poseOptional.orElseThrow();
        Key key = new Key(player.getUniqueId(), node.nodeId());
        Cursor cursor = cursors.computeIfAbsent(key, ignored -> new Cursor());
        if (!player.getWorld().getUID().equals(pose.worldId())) {
            reset(player.getUniqueId(), node.nodeId());
            return;
        }
        if (cursor.awaitingOperation) {
            return;
        }
        long now = System.nanoTime();
        if (cursor.deadlineNanos > 0 && now > cursor.deadlineNanos) {
            reset(player.getUniqueId(), node.nodeId());
            return;
        }
        int occupied = occupiedCell(player, pose, route);
        if (occupied < 0) {
            cursor.leftPrevious = true;
            return;
        }
        if (occupied == cursor.index - 1 && !cursor.leftPrevious) {
            return;
        }
        if (occupied != cursor.index) {
            reset(player.getUniqueId(), node.nodeId());
            return;
        }
        RouteCell expected = route.cells.get(cursor.index);
        if (!expected.posture.equals("ANY") && !player.isSneaking()) {
            reset(player.getUniqueId(), node.nodeId());
            return;
        }
        if (expected.holdTicks > 0) {
            cursor.holdTicks++;
            if (cursor.holdTicks < expected.holdTicks) {
                return;
            }
        }
        cursor.index++;
        cursor.holdTicks = 0;
        cursor.leftPrevious = false;
        cursor.deadlineNanos = now + route.timeoutSeconds * NANOS_PER_SECOND;
        if (cursor.index != route.cells.size()) {
            return;
        }
        live.booleanFact(player.getUniqueId(), node.nodeId(),
                PhysicalPredicateEvaluator.factKey("route_sequence_exact", "route"), true);
        if ("BI07".equals(node.nodeId())) {
            cursor.awaitingOperation = true;
            return;
        }
        engine.evaluate(node.nodeId(), player.getUniqueId(), Trigger.ROUTE_COMPLETE);
        reset(player.getUniqueId(), node.nodeId());
    }

    private int occupiedCell(Player player, SitePose pose, RouteSpec route) {
        for (int index = 0; index < route.cells.size(); index++) {
            RouteCell cell = route.cells.get(index);
            BlockPos world = FixtureTransform.toWorld(pose.origin(), pose.expectedFront(), cell.offset);
            double centerX = world.x() + 0.5;
            double centerZ = world.z() + 0.5;
            Location location = player.getLocation();
            if (Math.abs(location.getX() - centerX) > route.tolerance
                    || Math.abs(location.getZ() - centerZ) > route.tolerance
                    || Math.abs(location.getY() - (world.y() + 1.0)) > 0.45) {
                continue;
            }
            if (cell.block != null && player.getWorld().getBlockAt(
                    world.x(), world.y(), world.z()).getType() != cell.block) {
                return -2;
            }
            return index;
        }
        return -1;
    }

    private void tickSightline(Player player) {
        PhysicalPredicateAuthority.Node node = authority.requireNode("KO01");
        Optional<SitePose> poseOptional = fixtures.site(node.siteId());
        Key key = new Key(player.getUniqueId(), node.nodeId());
        if (poseOptional.isEmpty() || !validSightline(player, node, poseOptional.orElseThrow())) {
            sightlineTicks.remove(key);
            live.clearNode(player.getUniqueId(), node.nodeId());
            return;
        }
        int ticks = sightlineTicks.merge(key, 1, Integer::sum);
        live.booleanFact(player.getUniqueId(), "KO01",
                PhysicalPredicateEvaluator.factKey("player_in_cell", "view_cell"), true);
        live.stringFact(player.getUniqueId(), "KO01", "player_posture", "SNEAKING");
        live.stringList(player.getUniqueId(), "KO01", "ray_intersects_mark_sequence",
                List.of("N", "E", "S", "W", "N", "E"));
        live.integerFact(player.getUniqueId(), "KO01", "continuous_ticks", ticks);
        if (ticks == 20) {
            engine.evaluate("KO01", player.getUniqueId(), Trigger.SIGHTLINE_TIMER);
        }
    }

    private boolean validSightline(
            Player player, PhysicalPredicateAuthority.Node node, SitePose pose) {
        if (!player.getWorld().getUID().equals(pose.worldId()) || !player.isSneaking()) {
            return false;
        }
        JsonObject predicate = JsonParser.parseString(node.predicate().canonicalJson()).getAsJsonObject();
        Map<String, JsonObject> components = componentMap(predicate);
        JsonObject view = components.get("view_cell");
        LocalOffset viewOffset = offset(view.getAsJsonArray("offset"));
        BlockPos viewWorld = FixtureTransform.toWorld(pose.origin(), pose.expectedFront(), viewOffset);
        Location feet = player.getLocation();
        if (Math.abs(feet.getX() - (viewWorld.x() + 0.5)) > 0.35
                || Math.abs(feet.getZ() - (viewWorld.z() + 0.5)) > 0.35
                || Math.abs(feet.getY() - (viewWorld.y() + 1.0)) > 0.45) {
            return false;
        }
        double relativeEye = player.getEyeLocation().getY() - feet.getY();
        JsonArray eyeRange = view.getAsJsonArray("eye_y_range");
        if (relativeEye < eyeRange.get(0).getAsDouble()
                || relativeEye > eyeRange.get(1).getAsDouble()) {
            return false;
        }
        if (!marksExact(node)) {
            return false;
        }
        JsonObject target = components.get("target");
        BlockPos targetBlock = FixtureTransform.toWorld(pose.origin(), pose.expectedFront(),
                offset(target.getAsJsonArray("offset")));
        Location eye = player.getEyeLocation();
        Vector desired = new Vector(targetBlock.x() + 0.5 - eye.getX(),
                targetBlock.y() + 0.5 - eye.getY(), targetBlock.z() + 0.5 - eye.getZ());
        double distance = desired.length();
        if (distance <= 0.001) {
            return false;
        }
        desired.normalize();
        Vector actual = eye.getDirection().normalize();
        double desiredYaw = Math.toDegrees(Math.atan2(-desired.getX(), desired.getZ()));
        double actualYaw = Math.toDegrees(Math.atan2(-actual.getX(), actual.getZ()));
        double yawError = angleDifference(desiredYaw, actualYaw);
        double desiredPitch = Math.toDegrees(-Math.asin(desired.getY()));
        double actualPitch = Math.toDegrees(-Math.asin(actual.getY()));
        if (yawError > target.get("max_yaw_error_degrees").getAsDouble()
                || Math.abs(desiredPitch - actualPitch)
                        > target.get("max_pitch_error_degrees").getAsDouble()) {
            return false;
        }
        RayTraceResult hit = player.getWorld().rayTraceBlocks(
                eye, desired, distance, FluidCollisionMode.NEVER, true);
        return hit == null || hit.getHitBlock() != null
                && hit.getHitBlock().getX() == targetBlock.x()
                && hit.getHitBlock().getY() == targetBlock.y()
                && hit.getHitBlock().getZ() == targetBlock.z();
    }

    private boolean marksExact(PhysicalPredicateAuthority.Node node) {
        List<Binding> bindings = fixtures.bindings(node.nodeId(), "marks");
        List<String> values = List.of("N", "E", "S", "W", "N", "E");
        List<Integer> rotations = List.of(0, 2, 4, 6, 0, 2);
        if (bindings.size() != values.size()) {
            return false;
        }
        for (int index = 0; index < bindings.size(); index++) {
            Binding binding = bindings.get(index);
            World world = Bukkit.getWorld(binding.worldId());
            Entity entity = world == null ? null : binding.entityId().map(world::getEntity).orElse(null);
            if (!(entity instanceof ItemFrame frame) || !fixtures.verify(binding, frame)
                    || frame.getRotation().ordinal() != rotations.get(index)
                    || !binding.expectedPdc().containsValue(values.get(index))) {
                return false;
            }
        }
        return true;
    }

    private void tickAltarCrouch(Player player) {
        PhysicalPredicateAuthority.Node node = authority.requireNode("AR05");
        Optional<SitePose> poseOptional = fixtures.site(node.siteId());
        Key key = new Key(player.getUniqueId(), node.nodeId());
        if (poseOptional.isEmpty() || !player.isSneaking()
                || !inAltarCell(player, poseOptional.orElseThrow())) {
            crouchTicks.remove(key);
            live.booleanFact(player.getUniqueId(), "AR05",
                    PhysicalPredicateEvaluator.factKey(
                            "player_in_cell_posture_for_ticks", "crouch_cell"), false);
            live.integerFact(player.getUniqueId(), "AR05",
                    PhysicalPredicateEvaluator.factKey("hold_ticks", "crouch_cell"), 0);
            return;
        }
        int ticks = crouchTicks.merge(key, 1, Integer::sum);
        live.booleanFact(player.getUniqueId(), "AR05",
                PhysicalPredicateEvaluator.factKey(
                        "player_in_cell_posture_for_ticks", "crouch_cell"), ticks >= 10);
        live.integerFact(player.getUniqueId(), "AR05",
                PhysicalPredicateEvaluator.factKey("hold_ticks", "crouch_cell"), ticks);
    }

    private boolean inAltarCell(Player player, SitePose pose) {
        JsonObject predicate = JsonParser.parseString(authority.requireNode("AR05")
                .predicate().canonicalJson()).getAsJsonObject();
        JsonObject cell = componentMap(predicate).get("crouch_cell");
        BlockPos world = FixtureTransform.toWorld(
                pose.origin(), pose.expectedFront(), offset(cell.getAsJsonArray("offset")));
        Location location = player.getLocation();
        return player.getWorld().getUID().equals(pose.worldId())
                && Math.abs(location.getX() - (world.x() + 0.5)) <= 0.35
                && Math.abs(location.getZ() - (world.z() + 0.5)) <= 0.35
                && Math.abs(location.getY() - (world.y() + 1.0)) <= 0.45;
    }

    private RouteSpec route(PhysicalPredicateAuthority.Node node) {
        JsonObject predicate = JsonParser.parseString(node.predicate().canonicalJson()).getAsJsonObject();
        JsonObject route = componentMap(predicate).get("route");
        List<RouteCell> cells = new ArrayList<>();
        for (JsonElement cellElement : route.getAsJsonArray("cells")) {
            JsonObject cell = cellElement.getAsJsonObject();
            cells.add(new RouteCell(
                    offset(cell.getAsJsonArray("offset")),
                    cell.get("posture").getAsString(),
                    cell.has("hold_ticks") ? cell.get("hold_ticks").getAsInt() : 0,
                    cell.has("block") ? Material.valueOf(cell.get("block").getAsString()) : null));
        }
        return new RouteSpec(List.copyOf(cells), route.get("cell_tolerance").getAsDouble(),
                route.get("step_timeout_seconds").getAsInt());
    }

    private void reset(UUID actor, String nodeId) {
        cursors.remove(new Key(actor, nodeId));
        live.clearNode(actor, nodeId);
    }

    private static Map<String, JsonObject> componentMap(JsonObject predicate) {
        Map<String, JsonObject> result = new HashMap<>();
        predicate.getAsJsonArray("components").forEach(value -> {
            JsonObject component = value.getAsJsonObject();
            result.put(component.get("id").getAsString(), component);
        });
        return result;
    }

    private static LocalOffset offset(JsonArray values) {
        return new LocalOffset(
                values.get(0).getAsInt(), values.get(1).getAsInt(), values.get(2).getAsInt());
    }

    private static double angleDifference(double left, double right) {
        return Math.abs((left - right + 540.0) % 360.0 - 180.0);
    }

    private record Key(UUID actor, String nodeId) {
    }

    private static final class Cursor {
        private int index;
        private int holdTicks;
        private long deadlineNanos;
        private boolean leftPrevious = true;
        private boolean awaitingOperation;
    }

    private record RouteSpec(List<RouteCell> cells, double tolerance, int timeoutSeconds) {
    }

    private record RouteCell(LocalOffset offset, String posture, int holdTicks, Material block) {
    }
}
