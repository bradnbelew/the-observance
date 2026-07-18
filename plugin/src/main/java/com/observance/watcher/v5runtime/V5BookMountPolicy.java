package com.observance.watcher.v5runtime;

import com.observance.watcher.v5runtime.FixtureTransform.BlockPos;
import com.observance.watcher.v5runtime.FixtureTransform.Cardinal;

import java.util.Objects;

/**
 * Pure placement policy for auxiliary readable-book mounts around an authored fixture.
 *
 * <p>The fixture front points from its anchor into the public approach space. The lateral axis is
 * expressed from the approaching reader's point of view, not from behind the fixture. Keeping the
 * rule independent of Bukkit lets the exact direction contract run in the ordinary core test.
 */
public final class V5BookMountPolicy {
    private V5BookMountPolicy() {
    }

    public static BlockPos candidate(BlockPos anchor, Cardinal fixtureFront,
                                     int readerLateral, int approachDepth) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(fixtureFront, "fixtureFront");
        Cardinal readerRight = fixtureFront.opposite().clockwise();
        return anchor.add(
                readerRight.x() * readerLateral + fixtureFront.x() * approachDepth,
                0,
                readerRight.z() * readerLateral + fixtureFront.z() * approachDepth);
    }
}
