# Morrow G01-G15 Implementation Audit

Audited 2026-09-14 against the traditional layered ARG authority introduced by
`ARG-GUIDEBOOK.md`.

## Conclusion

The creative authority is complete enough to guide production, and the first playable
website/rehearsal slice now exists. Production launch remains blocked. `morrow-reboot.enabled` and all
live mutation paths stay disabled.

The former M01-M12 implementation, local audits, Paper builds, client checkpoints, Supabase rehearsal,
Discord transport proof, and Copperline builds are retained as historical engineering evidence. They
prove useful infrastructure properties—journaling, idempotency, restart cleanup, safe entry, native
interaction, authenticated projection, RLS, and accessible media patterns—but they do not prove any
G01–G15 gate or the new story.

The current reboot checkpoint does prove a reviewable opening: G01-G03 Copperline routes, a G01-G05
local receipt chain, a G01-G15 local spine, rehearsal APIs, and a locked director contract.

## Authority validation

The 2026-09-14 redesign structural check passes:

- 15 sequential gate IDs and 24 sequential discovery IDs;
- exactly 9 optional lore discoveries;
- 9 ordered Morrow states ending in `silent`;
- 19 unique event keys with valid owner/destination surfaces;
- every relationship transition and media prerequisite resolves to the new event catalog;
- all five rewritten JSON contracts parse successfully;
- the authority-document diff passes `git diff --check`.

`tools/check_morrow_authority.py` intentionally remains red because it is an implementation-parity
checker for the superseded schema. Its failures identify the expected work: plugin transitions, SQL
event seeds, rehearsal fixtures, media assertions, and M01–M12 investigation keys have not been
reimplemented for G01–G15. Do not weaken the new authority or relabel old events to make that checker
green.

## Gate status

| Gate | Required experience | Status | Potentially reusable infrastructure |
| --- | --- | --- | --- |
| G01 | Source-comment hidden Copperline archive | Website route implemented; local receipt modeled | Next.js routing and spoiler filtering |
| G02 | Forum archaeology and alias acrostic | Website route implemented; local receipt modeled | Authenticated case projection |
| G03 | Fictional recovery/login handoff from status evidence | Website route implemented; local receipt modeled | PKCE and owner-bound RLS patterns |
| G04 | Authenticated Mossfield server handoff | Local contract modeled; live runtime unbuilt | Safe non-op Paper entry and identity linking |
| G05 | Renamed-item and chest-order storehouse trail | Local contract modeled; live runtime unbuilt | Local-first Paper journal and item ownership |
| G06 | Book/page/line cipher to world coordinates | Local spine modeled; live runtime unbuilt | Native books, dialogs, and restart-safe state |
| G07 | Map/photo horizon comparison | Local spine modeled; final media missing | First-party authenticated media delivery |
| G08 | Frame-thirty-seven background action | Local spine modeled; final media missing | Bounded replay timing and accessible derivatives |
| G09 | Metadata password and current-session contamination proof | Local spine modeled; final media/runtime missing | Content hashing and consented behavior capture |
| G10 | Linked-player private Discord fragments | Local spine modeled; Discord worker unbuilt | Gateway worker, identity binding, ephemeral response |
| G11 | Fake-status-log chronology | Local spine modeled; database projection unbuilt | Ordered database projector |
| G12 | Source/audio/spectrogram/page-line memo | Local spine modeled; final media missing | OGG/transcript/hash catalog patterns |
| G13 | Continued-session identity boundary | Local spine modeled; live runtime unbuilt | Disconnect/rejoin and display-entity cleanup |
| G14 | Multi-surface shutdown assembly and arm | Local spine modeled; live runtime unbuilt | Event prerequisites and transactional world checks |
| G15 | Bounded finale, terminal stop, and cross-surface silence | Local spine modeled; live runtime unbuilt | Graceful Paper shutdown and projection receipts |

## Surface status

| Surface | New authority | Current gap |
| --- | --- | --- |
| Minecraft | One coherent haunted Mossfield world using native clue objects and bounded scares | Existing room structures must be retired or remapped; G04-G15 world content is unbuilt |
| Copperline | One domain with hosting, community, forum, blog, status, archive, login, and recovery console | G01-G03 exists; G04-G15 production projection remains unbuilt |
| Discord | Player-specific authored manipulation fragments with safe group codes | Existing contradiction flow must be remapped to G10 and terminal silence |
| Media | Found footage, photo matching, metadata, reversal, spectrogram, and accessible equivalents | New assets are design placeholders without final files or hashes |
| Director | Audited scares, page unlocks, authored messages, prop controls, hints, recovery, and finale | Dashboard does not exist |
| Database | G01–G15 events, 24 discoveries, Morrow escalation, allowlists, and terminal shutdown | Existing migration encodes the superseded capability model |

## Historical evidence policy

- Do not delete, rewrite, or relabel old receipts as new proof.
- Reuse code only after mapping it to a new gate and rerunning proportional tests.
- Do not preserve an old mechanic merely because it is implemented when it conflicts with the new
  creative authority.
- The final release requires fresh one-/two-/six-player human rehearsal, accessibility review,
  restart/outage recovery, authenticated Copperline, database-bound Discord, isolated Supabase, and
  complete director-action cleanup evidence against one new release ID.

## Next implementation boundary

1. Freeze old progression entry points and make sure no former room can be enabled accidentally.
2. Produce a Mossfield world/content inventory and map every G04–G15 clue to an allowlisted object.
3. Implement the G01–G04 discovery slice before building later acts.
4. Specify the director command schema and security model before adding any live scare control.
5. Author and hash required media with accessibility equivalents.
6. Extend the new rehearsal contract; never reuse the M01-M12 packet as a passing fixture.

No external validation or production action should resume until the matching implementation exists.

## 2026-09-15 local slice update

The first safe implementation step is now present as a local-only contract receipt, not a live runtime: `morrow.local.g01-g05.v1` covers G01-G05, D01-D05, Morrow's G04 transition from `support_software` to `observant`, and the delayed non-blocking `chest_correction` consequence after the storehouse trail. It is validated by `tools/check_morrow_g01_g05_vertical_slice.py` and retained at `morrow/rehearsal/g01-g05-vertical-slice/latest.json`.

G01-G03 Copperline routes and the G01-G15 local console/rehearsal APIs now exist. This evidence does
not claim Paper world installation, Supabase projection, Discord delivery, final media custody, or
production readiness. Its value is a deterministic hash-chained target for the next live-runtime
implementation pass.
