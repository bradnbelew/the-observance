package com.observance.watcher.morrow.presentation;

import com.observance.watcher.morrow.MorrowStage;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure authored pose/state authority for Morrow's vanilla fallback form. */
public final class MorrowBodyAuthority {
    public static final long MAXIMUM_LIFETIME_SECONDS = 21_600L;
    public static final long ACTIVE_SPEAKER_SECONDS = 12L;
    public static final long TRACKING_INTERVAL_TICKS = 10L;
    public static final double MAXIMUM_FOCUS_DISTANCE = 8.0D;
    public static final float MAXIMUM_HEAD_YAW = 35.0F;
    public static final float MAXIMUM_HEAD_PITCH = 15.0F;
    public static final float LABEL_SCALE = 0.30F;
    public static final double TERMINAL_LABEL_Y = 2.20D;
    public static final double TERMINAL_LABEL_Z = -0.10D;
    public static final double TERMINAL_INTERACTION_Y = 0.10D;
    public static final double TERMINAL_INTERACTION_Z = -0.56D;

    public enum Pose {
        IDLE("idle", false, "MORROW // TERMINAL"),
        ATTENDING("attending", true, "MORROW // ATTENDING"),
        MIRRORING("mirroring", true, "MORROW // MIRRORING"),
        RETAINING("retaining", true, "MORROW // RETAINING"),
        FRACTURED("fractured", true, "MORROW // FRACTURED"),
        NEGOTIATED("negotiated", true, "MORROW // NEGOTIATED");

        private final String key;
        private final boolean embodied;
        private final String accessibleLabel;

        Pose(String key, boolean embodied, String accessibleLabel) {
            this.key = key;
            this.embodied = embodied;
            this.accessibleLabel = accessibleLabel;
        }

        public String key() { return key; }
        public boolean embodied() { return embodied; }
        public String accessibleLabel() { return accessibleLabel; }
    }

    public enum Part { BASE, TORSO, HEAD, LEFT_ARM, RIGHT_ARM }

    public record PartSpec(
            Part part,
            String blockData,
            double x,
            double y,
            double z,
            float scaleX,
            float scaleY,
            float scaleZ) {
        public PartSpec {
            Objects.requireNonNull(part, "part");
            if (blockData == null || !blockData.matches("minecraft:[a-z0-9_]+")) {
                throw new IllegalArgumentException("invalid fallback block data");
            }
            if (scaleX <= 0 || scaleY <= 0 || scaleZ <= 0
                    || scaleX > 2 || scaleY > 2 || scaleZ > 2) {
                throw new IllegalArgumentException("fallback body scale is out of bounds");
            }
            if (Math.abs(x) > 2 || y < 0 || y > 3 || Math.abs(z) > 2) {
                throw new IllegalArgumentException("fallback body part is out of bounds");
            }
        }
    }

    private static final Map<MorrowStage, Pose> STAGE_POSES = Map.of(
            MorrowStage.HELPFUL, Pose.IDLE,
            MorrowStage.CURIOUS, Pose.ATTENDING,
            MorrowStage.INTIMATE, Pose.MIRRORING,
            MorrowStage.POSSESSIVE, Pose.RETAINING,
            MorrowStage.AFRAID, Pose.FRACTURED,
            MorrowStage.NEGOTIATED, Pose.NEGOTIATED);

    private static final Map<Pose, List<PartSpec>> POSES = poses();

    private MorrowBodyAuthority() { }

    public static Pose poseFor(MorrowStage stage) {
        Pose pose = STAGE_POSES.get(Objects.requireNonNull(stage, "stage"));
        if (pose == null) throw new IllegalArgumentException("missing authored Morrow pose");
        return pose;
    }

    public static List<PartSpec> parts(Pose pose) {
        List<PartSpec> parts = POSES.get(Objects.requireNonNull(pose, "pose"));
        if (parts == null) throw new IllegalArgumentException("missing authored pose geometry");
        return parts;
    }

