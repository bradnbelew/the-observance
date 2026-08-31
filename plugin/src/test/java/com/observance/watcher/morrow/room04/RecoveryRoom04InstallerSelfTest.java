package com.observance.watcher.morrow.room04;

import com.observance.watcher.morrow.room04.RecoveryRoom04Installer.Origin;
import com.observance.watcher.morrow.room04.RecoveryRoom04Manifest.Cell;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Dependency-free manifest, refusal, rollback, recovery, idempotency, and read-back tests. */
public final class RecoveryRoom04InstallerSelfTest {
    private static final String RELEASE = "morrow.rehearsal.001";
    private static final Origin ORIGIN = new Origin(120, 80, -40);

    private RecoveryRoom04InstallerSelfTest() { }

    public static void main(String[] args) throws Exception {
        RecoveryRoom04Manifest manifest = new RecoveryRoom04Manifest();
        check(manifest.cells().size() == 1_144, "manifest covers the exact bounded volume");
        check(manifest.manifestSha256().equals(
                        "9831893bf03387b6c59b3835b648f056aafd67e6e194f3ade0282a7192fff41a"),
                "manifest bytes remain pinned");
        check(manifest.manifestSha256().equals(new RecoveryRoom04Manifest().manifestSha256()),
                "manifest hashing is deterministic");
        safeCellsAreExplicit(manifest);
        buildReadBackAndIdempotency(manifest);
        occupiedCellRefusesWithoutWrites(manifest);
        failedReadBackRollsBackAndRestartRecovers(manifest);
        receiptAndSnapshotAreReleaseBound(manifest);
        journalAuthorizedMutableCellsAreNarrow(manifest);
        naturalCopperAgingRepairsButForeignDriftRefuses(manifest);
        System.out.println("RECOVERY ROOM 04 INSTALLER: PASS hash=" + manifest.manifestSha256());
    }

    private static void safeCellsAreExplicit(RecoveryRoom04Manifest manifest) {
        for (Cell safe : manifest.safeCells()) {
            check(RecoveryRoom04Manifest.AIR.equals(manifest.expected(safe)), "safe feet cell is air");
            check(RecoveryRoom04Manifest.AIR.equals(manifest.expected(safe.above())), "safe head cell is air");
            check(!RecoveryRoom04Manifest.AIR.equals(manifest.expected(safe.below())), "safe floor is solid");
        }
        check(!manifest.expected(RecoveryRoom04Manifest.TERMINAL_CELL)
                .equals(RecoveryRoom04Manifest.AIR), "terminal is physical and visible");
    }

    private static void buildReadBackAndIdempotency(RecoveryRoom04Manifest manifest) throws Exception {
        TestFiles files = TestFiles.create("room04-build-");
        try {
            FakeWorld world = new FakeWorld("room04-test-world:00000000-0000-4000-8000-000000000004");
            RecoveryRoom04Installer installer = files.installer(manifest);
            RecoveryRoom04Installer.Result built = installer.install(RELEASE, ORIGIN, world);
            check(built.status() == RecoveryRoom04Installer.Status.BUILT, "first install builds");
            check(built.blockCount() == manifest.cells().size(), "receipt counts full bounded volume");
            check(Files.isRegularFile(files.snapshot), "rollback snapshot is durable");
            check(Files.isRegularFile(files.receipt), "install receipt is durable");
            installer.audit(world);
            int writes = world.writes;

            RecoveryRoom04Installer.Result duplicate = installer.install(RELEASE, ORIGIN, world);
            check(duplicate.status() == RecoveryRoom04Installer.Status.ALREADY_PRESENT,
                    "receipt plus exact read-back is idempotent");
            check(world.writes == writes, "idempotent install performs zero world writes");

            world.blocks.put(RecoveryRoom04Manifest.TERMINAL_CELL, "minecraft:dirt");
            expectIo(() -> installer.install(RELEASE, ORIGIN, world));
            check("minecraft:dirt".equals(world.blocks.get(RecoveryRoom04Manifest.TERMINAL_CELL)),
                    "receipt drift audit refuses rather than overwrites");
        } finally {
            files.close();
        }
    }

