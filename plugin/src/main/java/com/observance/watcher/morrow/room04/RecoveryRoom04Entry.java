package com.observance.watcher.morrow.room04;

import com.observance.watcher.morrow.room04.RecoveryRoom04Installer.Origin;
import com.observance.watcher.morrow.room04.RecoveryRoom04Manifest.Bounds;
import com.observance.watcher.morrow.room04.RecoveryRoom04Manifest.Cell;

import java.util.Objects;

/** Bukkit-free coordinate and recovery policy for player entry into Recovery Room 04. */
public final class RecoveryRoom04Entry {
    private RecoveryRoom04Entry() { }

    public static AbsoluteCell safeFeet(Origin origin) {
        Objects.requireNonNull(origin, "origin");
        Cell safe = RecoveryRoom04Manifest.SPAWN_CELL;
        return new AbsoluteCell(
                Math.addExact(origin.x(), safe.x()),
                Math.addExact(origin.y(), safe.y()),
                Math.addExact(origin.z(), safe.z()));
    }

    public static boolean insideRoom(Origin origin, int x, int y, int z) {
        Objects.requireNonNull(origin, "origin");
        Bounds bounds = RecoveryRoom04Manifest.BOUNDS;
        return x >= Math.addExact(origin.x(), bounds.minimumX())
                && x <= Math.addExact(origin.x(), bounds.maximumX())
                && y >= Math.addExact(origin.y(), bounds.minimumY())
                && y <= Math.addExact(origin.y(), bounds.maximumY())
                && z >= Math.addExact(origin.z(), bounds.minimumZ())
                && z <= Math.addExact(origin.z(), bounds.maximumZ());
    }

    /** Recover only an unsafe player in the exact configured world; safe progress is never repositioned. */
    public static boolean shouldRecover(
            boolean configuredWorld,
            boolean feetClear,
            boolean headClear,
            boolean floorSolid) {
        return configuredWorld && (!feetClear || !headClear || !floorSolid);
    }

    public record AbsoluteCell(int x, int y, int z) { }
}
