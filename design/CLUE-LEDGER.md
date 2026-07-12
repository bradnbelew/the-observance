# The Observance - Clue Ledger

This is the source-of-truth control sheet for the overhaul. It exists so clues,
side stories, structures, media, website records, NPC claims, and required
traversal cannot drift apart.

The rule is simple: if a player-facing thing does not create evidence,
contradict evidence, invite action, pay off later, or deepen a real character or
place, it should be rewritten, merged, demoted, or cut.

## Ledger Schema

Each row must answer these questions.

| Field | Meaning |
| --- | --- |
| `id` | Stable clue/content id. |
| `expedition` | The play lane this belongs to. |
| `status` | `required`, `evidence`, `deepening`, `treasure`, `rewrite`, `merge`, or `cut`. |
| `truth_state` | `true`, `false`, `partial`, `forged`, `stale`, or `misunderstood`. |
| `surface_owner` | The surface that owns the clue: Minecraft structure, NPC, book, website, Discord, media, item, command, etc. |
| `players_see` | What players literally encounter. |
| `players_infer` | What the clue lets them reasonably conclude. |
| `invited_action` | What the clue asks players to do next. |
| `traversal_vector` | How it points somewhere: place, time, behavior, person, record, observer, media, memory, route, item, or consequence. |
| `later_proof` | What later confirms, corrects, or contradicts it. |
| `implementation` | Files/systems that own the current implementation. |
| `audit_status` | `ready`, `needs_rewrite`, `needs_build`, `needs_live_proof`, or `cut_candidate`. |

## Launch Rules

- Every required row needs a traversal vector.
- Every required row needs a later proof, contradiction, or payoff.
- Every mandatory destination needs at least two vectors by the time players must find it.
- Every NPC place claim must resolve to a real place or intentional lie.
- Every contradiction must be recoverable by later evidence.
- Every answer with special formatting must have the format taught before use.
- No keeper row may be solved only by "read a sign, apply a stock cipher, type a phrase."
- Optional content must either carry evidence weight or be marked for merge/cut.

## Expeditions

| Expedition | Purpose | Required Shape |
| --- | --- | --- |
| Hold Copy | Cold-open invitation artifact | Adventure-map to abandoned listing, not direct server pointer |
| Surface Evidence | First week grounding | Human claims tied to physical proof |
| Customs | Lived rules | Folk version, practical reason, physical proof, consequence, false version, late use |
| Record Website | Casework | Proof state, contradictions, provenance, fair answer handling |
| Keeper Field | Six investigations | Six different solve grammars |
| Unlit | Evidence expedition | Proves or breaks earlier assumptions |
| Wren | Companion/doubt arc | Player-spoken evidence, not exposition dump |
| Media | External proof | Confirms suspicion, contradicts claim, or opens route |
| Threshold | Convergence | Co-op action and evidence convergence |
| Finale | Earned release | Proof-based correction and physical action |

## Core Rows

