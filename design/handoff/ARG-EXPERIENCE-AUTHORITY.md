# The Observance — ARG Experience Authority

**Status:** BINDING CAMPAIGN-LEVEL EXPERIENCE AUTHORITY, 2026-07-17  
**Human gate:** Brad approval is null. P4 vNext is technical proof only and is experientially rejected.  
**Scope:** P1–P12, Minecraft, Copperline, Discord, dashboard/web, media, NPCs, replay, and aftermath.

## Controlling distinction

The Observance may contain lore scavenging and bounded puzzles, but neither is its campaign grammar.

- A **lore scavenger** asks players to collect or read canon.
- An **escape room or puzzle hunt** presents a known bounded game whose designed prompts are solved and
  submitted.
- An **ARG** behaves as a responsive distributed fiction and investigation. Ordinary-looking surfaces,
  characters, community history, files, places, and consequences continue to exist around the players.
  Players form theories, initiate consequential actions, and receive authored responses from the world.

Brad's exact controlling distinction is: story, lore, and immersion mean players feel they are **IN the
ARG and genuinely investigating**, not consuming lore documents and filing their summaries. Text may be
abundant. The rejected loop is a source stating a conclusion followed by the player restating it into an
answer field.

## Research basis

The 2006 IGDA ARG SIG whitepaper defines ARGs as active experiences that use the world around the
player, and describes the form as **exposition + interaction + challenge**, with the balance varying by
work. It warns that successful games invent experience-specific methods instead of treating a mechanics
list as exhaustive. It also treats player forums, shared resources, analysis, and action as central to a
community that makes an ARG come alive. See [IGDA ARG SIG, *2006 Alternate Reality Games White
Paper*, pp. 5, 30, 36–43, 58–60](https://www.christydena.com/wp-content/uploads/2007/11/igda-alternaterealitygames-whitepaper-2006.pdf).

McGonigal's research describes players investigating hidden affordances in everyday media and using
collective sleuthing to manage uncertainty; her history of TINAG also shows that deliberately ambiguous
signals become platforms for player debate rather than cleanly labeled prompts. See [Jane McGonigal,
*This Might Be a Game*, especially chapters 5–6](https://janemcgonigal.com/wp-content/uploads/2010/12/mcgonigal_this_might_be_a_game_sm-1.pdf).

The Beast crossed posters, sites, phone records, live conversations, rallies, and a player-created archive;
the distributed murder inquiry was the glue, not a page menu. See [Kim, Lee, Thomas, and Dombrowski,
“Storytelling in new media: The case of alternate reality games, 2001–2009”](https://firstmonday.org/ojs/index.php/fm/article/download/2484/2199).

Why So Serious? made participants act as Gotham citizens across web, mail, streaming media, artifacts,
events, and user-created work; player activity changed the campaign's ongoing public fiction. See [The
One Club case record](https://www.oneclub.org/awards/theoneshow/-award/11241/why-so-seriousa-the-dark-knight-alternate-reality-game/).

I Love Bees and Perplex City demonstrate different scales of distributed cognition: fragments and
specialisms were pooled through community infrastructure, while participation continued beyond any one
person's access. See the IGDA case material above and [WIRED's retrospective on Perplex City and its
player community](https://www.wired.com/story/perplex-city-satoshi/). The point is not to copy payphones,
public travel, or decade-long searches; controlled fictional equivalents must preserve accessibility,
privacy, safety, and any-subset play.

## Binding experience rules

1. Every active case begins with an anomaly or live unknown, not a docket asking for a known class of
   answers. At least two interpretations must remain defensible long enough to support real theory work.
2. No single source prints the final belief. Relationships among fragments, provenance, behavior, and
   consequences earn it.
3. Players initiate actions that matter: authenticate, test, restore, compare, contact, coordinate,
   repair, expose, withhold, trust, publish, configure, or invent another case-specific act. This list is
   illustrative, never a taxonomy or runtime enum.
4. The world answers. Exact A0/A1 authored triggers may change a page, account, NPC line, Discord thread,
   Minecraft state, available media, or later callback. Reactivity must be durable, idempotent, replayable,
   and catch-up safe. A2 remains approval-controlled exactly as already locked.
5. Answer inputs are a minority diegetic verb. Correct knowledge or action succeeds with zero observation,
   click, possession, or source-touch receipts. Receipt history remains provenance and contribution only.
6. Community cognition is authored: asymmetric routes, competing hypotheses, distinct specialisms,
   player-created timelines/maps/transcripts/theories, and discussion. No absent named player may block
   progress.
7. Minecraft is an investigated place, Copperline is lived-in hosting/community history, and Discord is
   community/character/action space. None is reduced to an answer portal or clue menu.
8. Rabbit-hole chains change where or how players search. Later events must recontextualize ordinary
   earlier material. Quiet human scenes remain rewarding even when they do not open a lock.
9. Difficulty comes from inference, provenance, synthesis, uncertainty, callbacks, action choice, and
   collaboration—not UI friction, click quotas, F3/log dependence, forced voice, unsafe public activity,
   or bespoke block-perfect archaeology.
10. A technical pass never implies experiential acceptance. Static checks can reject structural
    anti-patterns; cold humans must judge whether the world feels alive and whether the investigation is
    genuine. Brad approval remains a separate explicit gate.
11. No interpretive conclusion may depend on guessing a hidden canonical sentence. Long exact prose is
    forbidden unless the evidence itself fairly and intentionally yields that exact text. Codes, names,
    coordinates, decoded words, filenames, and other exact artifacts may use exact predicates. A theory,
    judgment, or synthesis must instead use a physical decision, clearly separated short claims, or
    deterministic meaning components with authored synonym and word-order coverage. Natural defensible
    paraphrases pass; partial, contradictory, and irrelevant accounts fail without leaking the solution.
    The input surface must state the response shape, not the answer. This clarity is interface design, not
    handholding.

## Answer-shape contract

- **Exact artifact:** use a short exact input only when the player can derive or copy the exact value from
  a fair transform or authenticated source.
- **Interpretive conclusion:** split the judgment into the few distinct claims the evidence must support,
  or embody it as a bounded world action. Never publish an invisible model sentence for players to guess.
- **Deterministic validation:** normalize each claim independently and match versioned meaning components,
  relationships, and contradictions. Do not use an LLM, unrestricted chat parser, observation receipt, or
  source possession as the judge.
- **Proof:** tests must cover multiple natural paraphrases and word orders, partial accounts, plausible wrong
  theories, negation/contradiction, irrelevant keyword stuffing, zero-observation correctness, throttle,
  idempotency, restart, and parity across primary and accessibility input surfaces.

## Case brief and review contract

Every P1–P12 brief must name its inciting anomaly, live unknown, competing hypotheses, distributed
fragments, provenance work, player-initiated actions, authored reactivity, collaborative/asymmetric paths,
cross-surface consequence, delayed callback, earned belief, and novelty against adjacent cases. It must
also state why the case is not a direct-source/restatement loop, a single-surface bounded exercise, an
interaction-free read, or a repeated answer-box flow.

The machine contract is `design/handoff/ARG-EXPERIENCE-AUTHORITY.json`; the executable structural audit is
`tools/check_arg_experience_authority.py`; and the active case choreography is
`campaign/arg-experience-redesign.json`. Passing them authorizes offline implementation work only. It does
not authorize another Brad server, hosted production, or a launch-readiness claim.
