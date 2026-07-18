package com.observance.watcher.structure;

import org.bukkit.block.BlockFace;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Dependency-free layout contract for the bounded Unlit village candidate. */
public final class UnlitVillageCandidatePlanSelfTest {
    public static void main(String[] args) {
        List<UnlitVillageCandidateBuilder.House> houses = UnlitVillageCandidateBuilder.houses();
        UnlitVillageCandidateBuilder.validatePlan(houses);
        require(houses.size() == 8, "seven houses plus base required");
        require(houses.stream().map(UnlitVillageCandidateBuilder.House::siteId).distinct().count() == 8,
                "site ids must be unique");
        require(houses.stream().allMatch(house -> Set.of(
                BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST).contains(house.front())),
                "all house fronts must be cardinal");
        require(houses.stream().allMatch(house -> Math.abs(house.doorX()) <= UnlitVillageCandidateBuilder.BUILD_RADIUS
                && Math.abs(house.doorZ()) <= UnlitVillageCandidateBuilder.BUILD_RADIUS),
                "every threshold must stay inside the audited border");
        Set<String> palettes = new HashSet<>();
        houses.forEach(house -> palettes.add(house.wall() + "/" + house.trim() + "/" + house.roof()));
        require(palettes.size() == 8, "each workplace needs a distinct structural palette");
        require(UnlitVillageCandidateBuilder.BORDER_RADIUS > UnlitVillageCandidateBuilder.BUILD_RADIUS,
                "border needs a recovery margin outside the authored build");
        require(UnlitVillageCandidateBuilder.yaw(BlockFace.SOUTH) == 0f
                        && UnlitVillageCandidateBuilder.yaw(BlockFace.WEST) == 90f
                        && UnlitVillageCandidateBuilder.yaw(BlockFace.NORTH) == 180f
                        && UnlitVillageCandidateBuilder.yaw(BlockFace.EAST) == 270f,
                "stored fixture yaw must match Observance cardinal orientation");
        System.out.println("UnlitVillageCandidatePlanSelfTest PASS houses=8 palettes=8 bounded=true");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