    private static Map<Pose, List<PartSpec>> poses() {
        EnumMap<Pose, List<PartSpec>> poses = new EnumMap<>(Pose.class);
        poses.put(Pose.IDLE, standard(0.0, -0.08, 0.08, 0.08));
        poses.put(Pose.ATTENDING, standard(0.0, 0.04, -0.04, 0.04));
        poses.put(Pose.MIRRORING, List.of(
                part(Part.BASE, "minecraft:oxidized_cut_copper", -0.42, 0.00, -0.30, 0.84F, 0.18F, 0.60F),
                part(Part.TORSO, "minecraft:cut_copper", -0.34, 0.25, -0.24, 0.68F, 1.05F, 0.48F),
                part(Part.HEAD, "minecraft:waxed_copper_block", -0.28, 1.43, -0.28, 0.56F, 0.56F, 0.56F),
                part(Part.LEFT_ARM, "minecraft:exposed_cut_copper", -0.76, 0.43, -0.18, 0.28F, 0.86F, 0.34F),
                part(Part.RIGHT_ARM, "minecraft:exposed_cut_copper", 0.48, 0.43, -0.18, 0.28F, 0.86F, 0.34F)));
        poses.put(Pose.RETAINING, List.of(
                part(Part.BASE, "minecraft:oxidized_cut_copper", -0.48, 0.00, -0.36, 0.96F, 0.22F, 0.72F),
                part(Part.TORSO, "minecraft:cut_copper", -0.38, 0.24, -0.28, 0.76F, 1.16F, 0.56F),
                part(Part.HEAD, "minecraft:waxed_copper_block", -0.30, 1.50, -0.30, 0.60F, 0.60F, 0.60F),
                part(Part.LEFT_ARM, "minecraft:exposed_cut_copper", -0.86, 0.35, -0.30, 0.34F, 1.02F, 0.40F),
                part(Part.RIGHT_ARM, "minecraft:exposed_cut_copper", 0.52, 0.35, -0.30, 0.34F, 1.02F, 0.40F)));
        poses.put(Pose.FRACTURED, List.of(
                part(Part.BASE, "minecraft:oxidized_cut_copper", -0.52, 0.00, -0.34, 0.78F, 0.18F, 0.58F),
                part(Part.TORSO, "minecraft:weathered_cut_copper", -0.22, 0.36, -0.18, 0.64F, 0.92F, 0.46F),
                part(Part.HEAD, "minecraft:exposed_copper", -0.52, 1.54, -0.38, 0.52F, 0.52F, 0.52F),
                part(Part.LEFT_ARM, "minecraft:oxidized_cut_copper", -1.04, 0.60, -0.38, 0.24F, 0.72F, 0.30F),
                part(Part.RIGHT_ARM, "minecraft:waxed_cut_copper", 0.62, 0.20, -0.04, 0.26F, 0.82F, 0.32F)));
        poses.put(Pose.NEGOTIATED, List.of(
                part(Part.BASE, "minecraft:waxed_oxidized_cut_copper", -0.42, 0.00, -0.30, 0.84F, 0.18F, 0.60F),
                part(Part.TORSO, "minecraft:waxed_copper_block", -0.34, 0.25, -0.24, 0.68F, 1.02F, 0.48F),
                part(Part.HEAD, "minecraft:waxed_copper_block", -0.28, 1.39, -0.28, 0.56F, 0.56F, 0.56F),
                part(Part.LEFT_ARM, "minecraft:waxed_exposed_cut_copper", -0.72, 0.50, -0.16, 0.26F, 0.72F, 0.32F),
                part(Part.RIGHT_ARM, "minecraft:waxed_exposed_cut_copper", 0.46, 0.50, -0.16, 0.26F, 0.72F, 0.32F)));
        if (poses.size() != Pose.values().length) throw new IllegalStateException("six authored body poses are required");
        poses.forEach((pose, parts) -> {
            if (parts.size() != Part.values().length
                    || parts.stream().map(PartSpec::part).distinct().count() != Part.values().length) {
                throw new IllegalStateException("pose must define every fallback body part: " + pose.key());
            }
        });
        return Map.copyOf(poses);
    }

    private static List<PartSpec> standard(double headX, double headY, double leftZ, double rightZ) {
        return List.of(
                part(Part.BASE, "minecraft:oxidized_cut_copper", -0.42, 0.00, -0.30, 0.84F, 0.18F, 0.60F),
                part(Part.TORSO, "minecraft:cut_copper", -0.34, 0.25, -0.24, 0.68F, 1.05F, 0.48F),
                part(Part.HEAD, "minecraft:waxed_copper_block", -0.28 + headX, 1.43 + headY, -0.28, 0.56F, 0.56F, 0.56F),
                part(Part.LEFT_ARM, "minecraft:exposed_cut_copper", -0.76, 0.43, -0.18 + leftZ, 0.28F, 0.86F, 0.34F),
                part(Part.RIGHT_ARM, "minecraft:exposed_cut_copper", 0.48, 0.43, -0.18 + rightZ, 0.28F, 0.86F, 0.34F));
    }

    private static PartSpec part(
            Part part, String blockData, double x, double y, double z,
            float scaleX, float scaleY, float scaleZ) {
        return new PartSpec(part, blockData, x, y, z, scaleX, scaleY, scaleZ);
    }
}
