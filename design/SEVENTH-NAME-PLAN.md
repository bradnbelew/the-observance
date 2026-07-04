# THE SEVENTH'S NAME — a plan for a *traceable* name (not a handed-over string)

> Ethan's ask (2026-07-03): can the Seventh's name be something the players can **trace back to
> something they'd recognize**, rather than an arbitrary word we invent? Yes — and it makes the finale
> hit far harder. This plan makes *restoring the name* an earned DERIVATION the players perform, and ties
> it to the reveal (the Watcher IS the Seventh) so the name they reconstruct is the name of the thing
> that has watched them for weeks. Plumbing already exists: `seventh_name` (arc flag / setting) is read by
> the release composer (I built that hook). This plan is about how the name gets DERIVED and SET.
>
> **Status: PLAN (awaiting Ethan's picks — see §6). No code yet.**

---

## 1. THE DESIGN GOAL

- **Earned, never handed.** The name is not printed anywhere legible. It is reconstructed from marks the
  players have walked past for weeks, illegible until they learned the runes — the "it was readable the
  whole time" turn (Tunic/Fez), applied to the emotional climax.
- **Retrace-fair (three-clue redundant).** No single missed clue can lock a group out. Three independent
  paths converge on the same name (§4), plus the whisper rail as the always-there valve.
- **It upgrades the restore act.** Right now "restore vs erase" is a bare marker-touch. Under this plan,
  **restore = knowing and speaking the true name** (a real derivation), **erase = leaving the seal a
  blank** (refusing the name). That is a far more meaningful fork — and it sets `seventh_name` for the
  finale automatically.
- **It pays off the reveal.** When they read the name, two things land at once: *it was on the very first
  thing we ever saw here*, and *this is the name of the Watcher.* The ending's "i have all your names, i
  give them back" becomes: the watching thing, having finally gotten its own name back from you, gives
  you yours.

---

## 2. THE CORE MECHANIC — "the six kept a letter of it each, without knowing"

The game already spells a hidden word from the six keeper maker's-marks: read in **fall-order** (Vaun,
Mara, Sella, Orin, Brann, Iss) they spell **UNKEPT** (the `spine-unkept-acrostic` / `meta-unkept` nodes).
This plan adds the **twin reading**: a SECOND faint glyph at each keeper site that, assembled, spells the
**Seventh's name** — the poetic inverse of UNKEPT. *The six spell what they did-not-keep one way, and the
name of the one they-could-not-keep the other.*

- **Iss's mark is the CUT, not a letter.** Iss is the one who unwrote the Seventh — so at Iss's stone the
  glyph is the **strike-through/erasure mark**, the visible wound where the name was cut. This is *why*
  the name is unwritten, shown mechanically. So **five keepers each preserve one letter** (Vaun, Mara,
  Sella, Orin, Brann) → a **5-letter name**; Iss's mark is the blank that has to be filled back in.
- **The order is learned last, at the unwriting.** The `seventh-unwriting` node already says "the seal is
  a name." We make that literal: its solve reveals the *order* to read the five preserved letters (e.g.
  the order the keepers fell, or a bearing carved on the wall) — not the name itself. Players who solved
  the unwriting but never collected the five marks still have to go read them; players who collected the
  marks but never solved the unwriting don't know the order. Both halves are required — classic
  cross-surface synthesis, no single-screen solve.

---

## 3. THE ACT — restore becomes "speak the name"

Replace the current touch-one-of-two-markers restore with a **name submission** at the unwriting wall
(keeps erase as a distinct act):

- **RESTORE** — the group assembles the five letters + the unwriting's order → they **carve/submit the
  name** (an in-world answer-sign at the unwriting, or the release/oracle path). On a correct match:
  set `seventh_name = <name>`, `seventh_named = true`, `seventh_choice = 'restore'`. The wall's blank
  fills with the name in runes (a `sign_write` / `NameOnWall` beat). This is the restore act.
- **ERASE** — a distinct marker (as today) that leaves the blank a blank: `seventh_choice = 'erase'`, no
  name set. The group that cannot (or will not) name the Seventh still finishes — the finale signs off
  unnamed ("— [ ]"), which the composer already handles.
- Wrong name submissions are silent (the usual oracle non-answer) — never a tell.

This means `seventh_name` is set **by the players' own derivation**, exactly where the finale composer
reads it. The name flows straight into the mask-off farewell and the kick-screen sign-off.

