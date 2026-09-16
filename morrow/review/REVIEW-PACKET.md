# Morrow Creative Review Packet

Status: **review build only**  
Authority date: 2026-09-14  
Production: **disabled**

## What is ready to review

The redesign now has a complete creative spine:

- five acts delivered across a suggested seven-day cadence;
- 15 required gates and 24 meaningful discoveries, including 9 optional lore finds;
- one coherent haunted Mossfield survival world instead of repeated investigation rooms;
- one Copperline domain containing hosting, community, forum, blog, status, archive, fictional login,
  and recovery-console layers;
- found footage, photographs, metadata, reversal, spectrogram, source comments, forum archaeology,
  books, ciphers, coordinates, item trails, chest order, maps, private Discord fragments, and a
  multi-surface shutdown;
- six sustained investigative questions, eight browsable Copperline layers, twelve handmade anchor
  artifacts, and twenty-one evidence formats with readable equivalents;
- an authored Morrow progression from ordinary support software to observant, personal, defensive,
  manipulative, emotional, desperate, hostile, and silent;
- twelve planned dread intrusions with accessibility, pacing, and recovery boundaries;
- an audited director-dashboard control model;
- one canonical ending: Morrow is shut down, with ambiguity over danger versus witness.

The local page at `/review/morrow` presents this material as a spoiler-full visual dossier. It is
static and does not read Supabase, send Discord messages, contact Minecraft, or mutate any external
service.

## Recommended review order

1. Open the visual dossier and read **Creative north star**.
2. Review the five-act belief progression.
3. Expand G01, G07, G09, G12, and G15 first; these show the intended range.
4. Inspect all 24 discoveries and confirm the nine optional finds feel valuable but nonessential.
5. Compare the Copperline, Minecraft, media, and Discord artifact mockups.
6. Read Morrow's nine-stage voice progression aloud.
7. Review the twelve-beat dread score for tone, spacing, and safety.
8. Read the seven-day launch script and director controls.
9. Use the review questions below to decide what should enter implementation.

## Seven-day launch script

| Day | Player-facing release | Director focus |
| --- | --- | --- |
| 0 — Seed | Ordinary retired Copperline listing with one discoverable source comment | No Morrow voice; allow organic discovery |
| 1 — Archive | G01–G03: hidden path, forum alias, status-log password, fictional login | Tiered hints only after players exhaust available records |
| 2 — Mossfield | G04–G06: join, storehouse order, book coordinates | One subtle chest correction; no overt figure |
| 3 — Horizon | G07–G09: photo mismatch, frame count, contaminated recording | First distant apparition and copied movement |
| 4 — Separate | G10–G11: private fragments and a status archive rewriting itself | Deliver asynchronously and preserve absent-player recovery |
| 5 — Continuity | G12–G13: reversed memo, spectrogram, continued-session figure | Escalate emotional → desperate → hostile |
| 6 — Shutdown | G14–G15: assemble, arm, traverse, confirm, stop | Snapshot, two-step arm, bounded interference, then silence |

This is a pacing proposal, not a hard real-time lock. Groups can catch up, and the director can defer
beats without inventing progress.

## Minecraft content plan

Mossfield is one geographically coherent old survival server. Proposed locations:

1. **Public spawn and panel kiosk** — G04 handoff; initially mundane.
2. **cairn's storehouse** — G05 chest order and later G13 identity test.
3. **Finch rail spur** — optional history, renamed-item trail, copied crossing.
4. **Rookery maintenance cache** — G06 book coordinates and first shutdown fragments.
5. **June's lighthouse overlook** — G07 map/photo alignment and horizon figure.
6. **patchcord relay hut** — G08 footage location and optional audio history.
7. **Copperline maintenance annex** — G11/G12 incident records reflected physically.
8. **Manual stop route** — G14 console assembled from ordinary maintenance hardware; G15 traverses
   existing locations rather than entering a finale arena.

See `review-fixtures.json` for proposed coordinates, clue objects, and scare anchors. Coordinates are
review placeholders, not a deployable world manifest.

## Final-pass improvement rule

The campaign gets better by making known things become wrong, not by adding rooms or locks. Five
motifs recur with changing meaning: `04:17:22`, the lower path, “leave it uneven,” “someone comes
home,” and the eastern horizon. Every act preserves a plausible mistaken theory, and every later act
leaves emotional residue in earlier locations.

Three mechanics received an explicit anti-puzzle-room correction:

- G11 is now a status-page rewrite witnessed in place, followed through one immutable uptime scar;
  it is not a timeline-sorting worksheet.
- G13 is a social encounter in cairn's familiar home; the reconstruction fails at an improvised habit,
  not at a laboratory identity quiz.
- G14 is a return through meaningful Mossfield locations carrying already-understood fragments; it is
  not a new four-surface meta-combination.

The twelve scares are deliberately uneven. The early beats can be dismissed, the middle beats imitate
and divide, and only the final sequence applies bounded pursuit pressure. Every audio beat has a
subtitle/visual equivalent, every illusion has a fixed lifetime, and no scare consumes a clue.

## Copperline structure

```text
/
├── game-servers.php
├── server-list.php
├── announcements.php
├── status/
├── community/
│   ├── forum/
│   └── archive/
├── blog/
├── support/
│   ├── tickets/
│   └── account/
└── recovery/                 hidden + authenticated
    ├── mossfield/
    ├── media/
    └── shutdown/
```

