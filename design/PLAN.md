# THE OBSERVANCE — The Full Plan (front to back)

> The single, simple, scannable plan. We are **evolving** The Observance, **not** scrapping it: the
> engine + every planned interaction stays; the **story** is the real rework. Minecraft-feasible only.
> Deep story answers are **sealed** (`arc/`) so Ethan plays unspoiled — this doc shows the *shape*, not
> the payoff. Built on `design/ARG-RESEARCH.md` (craft) + `design/COHERENCE-AUDIT.md` (what's built).

---

## 0. TL;DR — the whole thing in six lines
1. Friends spawn into a **normal-feeling Minecraft server**. No quest, no marker.
2. Out on the map are **many places** left by a vanished people (the *Kept*) — villages, a chapel, a
   warden's post, a drowned quarter — with **surface NPCs** living among them.
3. They explore and **reconstruct** who these people were, what this place was, what happened, what's on
   the surface with them now, and **were they even human** — from letters, reports, posters, journals,
   signs, NPC talk, and the world itself.
4. A **Watcher** notices them — indirectly. The world **spooks them** (lights out, a footstep behind,
   a book that knows their name, blocks that change) — **rarely, well-timed, with room to breathe.**
5. **Discord is the recovery archive**: every find becomes a card that clusters into **5 threads** the
   group watches fill in. Decoding is *texture* — its payoff is always **lore**, never just a key.
6. It's all **conducted live** (the showrunner + you at the dashboard) and filmed Wifies-style.

---

## 1. What we KEEP vs what we BUILD

| KEEP (already built / planned — good) | BUILD (the rework — mostly STORY) |
|---|---|
| The deterministic **engine** (anti-jank, reveal-discipline, Supabase bridge, fault isolation) | A **strong, layered story** + the sealed answer bible (the weak part, fixed) |
| The **~22-beat spook/interaction palette** (see §6) | The **world**: many authored places + the lost people's artifacts |
| **Customs/taboos** detection (bow, offering, unspoken, kept light, deep line, sacred beast, **dark hours**) | The **cast as characters** (keepers → people with names, lives, fates) |
| The **Watcher** (Discord persona + in-world presence) | The **diverse lore-delivery** layer (letters/reports/posters/NPC/inference) |
| The **oracle** (answer-sign + #the-record + /answer) | The **Recovery Archive** (Discord ship-log: rumor→verify cards, 5 threads) |
| The **showrunner** deterministic spine (drip + stall-gift, you've been extending it) | The **Attention system** + per-player "it-knows-me" beats |
| **Resource pack / FAWE / MythicMobs / Citizens / Simple Voice Chat** stack | **Pacing**: the breathing-room rhythm (so beats land) |
| The **dashboard** (your director's console) | The **fragment → revelation ledger** (every clue pays a planned debt) |

**Net:** ~80% of the *mechanics* survive. The new work is the *world, the people, the writing, the
delivery, and the timing.*

---

## 2. The experience (spoiler-safe premise)

You and your friends spawn into an ordinary survival world. You build, you wander. Then you find a
**place** — a half-sunk hamlet, a walled district, a waystation — and it's *lived-in but empty*: food
in the barrels, tools dropped mid-task, a **poster** nailed to a post in a clipped municipal voice. You
find a **letter** from someone named **Aro**, answering a letter you haven't found yet. A **surviving
NPC** mutters one strange line. You walk a certain stretch and a **torch behind you goes dark**. You
start asking *questions.* You compare finds in Discord; a friend across the map found Aro's other letter.
Slowly the picture forms — these were **normal people**, and something happened. And the thing watching
you now wasn't always watching. **Were they human?** You never get a clean look. The answer assembles
itself, over weeks, out of the pieces.

**The five threads** (the questions the whole game answers — every fragment is tagged to one):

| Thread | The question | Color |
|---|---|---|
| **WHO** | Who were the *Kept* — as people? | amber |
| **PLACE** | What was this place, and what was it *for*? | green |
| **HAPPENED** | What happened — the year it broke? | red |
| **SURFACE** | What's on the surface with us *now*? | grey |
| **HUMAN** | Were they human — and are we becoming the same? | black |

*(The actual answers — what happened, what the Watcher is, the ending — live sealed in `arc/`.)*

---

## 3. The story (direction + arc)

**Premise (spoiler-safe):** A people called the **Kept** lived here and followed **"the ways"** (the
customs) to keep something at bay — or to keep *themselves* from changing. Something went wrong the year
it "broke." They're gone; the **Watcher** remains, and it watches the new arrivals the way it once
watched them. The players reconstruct WHO / PLACE / HAPPENED / SURFACE / HUMAN.

**Arc shape — 5 movements, paced as an emotional rhythm (NOT a difficulty ramp):**

| Movement | Feel | What surfaces | Spook level |
|---|---|---|---|
| **I — Ordinary** | calm, normal server | the first places + first letters; one quiet wrongness | **very low** (1 small beat / few days) |
| **II — The Ways** | curiosity | the customs revealed as the Kept's rituals; NPCs open up; rumors point outward | low (a beat or two / session) |
| **III — The Break** | dread | contradictory accounts of "the year it broke"; the deep, wrong-scaled places | rising |
| **IV — The Turning** | grief + fear | the keepers' journals decay; the "were they human" horror dawns | high but **rationed** |
| **V — The Accepting** | the choice | the group is judged by how they kept the ways; the sealed ending | peak, then silence |

**Pacing rule (your note, load-bearing):** **never overwhelm.** Movement I is *mostly normal Minecraft*
with rare, single, well-placed beats — long quiet stretches between them so each one is felt. The
**drama budget** caps beats; **fallow days** (nothing new) are deliberate texture. Escalation is slow
and earned; the scary stuff only hits hard *because* it's rare.

**The cast:**
- **The Watcher** — never met face to face. Felt as reports, the spooks, the Discord persona, and rare
  censored glimpses (fog / behind-a-gap / forced blindness). *Indirect, always.*
- **The Kept (the keepers, re-cast as PEOPLE)** — ~6 named people with lives, relationships, and
  contradictory accounts (a warden, a baker's family, a child, the one who "turned away"). You
  reconstruct them as humans; their journals decay in tone across the arc.
- **Surface NPCs** — a few survivors/stragglers/scavengers (Citizens2) who *talk*, remember what you've
  done, and point you onward — some honest, some not.

---

## 4. Lore — what it is, and how it drops

**Truth comes in every register, NOT just "rumors"** (your note). The delivery menu:

| Channel | What it is | Where / how (Minecraft-feasible) |
|---|---|---|
| **Letters** | personal correspondence between named people, replying to each other | written books staged in homes (chests, lecterns, item frames) |
| **Reports** | clinical/municipal records — "Observers recorded…" | books/signs authored by ROLES ("WARDEN-3") + dates; redacted lines |
| **Posters / notices** | public warnings, ordinances, missing-persons | signs + resource-pack poster textures on walls |
| **Journals** | one person's diary that **decays in tone** over dated entries | book series staged in date order along a route |
| **Inference (the world itself)** | geography, decay, staged tableaux — read with no text | FAWE builds; a barricade facing inward; a child's bed by a packed chest |
| **NPC conversation** | survivors tell, hint, lie | Citizens2/ZNPCsPlus dialogue trees that branch on your dossier |
| **Rumors → verify** | heard secondhand, then confirmed (or contradicted) on-site | NPC lines + distant silhouettes plant rumors; arriving flips them |
| **Discord archive** | the present-day "recovery" framing; a decaying archivist voice | the bot: cards, threads, redacted "incident" drops |
| **Decoded fragments** | a cipher on a tomb wall whose **answer is lore** | the oracle (answer-sign / #the-record / /answer) — payoff = a line of story |

**The iceberg rule:** every fragment implies more than it says; the one thing they most want to know is
**redacted / torn / missing** — enough to guess, never enough to confirm. Documents **reference each
other** (consistent names — "Cistern 7", "Warden Aro") so the gaps imply a world larger than what survives.

---

## 5. NPCs (Minecraft-feasible)

- **Tech:** **Citizens2** (persistent NPCs + dialogue trees) and **ZNPCsPlus** (lightweight, per-player
  packet NPCs for apparitions). Both server-side, no client mod.
- **Surface survivors** — stand in/near places, give rumors, react to your conduct ("you're the one who
  broke the seals"), remember (dossier-branched). A few are unreliable narrators.
- **The keepers as apparitions** — glimpsed, per-player, never interactive in the normal sense
  (ZNPCsPlus packet figures + reveal discipline) — they *echo* a line, then are gone.
- **The Watcher** — NOT an NPC you talk to; it's the presence behind the spooks + the Discord voice.

---

## 6. The spook / interaction palette (KEEP all — this is what "happens to them")

All **built** as beats today; the rework is **authoring content for them + timing them.** Grouped:

| Group | Beats (built) | What the player experiences |
|---|---|---|
| **Discovery** | BookAppears, LecternFill, SignWrite, ChestArrange, ItemRelabel/Swap, MapMark, SmallStructure (incl. FAWE schematics) | a book/sign/poster appears or fills; items arranged into a *meaning*; a map fragment; a structure that's just *there* |
| **Creep (deniable)** | TorchGutter (lights out behind you), PrivateSound (a footstep / whisper, one player), PrivateParticle, PrivateDarkness (≤15s), PrivateTimeShift, DecayCreep | "did you see that?" — only your screen; never shown; never a jumpscare |
| **It-knows-me** | PrivateMessage, SignWrite/BookAppears with your data, NamedMob (the silent watcher — **AI-disabled, stand-and-stare**) | a sign that says "you have died here twice"; a figure at render-edge that never moves |
| **World-change** | FakeBlock, DoorOpen, SmallStructure swap, DecayCreep | a sealed door open that was shut; a room subtly rearranged on return; the city "un-lights" |
| **Custom 3D (Phase 2.5)** | MythicMobs + ModelEngine creatures (vanilla fallbacks) | bespoke apparitions — only in fog / glimpsed / censored |
| **Toll / consequence** | WhisperToll, custom-violation reports | the cost of a hint; the Watcher noting a broken custom |

**Wifies-grade techniques to add (Minecraft-feasible, in `ARG-RESEARCH.md`):** the **half-beat-late
footstep** (per-player sound from *behind*, delayed 4–6 ticks), **torches snuffing in silence behind
you**, the **chunk-load "grass grew where you left it"** proof-of-presence, **impossible-but-rule-based**
anomalies (a forest in a lightless cave). All achievable with the beat engine + scheduled tasks.

**The discipline:** 95% of the world is **safe, quiet, ordinary.** Spooks are **rare, per-player,
well-timed.** Restraint is the whole craft.

---

## 7. Clue & lore drops — in-game AND Discord

| | In-game | Discord (#the-record / the archive) |
|---|---|---|
| **Clue drop** | a new book/sign/structure appears overnight; an NPC opens a new line; a place becomes reachable | the showrunner drips a **forged clue card** (already wired) or a report line |
| **Lore drop** | found in the world (letters/reports/posters/journals/tableaux) | a "recovered document" / redacted incident PDF; the archivist annotates finds |
| **Answer / solve** | the **answer-sign** (carve the plaintext) → the world responds | **/answer** or the **#the-record** scan → the Watcher replies with **lore** |
| **Hints** | an NPC says more if you linger; a second cross-referencing paper | **/whisper** (tiered, paid; auto-gift backstop if stuck) |
| **Cadence** | overnight batches; fallow days | the drip (cadence-gated; AUTO or CONFIRM via your dashboard) |

**Rule:** clues feel **found, not handed** (no [Quest] tags, no "clue found!" toasts); every solve's
payoff is a **piece of the lost world**, never just a key.

---

## 8. The tech stack — full integration

| Component | Role | Status |
|---|---|---|
| **Paper plugin** (`plugin/`) | the engine: signal tracking, customs detection, the beat palette, oracle (in-world), reveal discipline, FAWE paste | **built** (you're extending customs) |
| **FastAsyncWorldEdit** | paste curated `.schem` set-pieces (the many places) out of sight | wired (`SmallStructureBeat`) — needs the `.schem` content |
| **Citizens2 / ZNPCsPlus** | surface NPCs + per-player apparitions, dialogue trees on the dossier | Phase 2 (mod stack chosen) |
| **MythicMobs + ModelEngine** | custom 3D creatures (vanilla fallbacks) — glimpsed only | Phase 2.5 (optional garnish) |
| **Resource pack** (one click) | rune font, ambient/whisper **sounds**, poster/redacted-page textures, custom item models, **fog/desaturation** for the wrong zones | spec'd (`atmosphere-stack.md`) — needs assets |
| **Simple Voice Chat** | late-arc spatial dread / proximity voice (optional client install) | Phase 3 |
| **Discord bot** (`discord/`) | the Watcher persona + the **Recovery Archive** (ship-log cards, 5 threads), /answer, /whisper, clue-card posts | **built** (you're extending the drip) |
| **Showrunner** (cron) | the deterministic conductor: drip, stall-gift, Attention reactions, reports — zero-LLM spine | **built** (you're extending it) |
| **Supabase** | shared state: dossiers, customs, beats, puzzles/oracle, settings | **live** (23 puzzles seeded) |
| **Dashboard** (Vercel) | **your director's console**: approve/stage beats (AUTO⇄CONFIRM), health, manual drip | **built** |
| **AI showrunner layer** | (later) personalized reports + reactive authoring, every write validated + gated | future, on top of the spine |

---

## 9. Pacing & timing (your breathing-room rule, made concrete)

- **Movement I is mostly normal play.** One small wrongness in the first days, then *quiet.*
- **Drama budget** caps beats per session and per hour; the showrunner enforces it.
- **Fallow days** scheduled — stretches where nothing new appears, so the next beat lands hard.
- **Per-player, not global** — spooks hit one person at a time (deniability), so the group isn't
  saturated at once.
- **Escalation is earned** — Movement IV's high-dread beats only work because I–II were sparse.
- **You conduct the peaks** — the dashboard lets you hold a big beat until the moment's right (everyone
  together, mics hot, on camera).

---

## 10. Build roadmap (simple, ordered — story-first)

1. **Seal the answer bible** — write the full truth (WHO/PLACE/HAPPENED/SURFACE/HUMAN/ending) in `arc/`,
   author the **fragment → revelation ledger**. *Plan the ending first.*
2. **Re-cast the keepers as characters** — names, lives, relationships, contradictory accounts, decaying
   journals.
3. **Design the world map** — the many places (normal-server spawn + discoverable sites + the wrong,
   deep, buried-era layer), each tagged to threads, geography-implies-history.
4. **Author the artifact corpus** — letters, reports, posters, journals, signs (lots), all
   cross-referencing, tagged to the ledger.
5. **Build the Recovery Archive** (Discord ship-log: rumor/verify cards, 5 colored threads).
6. **Wire the customs → reports/consequence** (the coherence audit's P0-4) + the **Attention system**.
7. **Build the `.schem` set-pieces + resource-pack assets** (the places, posters, sounds, fog).
8. **Re-home the puzzles as texture** (solutions = lore) + the it-knows-me beats.
9. **Stand up NPCs** (Citizens2 dialogue on the dossier).
10. **Runtime-test on a local Paper server**, pacing pass, then go live.

*(Each step changes story + clue + interaction + mechanic together — your consistency rule.)*

---

## 11. Open decisions for you
1. **Setting flavor** of the lost place (drowned harbor / sealed mountain hold / buried colony / sunken
   cathedral-state) — I can pitch 2–3 fully-realized, you pick; or you set it.
2. **Do you want to stay unspoiled on the ending**, or read the sealed bible so you can direct knowingly?
3. **How much existing keeper lore to preserve** vs re-author from scratch.
4. **Scope of the first build** — a tight vertical slice (one region, ~Movement I) to prove the feel
   before scaling to the whole world? (Recommended.)
