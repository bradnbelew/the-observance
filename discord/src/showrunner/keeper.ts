/**
 * keeper.ts — the Keeper-NPC dialogue resolver (D8 `backlog-keeper-npc-framework`, FACT 9, WEB-MASTER §7).
 *
 * THE GAP THIS CLOSES. `KeeperNpcBeat.java` fires on `NPCRightClickEvent` and renders a JSON-chat
 * dialogue tree, but the BRANCHING — which node a given player sees, conditioned on their measured
 * dossier + the arc state — is policy, not Java. This is that pure resolver: dossier + arc flags +
 * per-window state → the dialogue node (a voice key + its children) the NPC should speak to THIS player.
 *
 * DISTINCT MODULE (the manifest's hard rule). `keeper.ts` (this dialogue resolver) is NOT
 * `keeper-record.ts` (the Hold-Book page producer) and is NOT `reckoning.ts` (the difficulty engine).
 * It closes FACT 9 / LORE-BIBLE TODO-3 by the DIALOGUE route — `fact9.named` names the logged M-I beat
 * (the run wrapper reads `event_log` for it); the M-IV atonement node withholds a fragment until a
 * broken custom is honored.
 *
 * FACT-9: ONE SURFACE PER PLAYER PER WINDOW (coherence Batch-2 P1-5). The three FACT-9 surfaces — the
 * Hold-Book keeper-column re-read (keeper-record.ts), the offline-skin named line, and THIS dialogue
 * line — are the spine's deliberate ≥2 doors, but a given player meets FACT 9 through ONE surface per
 * window, never three spotlights at once. This resolver takes a `fact9ShownThisWindow` flag (set when
 * another surface already delivered it) and withholds its own `fact9.named` node when it is set.
 *
 * DEFERS TO THE APPARITION SLOT (INV-18). The per-player prior-keeper APPARITION (ZNPCsPlus) is ambient
 * and must defer to the conductor's `apparitionClaim` — this resolver takes `apparitionClaimedFor` and
 * returns no apparition-gated node for a player the conductor did not claim this window. The PRESIDING
 * Keeper (Citizens2) is a fixed NPC the player walks up to — it is NOT ambient, so its base dialogue is
 * always available; only the prior-keeper apparition node is slot-gated.
 *
 * PRECISION (the privacy law). A dossier-conditioned node names a behavior only on a MEASURED signal to
 * a confident margin; a flat dossier gets the neutral presiding-Keeper node, never a guessed callout.
 * The atonement node names the broken custom ONLY when it was genuinely measured as broken.
 *
 * PURE. No DB / network / clock / LLM. Every node is an authored voice key (`keeper.*` family;
 * determinism is the backstop). keeper.run.ts reads the dossier + `event_log` (for the M-I beat) + arc
 * flags + the window FACT-9 marker + the conductor claim, resolves the node, and renders it through the
 * Java beat. keeper.selftest.ts imports this with nothing.
 */

/** The keepers a prior-keeper apparition can embody (fall-order). */
export type KeeperId = 'vaun' | 'mara' | 'sella' | 'orin' | 'brann' | 'iss';

/** Which NPC is speaking — the fixed presiding Keeper vs a per-player prior-keeper apparition. */
export type KeeperKind = 'presiding' | 'prior';

/** The player's measured dossier reduced to what the dialogue branches on. */
export interface KeeperDialogueDossier {
  groupKey: string;
  name: string | null;
  /** the keeper this player rhymes with, or null when the dossier is flat (precision). */
  rhymesWith: KeeperId | null;
  /**
   * a custom this player has genuinely broken (measured), or null. Drives the M-IV atonement node —
   * which withholds a fragment until it is honored. NEVER a guess.
   */
  brokenCustom: string | null;
  /** has the broken custom since been honored (the atonement condition met)? */
  atoned: boolean;
}

export interface KeeperDialogueInput {
  /** which NPC the player interacted with. */
  kind: KeeperKind;
  dossier: KeeperDialogueDossier;
  /** arc_state.flags.iss_caught — gates the Iss cold node-text swap (same flag everywhere). */
  issCaught: boolean;
  /** the current arc movement (1..5) — the M-IV atonement node is movement-IV gated. */
  movement: number;
  /**
   * the logged M-I beat for this player (read from `event_log` by the run wrapper), or null. When
   * present, `fact9.named` can name the real first thing the record noted of them (FACT 9 delivery).
   */
  loggedFirstBeat: string | null;
  /** another surface already delivered FACT 9 to this player THIS window (P1-5: one surface per window). */
  fact9ShownThisWindow: boolean;
  /**
   * INV-18: did the conductor claim an apparition for THIS player this window? The prior-keeper
   * apparition node is withheld when false (only the presiding Keeper's non-ambient dialogue is free).
   */
  apparitionClaimedFor: boolean;
}

