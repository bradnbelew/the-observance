package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.oracle.AnswerNormalizer;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.util.PerPlayer;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * THE ASYMMETRIC CO-OP VAULT — {@code spine-threshold-vault} (design/PUZZLE-DESIGNS.md §8.2,
 * INTEGRATION signature #2, OVERHAUL Pillar 3). The social centerpiece: a sealed Threshold vault whose
 * combination NO ONE player can read. Each active player standing at the {@code threshold_vault} site is
 * shown — via per-player {@link PerPlayer#showEntityTo} — a DIFFERENT fragment of the combination, a rune
 * {@link TextDisplay} only that one client can see. To everyone else, and on everyone else's fragment,
 * the wall reads nothing. Only by <b>reading their fragments aloud together and combining them</b> can the
 * group assemble the full code; typing it at the vault sign opens the vault (Keep Talking And Nobody
 * Explodes, native to the illusion tech).
 *
 * <p><b>Dynamic-roster partition (OVERHAUL §4 invariant).</b> The combination is split into fragment
 * groups (config {@code combination}, space-separated groups). At solve-time the groups are partitioned
 * over the ACTIVE roster present at the vault — fewer players get fewer, larger fragments; more players
 * get more, smaller ones — so EVERY active player always holds a distinct, non-empty piece and together
 * they reconstruct the whole. The partition is DETERMINISTIC (players ordered by UUID, groups sliced
 * contiguously by {@link #partition}), so the same roster always sees the same split and a player's own
 * fragment is stable while they stay. The partition is recomputed on every refresh tick and on roster
 * change, so a late joiner is folded in (they get a slice next tick) and a leaver's slice is redealt to
 * the rest — the puzzle self-heals to whoever is actually here.
 *
 * <p><b>Entry + solve.</b> The group submits the assembled combination by editing the vault SIGN (a
 * {@link SignChangeEvent} at the {@code threshold_vault} site — the {@link AnswerSignListener} idiom: the
 * sign is an input slot, blanked after read, never a billboard). The typed lines are normalized with the
 * SAME {@link AnswerNormalizer} the oracle uses and compared to the config combination; on an exact match
 * the listener posts the puzzle's OPAQUE token (config {@code token}, byte-matching the seed's
 * {@code accepted_answers}) to {@link OracleResolver#resolveWorld}. The engine then records the solve
 * (idempotent via {@code insertSolveIfNew}) and fires the authored outcome — the door_open beat at
 * {@code threshold_vault} and the {@code threshold_vault_open} flag. The COMBINATION is never in the seed
 * (only the opaque token is); a player cannot bypass the fragments by typing the token, and cannot post a
 * solve by guessing the combination anywhere but this sign. Idempotent by construction (the oracle
 * de-dupes; a re-type after the vault is open just MISSes against a closed row).
 *
 * <p><b>Gate.</b> Both the fragment display AND the solve are gated on {@code deep_gate_open} (the seed's
 * {@code requires_flags}). The gate is read live but CACHED (a short async refresh, fail-CLOSED on an
 * unknown/failed read) so runes never appear — and the vault never opens — before the deep is unlocked,
 * and so the hot event path never blocks on a DB call. Once open the latch stays open for the session.
 *
 * <p><b>Async-safe, fault-isolated, reveal-safe.</b> All Bukkit work (display spawn/reveal, sign read) is
 * MAIN-thread; only the oracle resolve and the gate read hop async. Every scheduled follow-up re-resolves
 * players by UUID and null-/online-guards. Displays are {@code setVisibleByDefault(false)} +
 * non-persistent (no bystander catches them, no orphan survives a crash), PDC-tagged for cleanup, and are
 * despawned when the roster shrinks / a player leaves the site / on disable. Never cancels an event, never
 * mutates the world here (the door is the engine's job), never messages the room.
 */
public final class ThresholdVaultListener implements Listener {

    /** Site type the vault family lives under (shared with the coop-plate family). */
    private static final String VAULT_TYPE = "coop_plate";

    /** The resource-pack font carrying the keepers' rune alphabet (shared with the name-on-wall scare). */
    private static final String RUNE_FONT = "observance:runes";

    /** Barely-there blood-dark glyph colour for the rune fragment. */
    private static final TextColor RUNE_COLOR = TextColor.color(0x8a, 0x1c, 0x1c);

    /** Refresh cadence (ticks) for the per-player fragment display — recomputes the partition + reveals. */
    private static final int REFRESH_TICKS = 20;   // once/sec: cheap, and roster changes land within a second

    /** Coarse per-player submit cooldown at the sign (defense in depth atop the resolver's own limiter). */
    private static final long SUBMIT_COOLDOWN_MS = 3_000L;

    /** Gate re-read cadence (ms) — how stale the cached {@code deep_gate_open} answer may be. */
    private static final long GATE_CACHE_MS = 15_000L;

    /** PDC keys tagging our display entities so a sweep / our own cleanup can recognise them. */
    private static final String PDC_TAG = "threshold_vault_rune";

    private final Plugin plugin;
    private final Supplier<SitesConfig> sitesSupplier;
    private final OracleResolver oracle;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;

    private final boolean enabled;
    /** The OPAQUE token posted on a correct combination (byte-matches the seed's accepted_answers). */
    private final String token;
    private final String puzzleKey;
    /** The full combination the fragments assemble (space-separated groups). Never enters the seed. */
    private final List<String> combinationGroups;
    /** The normalized whole combination, precomputed once for the constant-time sign compare. */
    private final String combinationNormalized;

    /**
     * Convergence quorum (INV-19). The vault is a "needs quorum" beat: it must not open for a single lone
     * player when the group is meant to converge. The configured quorum is clamped to the live ACTIVE
     * roster ({@code min(configQuorum, activeRosterSize)}, reusing {@link AcceptingRiteListener#clampQuorum})
     * so an absent cast member never blocks the solve, and a genuinely smaller active group can still finish.
     */
    private final int quorum;

    /**
     * Live ACTIVE-roster size (INV-19): the showrunner's {@code readActiveRoster(windowMs)} count when
     * wired, else the count of players currently online. Feeds the quorum clamp; the fragment partition is
     * over the players ACTUALLY at the vault (a subset of the active roster), which is the real convergence.
     */
    private final IntSupplier activeRosterSize;

    /**
     * Live {@code deep_gate_open} reader (fail-CLOSED). Null = UNWIRED → the listener reads the flag
     * itself via the oracle's arc-state path is NOT available here; instead an unwired gate defaults to
     * CLOSED unless {@code gateOpenDefault} is true (used by tests / a placeholder deployment). When wired,
     * the answer is cached for {@link #GATE_CACHE_MS} so the event path never blocks on a DB read.
     */
    private final Supplier<Boolean> deepGateOpen;

    // --- runtime state (MAIN thread only) ---
    /** displayId → owner UUID, for our spawned per-player rune displays (cleanup + re-deal). */
    private final Map<UUID, UUID> ownerByDisplay = new HashMap<>();
    /** owner UUID → their current rune display id (so a re-deal replaces, never duplicates). */
    private final Map<UUID, UUID> displayByOwner = new HashMap<>();
    /** owner UUID → the fragment text last shown them (skip a respawn when nothing changed). */
    private final Map<UUID, String> shownFragment = new HashMap<>();

    // --- gate cache (written on the async refresh, read on the main path) ---
    private volatile boolean gateOpenCached;
    private volatile long gateCheckedAt;
    private volatile boolean gateLatched;   // once open, stay open for the session (matches arc-flag monotonicity)

    private org.bukkit.scheduler.BukkitTask refreshTask;

    public ThresholdVaultListener(Plugin plugin, Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                                  RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                                  boolean enabled, String token, String puzzleKey,
                                  List<String> combinationGroups, int quorum,
                                  IntSupplier activeRosterSize, Supplier<Boolean> deepGateOpen) {
        this.plugin = plugin;
        this.sitesSupplier = sitesSupplier;
        this.oracle = oracle;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.enabled = enabled;
        this.token = token == null ? "" : token.trim();
        this.puzzleKey = (puzzleKey == null || puzzleKey.isBlank()) ? "spine-threshold-vault" : puzzleKey.trim();
        this.combinationGroups = normalizeGroups(combinationGroups);
        this.combinationNormalized = AnswerNormalizer.normalize(String.join(" ", this.combinationGroups));
        this.quorum = Math.max(1, quorum);
        this.activeRosterSize = activeRosterSize;
        this.deepGateOpen = deepGateOpen;
    }

    /** Start the per-player fragment refresh loop. Call once after registration (MAIN thread). No-op when
     *  disabled or the combination is empty (an inert placeholder deployment). */
    public void start() {
        if (!enabled || combinationGroups.isEmpty() || scheduler == null) return;
        this.refreshTask = scheduler.runTimerSafe("vault.fragments.refresh",
                REFRESH_TICKS, REFRESH_TICKS, this::refreshTick);
    }

    /** Stop the loop and despawn every rune we spawned (MAIN thread). Idempotent. */
    public void stop() {
        try { if (refreshTask != null) refreshTask.cancel(); } catch (Throwable ignored) { }
        refreshTask = null;
        for (UUID displayId : new ArrayList<>(ownerByDisplay.keySet())) {
            removeDisplay(displayId);
        }
        ownerByDisplay.clear();
        displayByOwner.clear();
        shownFragment.clear();
    }

    /* ==================================================================== */
    /*  Fragment display (per-player illusion; dynamic-roster partition)    */
    /* ==================================================================== */

    /** MAIN thread, once/sec: partition the combination over the players AT the vault and reveal each
     *  their own fragment. Recomputes fresh each tick so roster changes self-heal. */
    private void refreshTick() {
        safety.run("vault.fragments.tick", () -> {
            // Refresh the cached gate answer off-thread if stale; never block here on the read.
            maybeRefreshGate();

            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) { clearAll(); return; }

            Site vault = firstPlacedVault(sites);
            if (vault == null || !isGateOpen()) { clearAll(); return; }   // unplaced or gate closed → no runes

            String world = vault.worldName();
            List<Player> present = playersInSite(vault, world);
            // Stable, deterministic order so a player's fragment is consistent across ticks.
            present.sort(Comparator.comparing(p -> p.getUniqueId().toString()));

            if (present.isEmpty()) { clearAll(); return; }

            // Partition the combination groups over the present roster (dynamic-N).
            List<String> fragments = partition(combinationGroups, present.size());

            // Reveal each present player their own fragment; drop anyone who left.
            java.util.Set<UUID> stillHere = new java.util.HashSet<>();
            for (int i = 0; i < present.size(); i++) {
                Player p = present.get(i);
                stillHere.add(p.getUniqueId());
                showFragmentTo(p, fragments.get(i));
            }
            // Anyone we were showing a rune to who is no longer present → despawn their rune.
            for (UUID owner : new ArrayList<>(displayByOwner.keySet())) {
                if (!stillHere.contains(owner)) removeOwner(owner);
            }
        });
    }

    /** Spawn-or-reuse the one rune display for this player and reveal it only to them. */
    private void showFragmentTo(Player p, String fragment) {
        if (!p.isOnline() || fragment == null || fragment.isBlank()) return;

        // Skip a respawn when we're already showing this exact fragment (roster + slice unchanged).
        UUID owner = p.getUniqueId();
        UUID existing = displayByOwner.get(owner);
        if (existing != null) {
            org.bukkit.entity.Entity e = Bukkit.getEntity(existing);
            if (e != null && e.isValid() && fragment.equals(shownFragment.get(owner))) {
                return;   // still valid + unchanged → leave it
            }
            removeOwner(owner);   // stale or changed → redeal
        }

        Location spot = runeSpot(p);
        if (spot == null || spot.getWorld() == null) return;

        // The rune carries ONLY this player's fragment (the other players' slices are invisible to them).
        Component label = Component.text(fragment).color(RUNE_COLOR).font(net.kyori.adventure.key.Key.key(RUNE_FONT));
        final Component finalLabel = label;

        TextDisplay display;
        try {
            display = spot.getWorld().spawn(spot, TextDisplay.class, td -> {
                td.setVisibleByDefault(false);          // invisible to everyone until we reveal to the one owner
                td.setPersistent(false);                // never survive a restart / chunk save → no orphan
                td.text(finalLabel);
                td.setBillboard(Display.Billboard.CENTER);
                td.setSeeThrough(true);
                td.setShadowed(true);
                td.setDefaultBackground(false);
                try { td.setBackgroundColor(org.bukkit.Color.fromARGB(0)); } catch (Throwable ignored) { }
                td.setBrightness(new Display.Brightness(15, 15));   // self-lit against a dark vault wall
                td.setViewRange(0.6f);                  // small — a private, close apparition
                td.setTransformation(new Transformation(
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f),
                        new Vector3f(1.2f, 1.2f, 1.2f),
                        new AxisAngle4f(0f, 0f, 0f, 1f)));
            });
        } catch (Throwable t) {
            return;   // a spawn hiccup for one player never affects the others
        }
        if (display == null || !display.isValid()) return;

        try {
            display.getPersistentDataContainer().set(
                    org.bukkit.NamespacedKey.fromString(PDC_TAG, plugin), PersistentDataType.STRING,
                    owner.toString());
        } catch (Throwable ignored) { }

        PerPlayer.showEntityTo(plugin, p, display);   // reveal to the ONE owner — the whole illusion

        ownerByDisplay.put(display.getUniqueId(), owner);
        displayByOwner.put(owner, display.getUniqueId());
        shownFragment.put(owner, fragment);
    }

    /** Where to hang a player's rune: a touch in front of them at eye level, in a loaded chunk. */
    private static Location runeSpot(Player p) {
        Location eye = p.getEyeLocation();
        if (eye.getWorld() == null) return null;
        org.bukkit.util.Vector dir = eye.getDirection().clone();
        dir.setY(dir.getY() * 0.2);
        if (dir.lengthSquared() < 1.0e-6) return null;
        dir.normalize();
        Location spot = eye.clone().add(dir.multiply(2.2));
        if (!spot.getWorld().isChunkLoaded(spot.getBlockX() >> 4, spot.getBlockZ() >> 4)) return null;
        org.bukkit.util.Vector back = p.getLocation().toVector().subtract(spot.toVector());
        if (back.lengthSquared() > 1.0e-6) spot.setDirection(back);
        return spot;
    }

    private void clearAll() {
        for (UUID owner : new ArrayList<>(displayByOwner.keySet())) removeOwner(owner);
    }

    private void removeOwner(UUID owner) {
        UUID displayId = displayByOwner.remove(owner);
        shownFragment.remove(owner);
        if (displayId != null) removeDisplay(displayId);
    }

    private void removeDisplay(UUID displayId) {
        ownerByDisplay.remove(displayId);
        try {
            org.bukkit.entity.Entity e = Bukkit.getEntity(displayId);
            if (e != null && e.isValid()) e.remove();
        } catch (Throwable ignored) { }
    }

    /* ==================================================================== */
    /*  Entry + solve (the vault sign)                                      */
    /* ==================================================================== */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!enabled || token.isBlank() || combinationGroups.isEmpty()) return;
        safety.run("vault.sign.change", () -> {
            Player p = event.getPlayer();
            if (p == null || oracle == null || scheduler == null) return;

            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Block block = event.getBlock();
            if (block == null) return;
            Location loc = block.getLocation();
            if (loc == null || loc.getWorld() == null) return;
            String world = loc.getWorld().getName();

            Site vault = nearestPlacedOfType(sites, VAULT_TYPE, world, loc.getX(), loc.getY(), loc.getZ());
            if (vault == null || !isVaultSite(vault)) return;   // an ordinary sign / a non-vault coop plate

            String raw = joinLines(event.getLines());
            blank(event);                                       // input slot, not billboard — never persist the guess
            if (raw.isBlank()) return;

            // Gate: the deep must be open (fail-closed). Silent when closed — no tell.
            if (!isGateOpen()) return;

            // The typed combination must match the assembled code exactly (same normalization as the oracle).
            if (!AnswerNormalizer.normalize(raw).equals(combinationNormalized)) return;   // wrong → silence

            // Convergence quorum (INV-19): the vault is a group beat — require that ENOUGH of the active
            // roster is present AT the vault, clamped to the live active set so an absent member can't block
            // it. A correct combination typed by a lone player when the group is meant to converge is
            // withheld silently (they still needed to assemble it together; this stops one player who
            // learned the whole code from opening it alone).
            int effectiveQuorum = AcceptingRiteListener.clampQuorum(quorum, readActiveRosterSize());
            if (playersInSite(vault, world).size() < effectiveQuorum) return;   // not the whole group yet → silence

            // Coarse anti-spam per player+site (the resolver also rate-limits durably).
            if (!rateLimiter.tryCooldown("vault:" + p.getUniqueId() + ":" + vault.id(), SUBMIT_COOLDOWN_MS)) return;

            final String mc = p.getUniqueId().toString();
            final String name = p.getName();
            safety.info("vault.solve",
                    name + " entered the assembled combination at " + vault.id() + " — posting spine-threshold-vault");
            // Post the OPAQUE token (never the combination). The engine records the solve + opens the vault.
            scheduler.runAsyncSafe("vault.resolve",
                    () -> oracle.resolveWorld(mc, name, token, puzzleKey));
        });
    }

    /* ==================================================================== */
    /*  Gate (deep_gate_open) — cached, fail-closed                         */
    /* ==================================================================== */

    /** True if the deep gate is (cached) open. Fail-CLOSED: an unwired/unknown gate reads as closed. */
    private boolean isGateOpen() {
        if (gateLatched) return true;
        if (deepGateOpen == null) return false;   // unwired → closed (never leak runes / open early)
        return gateOpenCached;
    }

    /** Refresh the cached gate answer off-thread when stale. Never blocks the event/refresh path. */
    private void maybeRefreshGate() {
        if (gateLatched || deepGateOpen == null) return;
        long now = System.currentTimeMillis();
        if (now - gateCheckedAt < GATE_CACHE_MS && gateCheckedAt != 0L) return;
        gateCheckedAt = now;   // set first so overlapping ticks don't all queue a read
        scheduler.runAsyncSafe("vault.gate.read", () -> {
            Boolean open = safety.call("vault.gate.call", deepGateOpen::get, Boolean.FALSE);
            boolean ok = Boolean.TRUE.equals(open);
            gateOpenCached = ok;
            if (ok) gateLatched = true;   // monotonic: once the deep opens it stays open for the session
        });
    }

    /* ==================================================================== */
    /*  Pure partition (the dynamic-roster core) — server-free testable     */
    /* ==================================================================== */

    /**
     * Deterministically partition {@code groups} (the combination's fragment groups) into exactly
     * {@code players} contiguous, non-empty slices — the dynamic-roster invariant made concrete.
     *
     * <ul>
     *   <li>{@code players <= 1} → one slice = the WHOLE combination (a lone active keeper holds it all);</li>
     *   <li>{@code players >= groups.size()} → every slice is a single group; any EXTRA players beyond the
     *       group count wrap to share (index modulo) so no active player is left with an empty rune — they
     *       simply double-up on a group, which still requires the group to be present;</li>
     *   <li>otherwise the groups are split as evenly as possible (earlier slices get the +1 remainder), so
     *       fewer players → fewer, larger fragments; more players → more, smaller ones.</li>
     * </ul>
     *
     * <p>The union of all slices is always the full, in-order combination (when players ≤ groups); each
     * slice is non-empty; the result size is exactly {@code max(1, players)}. Pure + null-safe.
     */
    static List<String> partition(List<String> groups, int players) {
        List<String> out = new ArrayList<>();
        if (groups == null || groups.isEmpty()) return out;
        int n = groups.size();
        int p = Math.max(1, players);

        if (p == 1) {                        // one holder → the whole code
            out.add(String.join(" ", groups));
            return out;
        }
        if (p >= n) {                        // one group each; extra players wrap to double-up (no empty rune)
            for (int i = 0; i < p; i++) {
                out.add(groups.get(i % n));
            }
            return out;
        }
        // Split n groups into p contiguous slices; first (n % p) slices get one extra group.
        int base = n / p;
        int extra = n % p;
        int idx = 0;
        for (int i = 0; i < p; i++) {
            int take = base + (i < extra ? 1 : 0);
            List<String> slice = groups.subList(idx, idx + take);
            out.add(String.join(" ", slice));
            idx += take;
        }
        return out;
    }

    /* ----------------------------- helpers ---------------------------- */

    /** Trim + drop blank groups; a combination is a list of non-empty space-free-ish tokens. */
    private static List<String> normalizeGroups(List<String> groups) {
        List<String> out = new ArrayList<>();
        if (groups == null) return out;
        for (String g : groups) {
            if (g == null) continue;
            String t = g.trim();
            if (!t.isBlank()) out.add(t);
        }
        return out;
    }

    /** Live active-roster size: the wired supplier (showrunner's readActiveRoster count) or, unwired/failed,
     *  the count of currently-online players. Always ≥ 0; fail-safe (a thrown supplier falls back to online). */
    private int readActiveRosterSize() {
        if (activeRosterSize != null) {
            Integer n = safety.call("vault.roster", activeRosterSize::getAsInt, null);
            if (n != null && n >= 0) return n;
        }
        try {
            return Bukkit.getOnlinePlayers().size();
        } catch (Throwable t) {
            return 0;
        }
    }

    /** First PLACED vault-family site that is actually the threshold vault (id contains "threshold_vault"),
     *  else the first placed coop_plate whose id names the vault. Keeps the plate/vault families separate. */
    private Site firstPlacedVault(SitesConfig sites) {
        for (Site s : sites.placedOfType(VAULT_TYPE)) {
            if (isVaultSite(s)) return s;
        }
        return null;
    }

    /** A coop_plate site belongs to the vault iff its id names the threshold vault (not the IV→V plate). */
    private static boolean isVaultSite(Site s) {
        String id = s == null ? null : s.id();
        return id != null && id.toLowerCase(java.util.Locale.ROOT).contains("threshold_vault");
    }

    private List<Player> playersInSite(Site site, String world) {
        List<Player> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null || !p.isOnline()) continue;
            Location l = p.getLocation();
            if (l == null || l.getWorld() == null) continue;
            if (!l.getWorld().getName().equals(world)) continue;
            if (site.contains(world, l.getX(), l.getY(), l.getZ())) out.add(p);
        }
        return out;
    }

    private Site nearestPlacedOfType(SitesConfig sites, String type,
                                     String world, double x, double y, double z) {
        Site best = null;
        double bestD2 = Double.MAX_VALUE;
        for (Site s : sites.placedOfType(type)) {
            if (!s.contains(world, x, y, z)) continue;
            Location c = s.location();
            if (c == null) { if (best == null) best = s; continue; }
            double dx = x - c.getX(), dy = y - c.getY(), dz = z - c.getZ();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestD2) { bestD2 = d2; best = s; }
        }
        return best;
    }

    private static String joinLines(String[] lines) {
        if (lines == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(line);
        }
        return sb.toString();
    }

    private static void blank(SignChangeEvent event) {
        try {
            for (int i = 0; i < 4; i++) event.setLine(i, "");
        } catch (Throwable ignored) { }
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the dynamic-roster partition the whole co-op mechanic leans on: EVERY active player gets a
     * distinct non-empty fragment; the fragments (for players ≤ groups) reassemble the FULL in-order
     * combination; fewer players → fewer/larger slices, more → more/smaller; a lone keeper holds it all;
     * and more players than groups never yields an empty rune (they wrap to double-up). A regression here
     * would either strand a player with no fragment (unsolvable) or drop part of the code (the assembled
     * combination would no longer match), silently breaking the signature puzzle.
     */
    static boolean partitionSelfTest() {
        List<String> five = List.of("v8k3", "mq2n", "x6w1", "t4d9", "c7s5");
        String whole = String.join(" ", five);

        // Solo → the whole code.
        List<String> p1 = partition(five, 1);
        if (p1.size() != 1 || !p1.get(0).equals(whole)) return false;

        // Two players → 3 + 2 (first slice gets the remainder), union = whole, both non-empty.
        List<String> p2 = partition(five, 2);
        if (p2.size() != 2) return false;
        if (!p2.get(0).equals("v8k3 mq2n x6w1") || !p2.get(1).equals("t4d9 c7s5")) return false;
        if (!String.join(" ", p2).equals(whole)) return false;

        // Three players → 2 + 2 + 1, union = whole.
        List<String> p3 = partition(five, 3);
        if (p3.size() != 3) return false;
        if (!String.join(" ", p3).equals(whole)) return false;
        for (String s : p3) if (s == null || s.isBlank()) return false;

        // Exactly groups → one group each, in order.
        List<String> p5 = partition(five, 5);
        if (p5.size() != 5) return false;
        for (int i = 0; i < 5; i++) if (!p5.get(i).equals(five.get(i))) return false;

        // MORE players than groups → still every slice non-empty (wrap to double-up), size = players.
        List<String> p7 = partition(five, 7);
        if (p7.size() != 7) return false;
        for (String s : p7) if (s == null || s.isBlank()) return false;
        if (!p7.get(5).equals("v8k3") || !p7.get(6).equals("mq2n")) return false;   // wrap: 5%5, 6%5

        // Fewer players ⇒ fewer, larger fragments than more players (monotonic split shape).
        if (partition(five, 2).size() >= partition(five, 4).size()) return false;

        // Degenerate inputs never throw / never yield garbage.
        if (!partition(five, 0).equals(p1)) return false;   // 0 players clamps to 1 (the whole)
        if (!partition(List.of(), 3).isEmpty()) return false;
        if (!partition(null, 3).isEmpty()) return false;

        // The assembled fragments (players ≤ groups) re-normalize to the SAME string the sign-compare uses,
        // for EVERY roster size 1..groups — the tie that makes the fragments a valid answer. If this drifts,
        // the group could hold the right pieces yet the sign would never accept the join (unsolvable).
        String signTarget = AnswerNormalizer.normalize(whole);
        for (int players = 1; players <= five.size(); players++) {
            String assembled = AnswerNormalizer.normalize(String.join(" ", partition(five, players)));
            if (!assembled.equals(signTarget)) return false;
        }

        return true;
    }
}