| id | expedition | status | truth_state | surface_owner | players_see | players_infer | invited_action | traversal_vector | later_proof | implementation | audit_status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| hold-address-reconstruction | Hold Copy | required | partial | `the-hold.zip` + public website root | A contained adventure map with front-door/common-web/root-path evidence instead of a raw address | The server is real, but the live address belongs to the old listing, not the zip | Complete the map, follow the old listing, then join from the listed address | place + item + route + memory | First server arrival and Record site receipt confirm it | `dashboard/public/the-hold/the-hold.zip`; `dashboard/src/app/page.tsx`; `tools/rebuild_hold_invitation.ps1`; `tools/check_hold_invitation.ps1`; `discord/src/oracle/web-audit.ts` | ready |
| first-report-mouth | Surface Evidence | required | true | Minecraft lectern/site | A dry first report near the descent mouth | This is a field record, not fantasy prophecy | Find the descent and first literacy surfaces | place + record | Rosetta and later archive records use the same record logic | `first_report_lectern_01`; `sites.yml`; puzzle seeds | needs_build |
| rune-literacy | Surface Evidence | required | true | `rune_rosetta` | A readable script-learning surface | The script can be decoded consistently | Decode later local marks | record + structure | Later glyphs round-trip against the same alphabet | `rune_rosetta`; resource pack font; clue specs | needs_build |
| reckoning-literacy | Surface Evidence | required | true | `stone_of_reckoning` | Digit/sign marks that make coordinate-like clues legible | Place clues have their own grammar | Use place marks only after learning them | record + place | Coordinate-bearing rows become fair only after this | `stone_of_reckoning`; puzzle seeds | needs_build |
| vaun-audit | Keeper Field | required | partial | Hoard and market records | Missing first items, mismatched ledgers, empty returned column | Vaun is about keeping/taking/accounting, not a Caesar stone | Audit containers and compare records | item + record + place | Offering custom and first-of-deep return reframe the hoard | `KEEPER-INVESTIGATION-DOSSIERS.md`; `vaun_hoard_chest`; `vaun_bookshelf`; puzzle seeds | ready |
| mara-editions | Keeper Field | required | partial | Lecterns and route | Books disagree in specific page/line details | Reading is not enough; the correct text implies a walk | Compare editions and physically walk the route | book + route + memory | A later phrase only works if remembered from the walked edition | `KEEPER-INVESTIGATION-DOSSIERS.md`; `mara_lectern_*`; `mara_map_marker`; puzzle seeds | ready |
| sella-seven-count | Keeper Field | required | true | Water/school/cistern sites | Land counts six, water/school records imply seven | The seventh existed before the name is known | Compare reflection, school, and water counts | water + record + place | Clip 2 and recovered archive confirm the missing count | `KEEPER-INVESTIGATION-DOSSIERS.md`; `sella_pool`; `cistern_7`; `school_stand`; media clip 2 | ready |
| orin-posture | Keeper Field | required | true | Low architecture and seals | Some marks are legible only while crouched or aligned | Body position can be part of the answer | Bow, crouch, rotate, and read from intended posture | behavior + structure | Accepting later requires learned physical consent | `KEEPER-INVESTIGATION-DOSSIERS.md`; `orin_marker_*`; `orin_frame_dial_*`; `the_threshold` | ready |
| brann-dark-hours | Keeper Field | required | partial | Watch floor and silence route | Fire/light/toll records change by time and silence | Dark hours are behavior, not slogan | Stay awake, listen, compare light states | time + sound + light | Clip 3 confirms the dark-hours instruction | `KEEPER-INVESTIGATION-DOSSIERS.md`; `watch_floor`; `brann_toll_tower`; media clip 3 | ready |
| iss-forgery | Keeper Field | required | forged | Shrine/records/Wren contrast | Iss's comfort is too polished and contradicts land records | Warmth can be forged | Compare Iss claims to structures, media, Wren behavior, and records | contradiction + person + record | Wren and late archive expose the false comfort | `KEEPER-INVESTIGATION-DOSSIERS.md`; `stone_iss`; `the_cold_hearth`; Record site; Wren systems | ready |
| custom-bow | Customs | evidence | true | NPCs + bow architecture | People lower themselves at marked places, but low sightlines make the act useful | The bow is practical memory, reading posture, and threshold consent | Crouch where architecture asks for it and compare what becomes readable | behavior + NPC + structure | Orin and Accepting prove the custom has teeth | `CUSTOMS-FIELD-GUIDE.md`; `bow_marker_01`; Orin sites; NPC dialogue | ready |
| custom-offering | Customs | evidence | partial | Cairn + ledgers | First things are left, not kept, and Vaun's returned column stays empty | The offering interrupts hoarding logic and proves return instead of payment | Return a first item at the right place and compare Vaun's accounts | item + record + place | Vaun and sacred beast trail explain why firstness matters | `CUSTOMS-FIELD-GUIDE.md`; `offering_cairn_01`; Vaun records | ready |
| custom-kept-light | Customs | evidence | true | Homes/watch/forge | Lamps, fuel, ash, and watch fires matter differently | Light is labor, warning, and continuity, not decoration | Keep, compare, and later follow light states | light + time + place | Brann and release room use the light checksum | `CUSTOMS-FIELD-GUIDE.md`; `kept_light_home_01`; `watch_floor`; `nether_forge` | ready |
| custom-deep-line | Customs | evidence | partial | Painted line/breach | People stop at a line, but the breach proves it was not a simple wall | Some borders are old safety systems and some explanations are false | Compare the line, breach, and false explanation | place + consequence | Unlit and breach sites explain the real danger | `CUSTOMS-FIELD-GUIDE.md`; `painted_line`; `third_bay_breach`; NPC dialogue | ready |
| custom-unspoken | Customs | required | partial | NPC silence + Wren leak | Certain names are avoided for different reasons | Not speaking can protect, erase, or preserve refusal | Track who refuses, who slips, and when proof makes speech necessary | person + memory + record | Wren reveal and recovered archive resolve it | `CUSTOMS-FIELD-GUIDE.md`; Wren systems; NPC dialogue; archive audio | ready |
| custom-sacred-beast | Customs | evidence | misunderstood | Coops/market/records | One marked witness pattern is treated differently | The custom preserves a witness, not a generic mascot | Inspect coops, feed, and household records | entity + item + record | Offering and seventh trail prove/use the beast's witness role | `CUSTOMS-FIELD-GUIDE.md`; `deep_bird_coops`; `deep_market`; dialogue | ready |
| custom-dark-hours | Customs | evidence | true | Watch logs + tower | Sleep is unsafe on black moon nights because nobody witnesses the change | Staying awake is a route condition and evidence practice | Watch, listen, and compare the right time window | time + sound | Brann route and clip 3 prove it | `CUSTOMS-FIELD-GUIDE.md`; `watch_floor`; `brann_toll_tower`; media clip 3 | ready |
| media-ash-13 | Media | evidence | true | Video clip 1 | Prior-base footage with `ASH-13` | The Record copied or preserved an older room | Use as provenance, not a random token | media + record | Base anomaly thread confirms it | `MANUAL-MEDIA-STAGING.md`; media gate flags | needs_live_proof |
| media-reeds | Media | evidence | true | Video clip 2 | Shore footage with `WHERE THE REEDS FOLD BACK` | Far-water route has external proof | Revisit water/reeds route | media + place | Sella seven-count trail confirms it | `MANUAL-MEDIA-STAGING.md`; media gate flags | needs_live_proof |
| media-stay-awake | Media | evidence | true | Video clip 3 | Watch-floor footage with `STAY AWAKE` | Dark-hours behavior is literal | Use the watch-floor timing clue | media + time | Brann route confirms it | `MANUAL-MEDIA-STAGING.md`; media gate flags | needs_live_proof |
| media-six-return | Media | evidence | true | Video clip 4 | Late room footage with `SIX RETURN, ONE IS NOT KEPT` | The seventh is not just missing; not kept | Use as release-room checksum | media + finale | Recovered archive and finale prove it | `MANUAL-MEDIA-STAGING.md`; media gate flags | needs_live_proof |
| archive-not-kept | Media | required | true | Recovered archive audio | Spectrogram resolves to `I WAS NOT KEPT` | The seventh refused the system | Use as high-confidence proof in late casework | media + record | Finale/Release uses the refusal as moral proof | recovered archive; Record site | needs_live_proof |
| unlit-custom-proof | Unlit | required | true | Unlit duplicate village | The same customs exist without the surface explanations | Earlier customs were practical survival systems | Compare living sites with Unlit sites | place + contradiction | Threshold/finale require the corrected reading | Unlit docs, plugin commands, sites | needs_build |
| wren-evidence-loop | Wren | required | partial | Companion behavior | Wren reacts to what players have actually said/done | The companion is part witness, part leak | Compare Wren with independent proof | person + memory | Iss/Unspoken resolution exposes the pattern | `WREN-EVIDENCE-LOOP.md`; Wren plugin systems; dialogue contracts | ready |
| accepting-convergence | Threshold | required | true | `unbroken_light` and threshold sites | Co-op physical action at the deepest kept light | The answer is an act the group learned over time | Gather, bow/accept, and release based on proof | co-op + behavior + light | Finale response confirms release | `unbroken_light`; `the_threshold`; `AcceptingRiteListener` | needs_build |

## Cut Or Merge Watchlist

Items should be added here when they repeat another clue without adding evidence.

| item | reason | action |
| --- | --- | --- |
| generic keeper cipher stones | Repeats "stone plus cipher plus answer" and collapses variety | Merge into keeper investigations; retain only as supporting surfaces |
| ungated answer-only signs | Encourages password hunting and brittle guessing | Reframe as local mechanisms or contextual Record entries |
| lore that only says "six kept / seventh missing" | Repeats the main thesis without new evidence | Give it a specific proof role or cut |
