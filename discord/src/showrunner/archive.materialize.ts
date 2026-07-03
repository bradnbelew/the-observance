/**
 * archive.materialize.ts — standalone one-shot to populate the Recovery Archive bodies at DEPLOY time,
 * so `v_archive` has text to reveal from the first cron tick (the cron also refreshes it each tick).
 *
 *   npm run archive:materialize
 *
 * Idempotent (upsert on card_key). Exits 0 on success, 1 on failure (a deploy step that must be visible
 * if it breaks, unlike the in-tick pass which is fault-isolated so it can never abort the spine).
 */
import { materializeArchive } from './archive.run.js';

materializeArchive()
  .then((r) => {
    console.log(`[archive] materialized ${r.materialized} card bodies into thread_card_bodies`);
    process.exit(0);
  })
  .catch((e) => {
    console.error('[archive] FATAL', e);
    process.exit(1);
  });
