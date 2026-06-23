# The Observance — Discord service ("The Watcher")

Node 20 + TypeScript Discord service for **The Observance**, the slow-burn
Minecraft ARG. The bot is **The Watcher** — the record-keeping facet of the
presence. It links Discord users to their in-world name, dispenses tiered
**whisper** hints from pre-authored copy (never invented), and the **clue forge**
carves ciphertext into the keepers' runes and renders editorial clue cards with
**satori** + **@resvg/resvg-js**.

It shares Braden's Supabase project with the dashboard and the Minecraft plugin,
connecting with the **service-role key** (server-side, bypasses RLS). It never
breaks character: every player-facing line comes from `src/voice.ts` — nothing
says *bot*, *AI*, *game*, *server*, or *command* to a player, and it never uses
normal capitalization in chat.

---

## What's in here

```
src/
  config.ts            env load + validation -> one typed, frozen `config`
  brand.ts             colors / fonts / canvas tokens for the cards
  voice.ts             THE WATCHER'S TONGUE — every player-facing string + BOT_PRESENCE
  bot/
    index.ts           `dev` entry — the Watcher client (presence + interaction routing)
    register.ts        `register` entry + registerGuildCommands() (REST deploy of /whisper + /link)
    commands/
      whisper.ts       /whisper handler  (resolve -> budget -> tier -> toll)
      link.ts          /link handler     (bind discord -> world name)
    services/
      poster.ts        postReport / postClue — the showrunner's hand into channels
  db/
    types.ts           row types for the tables the bot touches
    client.ts          service-role Supabase client (RLS-bypassing; server only)
    repo.ts            typed query helpers (players, budgets, events, beats, hints, log)
  forge/               CLUE FORGE — pure, no I/O
    ciphers.ts         caesar / atbash / vigenere / substitution / book / coord (+ self-tests)
    runes.ts           the keepers' alphabet — a clean 1:1 substitution + SVG glyph geometry
    index.ts           forgeClue(spec) -> { svg, solution, puzzleKey, meta } + forgeSelfTest()
    templates/         5 themed clue-card templates (satori) + sigil + svg helpers
  render/
    fonts.ts           satori font loading (from assets/fonts/)
    render.ts          satori -> SVG -> PNG (resvg) core
    cards.ts           plain brand-frame card templates (clueCard, brandCard)
    sample.ts          `sample` entry — renders ./out clue + whisper PNGs (runs forge self-tests first)
    assets.ts          `assets` entry — renders the sigil avatar + brand/channel art to ./out
assets/fonts/          drop body-regular.ttf / body-bold.ttf / mono-regular.ttf here
supabase/migrations/
  0003_discord.sql     adds players.discord_id + the service-role-only `hints` table
```

The forge (ciphers + runes) is pure and has no Discord/Supabase/network
dependency. `npm run sample` runs the forge self-tests before rendering, so a
broken cipher or a non-bijective rune map fails loudly before any artifact ships.

---

## Prerequisites

