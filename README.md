# The Observance

A server-side **Paper plugin + small cloud brain** that runs a slow-burn, personalized
horror/mystery ARG for a veteran Minecraft friend group.

**North star:** *"From The Fog, but it knows your name."*

It watches how people actually play, forms a per-player read, and authors an
emergent, **soft-pressure** mystery delivered through vanilla surfaces — signs,
books, sounds, named mobs, structures — plus a Discord layer. The land has
**customs** players must discover and perform; an unseen presence keeps a record
of who honors them; and the campaign builds to a collective **ritual** that
either makes the group *kept* or casts them out.

> Two reading levels:
> - **`DESIGN.md`** — the full technical build (safe to read; no story spoilers).
> - **`arc/`** — the *sealed* story bible. **Do not open if you want to play unspoiled.**

---

## Architecture (where everything lives)

| Piece | Home | Role |
|---|---|---|
| World + plugin | **PebbleHost** (Paper) | the deterministic engine — Phase 0 needs **zero** AI |
| Shared state | **Supabase** (Postgres) | dossiers, heatmap, Whisper ledger, custom + arc state |
| Live model | **Claude API** | rare, validated "scalpel" beats (text only) |
| Discord bot | **cheap cloud VPS** | the Whisper/hint surface + reports |
| Autonomous *showrunner* | **same VPS, scheduled** | authors/tunes content **between** sessions (not a live puppeteer) |
| Spoiler-free dashboard | **Vercel** | health / heatmap / jank metrics — no story |

**Core principle:** a deterministic *haunting engine* is the spine; the LLM is a
**scalpel** used rarely, for the one thing only it can do (personalized text),
always with a deterministic fallback. The world never breaks if the model is
slow, weird, or offline. See `DESIGN.md`.

---

## Provisioning checklist (do these to stand it up)

1. **VPS** — spin up a small Linux box (e.g. Hetzner CPX11 ~€4/mo, or DigitalOcean
   $6). Ubuntu LTS. This hosts the Discord bot + the scheduled showrunner.
2. **Supabase** — create a project; grab the connection string + service key.
3. **Discord** — create an application + bot, invite it to the server, grab the token.
4. **Claude API** — you have keys; set `ANTHROPIC_API_KEY`.
5. **Local dev** — JDK 21 + IntelliJ IDEA (you have it) + a local Paper jar for testing.
6. **PebbleHost** — confirm Paper, plugin-upload access via panel.

Phase 0 (below) needs **only #5 + #6**. The VPS/Supabase/Discord/Claude come online for Phase 1.

---

## Build phases (each phase is also the in-game escalation)

- **Phase 0 — The Haunting + The Customs.** Deterministic engine: signal tracker,
  traffic/territory map, drama budget, curated beat library, the custom-detection
  system, and the Archivist **reports**. No AI, no Discord. Ships first, proves the world.
- **Phase 1 — The Scalpel + Whispers.** The one validated LLM beat type
  (personalized reports/journals) + the Discord **Whisper** hint economy + the bridge.
- **Phase 2 — The Body & World.** Citizens2 Keeper NPC, FAWE structures, per-player
  PacketEvents surfaces, the ending ritual ("The Accepting").
- **Phase 3 — The Ear.** Simple Voice Chat → Whisper STT feeding the slow dossier.

---

## Build / run

- Java 21, Gradle, Paper API. Open in IntelliJ.
- `./gradlew build` → drop the jar in PebbleHost `plugins/` → restart.
- Local test: run a Paper jar locally, hot-reload during dev.

(The Gradle project scaffold is the next artifact — see DESIGN.md "Build setup".)
