package com.observance.watcher.morrow;

import com.observance.watcher.ObservancePlugin;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Fail-closed Paper lifecycle for the disabled-by-default Morrow reboot runtime. */
public final class MorrowRuntime implements AutoCloseable {
    private static final String JOURNAL_NAME = "morrow-reboot.journal";
    private static final String CURSOR_NAME = "morrow-reboot.projector.cursor";

    private final MorrowRuntimeSettings settings;
    private final MorrowLocalState localState;
    private final MorrowEventProjector projector;

    private MorrowRuntime(
            MorrowRuntimeSettings settings,
            MorrowLocalState localState,
            MorrowEventProjector projector) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.localState = Objects.requireNonNull(localState, "localState");
        this.projector = Objects.requireNonNull(projector, "projector");
    }

    /** Returns {@code null} while the shipped disabled gate is closed. */
    public static MorrowRuntime start(ObservancePlugin plugin) throws IOException {
        Objects.requireNonNull(plugin, "plugin");
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("morrow-reboot");
        if (section == null || !section.getBoolean("enabled", false)) return null;

        String secret = resolveSecret(section);
        MorrowRuntimeSettings settings = MorrowRuntimeSettings.create(
                section.getString("release-id", ""),
                section.getString("campaign-id", ""),
                section.getString("world", ""),
                section.getString("ingest-url", ""),
                secret,
                section.getLong("projector.connect-timeout-ms", 5_000L),
                section.getLong("projector.request-timeout-ms", 10_000L),
                section.getLong("projector.initial-retry-ms", 500L),
                section.getLong("projector.maximum-retry-ms", 30_000L));

        // This is the only world lookup in the lifecycle. It happens on Paper's enable thread before
        // the projector exists; no Bukkit object is retained by the worker.
        World world = plugin.getServer().getWorld(settings.worldName());
        if (world == null || !settings.worldName().equals(world.getName())) {
            throw new IllegalArgumentException("configured Morrow world is not loaded");
        }

        Path data = plugin.getDataFolder().toPath();
        MorrowLocalState state = MorrowLocalState.open(data.resolve(JOURNAL_NAME), settings.releaseId());
        MorrowEventProjector projector = null;
        try {
            projector = MorrowEventProjector.open(
                    state,
                    settings,
                    data.resolve(CURSOR_NAME),
                    plugin.getLogger());
            projector.start();
            return new MorrowRuntime(settings, state, projector);
        } catch (IOException | RuntimeException failure) {
            if (projector != null) projector.close();
            throw failure;
        }
    }

    public MorrowRuntimeSettings settings() {
        return settings;
    }

    public MorrowLocalState localState() {
        return localState;
    }

    public MorrowEventProjector.Snapshot projectorSnapshot() {
        return projector.snapshot();
    }

    @Override
    public void close() {
        projector.close();
    }

    private static String resolveSecret(ConfigurationSection section) {
        String direct = section.getString("ingest-secret", "");
        if (direct != null && !direct.isBlank()) return direct.trim();
        String environmentName = section.getString(
                "ingest-secret-env", "MORROW_MINECRAFT_INGEST_SECRET");
        if (environmentName == null || !environmentName.matches("[A-Z][A-Z0-9_]{1,63}")) {
            throw new IllegalArgumentException("morrow-reboot.ingest-secret-env has invalid grammar");
        }
        String environmentValue = System.getenv(environmentName);
        return environmentValue == null ? "" : environmentValue.trim();
    }
}
