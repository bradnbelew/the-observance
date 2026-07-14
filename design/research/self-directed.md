# Self-Directed Research — Personalization, Small-Group Mystery, Cross-Surface, Stego, Consent, Resilience, Payoffs

> **RESEARCH REFERENCE — NOT STORY, SETUP, OR RUNTIME AUTHORITY.** Any project-specific recommendation below is superseded by the V5 manifests and runbooks.

**Lane:** self-directed. **For:** The Observance (server-side Minecraft ARG, Paper 1.21.x + Discord bot + Supabase/Vercel dashboard, veteran friend group, Path A = one auto-pushed resource pack). North star: *"From The Fog, but it knows your name."*

This note is the **psychology + systems-design companion** to the existing corpus (`ARG-RESEARCH.md`, `research/arg-craft.md`, `research/mc-arg-genre.md`). Those cover environmental-storytelling technique, the genre's authored rules, and the Minecraft realization layer. This note deliberately does **not** repeat them. It chases the eight self-directed threads: (1) making "it knows me" uncanny not invasive; (2) what changes for a *small known group*; (3) hint/rescue + dynamic difficulty; (4) cross-surface integration & pitfalls; (5) steganography that survives Discord; (6) consent/care-ethics for profiling friends; (7) arc resilience to erratic play; (8) seeding payoffs that actually get noticed.

Every lesson is tagged **→ Observance:**.

---

## THREAD 1 — "It knows my name" without being creepy/invasive: the uncanny line

The single hardest design problem in this project. The reference games (DDLC, Petscop, Inscryption) all walk a razor's edge. The recurring lesson from every one of them: **the uncanny lives in the gap between what's shown and what's implied. Cross into explicit, literal, accurate-personal-data territory and it flips from "uncanny" to "invasive/cheap" instantly.**

