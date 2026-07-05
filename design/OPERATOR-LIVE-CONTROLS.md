# THE OBSERVANCE - OPERATOR LIVE CONTROLS

> Launch-facing guide. This file separates live operator controls from design/spec notes so the server
> runner does not tune a knob that no runtime reads.

## The Simple Runtime Model

- The Minecraft plugin watches what players do and performs in-world theater.
- Supabase remembers progress, flags, solves, events, and archive rows.
- The showrunner decides when authored content appears.
- Discord and the website display the record.

If a control does not clearly affect one of those four surfaces, treat it as a design note until a check
or code reference proves otherwise.

## Required Launch Controls

### Resource Pack

File: `plugin/src/main/resources/config.yml`

Live keys:

- `resource-pack.url`
- `resource-pack.sha1`
- `resource-pack.required`
- `resource-pack.prompt`
- `resource-pack.delay-ticks`

Launch rule:

- host the resource pack zip
- set URL and SHA1
- keep `required: false` until the URL is tested with a real client
- the rune font and rune-heavy beats depend on this

### Supabase

Live keys:

- `supabase.url`
- `supabase.service-key-env`
- `supabase.service-key`
- network timeout/retry settings
- `offline-queue-max`

Launch rule:

- never commit a service key
- prefer the env var
- rotate previously exposed service-role credentials before launch

### Drama Budget

Live keys:

- `drama.enabled`
- `personalized-cooldown-minutes`
- `personalized-max-per-session`
- `ambient-cooldown-minutes`
- `ambient-global-cooldown-minutes`
- `window-minutes`
- `window-max-beats`
- `personalized-confidence-min`

Launch rule:

- restraint is the horror
- increase only after playtest proves dead air
- do not make the world perform constantly

### Tracker / Customs

Live keys:

- `tracker.enabled`
- `tracker.deep-line.*`
- `tracker.kept-light.*`
- `tracker.dark-hours.*`
- `tracker.sacred-beast-pdc-key`
- `tracker.answer-sign.cooldown-seconds`
- `customs.unlit-deep.*`
- `restraint.enabled`

Launch rule:

- these are detection controls, not story text
- keep forbidden words empty unless you are intentionally enabling the Unspoken detector
- violations should take warmth and certainty, not progress

### Physical Puzzle Producers

Live keys under `puzzles:`:

- `orin-bow-fall-order`
- `m4-three-hands`
- `mara-walk-the-map`
- `sella-shore-memorial`
- `sella-reflection-bearing`
- `vaun-hoard-sorted`
- `mara-lectern-lock`
- `vaun-bookshelf-tally`
- `orin-frame-dials`
- `brann-silence-corridor`
- `spine-threshold-vault`
- `seventh-choice`
- `brann-black-moon-toll`

Launch rule:

- do not edit opaque tokens by hand unless you also update the SQL seed and run checks
- placement in `sites.yml` is required before a producer can be experienced

### Finale / Release

Live keys:

- `rites.accepting.*`
- `closing.enabled`
- `closing.theater-seconds`
- `closing.whitelist-after`
- `closing.release-rite-enabled`

Launch rule:

- run `/observance reading`
- run `/observance finale`
- keep whitelist-after off unless you want a hard theatrical lockout after the kick

## Spec-Only Or Documentation-Risk Areas

These sections may appear in config or docs, but should not be treated as live operator controls unless
code checks prove a reader exists.

- `customs.false-law.enabled` - documented lore-fiction toggle; current plugin does not read it.
- `herd.pale-cosmetic-pdc-key` and `herd.max-pale` - values match current intended constants, but parts
  of the beat path have historically hardcoded these or deferred the producer.
- `difficulty.*` in Java config - spec for the showrunner difficulty model, not a Java plugin control.
- `event-window.*` in Java config - spec unless the deployed showrunner reads its own matching config.

Director rule:

- dead/spec-only knobs should be moved out of live config over time
- until then, this guide wins for launch operation

## Required Manual Launch Steps

1. Apply migrations and reseed Supabase.
2. Run `npm run archive:materialize` or confirm cron materializes archive bodies.
3. Host resource pack and set URL/SHA1.
4. Survey and place all required sites.
5. Spawn townsfolk.
6. Stage cold open with `/observance placeprologue`.
7. Run `/observance reading`.
8. Run `/observance finale`.
9. Host `dashboard/public/the-hold/the-hold.zip`.
10. Prepare found footage, Drive folder, waveform/spectrogram.
11. Rotate exposed credentials.
12. Run the vertical slice.

## Verification Commands

From the repo root:

```powershell
python tools\check_experience_coherence.py
python tools\check_namespace_collisions.py
python tools\check_voice_register.py
```

From `discord/`:

```powershell
npx tsc --noEmit
npm run -s seedcheck
npm run -s gatecheck
npm run -s specscheck
npm run -s showrunner:test
npm run -s showrunner:test:autonomy
npm run -s showrunner:test:archive
npm run -s showrunner:test:scenario
npm run -s showrunner:test:customs
npm run -s showrunner:test:prologue
npm run -s showrunner:test:herd
```

From `plugin/`:

```powershell
gradle jar
```