    private static void occupiedCellRefusesWithoutWrites(RecoveryRoom04Manifest manifest) throws Exception {
        TestFiles files = TestFiles.create("room04-occupied-");
        try {
            FakeWorld world = new FakeWorld("room04-occupied:00000000-0000-4000-8000-000000000005");
            Cell occupied = new Cell(2, 1, 1);
            world.blocks.put(occupied, "minecraft:chest");
            RecoveryRoom04Installer installer = files.installer(manifest);
            try {
                installer.install(RELEASE, ORIGIN, world);
                throw new AssertionError("expected occupied-cell refusal");
            } catch (RecoveryRoom04Installer.OccupiedCellsException expected) {
                check(expected.occupiedCount() == 1 && occupied.equals(expected.firstOccupied()),
                        "occupied refusal identifies bounded collision");
            }
            check(world.writes == 0, "occupied refusal occurs before every world write");
            check(!Files.exists(files.snapshot) && !Files.exists(files.receipt),
                    "occupied refusal creates no authority files");
            check("minecraft:chest".equals(world.blocks.get(occupied)), "occupied cell is preserved");
        } finally {
            files.close();
        }
    }

    private static void failedReadBackRollsBackAndRestartRecovers(RecoveryRoom04Manifest manifest)
            throws Exception {
        TestFiles files = TestFiles.create("room04-rollback-");
        try {
            FakeWorld world = new FakeWorld("room04-rollback:00000000-0000-4000-8000-000000000006");
            world.dropWriteAt = RecoveryRoom04Manifest.TERMINAL_CELL;
            RecoveryRoom04Installer installer = files.installer(manifest);
            expectIo(() -> installer.install(RELEASE, ORIGIN, world));
            check(Files.isRegularFile(files.snapshot), "failed build retains durable rollback snapshot");
            check(!Files.exists(files.receipt), "failed read-back never creates install receipt");
            check(world.allAir(manifest), "failed read-back restores every bounded cell");

            world.dropWriteAt = null;
            RecoveryRoom04Installer.Result recovered = installer.install(RELEASE, ORIGIN, world);
            check(recovered.status() == RecoveryRoom04Installer.Status.RECOVERED_AND_BUILT,
                    "restart path restores durable snapshot before rebuilding");
            installer.audit(world);
        } finally {
            files.close();
        }
    }

    private static void receiptAndSnapshotAreReleaseBound(RecoveryRoom04Manifest manifest) throws Exception {
        TestFiles files = TestFiles.create("room04-binding-");
        try {
            FakeWorld world = new FakeWorld("room04-binding:00000000-0000-4000-8000-000000000007");
            RecoveryRoom04Installer installer = files.installer(manifest);
            installer.install(RELEASE, ORIGIN, world);
            expectIo(() -> installer.install("morrow.rehearsal.999", ORIGIN, world));
            expectIo(() -> installer.install(RELEASE, new Origin(121, 80, -40), world));

            String changed = Files.readString(files.snapshot)
                    .replace("manifest-sha256=" + manifest.manifestSha256(), "manifest-sha256=" + "0".repeat(64));
            Files.writeString(files.snapshot, changed);
            expectIo(() -> installer.install(RELEASE, ORIGIN, world));
        } finally {
            files.close();
        }
    }

    private static void journalAuthorizedMutableCellsAreNarrow(RecoveryRoom04Manifest manifest) throws Exception {
        TestFiles files = TestFiles.create("room04-mutable-scene-");
        try {
            FakeWorld world = new FakeWorld("room04-mutable:00000000-0000-4000-8000-000000000008");
            RecoveryRoom04Installer installer = files.installer(manifest);
            installer.install(RELEASE, ORIGIN, world);
            StaticRestoreManifest staticRestore = new StaticRestoreManifest();
            Cell sceneCell = staticRestore.factualInferenceError().cell();
            world.blocks.put(sceneCell, staticRestore.factualInferenceError().restoredBlock());
            expectIo(() -> installer.install(RELEASE, ORIGIN, world));
            RecoveryRoom04Installer.Result restart = installer.install(
                    RELEASE, ORIGIN, world, staticRestore.mutableCells());
            check(restart.status() == RecoveryRoom04Installer.Status.ALREADY_PRESENT,
                    "journal-authorized M02 cells defer to the stricter scene restart audit");

            world.blocks.put(RecoveryRoom04Manifest.TERMINAL_CELL, "minecraft:dirt");
            expectIo(() -> installer.install(RELEASE, ORIGIN, world, staticRestore.mutableCells()));
            check("minecraft:dirt".equals(world.blocks.get(RecoveryRoom04Manifest.TERMINAL_CELL)),
                    "mutable scene exemption never hides unrelated Room 04 drift");
        } finally {
            files.close();
        }
    }

