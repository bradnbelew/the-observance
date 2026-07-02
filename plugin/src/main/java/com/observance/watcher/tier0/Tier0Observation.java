package com.observance.watcher.tier0;

/**
 * OBSERVER TIER-0 — the closed set of GROUNDED observations the cheapest tier can derive from a
 * player's REAL in-world signals (BUILD-PLAN §13 / CHANGE-MANIFEST A2). This tier needs NO chat,
 * voice, or LLM: it profiles behavior only, so it is the always-works, ethically-cleanest tier.
 *
 * <p>Each observation is an IMPLICATION about behavior that is TRUE of the specific player from the
 * {@link com.observance.watcher.signal.SignalSnapshot}. It is deliberately lore-agnostic here — it
 * carries only:
 * <ul>
 *   <li>a stable {@link #key()} (used for the config line map + a per-observation delivery cooldown),</li>
 *   <li>a fallback implication line (lowercase, sparse, certain, IMPLICATION not accusation) used only
 *       if {@code config.yml}'s {@code tier0.lines} has none for this key.</li>
 * </ul>
 *
 * <p>The register is the Watcher's: it names what it has SEEN, never what the player IS. "you keep one
 * thing you never use", not "you are a hoarder". A line must be earnable — the selector only emits an
 * observation when the underlying signal clearly supports it, otherwise it emits nothing (silence).
 *
 * <p>This is a pure enum with no Bukkit references, so {@link Tier0Selector} and this type compile and
 * self-test with {@code javac} alone (mirrors {@code FlagGateSelfTest}).
 */
public enum Tier0Observation {

    /** Mines alone, in the dark, far from the others — the loner underground. */
    KEEPS_TO_THE_DARK("keeps_to_the_dark", "you keep to the dark, alone"),

    /** Consistently the farthest player from the group. */
    ALWAYS_THE_FARTHEST("always_the_farthest", "you are always the farthest"),

    /** Carries a heavy hoard — keeps much, and it stays kept. */
    KEEPS_WHAT_IS_NEVER_USED("keeps_what_is_never_used", "you keep one thing you never use"),

    /** Has died, repeatedly, down where the light does not reach. */
    DIES_IN_THE_DARK("dies_in_the_dark", "you die where the light does not reach"),

    /** Goes deeper than the others go — always downward. */
    ALWAYS_GOES_DEEPER("always_goes_deeper", "you always go deeper"),

    /** Takes and takes — a great deal broken, almost nothing given. */
    TAKES_AND_TAKES("takes_and_takes", "you take, and you take");

    private final String key;
    private final String defaultLine;

    Tier0Observation(String key, String defaultLine) {
        this.key = key;
        this.defaultLine = defaultLine;
    }

    /** Stable opaque key: the config-line map key AND the per-observation delivery cooldown key. */
    public String key() { return key; }

    /** Fallback implication line if config supplies none for {@link #key()}. Never null/blank. */
    public String defaultLine() { return defaultLine; }
}