/** The resolved dialogue node the NPC should speak to this player. */
export interface KeeperDialogueNode {
  /** the authored voice key for the node body (TS-VOICE owns the text; this is a KEY). */
  voiceKey: string;
  /** does this node deliver FACT 9 (names the logged M-I beat)? at most one surface per window. */
  deliversFact9: boolean;
  /** does this node withhold a fragment pending atonement (the M-IV node)? */
  withholdsFragment: boolean;
  reason: string;
}

export interface KeeperDialogueDecision {
  node: KeeperDialogueNode | null;
  notes: string[];
}

/**
 * resolveKeeperDialogue — the pure dialogue resolver. Fixed precedence:
 *   1. prior-keeper apparition NOT claimed by the conductor → no node (defer to the slot, INV-18).
 *   2. M-IV atonement: a measured broken custom, not yet atoned, at movement >= 4 → the withholding node.
 *   3. FACT 9: a logged M-I beat exists AND no other surface showed it this window → the named node.
 *   4. the dossier-rhymed node (Iss cold-swapped post-catch) when the player confidently rhymes.
 *   5. the neutral presiding-Keeper node (the floor — flat dossier, no callout).
 * Same input → same node.
 */
export function resolveKeeperDialogue(inp: KeeperDialogueInput): KeeperDialogueDecision {
  const notes: string[] = [];
  const d = inp.dossier;

  // 1. The prior-keeper apparition is AMBIENT → it defers to the single-arbiter slot (INV-18). The
  //    presiding Keeper is a fixed NPC (not ambient) and is never slot-gated.
  if (inp.kind === 'prior' && !inp.apparitionClaimedFor) {
    return { node: null, notes: ['prior-keeper apparition not claimed by the conductor this window — defer (INV-18)'] };
  }

  // 2. M-IV atonement — only on a genuinely-measured broken custom, withheld until honored.
  if (inp.movement >= 4 && d.brokenCustom && !d.atoned) {
    return {
      node: {
        voiceKey: 'keeper.atone.withheld',
        deliversFact9: false,
        withholdsFragment: true,
        reason: `M-IV atonement: ${d.name ?? d.groupKey} broke ${d.brokenCustom}, not yet honored — fragment withheld`,
      },
      notes,
    };
  }
  if (inp.movement >= 4 && d.brokenCustom && d.atoned) {
    return {
      node: {
        voiceKey: 'keeper.atone.cleared',
        deliversFact9: false,
        withholdsFragment: false,
        reason: `M-IV atonement honored (${d.brokenCustom}) — fragment released`,
      },
      notes,
    };
  }

  // 3. FACT 9 — name the logged M-I beat, but ONLY if no other surface delivered it this window (P1-5)
  //    and the player is namable (precision: never a nameless callout).
  if (inp.loggedFirstBeat && d.name && !inp.fact9ShownThisWindow) {
    return {
      node: {
        voiceKey: 'keeper.fact9.named',
        deliversFact9: true,
        withholdsFragment: false,
        reason: `FACT 9 via dialogue: names the logged first beat (${inp.loggedFirstBeat}); sole surface this window`,
      },
      notes,
    };
  }
  if (inp.loggedFirstBeat && inp.fact9ShownThisWindow) {
    notes.push('FACT 9 already shown this window by another surface — withholding the dialogue node (one surface per window)');
  }

  // 4. The dossier-rhymed node — Iss cold-swapped post-catch (same iss_caught flag everywhere).
  if (d.rhymesWith) {
    const cold = d.rhymesWith === 'iss' && inp.issCaught;
    return {
      node: {
        voiceKey: cold ? 'keeper.iss.cold' : `keeper.rhyme.${d.rhymesWith}`,
        deliversFact9: false,
        withholdsFragment: false,
        reason: cold
          ? 'iss_caught — the liar node-text reads cold (same flag as the activation lane)'
          : `dossier rhymes with ${d.rhymesWith} — the rhymed node`,
      },
      notes,
    };
  }

  // 5. The neutral floor — flat dossier, no callout (precision over recall).
  return {
    node: {
      voiceKey: 'keeper.presiding.neutral',
      deliversFact9: false,
      withholdsFragment: false,
      reason: 'flat dossier — the neutral presiding-Keeper node (no guessed callout)',
    },
    notes,
  };
}
