package com.observance.watcher.morrow.presentation;

import com.observance.watcher.morrow.MorrowStage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Main-driven pose, bounds, lifetime, tracking, dependency, and cleanup authority receipt. */
public final class MorrowBodyAuthoritySelfTest {
    private static final Set<String> VANILLA_FALLBACK_BLOCKS = Set.of(
            "minecraft:oxidized_cut_copper",
            "minecraft:cut_copper",
            "minecraft:waxed_copper_block",
            "minecraft:exposed_cut_copper",
            "minecraft:weathered_cut_copper",
            "minecraft:exposed_copper",
            "minecraft:waxed_cut_copper",
            "minecraft:waxed_oxidized_cut_copper",
            "minecraft:waxed_exposed_cut_copper");

    private MorrowBodyAuthoritySelfTest() { }

    public static void main(String[] args) throws Exception {
        check(MorrowBodyAuthority.Pose.values().length == 6, "exactly six authored poses");
        check(MorrowStage.values().length == 6, "every relationship stage maps to one pose");
        check(MorrowBodyAuthority.poseFor(MorrowStage.HELPFUL) == MorrowBodyAuthority.Pose.IDLE,
                "helpful remains terminal-only idle");
        check(!MorrowBodyAuthority.Pose.IDLE.embodied(), "idle does not imply early embodiment");
        check(MorrowBodyAuthority.poseFor(MorrowStage.CURIOUS) == MorrowBodyAuthority.Pose.ATTENDING,
                "curious maps to attending");
        check(MorrowBodyAuthority.poseFor(MorrowStage.INTIMATE) == MorrowBodyAuthority.Pose.MIRRORING,
                "intimate maps to mirroring");
        check(MorrowBodyAuthority.poseFor(MorrowStage.POSSESSIVE) == MorrowBodyAuthority.Pose.RETAINING,
                "possessive maps to retaining");
        check(MorrowBodyAuthority.poseFor(MorrowStage.AFRAID) == MorrowBodyAuthority.Pose.FRACTURED,
                "afraid maps to fractured");
        check(MorrowBodyAuthority.poseFor(MorrowStage.NEGOTIATED) == MorrowBodyAuthority.Pose.NEGOTIATED,
                "negotiated maps to negotiated");

        Set<String> signatures = new HashSet<>();
        for (MorrowBodyAuthority.Pose pose : MorrowBodyAuthority.Pose.values()) {
            var parts = MorrowBodyAuthority.parts(pose);
            check(parts.size() == MorrowBodyAuthority.Part.values().length,
                    pose.key() + " defines all five vanilla parts");
            check(parts.stream().map(MorrowBodyAuthority.PartSpec::part).distinct().count()
                            == MorrowBodyAuthority.Part.values().length,
                    pose.key() + " does not duplicate a part");
            check(parts.stream().allMatch(part -> VANILLA_FALLBACK_BLOCKS.contains(part.blockData())),
                    pose.key() + " uses only the pinned vanilla copper palette");
            String signature = parts.toString();
            check(signatures.add(signature), pose.key() + " geometry is authored rather than aliased");
            check(!pose.accessibleLabel().isBlank() && pose.accessibleLabel().contains("MORROW"),
                    pose.key() + " has a text accessibility label");
        }
        check(MorrowBodyAuthority.MAXIMUM_LIFETIME_SECONDS > 0
                        && MorrowBodyAuthority.MAXIMUM_LIFETIME_SECONDS <= 21_600,
                "fallback entity lifetime is bounded to six hours");
        check(MorrowBodyAuthority.TRACKING_INTERVAL_TICKS >= 10,
                "head tracking update rate is restrained");
        check(MorrowBodyAuthority.MAXIMUM_FOCUS_DISTANCE <= 8.0
                        && MorrowBodyAuthority.MAXIMUM_HEAD_YAW <= 35.0F
                        && MorrowBodyAuthority.MAXIMUM_HEAD_PITCH <= 15.0F,
                "head tracking distance and angles are bounded");
        check(MorrowBodyAuthority.LABEL_SCALE >= 0.15F && MorrowBodyAuthority.LABEL_SCALE <= 0.40F,
                "Morrow label remains readable without filling the first-person view");
        check(MorrowBodyAuthority.TERMINAL_LABEL_Y > 2.0D
                        && Math.abs(MorrowBodyAuthority.TERMINAL_LABEL_Z) <= 0.25D,
                "idle label sits above rather than inside the two-block terminal");
        check(MorrowBodyAuthority.TERMINAL_INTERACTION_Y >= 0.0D
                        && MorrowBodyAuthority.TERMINAL_INTERACTION_Y <= 0.25D
                        && MorrowBodyAuthority.TERMINAL_INTERACTION_Z <= -0.50D,
                "idle interaction volume sits on the spawn-facing terminal surface");

        String bukkit = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/presentation/BukkitMorrowBody.java"),
                StandardCharsets.UTF_8);
        for (String required : new String[]{
                "BlockDisplay", "TextDisplay", "Interaction", "PersistentDataType",
                "cleanupOwned()", "setPersistent(true)", "expiresAtMillis", "trackingTick()",
                "MorrowBodyAuthority.LABEL_SCALE", "MorrowBodyAuthority.TERMINAL_LABEL_Y",
                "MorrowBodyAuthority.TERMINAL_INTERACTION_Z", "setTransformation"}) {
            check(bukkit.contains(required), "Bukkit fallback body missing " + required);
        }
        for (String forbidden : new String[]{"net.minecraft", "craftbukkit", "FakePlayer", "ServerPlayer"}) {
            check(!bukkit.contains(forbidden), "fallback body must not depend on " + forbidden);
        }
        System.out.println("MORROW FALLBACK BODY AUTHORITY: PASS poses=6 parts=5 max_entities=7");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