### 1.1 Salvato's distinction: aim for *uncanny*, not *scary*
Dan Salvato (DDLC creator) explicitly prioritized **the uncanny** — "a feeling of not belonging, loss of control, or distorted reality" — over conventional scares. DDLC's thesis is "letting the player know the game *sees them*." The mechanism is not a jumpscare; it's **a normal thing behaving as if it has a will of its own and is aware of you specifically.** ([intermittentmechanism.blog](https://intermittentmechanism.blog/2022/11/23/doki-doki-literature-club-a-hidden-horror-story/), [Wikipedia: DDLC](https://en.wikipedia.org/wiki/Doki_Doki_Literature_Club!))

**The four DDLC mechanics, decomposed:**
1. **Earn it with a long boring runway.** "The slow start... is actually a very deceitful tactic, as it lulls the player into thinking the game is nothing out of the ordinary." The first awareness-beat lands *after* the player has fully accepted the surface fiction. Uncanny requires an established normal to violate.
2. **Escalate via small loss-of-control, not big reveals.** First beats: "Yuri's eye drifting off the screen, the mouse being forced to choose a certain option." Tiny agency-thefts read as *the thing is awake*, long before anything names the player.
3. **Reach beyond the frame, but optionally.** Files appear in the game folder; cryptic decode-able messages; a hidden image. Critically: *"One can delve deep into the game if they would like or they can simply play the game as is."* The invasive layer is **opt-in by curiosity** — the player chooses how deep the haunting reaches.
4. **The name is used sparingly.** Monika references the player's name "several times" — not constantly. Scarcity is what makes each use land. Spam it and it becomes a mail-merge token.

### 1.2 Petscop: address a *specific person*, exclude everyone else
Petscop's defining uncanny move: the narrator Paul opens with *"This is just to prove to you that I'm not lying about this game I found"* — the viewer assumes "you" = the audience, then slowly realizes "you" is **one specific person ("Belle") who knows things the viewer can't.** The dialogue "feels strangely uncanny, as if there is some in-joke that the audience is excluded from." ([EGM](https://egmnow.com/theres-something-hiding-in-petscop/), [Wikipedia: Petscop](https://en.wikipedia.org/wiki/Petscop))

The uncanny here is **directed intimacy you're not sure is meant for you.** Creator Tony Domenico deliberately **omitted most of the actual plot** — "scrapping footage that was already done" — to produce "a feeling like there's something strange and complex happening in the background, and you just aren't getting a full view of it."

**→ Observance:** This is the single most important transferable pattern. The Watcher should address a player as if it has been watching *that one player specifically* and knows things about them the others don't — but should **withhold the specifics**. "You came back to the same spot. You always do." beats "You died 14 times at -211,64,883." The first is intimate and unfalsifiable; the second is a stat readout that reveals the plugin. **Direct the intimacy at one named player while excluding the group from the in-joke** — the others seeing a message they can tell wasn't "for them" amplifies the effect for everyone.

### 1.3 The implication/ambiguity rule (why showing less reads as "it knows")
Horror-craft consensus: *"Readers will usually imagine something far nastier than whatever you were about to spell out."* Ambiguity isn't just scarier — **it personalizes automatically:** "Not explaining the horror completely gives it a thousand possible faces. Every one of them belongs to the reader's own fear... You fill in the blanks with the things you fear most." ([The Artifice: Vague Horror](https://the-artifice.com/vague-horror-the-scariest-kind-of-horror/), [Hushicho: Shown vs Implied](http://hushicho.blogspot.com/2010/10/horror-technique-shown-versus-implied.html), [TV Tropes: Nothing Is Scarier](https://tvtropes.org/pmwiki/pmwiki.php/Main/NothingIsScarier))

This is the resolution to the project's core tension. **You do not need accurate personal data to feel personal.** A vague, true-of-almost-anyone observation that the player *projects their own specifics onto* outperforms a precise data readout. The plugin can know very little and still feel omniscient if it speaks in implication.

**→ Observance:** Make the Watcher's "knowledge" load-bearing on the *player's* projection, not the *plugin's* database. Author lines that are (a) vaguely true of most players ("You keep one thing you never use. You know the one."), (b) unfalsifiable, (c) phrased as if it's referencing something specific. The player's brain supplies a real memory and assigns it to the Watcher. This is also the **cheapest** path — far less DB profiling, far less creepy, far more uncanny.

### 1.4 The Nemesis-System lesson: memory + interpretation gap = "personal" with tiny content
Shadow of Mordor's orcs feel like *your* nemeses with almost no authored content. The mechanism: persistent memory of wins/losses/escapes + **"short stabs of dialogue, not massive plot-driving cutscenes"** + visible scars from your past encounters. "Much of the narrative takes place in the player's head, as they make connections, infer relationships, and generate histories that may not actually be there." ([Medium: Psychology of the Nemesis System](https://medium.com/@jaygaracini/the-psychology-behind-the-nemesis-system-in-shadow-of-mordor-63eeaea34a2a), [Medium: Niklas Eckstein](https://medium.com/@niklaseckstein/how-the-nemesis-system-creates-stories-d26754b30d2e))

The recipe: **persistent memory + a callback ("you escaped me last time") + a visible scar (state that proves continuity) + deliberately sparse dialogue so the player fills the gap.**

**→ Observance:** The Watcher needs only a thin per-player memory (last death spot, a structure they built, a recurring behavior) plus **one callback that proves continuity** ("I remember the tower."). Don't write the whole relationship — write the anchor and let the veteran's brain build the rest. Keep Watcher lines to "short stabs." The scar = a persistent in-world mark (a block that stays changed, a sign that re-appears) that proves *the same entity remembers across sessions*.

### 1.5 The hard line: where "uncanny" becomes "invasive/cheap"
Synthesizing all four references, the line is crossed when:
- **The reference is *precise and verifiably true* of real-world personal data** (their real name, real location, scraped Discord history, account creation date). This reads as surveillance, not haunting — it breaks the in-world contract and feels like a privacy violation by *you the builder*, not the fiction.
- **The personalization is *frequent*** — name-spam turns the uncanny into a merge field.
- **It's *inescapable*** — DDLC's invasive layer is opt-in by curiosity. Forced deep-personal beats with no off-ramp produce resentment, not dread.
- **It *explains itself*** (reveals the mechanism / sounds like a game system) — kills the uncanny per the implication rule.

**→ Observance:** Hard rules: (1) reference *in-world* behavior (blocks, deaths, paths), never *out-of-world* identity. (2) Cap name/direct-address frequency hard (e.g. ≤ once per session, often zero). (3) Keep the deepest personal beats behind a curiosity gate. (4) Never let a Watcher line read as a stat. See Thread 6 for the consent frame that makes even this safe.

---

## THREAD 2 — Small known group ≠ public ARG: what actually changes (Cogmind post-mortem)

The Cogmind 2020 ARG is the best-matched real case study found: a single dev, a **known community** (not a mass-public hivemind), 185 unique participants tracked, cross-surface (game + website + Discord + Pastebin). The lessons are directly applicable and several contradict mass-ARG canon. ([gridsagegames.com post-mortem](https://www.gridsagegames.com/blog/2020/12/cogmind-2020-arg-post-mortem/), also [gamedeveloper.com mirror](https://www.gamedeveloper.com/design/cogmind-2020-arg-post-mortem))

### 2.1 Don't build a hint system — leverage the existing social graph
The dev built **no formal hint system.** Instead, organic mentorship in Discord carried stuck players: an observed 6-hour session where "experienced players quickly playing to the relevant in-game areas, while others were thinking through puzzles." **Lesson (verbatim spirit):** for small communities, lean on existing social structures rather than building formal hint infra.

**→ Observance:** The veteran group *is* the rescue mechanism — but only if there's a shared channel where being-stuck is visible. The showrunner's job is less "drop hints" and more "make sure the group is talking in one place so the strong carry the stuck." Build the **gathering point**, not the hint vending machine. (See 2.4 for the caveat.)

### 2.2 Passive metrics beat active tracking — and feel less invasive
Cogmind's key progress metric was a **unique-hit counter on an unlisted Pastebin** (185 hits), plus opt-in stat upload (70.5% in ARG mode). The dev explicitly avoided intrusive per-player tracking: "over-surveillance feels intrusive." **Lesson:** passive signals (page views, timestamps, did-they-reach-URL-X) work better than active stat-scraping for small groups.

**→ Observance:** The dashboard should infer progress from **passive trailhead-hits** (did this player trigger phenomenon X / reach site Y / read book Z) rather than logging everything they do. This is also the consent-cleaner posture (Thread 6) and keeps the data minimal.

### 2.3 Linear spine, generous deadline, asynchronous-friendly
Cogmind was a **month-long event** even though a hard-working solo could finish in 1–2 weeks and a group "in a day or two" (fastest team: ~22 hours). The dev concluded **linear design was correct** but regretted not having more optional **side-quests** — of three hidden secrets, only two were ever found. **Lesson:** linear progression + extended deadlines to absorb asynchronous play; add optional side content for the obsessives without gating the main spine on it.

**→ Observance:** A linear *spine* (so nobody is permanently lost) with **optional, non-gating side threads** for the deep-divers. Calibrate the spine to be completable by a casual player over the real calendar window, and assume the obsessive will outrun it — feed *them* the side-quests, not the casuals.

### 2.4 The small-group fragility multiplier (cross-ref arg-craft §0)
Cogmind had 185 participants — still big enough for organic rescue. **The Observance has 5–15, often solo.** The organic-rescue assumption *weakens* at this scale: if the 2–3 people who'd carry the group log off for two weeks, there is no crowd. So: keep 2.1's gathering point, but **do not rely solely on it.** The showrunner must have a *guaranteed* fallback (an in-world escalation that re-opens the trail) — see Thread 3.4.

**→ Observance:** Adopt Cogmind's "lean on the group" *plus* a designed rescue floor, because the group is too small to be a reliable hivemind. Belt and suspenders.

### 2.5 Light obfuscation, not security
Cogmind "spread it around within the executable" and encrypted strings to avoid trivial scraping, while acknowledging "a dedicated individual could still hack it in less than a day" — and decided that was *fine*: "players value authenticity over security."

**→ Observance:** Don't burn effort making clues un-dataminable. The friends won't decompile the plugin to cheat — and if one does, that's a *player*, not an attacker. Spend the effort on the experience, not the lock.

---

## THREAD 3 — Hint / rescue / dynamic difficulty without breaking immersion

### 3.1 Progressive (escalating) hints, time-gated — the Layton/Drawn pattern
Professor Layton: three **progressively-better hints per puzzle**, each costing a coin. The *Drawn* series time-gated hints (wait between clicks) — and the sequel removed the wait, which players preferred. ([TV Tropes: Hint System](https://tvtropes.org/pmwiki/pmwiki.php/Main/HintSystem))

The robust pattern: **tiered hints (nudge → strong nudge → near-solution)**, with the *next* tier unlocking on **time elapsed without progress** rather than on demand. Time-gating prevents instant spoiling; escalation guarantees nobody is permanently walled.

**→ Observance:** Encode rescue as **time-since-last-progress escalation, delivered in-world.** Tier 1 (12–24h stuck): an ambient nudge (a subtle environmental tell points toward the trailhead). Tier 2 (2–3 days): a stronger, more legible clue. Tier 3 (rescue floor): the Watcher itself / showrunner re-stages the trailhead so it cannot be missed. The escalation is invisible-as-a-system because **every tier is diegetic** — it reads as the haunting intensifying, not a hint button.

### 3.2 Hidden DDA breeds suspicion — *availability* can be transparent even if *content* isn't
Design research caveat: "hidden difficulty manipulation can break immersion or create suspicion... transparency about hint *availability* may actually support player experience better than complete concealment." ([Wayline: DDA](https://www.wayline.io/blog/dynamic-difficulty-adjustment-personalized-gaming), [Bootcamp: DDA](https://medium.com/design-bootcamp/product-design-and-psychology-the-use-of-dynamic-difficulty-adjustment-in-video-game-design-7a1e2d919b96))

This is in tension with TINAG (you can't have a visible "hint" button in a no-game-space ARG). Resolution: the **availability** of rescue is communicated *out-of-fiction, once, up front* ("if the group is ever truly stuck, the world will help you" — framed as a consent/expectations note, see Thread 6), while the **content** of each rescue stays fully diegetic.

**→ Observance:** In the one-time consent/onboarding message, promise that *the experience will never hard-lock you* — players then trust the world to escalate rather than rage-quitting when stuck, and you never have to surface a literal hint UI.

### 3.3 Calibrate puzzles for the group, let them lower *game* difficulty for clue-gathering
Cogmind players "switched to easier difficulty modes specifically to collect clues more quickly" — the designer respected this. The principle: **adjust the surrounding game difficulty to match the puzzle, don't make the puzzle compete with survival.**

**→ Observance:** When a beat requires careful observation (reading a structure, decoding a sign), don't simultaneously make it a survival gauntlet. Lower the ambient threat during cognition-heavy beats so the puzzle is the challenge, not staying alive while solving it.

### 3.4 The rescue *floor* — the one channel that always works
For a 5–15 person group, the canonical "the community will figure it out" safety net is too thin (Thread 2.4). The proven mitigation across ARG post-mortems is a **guaranteed minimum-viable path**: at least one trailhead/clue that is *redundant and impossible to permanently miss*, plus a showrunner who re-stages it if telemetry shows the group stalled. ([Game Detectives: How To Basic ARG](https://wiki.gamedetectives.net/w/How_To_Basic_ARG) on redundant trailheads; cross-ref arg-craft §2 "three overlapping rabbit holes.")

**→ Observance:** Every spine-critical clue needs **≥2 independent surfaces** (e.g. in-world sign *and* a Discord-bot whisper) so missing one doesn't break the chain, and the showrunner watches the dashboard for "no progress in N days → trigger the Watcher to physically lead them to it." This is the belt to 2.1's suspenders.

---

## THREAD 4 — Cross-surface (Discord bot + website + game) integration patterns & pitfalls

### 4.1 The core pattern: surfaces *converge*, they don't *fragment*
Cogmind spread engagement across website (passwords/clue pages), game (NPC dialogue, terminals, scene text), and community spaces (Discord, Pastebin, subreddit, wiki). The dev's standout observation: **"hosting an ARG partially inside the game is that people pay *a lot* more attention to the game's details"** — players re-examined years-old content ("was this here before?"). Cross-surface play makes the *whole* surface area suddenly meaningful. ([gridsagegames.com](https://www.gridsagegames.com/blog/2020/12/cogmind-2020-arg-post-mortem/))

**→ Observance:** Each surface should make the *others* feel more alive. A Discord whisper that references an in-world block; a dashboard page that only makes sense after you've seen a sign in-game; an in-game book that points at a website. Done right, the friends start scrutinizing *everything* — the Minecraft world, the Discord server, the site — for hidden meaning. That hyper-attention is the goal.

### 4.2 The fragmentation pitfall (small-group-specific)
The danger with multiple surfaces in a *small* group: a clue posted on surface B is *missed* because everyone was looking at surface A, and there's no crowd to catch it. Webhooks are **one-directional** (external app → Discord) which is fine for *pushing* clues but means the bot can't easily *observe* whether anyone read them without extra wiring. ([Make: Discord Integrations](https://www.make.com/en/blog/discord-integrations-guide), [Discord webhooks docs](https://support.discord.com/hc/en-us/articles/360045093012-Server-Integrations-Page))

**→ Observance pitfalls list:**
- **Don't split spine clues across surfaces with no redundancy** (see 3.4 — ≥2 surfaces per critical clue).
- **Instrument read-receipts.** The bot/site must report back to the dashboard (did anyone open the link / react / reach the page) so the showrunner knows a surface was *seen*, not just *sent*. Webhooks alone won't tell you this — use a real bot read or a tracked URL.
- **One canonical home.** Designate a single "where everyone gathers" channel (Cogmind's lesson 2.1). Scatter clues across surfaces but keep the *discussion* converging in one place, or the small group loses the thread.
- **Surface voice consistency.** The Watcher's voice on Discord and in-game must be the *same entity*. If the bot sounds like a bot and the in-game haunt sounds like a ghost, the cross-surface illusion breaks. (Cross-ref arg-craft §1: every surface must survive being read aloud by someone who believes the Deep Hold is real.)

### 4.3 Don't make the website feel like a "real game website"
TINAG-the-aesthetic (arg-craft §1) extends to the dashboard/site: it must read as an *in-world artifact* (a log, an archive, a monitoring console for the Deep Hold), never as "The Observance — A Minecraft ARG, click to play." The moment a surface labels itself as the game, the no-game-space contract breaks.

**→ Observance:** The Vercel site should present as a diegetic object (the Observance's own records / a found terminal), with the *director's console* gated behind auth and invisible to players.

---

## THREAD 5 — Steganography that survives Discord re-compression (CONCRETE technical finding)

This thread produced a precise, build-relevant result. **The headline: LSB PNG steganography survives Discord *only if the recipient downloads the original via `cdn.discordapp.com` — never the `media.discordapp.net` preview*, which resizes/re-encodes and destroys LSB data.**

### 5.1 Discord's actual image pipeline (the rules)
([Knewest/uncompressed-discord-images](https://github.com/Knewest/uncompressed-discord-images), [aCropalypse/Discord coverage](https://www.androidpolice.com/android-pixel-markup-exploit-discord-acropalypse/), [HN: Discord strips EXIF](https://news.ycombinator.com/item?id=35212899))

- **Two endpoints:** `cdn.discordapp.com` = the **original uploaded file, byte-preserved** (for PNG/JPEG/etc.). `media.discordapp.net` = a **resized/re-encoded thumbnail/preview** (lossy; the in-client preview and many embeds use this).
- **PNG original on cdn:** pixel data preserved (PNG is lossless; the original bytes are served). **BUT** Discord (since Jan 17 2023, post-aCropalypse) **strips trailing data after the PNG IEND chunk** and strips APNG/EXIF/most ancillary metadata. So: **data hidden in pixels survives; data appended after the file or in metadata chunks is destroyed.**
- **PNG preview on media.discordapp.net:** **resized → pixel values changed → LSB destroyed.**
- **JPEG / WebP:** previews are lossy/blurry; even the "original" JPEG path is risky for LSB because JPEG is lossy by nature. **WebP re-encode also lossy.**
- **AVIF:** thumbnails render identically to lossless ("Discord chooses not to process AVIF") — an interesting lossless-preview exception, but exotic.

### 5.2 The robust-stego literature (if you ever need recompression-survival)
If a clue must survive *unknown* re-encoding (e.g. someone screenshots it, or it goes through a lossy path), spatial LSB will not survive. The robust approach is **DCT-domain embedding** — hide bits in the quantized DCT coefficients that survive JPEG recompression. Named technique: **DCT Residual Modulation (DRM)**, which handles spatial pixel overflow (the main cause of coefficient changes on recompression); coefficients are organized into 64 non-overlapping lattices because one change perturbs others in the same block. These methods are **probabilistic, not errorless**, so pair with error-correcting codes / redundancy. ([ScienceDirect: DCT Residual Modulation](https://www.sciencedirect.com/science/article/abs/pii/S0165168424000501), [arXiv 2211.04750: Errorless Robust JPEG Stego](https://arxiv.org/abs/2211.04750))

### 5.3 Practical recommendations
**→ Observance steganography playbook:**
1. **Prefer non-pixel channels that don't fight Discord at all.** The most reliable "hidden in plain sight" Discord clues are *not* LSB stego — they're **visible-but-overlooked** content (text in image corners, a barely-legible string, a QR fragment, audio static), exactly what real ARG trailheads use ([Game Detectives wiki](https://wiki.gamedetectives.net/w/How_To_Basic_ARG): "hiding information in image corners, audio static, and metadata"). These survive any pipeline because they're *in the visible image*.
2. **If you do LSB:** use **PNG**, and **instruct players (in-world) to download the original** — or distribute the PNG as a **file attachment / link to `cdn.discordapp.com`, never relying on the inline preview.** Test: upload, then open the link, swap `media.discordapp.net`→`cdn.discordapp.com`, decode. Better: **host the PNG yourself** (Supabase storage / the Vercel site) and link it, bypassing Discord's pipeline entirely — then LSB is 100% reliable.
3. **Never put the payload in metadata or trailing bytes** — Discord strips both.
4. **Redundancy + ECC** for anything that might be screenshotted or re-shared.
5. **Calibrate to the audience:** for 5–15 friends, a clever *visible* cipher (Thread 1.3 implication) beats fragile bit-level stego that one screenshot kills.

---

## THREAD 6 — Ethical / consent framing for profiling friends (the part that makes this safe)

This is not optional polish — it's what separates "delightful uncanny gift for my friends" from "creepy surveillance that damages trust." The strongest framework found is **care ethics applied to horror RPGs** (Hartford et al., *Beyond Consent: Care Ethics in Horror Role-Playing Games*, IJRP Issue 16), which supplements informed consent with ongoing care. ([journals.uu.se/IJRP](https://journals.uu.se/IJRP/article/view/641))

### 6.1 Torner's disclosure principle — the exact rule for "surprise" personalization
The paper cites **Evan Torner**: a designer asserting "the moral authority of the designer by violating both transparencies" — using *information withheld until a later date* to "prompt a perhaps-undesired moment of moral self-reflection." The paper's resolution (direct quote):

> *"Torner is not saying that such surprises should not occur, but it should be disclosed that a twist will occur even if the details, the information, of the twist will be kept secret."*

The worked example: a player declares at "Session Zero" that they want a betrayal twist *to exist*; everyone consents to *the existence of an unknown twist*; the specifics stay secret. **You disclose that something will surprise you, not what.**

**→ Observance:** This is the exact consent move for a project that profiles friends. Before launch, a single out-of-fiction message: *"Something is going to be on the server. It will watch, it will react to what you do, and over time it will seem to know you. You won't always know what's real. That's the point. You can opt out any time."* This discloses the *existence* of personalized surprise without spoiling a single beat — and it makes the entire "it knows me" arc ethically clean.

### 6.2 Care ethics: consent-before is necessary but not sufficient
The paper's central argument: "conventional consent models may be insufficient for addressing the deep emotional [needs]" because of **bleed** (Montola 2010; Bowman 2013) — fiction-induced emotion crossing into real feeling, and sometimes real antipathy *between players*. The fix is **ongoing care**, not a one-time signature: check-ins *during*, debriefs *after*, and watching for distress. Care ethics "grounds ethical relationships in ongoing processes of concern and care."

**→ Observance:** Three obligations beyond the opening disclosure:
1. **A real opt-out that works mid-experience** (a phrase, a DM to the showrunner) that immediately dials the player's personalization/intensity down — no questions, no penalty. This is the X-Card analog (Stavropoulos's X-Card and "lines and veils" are the canonical LARP safety tools the paper builds on).
2. **The showrunner watches for bleed,** especially with *veterans* — some content (isolation, being watched, helplessness) can hit harder for this audience than intended. Have lines (hard no's) mapped per player if you know their history.
3. **A debrief / curtain.** The ending must let people decompress and re-establish that it was a gift, not a violation. (Cross-ref arg-craft "the curtain.")

### 6.3 Data minimization — only profile what the fiction needs
Game-ethics consensus: collect only data necessary, don't gather PII beyond need, get explicit consent for third-party sharing. ([ixiegaming: Ethics in Game Analytics](https://www.ixiegaming.com/blog/ethical-considerations-in-game-analytics/), [SCU Markkula: Unauthorized Use of Personal Data](https://www.scu.edu/ethics/focus-areas/internet-ethics/resources/unauthorized-transmission-and-use-of-personal-data/))

**→ Observance:** Reinforces Thread 1.3 and 2.2 from the *ethics* side: profile **in-world behavior only** (blocks, deaths, paths, build sites), never out-of-world identity (real names, scraped chat, real location). This is simultaneously (a) more uncanny, (b) cheaper, and (c) ethically clean. The Supabase profile should be a **behavior ledger, not a dossier.** Never reference anything you couldn't have learned by "watching them play."

---

## THREAD 7 — Arc resilience to erratic play: the storylet / quality-based architecture

This thread found the *correct architectural answer* to "friends play erratically, solo, with multi-week gaps": **quality-based / salience-based narrative (storylets)** — Emily Short / Failbetter Games. It is purpose-built for out-of-order, drop-in, asynchronous engagement and is a far better fit than branching arcs. ([emshort.blog: Beyond Branching](https://emshort.blog/2016/04/12/beyond-branching-quality-based-and-salience-based-narrative-structures/), [emshort.blog: Survey of Storylets (Kreminski)](https://emshort.blog/2019/01/06/kreminski-on-storylets/), [SimpleQBN](https://videlais.github.io/simple-qbn/qbn.html))

### 7.1 The storylet data model (implementable as-is)
A **storylet** = three parts:
1. **Precondition** — quality thresholds that must hold (e.g. `seen_tower ≥ 1 AND nights_survived ≥ 3`).
2. **Content** — the beat itself (a Watcher appearance, a sign, a Discord whisper, an environmental change) + optional player choice.
3. **State-change** — outcomes that mutate qualities (`set saw_watcher_at_bed = true`).

**Qualities** = the world/player state (numbers/flags in Supabase). A **storylet registry** = the list of all `{preconditions, content, outcomes}`. The **world state** = current quality values, which determine the *legal set* of storylets at any moment.

- **QBN:** of all currently-legal storylets, the *player* (or here, the showrunner/engine) picks which fires.
- **SBN (salience-based):** the *engine* auto-picks the most applicable legal storylet (the Façade-style drama manager). Short coined "salience-based" for exactly this.

### 7.2 Why this is resilient (the murder-mystery proof)
Short's canonical example: gathering three clues (means/motive/opportunity) *in any order* needs **6+ branches** as a branching tree, but only **4 independent storylets** as QBN — three "find X" storylets (each gated on nothing) + one "accuse" storylet gated on `has_means AND has_motive AND has_opportunity`. Players "weave their own narrative through available options" — "you may not even remember what the chain of causality is." **Order-independence and skip-tolerance are intrinsic**, because beats fire when their *preconditions* are met, not when a linear pointer reaches them.

### 7.3 Named patterns for pacing storylets
Failbetter's documented patterns for organizing storylet pools: **Carousel** (a rotating pool of beats that recur until a condition advances) and **Midnight Staircase** (a sequence that gates progression). These give you *pacing control* on top of the order-independence.

**→ Observance:** **Model the entire arc as a storylet engine in Supabase, not a linear script.** Each haunting beat = a storylet with preconditions over per-player qualities (deaths, builds, days-active, prior-beats-seen, group-progress). The showrunner's between-session job becomes "evaluate which storylets are now *legal* for each player and fire the most salient one" — which is exactly what an asynchronous, solo-friendly, gap-resilient group needs. A player who vanishes for two weeks returns and the engine simply fires whatever beat their *current* state now qualifies for — no broken pointer, no missed-cutscene dead-end. Use **Carousel** for ambient dread (recurring low beats while the player isn't advancing) and a **Midnight Staircase** for the spine. This is the technical backbone that makes Thread 3 (escalation), Thread 2.3 (linear spine + optional sides), and resilience all fall out of one architecture.

---

## THREAD 8 — Seeding "oh THAT is what that was for" payoffs that actually get noticed

### 8.1 The rule: great foreshadowing is "inevitable in hindsight, surprising in the moment"
The retroactive-recognition test: a payoff works when "a twist makes your mind reel back to earlier moments searching for the clues" — and crucially, **"if you can think to look back for these clues."** The seed must be *memorable enough to be recalled* but *innocuous enough not to be solved early.* Super Metroid plants a mini-Kraid so the boss "knows what to expect" — the seed teaches a pattern that pays off later. ([myersfiction.com: Foreshadowing and Payoff](https://myersfiction.com/2025/06/17/foreshadowing-and-payoff-planting-seeds-for-future-plot-points/), [Game Design Snacks: Foreshadowing](https://game-design-snacks.fandom.com/wiki/Foreshadowing_Allows_Players_to_get_a_Hint_of_What_to_Expect))

### 8.2 The small-group noticing problem
The failure mode (Cogmind's regret, Thread 2.3): of three hidden secrets, **only two were ever found.** In a 5–15 person group with no hivemind, a too-subtle seed is simply *never noticed*, and the payoff lands on no one. So the seed must be **odd enough to lodge in memory** without being **solvable**, and ideally **witnessed by multiple players** so the group can collectively "reel back."

### 8.3 Techniques that get payoffs noticed
**→ Observance payoff playbook:**
1. **Make the seed *anomalous*, not hidden.** An out-of-place block, a sign with a word that's slightly wrong, a Watcher line that's a non-sequitur *now*. Anomaly creates a memory hook ("that was weird") that the player can later recall. Petscop's whole engine is "this is weird and I don't know why yet."
2. **Repeat the seed across surfaces** (Thread 4) so more of the group encounters it — the same motif in-game *and* on the dashboard *and* in a Discord whisper triples the odds someone remembers it at payoff time.
3. **Provide the "reel-back" handle at payoff.** When the payoff fires, *echo the seed verbatim* — re-use the exact phrasing/block/image so the brain snaps the connection. Don't rely on memory alone; **re-present the seed inside the payoff.** ("You said the tower was empty. It was never empty." re-quoting the player's own earlier in-world action.)
4. **Use the storylet engine (Thread 7) to *guarantee* the payoff only fires after the seed was seen.** Gate the payoff storylet on `saw_seed_X = true`. This solves the "payoff landed on someone who missed the setup" problem mechanically — nobody gets the payoff cold, and everyone who gets it *has* the reel-back memory.
5. **Let the group do the connecting publicly.** The "oh THAT'S what that was for" moment is most powerful when *one friend says it out loud* in the shared channel and the others go "ohhh." Design payoffs to be *discussable* — drop them where the group will compare notes (Thread 2.1's canonical channel).

### 8.4 Restraint at payoff (don't over-explain)
Per Thread 1.3 and Petscop's omitted-plot philosophy: the payoff should *click into place* but not *fully explain itself.* The best "that's what it was for" still leaves the next "...but why?" open. Domenico's whole method was withholding the plot so "you just aren't getting a full view." Over-explaining a payoff spends the mystery; let each click open the next door.

**→ Observance:** Author payoffs to **confirm one connection while opening one new question.** The friends should finish a beat saying both "ohhh, *that's* what the X was" *and* "...wait, so what's the Y?" — that's what sustains a multi-week arc across an erratic group.

---

## Cross-thread synthesis: the five load-bearing decisions

1. **Profile in-world behavior only; speak in implication.** (Threads 1.3, 6.3) — simultaneously more uncanny, cheaper, and ethically clean. Never reference out-of-world identity. Never let a line read as a stat.
2. **Build the arc as a Supabase storylet engine, not a script.** (Thread 7) — gives order-independence, skip-tolerance, gap-resilience, escalation, and gated payoffs from one architecture.
3. **Disclose the existence of personalized surprise up front; care for bleed throughout; debrief at the end.** (Thread 6.1–6.2) — Torner's principle is the exact consent move; a working mid-experience opt-out is mandatory for a veteran audience.
4. **Every spine-critical clue on ≥2 surfaces + a showrunner rescue floor.** (Threads 3.4, 4.2) — the 5–15 group is too small to self-rescue reliably; redundancy + diegetic time-based escalation guarantees no hard-lock.
5. **Seed anomalies, re-present them at payoff, gate payoffs on seed-seen.** (Thread 8) — the only way "oh THAT's what that was for" reliably lands on a small, erratic group.

---

## Source ledger (primary / authoritative)
- **DDLC as uncanny / file-system horror:** [intermittentmechanism.blog](https://intermittentmechanism.blog/2022/11/23/doki-doki-literature-club-a-hidden-horror-story/), [Wikipedia](https://en.wikipedia.org/wiki/Doki_Doki_Literature_Club!)
- **Petscop (addressed-to-one-person, omitted plot):** [EGM](https://egmnow.com/theres-something-hiding-in-petscop/), [Wikipedia](https://en.wikipedia.org/wiki/Petscop)
- **Nemesis System (memory + interpretation gap):** [Medium/Garacini](https://medium.com/@jaygaracini/the-psychology-behind-the-nemesis-system-in-shadow-of-mordor-63eeaea34a2a), [Medium/Eckstein](https://medium.com/@niklaseckstein/how-the-nemesis-system-creates-stories-d26754b30d2e)
- **Implication/ambiguity horror craft:** [The Artifice](https://the-artifice.com/vague-horror-the-scariest-kind-of-horror/), [TV Tropes: Nothing Is Scarier](https://tvtropes.org/pmwiki/pmwiki.php/Main/NothingIsScarier), [Hushicho](http://hushicho.blogspot.com/2010/10/horror-technique-shown-versus-implied.html)
- **Small-group ARG post-mortem:** [gridsagegames Cogmind 2020](https://www.gridsagegames.com/blog/2020/12/cogmind-2020-arg-post-mortem/)
- **Hint systems / DDA:** [TV Tropes: Hint System](https://tvtropes.org/pmwiki/pmwiki.php/Main/HintSystem), [Wayline: DDA](https://www.wayline.io/blog/dynamic-difficulty-adjustment-personalized-gaming)
- **Discord image pipeline / stego survival:** [Knewest/uncompressed-discord-images](https://github.com/Knewest/uncompressed-discord-images), [AndroidPolice: aCropalypse/Discord](https://www.androidpolice.com/android-pixel-markup-exploit-discord-acropalypse/), [HN](https://news.ycombinator.com/item?id=35212899)
- **Robust DCT stego (if needed):** [ScienceDirect: DCT Residual Modulation](https://www.sciencedirect.com/science/article/abs/pii/S0165168424000501), [arXiv 2211.04750](https://arxiv.org/abs/2211.04750)
- **Consent / care ethics / Torner disclosure:** [Beyond Consent: Care Ethics in Horror RPGs, IJRP 16](https://journals.uu.se/IJRP/article/view/641); [Game Analytics ethics](https://www.ixiegaming.com/blog/ethical-considerations-in-game-analytics/)
- **Storylets / QBN / SBN:** [emshort.blog: Beyond Branching](https://emshort.blog/2016/04/12/beyond-branching-quality-based-and-salience-based-narrative-structures/), [emshort.blog: Survey of Storylets](https://emshort.blog/2019/01/06/kreminski-on-storylets/), [SimpleQBN](https://videlais.github.io/simple-qbn/qbn.html)
- **Foreshadowing/payoff:** [myersfiction.com](https://myersfiction.com/2025/06/17/foreshadowing-and-payoff-planting-seeds-for-future-plot-points/), [Game Design Snacks](https://game-design-snacks.fandom.com/wiki/Foreshadowing_Allows_Players_to_get_a_Hint_of_What_to_Expect)
- **ARG trailhead/redundancy basics:** [Game Detectives: How To Basic ARG](https://wiki.gamedetectives.net/w/How_To_Basic_ARG)
