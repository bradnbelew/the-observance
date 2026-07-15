# 03 — Prose, Voice, and Media

Two hard laws (Brad's, verbatim intent): **(1) no AI slop. (2) no ultra-mysterious ARG language.**
Everything a player reads must sound like **real human records and conversation** in a real world — not
a narrator telling them they're in an ARG. This doc lists the specific, known violations to fix and the
rules for any new copy.

Line numbers below are *approximate* (Wave 1/2 edits shifted them). **Grep the exact quoted string** —
that's stable.

---

## 1. The AI-slop fingerprints to purge

Search-and-destroy these tics across `arc/v5/*.json`, `arc/corpus/*`, and the plugin's sign/lectern
text in `plugin/.../command/ObservanceCommand.java`:

- **Perfect parallelism at corpus scale.** The six keeper affidavits are one machine:
  *"I did X. I should have Y. [Exhibit] is filed with this statement. -Name."* Give each keeper a
  distinct hand (Orin's certificate already proves the corpus *can* — the affidavits don't). A shared
  legal *form* is fine; identical *sentence rhythm* in the confessions is the tell.
- **"Not X but Y" constructions** (endemic in the plugin lecterns): "This is not a rehearsal room. It
  is the old result." / "The date is not prophecy. It is appointment language." / "The altar is not
  worship. It is where the keepers stopped pretending procedure was mercy." Individually fine; at this
  density it's a fingerprint. Rewrite most to plain statement.
- **Planted aphorisms** dressed as speech: "A signed manual carries more weight than a spoken warning."
  / "No one can remove the finding by removing its author." / "A record is meant to serve the person
  named in it." Cut or bury; people don't speak in thesis statements.
- **Rule-of-three portent** (retired V4 corpus, but still bundled): "it waits, and it watches, and it
  takes what stops being watched." Keep it out of anything that can ship.

## 2. The ARG-mystery-voice to purge — and the worst offenders

The single worst class: **facilitator/design notes carved into the in-world fiction.** These break the
world instantly. Purge every one (grep the quoted phrase):

- `"finale button"` — lectern *case board*: "the accepting floor stops being a **finale button**."
  Game-design vocabulary inside the fiction. Disqualifying.
- `"the room should feel wrong"` — lectern *threshold note*: "If the group reaches this alone, the room
  should feel wrong." A director's stage note.
- `"Parallel work is expected"` — lectern *open rows*. Facilitator whiteboard; names the players'
  workflow.
- `"A group should argue here"` — record station *threshold hands*. GM advice signed as a work note.
- `"keep reading"` / "If the market feels too ordinary, keep reading." — record station *closure
  docket*. Addresses the player's aesthetic experience.
- `"the feeling of certainty"` — lectern *vault split*: "The sign accepts the assembled form, not the
  feeling of certainty." Mechanics-speak about an input widget.

Then the **voice-of-the-dungeon signs** — grounded civic signage, not portent. A real Hold sign says
`VENT EAST BEFORE SECOND BELL`, not "the stair / ends where / the count / turns" or "the archive / does
not open / for noise." Rewrite the ~10 portent signs to competent, mundane records. (The game already
proves it can do this — book `lc_school_day` and the ticket #9137 thread are the register to match.)

**Already fixed in V5.1 (do not redo, just don't regress):** the two finale title cards
("THE RECORD CLOSES / Do not look away." → "the record is closing"; "RECORD // SEVERED / The server is
saying goodbye." → "i am still here / for a moment").

**Evidence-item relay outputs** (WR01): "NAMES GO HOME BEFORE BODIES" and "THE SHAPE LEFT BEHIND IS
ALSO A PERSON" are poet-oracle. The antagonist has *partial* license, but rewrite these to sound like a
monitoring system regurgitating overheard speech (the sibling line "THREE CUTS STILL MEAN RETURN" works
because it answers a logged question — match that).

## 3. NPC dialogue — break the machine meter

`arc/v5/npc-dialogue.json`: nearly every line (all five townsfolk + Wren) is a **two-sentence couplet
of the same length and rhythm.** The *content* is good; the *meter* is a machine's. Fix:
- Give each speaker one syntactic habit (a clipped trader, an over-explainer, someone who trails off).
- Vary sentence length and count. Let Wren's confession *fracture* as it escalates (before_c07 →
  evidence → confession → reckoning currently reads at one flat cadence throughout).
- Superseded V4 corpus had real idiolects ("Sit, sit, you're letting the cold in..."); V5 de-slopped
  the cast into starch. Restore *character*, not slop.

Run `python tools/check_voice_register.py` after edits — it lints the Watcher/Keeper register (caps,
named emotions, chiasmus). It currently passes; keep it passing.

## 4. The bundled legacy corpus (a live regression risk)

`discord/src/voice.archive.ts` and retired blocks of `discord/src/voice.ts` are the pre-V5 campaign.
They are **suppressed** in production (`run.ts` routes to `runV5SafeHeartbeat` when
`isV5CampaignActive()`), and the live flag is `v5`. **But they still ship**, and they contradict V5
canon (they have an *eight-year-old Sella*, a lampwright Mara, "seven ways / deep-bird / black moon"
cosmology). One campaign-flag regression from posting. Also, `voice.whisperReply` tier-1 is a hardcoded
fossil ("look again at what repeats. it is not stone. it is sound.") that's wrong for nearly every V5
node. Fix:
- Quarantine or delete `voice.archive.ts` and the retired `voice.ts` blocks from the shipped bundle.
- Make tier-1 whisper speak the node's seeded H1 hint, not the fossil line.
- Confirm `check_v5_content.py`'s forbidden-runtime-pattern scan (it greps for `six-as-one`,
  `mkept-not-person`, etc.) still passes — it's the tripwire for exactly this contradiction.

## 5. Media de-branding

The five `required_media` payloads are timed correctly, but delivery punctures the found-footage frame:
- Raw `youtu.be/...` links (channel identity, recommendations, comments) for the four video clips.
- A Dropbox folder link (`dl=0`, opens Dropbox chrome) for the Averyn spectrogram.
Fix: host the clips behind the dashboard's own media route (the `the-hold.zip` route proves the
pattern) or at minimum unlisted YouTube on a diegetic channel + a `record`-styled embed page; replace
the Dropbox link with a direct-download WAV. Also add a Discord/website mirror so absent players learn
a clip arrived (currently they must re-scroll the archive footer — the "world moved and I missed it"
gap). Media is Google-Drive-hosted per Brad's setup, so this is **Codex's domain** (doc 7).

