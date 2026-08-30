# Researched Capability Matrix

Research checked 2026-08-30 against the current Paper, Discord, Supabase, and Next.js documentation.

## Supported and selected

| Experience | Implementation | Decision |
| --- | --- | --- |
| Morrow conversations in Minecraft | Paper Dialog API: notice, confirmation, multiple action, text, boolean, option, item, and range inputs | Use for authored choices and short concrete inputs |
| Morrow's physical body | Block/item/text display entities plus an interaction entity and resource-pack model | Use; stable, visually controllable, no fake-player dependency |
| Smooth body/echo motion | Display transformation and teleport interpolation | Use for bounded authored motion and replay clips |
| Per-player apparitions | `setVisibleByDefault(false)` plus `showEntity`/`hideEntity` | Use for non-critical personal echoes |
| Different versions of one room | Separate physical clone rooms and controlled teleports | Use for authoritative split-backup scenes |
| Player movement echo | Record bounded location/look/action samples locally, compress, replay through display model | Use with hard duration/entity/sample limits |
| Persistent clue identity | Paper Persistent Data Container on items, entities, chunks, and tile states | Use; never encode authority only in lore text |
| Required custom audio/models | Required server resource pack with UUID and SHA-1 | Use with visual/text accessibility mirrors |
| Website-to-game consequences | Append-only event ingest plus idempotent projection outbox | Use; no direct world mutation from a web request |
| Live Copperline updates | Supabase private Realtime Broadcast or bounded polling | Use Broadcast for presence/UI hints; database remains authority |
| Discord inputs | Slash commands, buttons, select menus, and modals | Use for private receipts and deliberate actions |
| Website inputs | Next.js Server Actions; Route Handlers for Minecraft/Discord webhooks | Use Node runtime and server-only secrets |
| Offline Minecraft operation | Local hash-chained event journal with retrying async projection | Required; remote outage cannot freeze a tick or lose progress |

## Supported but bounded

| Experience | Boundary |
| --- | --- |
| Logged-out player continuation | A visibly reconstructed display-model echo may follow an authored or recorded route; it cannot act as a real authenticated player |
| Morrow head tracking | Rotate the display-model head toward the active speaker; do not force player camera movement except in a clearly authored scene |
| Client-specific evidence | Safe for atmosphere and optional personal clues; mandatory physical proof must exist in a shared or cloned authoritative room |
| Dynamic dialogue | Select from authored lines using state and player receipts; an optional language model may paraphrase only non-canonical chatter behind a strict schema and fallback |
| World rollback | Swap pre-authored bounded regions in a rehearsed order; never attempt a live arbitrary whole-world rollback |
| Website-generated player diagrams | Render compact route/block-layout data supplied by the plugin; never claim access to client screenshots |
| Player-specific sound | Resource-pack sound events may be sent to selected players; required content also needs subtitle/text/visual form |

## Rejected

| Approach | Reason |
| --- | --- |
| NMS/packet fake-player NPC as the primary Morrow body | Version-fragile, skin/profile complexity, and unnecessary for the intended blocky reconstruction aesthetic |
| Freeform chat as the only answer/input surface | Unclear affordance, unsafe grading, spam/privacy risk, and poor recovery behavior |
| Unrestricted LLM controlling progression or Minecraft commands | Non-deterministic, canon-breaking, unsafe, and impossible to rehearse honestly |
| Permanent `sendBlockChange` illusions for required clues | Client desynchronization and reconnect/restart ambiguity |
| Continuous high-frequency remote movement streaming | Wasteful, privacy-hostile, latency-sensitive, and unnecessary |
| Remote HTTP/database calls on the Paper main thread | Violates the 50 ms tick budget and can freeze the server |
| Discord as a compulsory live chat room for every step | Splits attention and turns the experience into a bot workflow |
| YouTube/Dropbox-only custody for required media | Weak delivery control, accessibility, and launch verification |

## Sources

- Paper Dialog API: https://docs.papermc.io/paper/dev/dialogs/
- Paper display entities: https://docs.papermc.io/paper/dev/display-entities/
- Paper scheduling: https://docs.papermc.io/paper/dev/scheduler/
- Paper PDC: https://docs.papermc.io/paper/dev/pdc/
- Paper databases: https://docs.papermc.io/paper/dev/using-databases/
- Paper server resource-pack properties: https://docs.papermc.io/paper/reference/server-properties/
- Discord interactions: https://docs.discord.com/developers/interactions/overview
- Supabase Realtime: https://supabase.com/docs/guides/realtime
- Supabase subscribing to changes: https://supabase.com/docs/guides/realtime/subscribing-to-database-changes
- Supabase RLS: https://supabase.com/docs/guides/database/postgres/row-level-security
- Supabase Edge Functions: https://supabase.com/docs/guides/functions
- Next.js App Router: https://nextjs.org/docs/app