    private static void naturalCopperAgingRepairsButForeignDriftRefuses(
            RecoveryRoom04Manifest manifest) throws Exception {
        TestFiles files = TestFiles.create("room04-copper-aging-");
        try {
            FakeWorld world = new FakeWorld("room04-aging:00000000-0000-4000-8000-000000000009");
            RecoveryRoom04Installer installer = files.installer(manifest);
            installer.install(RELEASE, ORIGIN, world);
            Cell west = new Cell(-1, 0, 2);
            Cell east = new Cell(1, 0, 2);
            world.blocks.put(west, "minecraft:exposed_cut_copper");
            world.blocks.put(east, "minecraft:weathered_cut_copper");

            RecoveryRoom04Installer.Result restart = installer.install(RELEASE, ORIGIN, world);
            check(restart.status() == RecoveryRoom04Installer.Status.ALREADY_PRESENT,
                    "ambient copper repair retains the installed status");
            check(restart.naturalAgingRepairs() == 2,
                    "restart reports both naturally aged authored cells");
            check("minecraft:cut_copper".equals(world.blockData(west))
                            && "minecraft:cut_copper".equals(world.blockData(east)),
                    "ambient copper aging is restored to authored provenance");

            world.blocks.put(west, "minecraft:waxed_cut_copper");
            expectIo(() -> installer.install(RELEASE, ORIGIN, world));
            check("minecraft:waxed_cut_copper".equals(world.blockData(west)),
                    "non-natural copper drift refuses without overwrite");
        } finally {
            files.close();
        }
    }

    private static void expectIo(Throwing action) throws Exception {
        try {
            action.run();
            throw new AssertionError("expected IOException");
        } catch (IOException expected) {
            // expected
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface Throwing { void run() throws Exception; }

    private static final class FakeWorld implements RecoveryRoom04Installer.WorldPort {
        private final String binding;
        private final Map<Cell, String> blocks = new LinkedHashMap<>();
        private int writes;
        private Cell dropWriteAt;

        private FakeWorld(String binding) {
            this.binding = binding;
        }

        @Override public String binding() { return binding; }

        @Override
        public String blockData(Cell relative) {
            return blocks.getOrDefault(relative, RecoveryRoom04Manifest.AIR);
        }

        @Override
        public boolean isAir(Cell relative) {
            String value = blockData(relative);
            return value.equals("minecraft:air") || value.equals("minecraft:cave_air")
                    || value.equals("minecraft:void_air");
        }

        @Override
        public void setBlockData(Cell relative, String blockData) {
            writes++;
            if (relative.equals(dropWriteAt) && !RecoveryRoom04Manifest.AIR.equals(blockData)) return;
            if (RecoveryRoom04Manifest.AIR.equals(blockData)) blocks.remove(relative);
            else blocks.put(relative, blockData);
        }

        private boolean allAir(RecoveryRoom04Manifest manifest) {
            return manifest.cells().keySet().stream().allMatch(this::isAir);
        }
    }

    private static final class TestFiles implements AutoCloseable {
        private final Path directory;
        private final Path snapshot;
        private final Path receipt;

        private TestFiles(Path directory) {
            this.directory = directory;
            this.snapshot = directory.resolve("rollback.snapshot");
            this.receipt = directory.resolve("install.receipt");
        }

        private static TestFiles create(String prefix) throws IOException {
            return new TestFiles(Files.createTempDirectory(prefix));
        }

        private RecoveryRoom04Installer installer(RecoveryRoom04Manifest manifest) {
            return new RecoveryRoom04Installer(manifest, snapshot, receipt);
        }

        @Override
        public void close() throws IOException {
            Files.deleteIfExists(receipt);
            Files.deleteIfExists(snapshot);
            Files.deleteIfExists(directory);
        }
    }
}