## 6. Minecraft text formatting — the real limits (research, per Brad's rule)

Author within these or the client silently truncates/wraps and breaks a puzzle:
- **Written book**: title ≤ 32 chars; author ≤ 32; **≤ 100 pages**; a page renders ~**14 lines × ~19
  chars** and hard-caps near **798 chars** — but the repo's own authority caps pages at **240 chars**
  (`check_v5_content.py` enforces it). Stay ≤ 240/page. Line breaks inside a page are client-wrapped, so
  **never build a puzzle that depends on which physical line a word falls on** unless you control it —
  the KM02/LS04 "count the written lines exactly as displayed" investigation is fragile for this reason;
  if you keep a shelf-line-word address, pin the wrapping (short lines, explicit newlines) or key it off
  a numbered physical shelf instead (doc 1 makes shelves countable).
- **Sign**: 4 lines, ~**15 chars/line** front (and back). Wall signs need a solid backing and correct
  facing (the installer self-heals backing; keep facing authored).
- **Lectern**: holds one written book; the book obeys the book limits above.
- **Item display name / lore**: keep names short and unmistakable; lore lines are the tooltip. The
  evidence-appearance authority (`arc/v5/evidence-item-appearance.json`) renders title = gray display
  name, lore = dark-gray lines. Do not print the puzzle answer in lore (that was the Unlit's original
  sin — doc 2).

## 7. Verify

- `python tools/check_voice_register.py` — Watcher/Keeper register lint.
- `python tools/check_v5_content.py` — book page limits, forbidden-canon patterns, no placeholder copy,
  exact evidence-appearance inventory.
- `python tools/check_deep_hold_book_manuscripts.py` — book page-count/extraction contracts.
- A human read of any rewritten passage against §1–§2: if a line could be a design note, a thesis, or a
  fortune cookie, it fails.
