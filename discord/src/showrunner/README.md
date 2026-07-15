# Showrunner runtime

`run.ts` exports one import-safe `runShowrunnerTick()` and executes it only when launched directly.
Every live tick takes the SQL lease from migration `0011_showrunner_lock.sql`, applies deterministic
policy, and releases in `finally`.

`persistent.ts` is the production owner:

- immediate first tick when the Discord client is ready;
- 12-second default cadence, clamped to 10–15 seconds;
- local in-flight guard so a slow tick never piles up in one process;
- 300-second default SQL lease, shared with every other process;
- caught errors logged by the bot without terminating Discord.

Railway also runs `npm run showrunner` every ten minutes. It is recovery for a dead worker, not a
second authority: if the worker owns the lease, the cron exits cleanly as `locked`.

Useful commands:

```powershell
npm.cmd run showrunner:dry
npm.cmd run showrunner:test:persistent
npm.cmd run showrunner:test:all
npm.cmd run runtimecheck
```

`--dry-run` takes no lease and performs no writes. The legacy autonomy policies remain fault-isolated,
but V5 progression itself is the mandatory investigation-node/receipt graph. No showrunner policy may
open a future V5 node, substitute for a required receipt, or choose a finale branch for players.
