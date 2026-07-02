# THE OBSERVANCE — FIRST PLAYTEST (the vertical slice)

> Written 2026-07-02, the day the full stack went live (plugin 0.2.2 + resourcepack + datapack on the
> server · Discord bot + showrunner cron on Railway · the Record website on Vercel). This is the ONE gate
> the design's own checkpoints demanded: **see the loop work end-to-end, live.** One run proves ~15
> "wired-but-unproven" layers at once. Solo is fine — you are the group.

## 0. PRE-FLIGHT (confirm all four surfaces are up)
1. **Server:** `/observance status` → `supabase:true · last db call ok:true · queued:0`.
2. **Bot:** it shows online in your server and `/whisper` autocompletes (if the slash commands are missing,
   re-invite with the `applications.commands` scope — see RUNBOOK).
3. **Website:** open `/record` (public archive renders) and `/author` (your admin console loads).
4. **Cron:** the showrunner runs every 10 min and **exits 0** each time (a quiet finish is success, not a crash).

## 1. BUILD THE WORLD (once, on flat open DRY ground)
```
/observance placeregion      # rosetta + 6 keeper stones, seated on the terrain, each with a blank answer-sign
/observance placedeep        # the deep-half payoff sites (later-weeks; place them elsewhere)
/observance townsfolk spawn  # REQUIRED: the 5 surface townsfolk (Aro/Wenna/Coll/Dob/Pell) — placeregion does
                             #   NOT spawn them, and without this the world's social surface is dark
/observance status           # sites placed should jump to 7 (or 13 with placedeep)
```
Eyeball: a branching field (not a flat row), sparse light, each keeper stone has a **blank** wall-sign or
lectern. If any float, you're on uneven/forested ground — re-run somewhere flatter.

## 2. RUN THE SLICE (the proof)
1. **Ignite:** sneak + right-click the rosetta, OR admin `/observance flag set prologue_ignited true`.
2. **Solve the first gate by hand** — read the rune ring, then edit a **blank keeper-stone sign** (Vaun or
   Sella have wall-signs; Mara/rosetta use lecterns). Split across the 4 lines at word boundaries:
   ```
   bow offering kept
   light deep line
   unspoken sacred
   beast
   ```
   → `/observance flag list` should now show `rosetta_known: true`, opening the six keeper stones.
3. **Watch for the payoff** (this is the ~15 layers proving themselves at once):
   - a **reward toast** pops (datapack + resourcepack live),
   - the **rune font** renders in-world (resourcepack live),
   - the **author dashboard** reflects the flag/solve (website ↔ DB),
   - within ~10 min the **showrunner cron** drips a clue/thread to **#the-record** (bot + cron live),
   - the **record website** un-redacts a notch as `arc_state` advances.

## 3. SPOT-CHECK THE REST (fast-forward with flags)
Use `/observance flag set <flag> true` to REACH any beat without grinding (bypasses the puzzle — use to
eyeball, not to test the solve): `iss_caught` · `undercroft_open` · `seventh_suspected` · `seventh_named`
· `threshold_open` · `true_coord_known`. Companion arc: `/observance wren spawn` → `flag set
companion_revealed true` → `/observance wren reckoning` → pick a branch → confirm his last-words fire +
the finale posts to #the-record.

## 4. WHAT TO CAPTURE (this is playtest DATA)
Findability (dead air?) · does the sign-as-answer read as obvious? · do beats/toasts land? · does the
Discord drip feel alive or noisy? · pacing. That feedback is what turns "it runs" into "it's good."

## 5. KNOWN — not active yet (don't mistake these for bugs)
- **Keeper voices** (6 per-keeper OGGs) are built in source but need a **resourcepack re-host** (new zip +
  sha1) to be heard. Current pack has the rune font + the 4 base ambient sounds.
- **Observer Tiers 1–2** (the chat/voice "it knows your name" scares) are **scope decisions**, not built:
  the LLM director brain and the `0008_observations` table (see `design/LAYER-LEDGER.md`). Tier-0 (behavior
  implication) is live.
- **The deep half** (Undercroft descent, the Seventh finale, the co-op vault) is later-weeks content; the
  `the_threshold` deep-site answer sign still needs a corridor redesign (tracked).
- **Reward-the-theory batch-confirm** demonstrates for the **Iss** cluster in week one (stone-iss-wall →
  iss_key_turned → iss-which-is-true). Other keepers' second cluster-evidence needs the `object`/`code`
  plugin producers (vaun-hoard-sorted, vaun-bookshelf-tally, mara-lectern-lock, …) — flagged "NEEDS PLUGIN
  PRODUCER" in the seed. Until they ship, those keepers un-redact on the **stonesRead fallback** (correct +
  backward-compatible), just not via assembled theory. **Top post-playtest build item.**
- **Literacy debt (minor, fair):** the 10 rune-cribs teach every letter in the rosetta answer **except U**
  (in "unspoken"). Cold players infer that one letter from context (fair by the ~25-char cryptanalysis proof).
  Admin-guided slice is unaffected. Add a U-crib only if a cold real-player run stalls.
- **`v_record` theories view** (S-D theory un-redaction) is not yet applied to live Supabase — the route
  falls back to stonesRead correctly meanwhile. Deploy with the migrations (OPS list) to activate it.
- **Cold-open P2:** on a `computeAutonomyGates()` crash, curatorial drip defaults to allowed (`?? true`, a
  deliberate test back-compat). Irrelevant to an admin run (you fire `prologue_ignited`). Harden later with a
  test update, not a bare flip.

**Bottom line:** if the slice in §2 lands end-to-end, week one is proven. Everything else is tuning + the
two scope calls, not missing pieces.
