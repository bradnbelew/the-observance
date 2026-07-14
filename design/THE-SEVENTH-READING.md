# THE SEVENTH READING — the capstone cipher (the six keepers spell the name)

> **SUPERSEDED PRE-V5 ARCHIVE — DO NOT USE THIS CAPSTONE OR ITS FLAGS.** Current AVERYN synthesis and finale are defined in the V5 casebook and finale authority.

> The final puzzle. The Seventh's name — **AVERYN** — was cut from the record, but the six keepers each
> hid one letter of it before they fell, and each could keep it only the one way they knew: **in their
> own technique.** To read the name the group returns to all six and reads each *in their own tongue* one
> final time — the final exam over everything the game taught, six DIFFERENT techniques (no Vigenère
> rerun). Read in **fall-order** (Vaun · Mara · Sella · Orin · Brann · Iss) the six letters spell it.
> **Saying the name IS the release trigger** (the mask-off farewell → the world dies → the kick).
>
> **Status: BUILT + GREEN.** The trigger row, the flag-chain into the finale, the confirmation line, the
> hint rail, the capstone-integrity guard (`seventh-reading.selftest`, folded into `specscheck`), AND the
> in-world carving (`/observance reading` stamps all six fragments — §3) all ship green. The integrity
> guard proves the six fragments round-trip under their real ciphers and spell AVERYN. Nothing manual
> remains but running the command once the keeper sites are placed (+ optional site-reveal polish).
>
> **Canon note (2026-07-05 — the Watcher-identity fork, closed elsewhere, confirmed compatible here.)** This
> capstone already plays true under the closed synthesis (`canon-spine.md §0`, `WORLD-BIBLE.md §3`,
> `FINALE-THE-RELEASE.md §1`): the six did not each independently choose to protect a stranger's secret —
> they *are* the one hand that had the secret, split six ways, so no single erasure could take the whole
> name at once. Nothing below needs to change; the mechanic was always compatible; it just wasn't yet true
> on purpose.

---

## 1. THE SIX FRAGMENTS — one letter each, six different techniques

Each fragment DECODES to a short keeper confession whose **first token is the letter that keeper kept**
(so the letter is *earned by the reading*, never sight-read). Machine-verified in `seventh-reading.ts`.

| Fall # | Keeper | Technique (their own tongue) | Decodes to (the confession) | Letter |
|---|---|---|---|---|
| 1 | **Vaun** | **Caesar** (his shift 3) | *"a — the first of their name; i kept it as i kept everything and gave none back"* | **A** |
| 2 | **Mara** | **Book cipher** (a capstone shelf) | *"v — i read it and did not walk it to them"* | **V** |
| 3 | **Sella** | **Atbash** + the reflection in the water | *"e — i kept it at the far water"* | **E** |
| 4 | **Orin** | **The bow** — his runes appear only to a hand that **crouches** (posture-gated, not a decode: the keeper who would not bow gives his letter only to one who does) | *"r — i would not bow to give it; i give it now"* | **R** |
| 5 | **Brann** | **Rail-fence** (rails = his fire-count), legible **only after dark / on the black moon** | *"y — i kept it lit by the one fire"* | **Y** |
| 6 | **Iss** | **The catch, one last time** — read straight, a warm lie (the last letter is "m"); the truth is the acrostic (first mark of each line, down — the prophet-wall way) | true: *"n — the last of it i cut and called m"* | **N** |

**Fall-order → A·V·E·R·Y·N → AVERYN.**

**Why this isn't "see the rune":** five of the six are real work — a Caesar decode, a book-cipher lookup,
an Atbash reflection, a rail-fence unravel by night, and a *lie you must detect and correct.* The sixth
(Orin) is gated by the **bow** — his letter does not exist for a hand that stands; you have to crouch to
receive it, which is Orin's whole arc. Each fragment pays **story** (a last confession from a dead keeper),
not just a letter.

**Iss's last lie (the catch, one final time).** Read straight, his fragment tells you the last letter warm
and wrong ("m", be easy). Catch him the way you caught him before — read the **first mark of each of his
lines, down**:

```
 i told you the last of it was m          → I
 take the first mark of each line down    → T
 see what the warm words were laid over   → S
 n is the letter i cut and called m       → N
```
→ **I·T·S·N** = "it's n." The Seventh's name is completed by *refusing to trust the warm one, even now* —
the whole game's lesson, delivered as the final beat.

---

## 2. THE TRIGGER — "say the name" fires the finale (BUILT)