- **Node 20+** (`node -v`). The project is ESM + `tsx` — no build step to run it.
- A **Discord application + bot** (token, application id).
- The **guild (server) id** and the four **channel ids** the bot posts to.
- Access to the shared **Supabase** project (URL + service-role key).
- Three **font files** under `assets/fonts/` (see that folder's README). satori
  cannot read system fonts; without these, rendering throws a clear error.

---

## 1. Create the bot and gather IDs

### Application + bot token
1. Go to the **Discord Developer Portal** → *Applications* → **New Application**.
2. Copy the **Application ID** (General Information) → this is `DISCORD_APP_ID`.
3. Open the **Bot** tab → **Reset Token** → copy it → this is `DISCORD_BOT_TOKEN`.
   - This token is a **secret**. Treat it like a password (see *Secrets* below).
4. Invite the bot to your server: **OAuth2 → URL Generator**, scopes
   `bot` + `applications.commands`, then a permission set that at minimum allows
   *Send Messages*, *Embed Links*, *Attach Files*, and *Use Slash Commands*.
   Open the generated URL and add it to the guild.

### Enable the **Message Content Intent**
The Watcher's gateway intents include `MessageContent`. You must enable it or the
bot will fail to log in:
- Developer Portal → your app → **Bot** → **Privileged Gateway Intents** →
  toggle **Message Content Intent** ON → Save.

### Get the **guild id** and the **channel ids** (Developer Mode → Copy ID)
1. In the Discord client: **User Settings → Advanced → Developer Mode = ON**.
2. **Guild id:** right-click the server icon → **Copy Server ID** →
   `DISCORD_GUILD_ID`.
3. **Channel ids:** right-click each channel → **Copy Channel ID**:
   - `#the-record`  → `CHANNEL_THE_RECORD`  (public lore / status posts)
   - `#cipherwork`  → `CHANNEL_CIPHERWORK`  (puzzle discussion + clue cards)
   - `#whispers`    → `CHANNEL_WHISPERS`    (where tiered whisper hints land)
   - `#the-ways`    → `CHANNEL_THE_WAYS`    (meta / out-of-character coordination)

### Supabase
From the shared project: **Project Settings → API** → copy the **Project URL**
(`SUPABASE_URL`) and the **service_role** secret (`SUPABASE_SERVICE_ROLE_KEY`).
The service-role key bypasses RLS — it is **server-side only**, never shipped to a
browser.

---

## 2. Secrets and `.env`

```bash
cp .env.example .env
# then fill in every value you gathered above
```

- The bot reads **everything** from the environment via `src/config.ts`, which
  validates on startup and throws ONE error listing any missing keys. There are
  **no hardcoded secrets** anywhere in the source.
- **`.env` is git-ignored** (see `.gitignore`). Never commit it — it holds the
  bot token and the service-role key.
- **If a token leaks, rotate it immediately:** Developer Portal → Bot → *Reset
  Token* (Discord); Supabase → Project Settings → API → *Reset service_role*
  (or rotate the project's keys). Then update `.env`.

---

## 3. Database

Apply the Discord migration to the shared Supabase project:

```
supabase/migrations/0003_discord.sql
```

It adds `players.discord_id` (links a Discord user to a Minecraft player) and the
service-role-only `hints` table. The `hints` rows (and the `whisper_budgets`) are
**seeded later from the sealed arc** — they are intentionally left empty in code.

---

## 4. Fonts

Drop three font files into `assets/fonts/` (filenames configured in
`src/render/fonts.ts`):

- `body-regular.ttf` (serif, 400) → `brand.fonts.body`
- `body-bold.ttf` (serif, 700)
- `mono-regular.ttf` (monospace, 400) → `brand.fonts.mono`

Any `.ttf`/`.otf` works. See `assets/fonts/README.md`.

---

## 5. Install and run

```bash
npm install        # installs discord.js v14, @supabase/supabase-js, satori, resvg, dotenv, tsx
npm run register   # deploy the /whisper + /link slash commands to DISCORD_GUILD_ID (instant)
npm run dev        # start the Watcher (it also re-registers commands on boot)
```

| Script             | What it does                                                          |
| ------------------ | -------------------------------------------------------------------- |
| `npm run register` | REST-deploy the guild slash commands (`/whisper`, `/link`). Instant. |
| `npm run dev`      | Start The Watcher (presence + interaction routing).                  |
| `npm run sample`   | Render sample clue + whisper PNGs to `./out` (runs forge self-tests).|
| `npm run assets`   | Render the sigil avatar + brand/channel art to `./out` and `brand/`. |

`npm run sample` and `npm run assets` need only the fonts — no Discord, no
Supabase, no network. Use them to eyeball card designs locally.

---

## 6. Set the avatar + nickname

```bash
npm run assets     # writes ./out/sigil.png AND brand/sigil.png (the Keeper's Eye)
```

- **Avatar:** Developer Portal → your app → **Bot** → upload **`brand/sigil.png`**
  as the bot's icon. (It is rasterised from `brand/sigil.svg`; if that file is
  ever missing, `assets` falls back to a ring placeholder and says so.)
- **Nickname:** in the server, set the bot's nickname to **`The Watcher`** (Server
  Settings → Members, or right-click the bot → *Change Nickname*). The presence is
  set in code (`BOT_PRESENCE` in `voice.ts`), so the member list reads
  *The Watcher — Watching the ways*.

---

## Slash commands (two rites)

- **`/link <name>`** — tell the Watcher the name you wear in the world. It matches
  the name against the keepers it has already seen (case-insensitive) and binds
  your Discord id. Replies are ephemeral, in the Watcher's tongue.
- **`/whisper <puzzle>`** — ask for a hint and pay the toll. There is **no tier
  option**: the tier rises on its own —
  `tier = (whispers already given for that puzzle) + 1`. Tier 1 is the Watcher's
  own nudge; deeper tiers speak the seeded `hints` rows. Spending a whisper
  records a `whisper_events` row and enqueues a `whisper_toll` beat so the plugin
  enacts the in-world cost. If there is no budget, or no seeded words at this
  tier, the Watcher **withholds in character** — it never errors at the player.

---

## How the showrunner calls the poster

The autonomous showrunner (scheduled VPS process) lays words and artifacts into
the channels through `src/bot/services/poster.ts`. It is the Watcher's hand —
these functions **compose no English of their own**:

```ts
import { postReport, postClue } from './bot/services/poster.js';
import { voice } from './voice.js';
import { forgeClue } from './forge/index.js';
import { config } from './config.js';

// 1. A line of the record — pass a pre-worded voice.ts string, never ad-hoc text.
await postReport(client, config.channels.theRecord, voice.recordOpened());
await postReport(
  client,
  config.channels.theRecord,
  voice.reportObserved('emberlyn', 7, 'kept the light'),
);

// 2. A clue card. postClue accepts a ready PNG buffer, a forged clue, a
//    { png, name } result, or an explicit { card } spec — it renders as needed.
const clue = forgeClue({ cipher: 'caesar', text: 'BOW AT THE MARKER', shift: 7 });
await postClue(client, config.channels.cipherwork, clue);
```

`postReport(client, channelId, text)` posts one line — `text` MUST already be a
`voice.ts` string. `postClue(client, channelId, payload)` posts a clue artifact
PNG, framing a `ForgedClue` into the brand clue card (with the Keeper's Eye
watermark) when it isn't handed a ready buffer. Both resolve the channel from an
id you pass from `config.channels`.

---

## The voice rule (do not break it)

Every player-facing string the bot utters lives in `src/voice.ts` and is written
in the Watcher's register: lowercase, sparse, calm, certain; no exclamation
marks, no emoji; speaks of *the ways*, *the record*, *the keepers*, *what is
owed*, *kept*. Handlers import from `voice.ts`, pass their data in, and post what
comes back, verbatim. They never inline English at the call site, and they never
say *bot / AI / game / server / command* or use normal capitalization in chat.
Operator logs (`console`, `event_log`) and developer error messages are exempt —
players never see them.