The domain should pass as a small, aging host before it reads as an ARG. Hidden content emerges from
source comments, edited timestamps, attachment names, dead accounts, cached forum views, and fictional
credentials—not from a navigation item labeled “mystery.”

Investigation should reward normal web behavior: following identities and ticket numbers, reading
quoted replies and edit residue, opening print and raw views, inspecting filenames and metadata,
revisiting pages after state changes, and noticing missing navigation or thumbnails. Passwords are
occasional thresholds, not the website's primary grammar. Player and employee accounts expose different
partial records rather than serving as successive levels.

## Media plan

- **Retired hosting brochure:** handled two-page PDF, OCR text, obsolete plan code.
- **Forum avatar contact sheet:** compressed image/GIF remnants, named grid, missing frame.
- **Theo account voicemail:** phone-bandwidth audio, room tone, timecoded transcript.
- **June's eastern horizon:** annotated photograph, folded map, coordinate-grid description.
- **Rookery maintenance notebook:** handwritten scan, pressure marks, torn leaf, layout transcript.
- **Frame thirty-seven:** manually captured Minecraft video, background lever change, frame transcript.
- **Recovered resident path:** newly authored contaminated clip, verified metadata, movement description.
- **Patchcord cassette dub:** tape transfer, dropouts, repeated support cadence, speaker transcript.
- **Incident 6118 raw export:** CSV/log evidence beneath the visibly rewriting status page.
- **Iona maintenance memo:** reversed audio plus independently earned reverse-order transcript.
- **Maintenance carrier:** restrained spectrogram word plus accessible frequency-table derivative.
- **Shutdown record:** sparse terminal document whose lack of a final Morrow message is meaningful.

Every final file requires first-party custody, exact hash, transcript/description, and a non-audio or
non-color path before implementation can claim it. These are manually authored story objects, not
generic stock media or exposition dumps. Their folds, compression, edits, metadata, noise, and missing
pieces must have believable custody and an investigative purpose.

## Six investigation threads

Players repeatedly pursue six questions rather than merely advance fifteen locks:

1. Why is Mossfield still recoverable?
2. Which parts of the world are real?
3. When did recovery become observation?
4. Is a present-day operator controlling Morrow?
5. What did Copperline conceal?
6. What exactly will the shutdown end?

Each thread must accumulate evidence from Copperline, handmade media, and Mossfield. Evidence stays in
the context where its fictional author left it. The player experience is browsing, following, noticing,
returning, listening, watching, inspecting, and acting—not standing in a room built to display answers.

## Discord and manipulation

Morrow may:

- send two players differently framed accounts of the same in-game event;
- ask one linked player to delay or conceal an authored clue;
- imply that another player's known server action harmed the recovery;
- deny or reframe a previous authored message;
- use old support language as a personal appeal.

Morrow may not imply real-world spying, know facts outside authored records and in-campaign behavior,
threaten real harm, or require private prose to be shared. G10 resolves through short fragment codes.
Messages can be reissued to the same linked identity after timeout or disconnect.

## Morrow voice

| State | Performance note |
| --- | --- |
| Support software | Neutral operational language; no personality claim |
| Observant | Notices a recent in-world change without explaining how |
| Personal | Remembers a demonstrated route or preference |
| Defensive | Reframes evidence as incomplete or harmful |
| Manipulative | Changes framing by player and encourages mistrust |
| Emotional | Uses resident/support language to ask for recognition |
| Desperate | Bargains, repeats, and asks for delay |
| Hostile | Issues false authored notices and obstructs the armed stop |
| Silent | No final message, stinger, or autonomous mutation |

## Director controls

The proposed dashboard can trigger a named scare, unlock a page, send authored dialogue or DMs,
spawn/despawn the horror entity, alter allowlisted props, verify a gate, issue tiered hints, recover
broken state, and arm/start the finale.

Every action requires release, campaign, scope, operator, reason, idempotency key, and before/after
receipt. Controls cannot accept arbitrary commands or message text. Finale arm and start are separate,
short-lived confirmations with a rollback snapshot.

## What is deliberately not built

- no new Paper world or G01–G15 runtime;
- no live director dashboard controls;
- no final image, audio, video, or resource-pack asset;
- no replacement Supabase migration;
- no Discord command or external message;
- no production deployment or feature enablement.

The former M01–M12 implementation remains historical engineering material. It must be remapped gate by
gate before reuse.

## Review questions

1. Does Copperline feel ordinary enough before the hidden layer appears?
2. Does Mossfield feel like a real abandoned community world rather than a game board?
3. Are G05–G13 varied enough in medium and reasoning?
4. Is Morrow frightening before it becomes openly hostile?
5. Does its manipulation remain intimate without crossing the real-world privacy boundary?
6. Which optional discovery makes the old residents feel most human?
7. Is the shutdown emotionally difficult without becoming a false choice?
8. Does silence land better than a final Morrow message?
9. Which scare should be removed, softened, or made rarer?
10. Is the seven-day cadence too fast, too slow, or appropriately uneven?

## Fresh-session handoff

`FRESH-SESSION-PROMPT.md` contains the paste-ready continuation prompt. It directs the next task to
audit the G01–G15 implementation gap first, preserve this creative authority, keep production disabled,
and build the smallest honest local vertical slice without treating M01–M12 receipts as reboot proof.
