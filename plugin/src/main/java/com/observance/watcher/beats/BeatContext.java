package com.observance.watcher.beats;

import com.observance.watcher.config.ObservanceConfig;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Reveal;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.plugin.Plugin;

/**
 * Immutable bundle of foundation services handed to every {@link Beat}. Beats never reach back into
 * the plugin singleton — they take exactly what they need from here. This keeps beats independently
 * testable and makes their dependencies explicit.
 *
 * <p>All fields are the SAME instances the foundation built (Safety, Scheduler, Reveal, etc.) so
 * beats inherit fault isolation, threading discipline, and reveal/placement validation for free.
 */
public final class BeatContext {

    private final Plugin plugin;
    private final ObservanceConfig config;
    private final SitesConfig sites;
    private final SupabaseClient supabase;
    private final Scheduler scheduler;
    private final Safety safety;
    private final Reveal reveal;
    private final RateLimiter rateLimiter;
    private final ProtectedRegistry protectedRegistry;

    /** The Lens registry ("second sight", INTEGRATION §SIGNATURE #3) — beats register per-player gated
     *  displays here so the {@code LensListener} reveals them only while the Lens is held. Nullable
     *  (a build without the Lens wired still runs; reflection-style beats just skip gating). */
    private final com.observance.watcher.lens.LensRegistry lensRegistry;

    /** A namespaced key root the beats use for PersistentDataContainer tags + advancement ids. */
    private final String namespace;

    public BeatContext(Plugin plugin,
                       ObservanceConfig config,
                       SitesConfig sites,
                       SupabaseClient supabase,
                       Scheduler scheduler,
                       Safety safety,
                       Reveal reveal,
                       RateLimiter rateLimiter,
                       ProtectedRegistry protectedRegistry,
                       com.observance.watcher.lens.LensRegistry lensRegistry,
                       String namespace) {
        this.plugin = plugin;
        this.config = config;
        this.sites = sites;
        this.supabase = supabase;
        this.scheduler = scheduler;
        this.safety = safety;
        this.reveal = reveal;
        this.rateLimiter = rateLimiter;
        this.protectedRegistry = protectedRegistry == null ? new ProtectedRegistry() : protectedRegistry;
        this.lensRegistry = lensRegistry;
        this.namespace = (namespace == null || namespace.isBlank()) ? "observance" : namespace;
    }

    public Plugin plugin() { return plugin; }
    public ObservanceConfig config() { return config; }
    public SitesConfig sites() { return sites; }
    public SupabaseClient supabase() { return supabase; }
    public Scheduler scheduler() { return scheduler; }
    public Safety safety() { return safety; }
    public Reveal reveal() { return reveal; }
    public RateLimiter rateLimiter() { return rateLimiter; }
    public ProtectedRegistry protectedRegistry() { return protectedRegistry; }
    /** The Lens registry, or null if the Lens is not wired in this build. */
    public com.observance.watcher.lens.LensRegistry lensRegistry() { return lensRegistry; }
    public String namespace() { return namespace; }
}
