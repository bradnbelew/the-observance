"use server";

import { revalidatePath } from "next/cache";
import { createAdminClient } from "@/lib/supabase/admin";
import { assertAuthor } from "@/lib/author-auth";
import { coerceFate } from "@/app/author/fate-preview";
import type { BeatStatus, Json } from "@/lib/database.types";

/**
 * Server actions — the dashboard's write surface.
 *
 * Author mode is an operator console: actions use the server-only service-role
 * client directly, with no Supabase magic-link gate.
 *
 * Each action revalidates /author so the server-rendered control surface
 * reflects the new state on the next paint.
 */

export type ActionResult = { ok: boolean; error?: string };

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
  await assertAuthor();
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

export async function advanceArcForm(formData: FormData): Promise<void> {
  await advanceArc(formData);
}

/**
 * Rewind the arc by one act (form-action shaped).
 */
export async function rewindArc(formData: FormData): Promise<ActionResult> {
  const current = Number(formData.get("current_act") ?? 1);
  return setArcAct(current - 1);
}

export async function rewindArcForm(formData: FormData): Promise<void> {
  await rewindArc(formData);
}

// ---------------------------------------------------------------------------
// Beat queue — approve / skip.
// ---------------------------------------------------------------------------

/**
 * Decide a queued beat. Sets status + decided_at. The plugin's poller fires
 * `status = 'approved'` ONLY, so a `pending` beat sits untouched until approved
 * here — that IS the gate:
 *   - approve: open the gate; the beat fires on the plugin's next poll (seconds).
 *   - skip:    drop it without firing (status 'skipped').
 * (2026-07-05 audit: a separate `forceBeat` action/button used to exist here, but it called this
 * SAME function with the SAME "approved" status — a cosmetic duplicate, not a distinct capability.
 * Removed rather than relabeled, since two buttons doing one thing is confusing either way.)
 */
async function decideBeat(
  id: number,
  status: Extract<BeatStatus, "approved" | "fired" | "skipped">,
): Promise<ActionResult> {
  await assertAuthor();
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

export async function skipBeat(formData: FormData): Promise<ActionResult> {
  return decideBeat(Number(formData.get("id")), "skipped");
}

export async function approveBeatForm(formData: FormData): Promise<void> {
  await approveBeat(formData);
}

export async function skipBeatForm(formData: FormData): Promise<void> {
  await skipBeat(formData);
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
  await assertAuthor();
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

export async function updateWhisperBudgetForm(
  formData: FormData,
): Promise<void> {
  await updateWhisperBudget(formData);
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
  await assertAuthor();
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
  await assertAuthor();
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

// ---------------------------------------------------------------------------
// Ending selector — guarded manual fate override (testing the M5 colorants).
// ---------------------------------------------------------------------------

/**
 * Insert a beat_queue row of type 'override_ending_fate' carrying one of the four base fates
 * (A2 `divergent-fates`, WEB-MASTER §5/§8). This is the manual, FOR-TESTING path to force which
 * colorant the M5 composer opens with; like the Accepting trigger it lands as a PENDING beat so it
 * still flows through the same approve/force gate (the row is the request, NOT an immediate
 * arc_state.ending_fate write — the engine's resolver owns the set-once write, idempotent).
 *
 * The fate is re-validated server-side against the four-value enum (coerceFate); a value outside it
 * (a player name, "inheritors", junk) is rejected. INV-11/16: the fate is a GROUP enum and names no
 * player — there is nothing here that can elect a chosen one. The action never reads or writes a
 * per-player field.
 */
export async function overrideEndingFate(
  formData: FormData,
): Promise<ActionResult> {
  await assertAuthor();
  if (String(formData.get("confirm")) !== "FATE") {
    return { ok: false, error: "Confirmation phrase did not match." };
  }

  const fate = coerceFate(formData.get("fate"));
  if (fate === null) {
    return { ok: false, error: "Not a valid ending fate." };
  }

  const supabase = createAdminClient();

  const payload: Json = { manual: true, source: "dashboard", fate };
  const { error } = await supabase.from("beat_queue").insert({
    type: "override_ending_fate",
    target: null,
    payload,
    status: "pending",
  });

  if (error) return { ok: false, error: error.message };

  await supabase.from("event_log").insert({
    level: "warn",
    source: "dashboard",
    message: `Ending fate override "${fate}" was manually queued from the Author control surface.`,
  });

  refresh();
  return { ok: true };
}
