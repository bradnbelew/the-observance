"use server";

import { revalidatePath } from "next/cache";
import { createAdminClient } from "@/lib/supabase/admin";
import type { BeatStatus, Json } from "@/lib/database.types";

/**
 * Server actions — the dashboard's write surface.
 *
 * EVERY action re-checks isAdmin() before touching the service-role client, so
 * authorization never relies on the page-level gate alone. The admin client
 * bypasses RLS; isAdmin() (ADMIN_EMAILS allowlist) is the only thing standing
 * between a request and a privileged write, so it must be the first line.
 *
 * Each action revalidates /author so the server-rendered control surface
 * reflects the new state on the next paint.
 */

export type ActionResult = { ok: boolean; error?: string };

const FORBIDDEN: ActionResult = {
  ok: false,
  error: "Not authorized.",
};

// Author mode is intentionally open (no login). Restore `return isAdmin()`
// (and re-add the import) to re-gate writes behind the ADMIN_EMAILS allowlist.
async function guard(): Promise<boolean> {
  return true;
}

function refresh() {
  revalidatePath("/author");
}

// ---------------------------------------------------------------------------
// Arc control — advance / rewind the act, edit gates & flags.
// ---------------------------------------------------------------------------

/**
 * Move the arc to an explicit act. Clamped to [1, 3] (Establishment → Ways →
 * Accepting). There is a single arc_state row (id = 1, enforced by a check
 * constraint), so we always target it.
 */
export async function setArcAct(act: number): Promise<ActionResult> {
  if (!(await guard())) return FORBIDDEN;

  const next = Math.max(1, Math.min(3, Math.trunc(act)));
  const supabase = createAdminClient();

  const { error } = await supabase
    .from("arc_state")
    .update({ current_act: next, updated_at: new Date().toISOString() })
    .eq("id", 1);

  if (error) return { ok: false, error: error.message };

  await supabase.from("event_log").insert({
    level: "warn",
    source: "dashboard",
    message: `Arc act set to ${next} from the Author control surface.`,
  });

  refresh();
  return { ok: true };
}

/**
 * Advance the arc by one act (form-action shaped). Reads the current act from
 * the hidden field so we don't need a second round-trip.
 */
export async function advanceArc(formData: FormData): Promise<ActionResult> {
  const current = Number(formData.get("current_act") ?? 1);
  return setArcAct(current + 1);
}

/**
 * Rewind the arc by one act (form-action shaped).
 */
export async function rewindArc(formData: FormData): Promise<ActionResult> {
  const current = Number(formData.get("current_act") ?? 1);
  return setArcAct(current - 1);
}

// ---------------------------------------------------------------------------
// Beat queue — approve / force / skip.
// ---------------------------------------------------------------------------

/**
 * Decide a queued beat. Sets status + decided_at (the timestamp the anti-jank
 * gate cares about). "approved" and "fired" are both real decisions:
 *   - approve: let the engine fire it on its own cadence
 *   - force:   mark it fired now (used to push a beat immediately / for testing)
 *   - skip:    drop it without firing
 */
async function decideBeat(
  id: number,
  status: Extract<BeatStatus, "approved" | "fired" | "skipped">,
): Promise<ActionResult> {
  if (!(await guard())) return FORBIDDEN;

  const supabase = createAdminClient();

  const { error } = await supabase
    .from("beat_queue")
    .update({ status, decided_at: new Date().toISOString() })
    .eq("id", id);

  if (error) return { ok: false, error: error.message };

  await supabase.from("event_log").insert({
    level: "info",
    source: "dashboard",
    message: `Beat #${id} ${status} from the Author control surface.`,
  });

  refresh();
  return { ok: true };
}

export async function approveBeat(formData: FormData): Promise<ActionResult> {
  return decideBeat(Number(formData.get("id")), "approved");
}

export async function forceBeat(formData: FormData): Promise<ActionResult> {
  return decideBeat(Number(formData.get("id")), "fired");
}

