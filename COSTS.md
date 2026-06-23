# Claude API cost — 2-week run estimate

> You'll use your **own** new Anthropic account + key. The code reads `ANTHROPIC_API_KEY`
> from the environment — no key is ever embedded in the repo. Nothing here uses any
> other account.

**Current pricing (per 1M tokens):**

| Model | Input | Output | Use for |
|---|---|---|---|
| Opus 4.8 (`claude-opus-4-8`) | $5.00 | $25.00 | rare marquee beats |
| Sonnet 4.6 (`claude-sonnet-4-6`) | $3.00 | $15.00 | most scalpel beats |
| Haiku 4.5 (`claude-haiku-4-5`) | $1.00 | $5.00 | cheap classify/pulse |
| Prompt cache read | ~0.1× input | — | stable arc/design context |

## Where the money actually goes
The deterministic engine is free. Three things can hit the API:

1. **Scalpel beats** (personalized reports/journals) — rare by design. ~3/session × ~8
   sessions = ~24 calls, ~3k in / ~500 out each → ~72k in + 12k out total.
   - Sonnet: ≈ **$0.40** total. Opus: ≈ $0.66. *Pennies either way.*
2. **Whispers** (Discord hints) — **pre-authored**, so ~**$0** per request at runtime.
3. **Showrunner** (between-session authoring agent) — the only real cost. ~8 runs,
   each agentic: ~150k in / ~15k out.
   - Sonnet, no caching: ~$0.68/run → ~**$5.40**.
   - Opus, no caching: ~$1.13/run → ~$9.
   - **With prompt caching** (the arc bible + design docs are stable → read at 0.1×):
     input cost drops ~80–90% → showrunner ≈ **$2–4 total**.

## Bottom line (a couple-week friend-group run)

| Setup | Estimated 2-week cost |
|---|---|
| **Lean** — Sonnet beats + Sonnet showrunner + caching | **~$3–6** |
| **Mid** — Sonnet beats, occasional Opus marquee, Opus showrunner + caching | **~$8–15** |
| **Heavy** — Opus everywhere, frequent showrunner, no caching | **~$25–40** |

It's a hobby-tier bill. Two levers if you want it near-zero:
- **Local Ollama** for the frequent/cheap stuff (pulse, simple beats) → only marquee
  Opus beats hit the API.
- **Prompt caching** on the showrunner's stable context (biggest single saver).

**Recommendation:** Opus 4.8 for the handful of marquee beats (quality where it's seen),
Sonnet 4.6 for routine beats + the showrunner, caching on the stable context. Lands
in the **~$8–15** band for two weeks. Start your new key with a $20 cap and you can't
overshoot.