- Seed row **`seventh-name`** (`puzzles_seed.sql`): `accepted_answers = ['averyn']`. Gated
  `{seventh_named, bowed_as_one}` (`metapuzzle_seed.sql §2`) — only sayable at the very end, after the
  Seventh is named AND the Accepting is made.
- On a correct say (`averyn`, on any surface — in-world sign, Discord `/answer`, the record terminal, or
  **spoken aloud** via the voice tier for maximum impact), `set_flags` sets:
  `seventh_name='averyn'` · `seventh_choice='restore'` · `seventh_named=true` · `record_released=true`.
- `record_released` → the showrunner's `runReleasePass` composes the mask-off farewell (signing off
  *"— averyn, the seventh, kept no longer"*) and enqueues `the_closing` (the world dies + the kick). The
  composer already reads `seventh_name`.
- Confirmation line the instant before the world closes: `voice.oracleSeventhName()` — *"the seal was a
  name. it is spoken. it gives."*
- **RESTORE = say the name** (this). **ERASE = leave it** (the release marker / `ReleaseRiteListener`,
  unchanged) → the finale fires unnamed (`— [ ]`). Both end the game.
- Whisper rail (`hints_seed.sql`, tiers 2–3) points at the READING (go back to the six, read each their
  own way, catch Iss) — never the name itself. The rescue floor, so no group hard-locks.

---

## 3. WORLD-BUILD — carve the six fragments (now ONE command)

**Run `/observance reading`** at any point after the keeper sites are placed (placeregion/placedeep). It
stamps all six fragments as persistent rune carvings (TextDisplay) at the keeper sites automatically —
Vaun/Sella/Orin/Brann in the `observance:runes` font, Mara's book-refs + Iss's warm-prose acrostic in
plain text (numbers + readable words, as their stones are). Skips any keeper site not yet placed. The
strings it carves are exactly those below (kept in sync with the discord `seventh-reading.ts` source that
the integrity guard verifies). Hand-carving is no longer required — but the exact strings are here if you
want to place them by hand or add the site-specific reveals (Orin low/crouch, Brann night, Sella
reflection — the command places them plainly; those reveals are optional polish, the ciphers are the work):

- **Vaun** (Caesar, at `stone_vaun`): `D WKH ILUVW RI WKHLU QDPH L NHSW LW DQG JDYH QRQH EDFN`
- **Mara** (book, at `stone_mara` — carve the capstone shelf, see below): `1-1-1  1-1-5  2-1-2  2-1-3  1-1-7  1-1-8  1-1-9  3-1-4  2-1-3  3-1-6  3-1-7`
- **Sella** (Atbash, at `the_far_water` — reflection-carved): `V R PVKG RG ZG GSV UZI DZGVI`
- **Orin** (crouch-revealed runes, at `stone_orin`): `R I WOULD NOT BOW TO GIVE IT AND GIVE IT NOW` — placed so it is legible **only when crouched** (low/angled, like his stone).
- **Brann** (rail-fence, at `stone_brann`): `YK LBHNI  ETI I YTEOEFRIPTT   E` — legible **only after dark / on the black moon** (beacon-glow reveal, like `stone-brann-cipher`).
- **Iss** (the acrostic lines, at `stone_iss` / the prophet wall): carve the four lines in §1 verbatim.

**Mara's capstone shelf** (the book her fragment indexes into — place these four lines as the lectern
books at her site, in order):
```
v is a way i marked and did not take
i read it and i read it again
and did not walk it to them not once
walk it to them now if you still can
```

The reveal (`voice.revealWatcherIsSeventh`) already points to "the wall where you named me"; the unwriting
frames the reading (*"the seal is a name; the six kept a letter of it each"*). Everything else is wired.

---

## 4. INVARIANTS / NOTES
- **AVERYN is the canon name** — set in the seed (`seventh-name`) and verified by `seventh-reading.ts`. To
  change it later, edit the fragments' plaintexts so their first tokens spell the new name and update the
  seed answer; `readingSelfTest` fails the build if they drift apart.
- Six DISTINCT techniques (Caesar · book · Atbash · bow-gate · rail-fence · acrostic-catch) — no Vigenère
  reuse (the catch here is the acrostic, not a keyed decode).
- Retrace-fair: every technique is one the group already mastered; the reveal + hint rail breadcrumb the
  reading; Iss's lie is caught the same way as the original catch.
- Broad: six sites across the whole world + a night/black-moon condition (Brann) + the crouch (Orin) — it
  cannot be solved from one place or one screen.
