package com.observance.watcher.v5runtime.ritual;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Integration metadata required to bind RP04 to a built world. The authority intentionally names
 * the coop-plate radius without inventing a number, so installation must supply and preflight it.
 */
public record Rp04WorldBindings(
        String worldName,
        Point rosterCenter,
        double eligibilityRadius,
        List<SectorBinding> sectors) {
    public static final String SITE_ID = "coop_plate";
    public static final String HANDLE_PDC_KEY = "v5_rp04_sector";
    public static final int WINDOW_SECONDS = 45;
    public static final int DISCONNECT_GRACE_SECONDS = 20;

    public record Point(int x, int y, int z) {
    }

    public record SectorBinding(
            int index,
            Point pressurePlate,
            Point confirmationHandle,
            Point visibleLamp) {
        public SectorBinding {
            if (index < 0) {
                throw new IllegalArgumentException("sector index cannot be negative");
            }
            Objects.requireNonNull(pressurePlate, "pressurePlate");
            Objects.requireNonNull(confirmationHandle, "confirmationHandle");
            Objects.requireNonNull(visibleLamp, "visibleLamp");
            if (pressurePlate.equals(confirmationHandle) || pressurePlate.equals(visibleLamp)
                    || confirmationHandle.equals(visibleLamp)) {
                throw new IllegalArgumentException("plate, handle, and lamp need distinct blocks");
            }
        }
    }

    public Rp04WorldBindings {
        if (worldName == null || worldName.isBlank()) {
            throw new IllegalArgumentException("RP04 world name is required");
        }
        Objects.requireNonNull(rosterCenter, "rosterCenter");
        if (!Double.isFinite(eligibilityRadius) || eligibilityRadius <= 0.0) {
            throw new IllegalArgumentException("RP04 site-config radius must be positive and finite");
        }
        sectors = List.copyOf(Objects.requireNonNull(sectors, "sectors"));
        if (sectors.isEmpty()) {
            throw new IllegalArgumentException("RP04 requires at least one physical sector");
        }
        Set<Integer> indexes = new LinkedHashSet<>();
        Set<Point> occupiedBlocks = new LinkedHashSet<>();
        for (SectorBinding sector : sectors) {
            if (!indexes.add(sector.index())) {
                throw new IllegalArgumentException("duplicate RP04 sector index " + sector.index());
            }
            if (!occupiedBlocks.add(sector.pressurePlate())
                    || !occupiedBlocks.add(sector.confirmationHandle())
                    || !occupiedBlocks.add(sector.visibleLamp())) {
                throw new IllegalArgumentException("RP04 sector fixtures overlap");
            }
        }
        for (int index = 0; index < sectors.size(); index++) {
            if (!indexes.contains(index)) {
                throw new IllegalArgumentException("RP04 sector indexes must be contiguous from zero");
            }
        }
    }

    public void requireCapacity(int eligibleRosterSize) {
        if (eligibleRosterSize < 1 || eligibleRosterSize > sectors.size()) {
            throw new IllegalStateException("RP04 has " + sectors.size()
                    + " bound sectors for an eligible roster of " + eligibleRosterSize);
        }
    }

    public void requireSpareAccessibilitySector(int eligibleRosterSize) {
        requireCapacity(eligibleRosterSize);
        if (sectors.size() <= eligibleRosterSize) {
            throw new IllegalStateException("RP04 accessibility replacement requires at least one "
                    + "bound spare sector beyond the active roster sectors");
        }
    }
}