export async function skipBeat(formData: FormData): Promise<ActionResult> {
  return decideBeat(Number(formData.get("id")), "skipped");
}

// ---------------------------------------------------------------------------
// Whisper budgets — edit the hint economy per player/act.
// ---------------------------------------------------------------------------

/**
 * Update a whisper budget row's editable counters (budget / spent / earned).
 * Only fields present in the form are written; values are clamped to >= 0.
 */
export async function updateWhisperBudget(
  formData: FormData,
): Promise<ActionResult> {
  if (!(await guard())) return FORBIDDEN;

  const id = Number(formData.get("id"));
  if (!Number.isFinite(id)) return { ok: false, error: "Bad budget id." };

  const clamp = (raw: FormDataEntryValue | null) => {
    const n = Math.trunc(Number(raw));
    return Number.isFinite(n) ? Math.max(0, n) : 0;
  };

  const patch = {
    budget: clamp(formData.get("budget")),
    spent: clamp(formData.get("spent")),
    earned: clamp(formData.get("earned")),
  };

  const supabase = createAdminClient();

  const { error } = await supabase
    .from("whisper_budgets")
    .update(patch)
    .eq("id", id);

  if (error) return { ok: false, error: error.message };

  await supabase.from("event_log").insert({
    level: "info",
    source: "dashboard",
    message: `Whisper budget #${id} edited (budget=${patch.budget}, spent=${patch.spent}, earned=${patch.earned}).`,
  });

  refresh();
  return { ok: true };
}

// ---------------------------------------------------------------------------
// Watcher sleep — mute everything for a sensitive session.
// ---------------------------------------------------------------------------

/**
 * Toggle settings.watcher_sleep (a JSONB boolean). The plugin reads this key to
 * decide whether to suppress all beats. We upsert so the row exists even if the
 * seed migration hasn't run.
 */
export async function setWatcherSleep(
  asleep: boolean,
): Promise<ActionResult> {
  if (!(await guard())) return FORBIDDEN;

  const supabase = createAdminClient();

  const value: Json = asleep;
  const { error } = await supabase
    .from("settings")
    .upsert(
      { key: "watcher_sleep", value, updated_at: new Date().toISOString() },
      { onConflict: "key" },
    );

  if (error) return { ok: false, error: error.message };

  await supabase.from("event_log").insert({
    level: "warn",
    source: "dashboard",
    message: asleep
      ? "Watcher put to sleep — all beats muted."
      : "Watcher woken — beats resume.",
  });

  refresh();
  return { ok: true };
}

/** Form-action wrapper: reads the desired next state from a hidden field. */
export async function toggleWatcherSleep(
  formData: FormData,
): Promise<ActionResult> {
  const next = String(formData.get("next")) === "true";
  return setWatcherSleep(next);
}

// ---------------------------------------------------------------------------
// The Accepting — guarded manual trigger (testing the Act-3 ritual).
// ---------------------------------------------------------------------------

/**
 * Insert a beat_queue row of type 'trigger_accepting'. This is the manual,
 * for-testing path to fire the climax; it lands as a PENDING beat so it still
 * flows through the same approve/force gate as everything else (the row is the
 * trigger, not an immediate world write). A confirmation token from the guarded
 * button must match to proceed.
 */
export async function triggerAccepting(
  formData: FormData,
): Promise<ActionResult> {
  if (!(await guard())) return FORBIDDEN;

  if (String(formData.get("confirm")) !== "ACCEPTING") {
    return { ok: false, error: "Confirmation phrase did not match." };
  }

  const supabase = createAdminClient();

  const { error } = await supabase.from("beat_queue").insert({
    type: "trigger_accepting",
    target: null,
    payload: { manual: true, source: "dashboard" },
    status: "pending",
  });

  if (error) return { ok: false, error: error.message };

  await supabase.from("event_log").insert({
    level: "warn",
    source: "dashboard",
    message: "The Accepting was manually queued from the Author control surface.",
  });

  refresh();
  return { ok: true };
}