---

## 4. THE THREE TRAILHEADS (retrace-fair; all converge on the same name)

1. **PRIMARY — the six keeper marks + the unwriting order** (§2). The main path.
2. **SECONDARY — "it was there all along": the cold-open anomaly.** The very first marker the group ever
   found (the one that knew their world seed / the prologue anomaly, `placeprologue`) carries the name in
   runes — illegible on day one, readable once they have literacy. When they derive the name the other
   way and go back (or screenshot-check), it was on the first thing the Watcher ever showed them. This is
   the "something they'd recognize" beat at its strongest: the most memorable object in the whole game.
3. **TERTIARY — the whisper rail.** `seventh-unwriting`'s tier-3 hint escalates to near-spell-it (the
   rescue floor), so a stuck group is never hard-locked. (Never reveals the name outright at tier 2.)

Three-clue redundancy per the ARG-craft rule: any one path missed, the other two still land it.

---

## 5. CANDIDATE NAMES (Ethan picks — this is your call, like naming Wren)

A 5-letter old colony name (matches Vaun/Mara/Sella/Orin/Brann/Iss — short, hard, old), ideally carrying
a faint apt meaning for the one who *kept the light and was cast out for nothing*:

- **AVREN** — invented, old-sounding, soft; no baggage. (Safe default.)
- **SEREN** — Welsh for *star* (light, peace) — recognizable, a quiet meaning for the keeper of the kept
  light. Slightly more "real-world" than the others.
- **MAERL** / **MAREN** — old, gentle, colony-adjacent.
- **ELDIS** / **AERIN** — softer, more elegiac.
- Keep **VAEL** (my current placeholder, 4 letters) — then only four keepers carry a letter and Iss's cut
  still stands; the mechanic flexes to any length.

*(The exact letter → keeper distribution is set once the name is chosen. E.g. AVREN → Vaun:A, Mara:V,
Sella:R, Orin:E, Brann:N, Iss:✕.)*

---

## 6. THE ONE DECISION — which kind of "recognize"?

Ethan's phrase "trace back to something they'd recognize" has two readings. They're not exclusive, but
they change what we build:

- **(A) In-fiction traceable (recommended, this plan).** "Recognize" = *we've seen these marks / this was
  on the first thing we found.* Fully buildable now; strongest as an ARG puzzle; pays off literacy + the
  reveal. Everything above assumes this.
- **(B) Meta / personal.** "Recognize" = something the *real friend group* would know from outside the
  game — the server's name, a shared in-joke, a date, an anagram of something of theirs. Riskier
  (bespoke to your group, harder to keep spoiler-safe, leans on the consent line), but can be layered
  ON TOP of (A): the derived in-fiction name could *also* be a nod only your group catches. Tell me if
  you want a meta layer and what it should reference.

---

## 7. BUILD TASKS (once §5 + §6 are chosen)

1. **Pick the name + letter distribution** (Ethan) → set `seventh_name` canon (config/setting/lore).
2. **Seeds:** add the name-submission accepted_answers to a restore node (or a new `seventh-name` node)
   that sets `seventh_name` + `seventh_named` + `seventh_choice='restore'`; keep the erase marker.
   Author the unwriting's *order* reveal (its solve/hint) + the five per-keeper second-mark carvings.
3. **Plugin:** the five faint second-glyph carvings at the keeper sites (rune font); the erasure mark at
   Iss's; the cold-open anomaly rune-name (SECONDARY trailhead); the wall-fills-with-the-name beat on a
   correct restore.
4. **Voice:** the unwriting order-reveal line; the "the six kept a letter each" framing; the restore
   confirmation. All in `voice.ts`.
5. **Wire:** the restore submission → `seventh_name` (the composer hook already reads it). Update the
   finale-marker command / `/observance finale` for the new restore-by-name surface.
6. **Green + retrace-fair critique + playtest.**

---

### TL;DR
Make the name a **five-letter word the six keepers spell between them** (Iss's mark is the cut that
unwrote it), with the reading-order learned at the unwriting, and the same name hidden in runes on the
**first anomaly** the group ever found. Restoring the name = deriving and speaking it (which sets
`seventh_name` straight into the finale); erasing = leaving the blank. Pick a name from §5 and tell me
(A) in-fiction or (B) also a personal/meta nod, and I'll build it.
