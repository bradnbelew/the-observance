package com.observance.watcher.morrow;

import java.util.LinkedHashMap;
import java.util.Map;

/** Dependency-free boundary tests for authored-copper stability. */
public final class CopperAgingPolicySelfTest {
    private CopperAgingPolicySelfTest() { }

    public static void main(String[] args) {
        check(CopperAgingPolicy.isNaturalAging(
                "minecraft:cut_copper", "minecraft:exposed_cut_copper"),
                "cut copper may naturally expose");
        check(CopperAgingPolicy.isNaturalAging(
                "minecraft:copper_bulb[lit=true,powered=false]",
                "minecraft:weathered_copper_bulb[lit=true,powered=false]"),
                "block properties do not hide a natural family transition");
        check(!CopperAgingPolicy.isNaturalAging(
                "minecraft:weathered_copper", "minecraft:exposed_copper"),
                "scraping to an earlier stage is not ambient aging");
        check(!CopperAgingPolicy.isNaturalAging(
                "minecraft:waxed_cut_copper", "minecraft:exposed_cut_copper"),
                "waxed provenance is never classified as ambient aging");
        check(!CopperAgingPolicy.isNaturalAging(
                "minecraft:cut_copper", "minecraft:dirt"),
                "foreign drift is never normalized");
        check(CopperAgingPolicy.isWeatherable("minecraft:chiseled_copper"),
                "unwaxed authored copper is protected");
        check(!CopperAgingPolicy.isWeatherable("minecraft:oxidized_chiseled_copper"),
                "final oxidation stage cannot age further");
        Map<String, String> manifest = new LinkedHashMap<>();
        manifest.put("authored", "minecraft:copper_grate");
        manifest.put("foreign", "minecraft:cut_copper");
        Map<String, String> world = new LinkedHashMap<>();
        world.put("authored", "minecraft:exposed_copper_grate[waterlogged=false]");
        world.put("foreign", "minecraft:waxed_cut_copper");
        int repairs = CopperAgingPolicy.restoreNaturalAging(
                manifest, world::get, world::put);
        check(repairs == 1 && "minecraft:copper_grate".equals(world.get("authored")),
                "receipt-bound restoration repairs natural aging");
        check("minecraft:waxed_cut_copper".equals(world.get("foreign")),
                "receipt-bound restoration leaves foreign drift untouched");
        System.out.println("MORROW COPPER AGING POLICY: PASS families=5 foreign-drift=refused");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
