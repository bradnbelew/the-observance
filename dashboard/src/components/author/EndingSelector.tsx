"use client";

import { useMemo, useState, useTransition } from "react";
import { previewFate, type FateInput, type EndingFate } from "@/app/author/fate-preview";
import { overrideEndingFate } from "@/app/author/actions";

/**
 * EndingSelector — the divergent-ending director surface (A2 `divergent-fates`, WEB-MASTER §5/§8).
 *
 * THE GAP THIS CLOSES. The Accepting fires ONE opaque bow token; the engine's fate-sentinel branch
 * (resolve.ts) reads the measured arc and sets `arc_state.ending_fate` set-once via the pure
 * `decideFate` policy (discord/src/showrunner/fate.ts — the SINGLE source of truth). For an unspoiled
 * director, this surface (a) PREVIEWS which of the five colorants the live arc would resolve to
 * RIGHT NOW (running the identical pure policy on the same active-only inputs, so the preview never
 * lies relative to the engine), and (b) offers a guarded MANUAL OVERRIDE through the approval gate
 * for testing each ending — never an auto-write.
 *
 * THE LAWS THIS HONORS:
 *   - INV-11: the preview reads measured group tallies over ACTIVE players only, returns an enum,
 *     and NAMES NO PLAYER. The bond/Whisper tally is not an input (it is not a field on FateInput).
 *   - NEVER AUTO-ELECT A CHOSEN ONE: this is a group enum. The surface shows the four base fates +
 *     the INHERITORS codicil as group states; nothing here can derive WHICH active player is on the
 *     honored vs. violated side (INV-16). DIVIDED is a floor split by geometry, never by player.
 *   - NEVER PUNISH AN ABSENT MEMBER: the active-roster size is shown so the director can confirm the
 *     fate was computed over active players only (the counts the page passes are active-only).
 *   - The override lands as a PENDING beat through the same approval gate as everything else; the
 *     server action re-checks isAdmin and re-validates the fate enum. The preview is never a write.
 *
 * The pure preview logic is a verbatim mirror of `decideFate` (fate-preview.ts, self-tested), kept
 * client-local so the surface is fully self-contained and renders with no round-trip.
 */

const FATES: ReadonlyArray<{
  fate: EndingFate;
  label: string;
  blurb: string;
}> = [
  {
    fate: "kept",
    label: "Kept",
    blurb: "honored dominates + (seventh or iss) + quorum. markers face out.",
  },
  {
    fate: "cast_out",
    label: "Cast out",
    blurb: "violated dominates + a real leaving. markers face away.",
  },
  {
    fate: "divided",
    label: "Divided",
    blurb: "a real spread, or an arc that earned neither pole. light holds on half the floor — by geometry, never by player.",
  },
  {
    fate: "refusers",
    label: "Refusers",
    blurb: "secret. quorum present + a positive defiance signal + the bow window empty. never read from slowness.",
  },
];

/**
 * The active-only, measured inputs the page reads off live state and passes in. Every count is over
 * ACTIVE players (INV-11). The codicil is independent of the base fate (the Seventh restore act).
 */
export interface EndingSelectorProps {
  input: FateInput;
  /** the live active-roster size (the window the engine reads — shown so active-only is visible). */
  activeRosterSize: number;
  /** the INHERITORS codicil — the Seventh restore/deposit act (a boolean, set independently). */
  codicil: boolean;
  /** the already-resolved fate, if the Accepting has set it (set-once). null = not yet resolved. */
  resolved: EndingFate | null;
  /** the resolved Seventh choice tint, if any (colors one clause; never gates). */
  seventhChoice: "restore" | "erase" | null;
}

function FateChip({
  label,
  active,
}: {
  label: string;
  active: boolean;
}) {
  return (
    <span
      className={`rounded-full px-2 py-0.5 font-mono text-xs ${
        active
          ? "bg-neutral-200/15 text-neutral-100"
          : "bg-neutral-800/60 text-neutral-500"
      }`}
    >
      {label}
    </span>
  );
}

export function EndingSelector({
  input,
  activeRosterSize,
  codicil,
  resolved,
  seventhChoice,
}: EndingSelectorProps) {
  const [override, setOverride] = useState<EndingFate | "">("");
  const [confirm, setConfirm] = useState("");
  const [result, setResult] = useState<{ ok: boolean; msg: string } | null>(
    null,
  );
  const [pending, startTransition] = useTransition();

  // The live preview — the IDENTICAL pure policy the engine runs, on the same active-only inputs.
  // This is a read; it never writes. It tells the unspoiled director where the arc would land now.
  const preview = useMemo(() => previewFate(input), [input]);

  const CONFIRM_PHRASE = "FATE";
  const armed = override !== "" && confirm === CONFIRM_PHRASE;

  function fire() {
    if (!armed) return;
    setResult(null);
    const formData = new FormData();
    formData.set("fate", override);
    formData.set("confirm", confirm);
    startTransition(async () => {
      const res = await overrideEndingFate(formData);
      if (res.ok) {
        setResult({
          ok: true,
          msg: `Override "${override}" queued as a pending beat through the approval gate.`,
        });
        setOverride("");
        setConfirm("");
      } else {
        setResult({ ok: false, msg: res.error ?? "Failed to queue." });
      }
    });
  }

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex items-center justify-between">
        <h2 className="font-mono text-lg text-neutral-100">Ending selector</h2>
        <span className="font-mono text-xs text-neutral-500">
          active-only · group enum
        </span>
      </div>
      <p className="mt-1 max-w-prose text-sm text-neutral-400">
        Preview which of the five colorants the live arc resolves to, and queue a
        manual override for testing. The fate is a <em>group</em> state — it
        names no player and never elects a chosen one (INV-11/16). Every count is
        over <span className="font-mono">{activeRosterSize}</span> active{" "}
        {activeRosterSize === 1 ? "player" : "players"} only.
      </p>

      {/* The live preview — the same pure policy the engine's fate sentinel runs. */}
      <div className="mt-4 rounded-md border border-neutral-800 bg-ash/40 px-4 py-3">
        <div className="flex items-center justify-between">
          <span className="text-xs uppercase tracking-wide text-neutral-500">
            {resolved ? "Resolved fate" : "Would resolve to (live preview)"}
          </span>
          <FateChip
            label={(resolved ?? preview.fate).replace("_", " ")}
            active
          />
        </div>
        <p className="mt-2 font-mono text-xs text-neutral-500">
          {resolved
            ? "the accepting has resolved; this is set-once."
            : preview.reason}
        </p>

        <div className="mt-3 flex flex-wrap items-center gap-2 border-t border-neutral-800/70 pt-3">
          <span className="text-xs text-neutral-500">colorants:</span>
          <FateChip label="inheritors" active={codicil} />
          <FateChip
            label={
              seventhChoice ? `seventh: ${seventhChoice}` : "seventh: —"
            }
            active={seventhChoice !== null}
          />
        </div>

        {/* The active-only spread the selector read — visible so the director can audit INV-11. */}
        <dl className="mt-3 grid grid-cols-3 gap-2 border-t border-neutral-800/70 pt-3 text-xs text-neutral-500">
          <div>
            <dt>honored</dt>
            <dd className="font-mono text-neutral-300">{input.honoredActive}</dd>
          </div>
          <div>
            <dt>violated</dt>
            <dd className="font-mono text-neutral-300">{input.violatedActive}</dd>
          </div>
          <div>
            <dt>left at</dt>
            <dd className="font-mono text-neutral-300">{input.leftAtActive}</dd>
          </div>
          <div>
            <dt>seventh</dt>
            <dd className="font-mono text-neutral-300">
              {input.seventhFound ? "found" : "—"}
            </dd>
          </div>
          <div>
            <dt>iss</dt>
            <dd className="font-mono text-neutral-300">
              {input.issCaught ? "caught" : "—"}
            </dd>
          </div>
          <div>
            <dt>quorum</dt>
            <dd className="font-mono text-neutral-300">
              {input.quorumMet ? "met" : "—"}
            </dd>
          </div>
        </dl>
      </div>

      {/* The guarded manual override — for testing each ending; lands as a pending beat. */}
      <div className="mt-5 rounded-md border border-amber-900/40 bg-amber-950/10 p-4">
        <div className="flex items-center justify-between">
          <h3 className="font-mono text-sm text-amber-200">
            Manual override (testing)
          </h3>
          <span className="font-mono text-xs text-amber-400/70">
            through the gate
          </span>
        </div>
        <p className="mt-1 max-w-prose text-xs text-neutral-400">
          Force a fate for testing the M5 composer. It lands as a{" "}
          <span className="font-mono">pending</span> beat and still flows through
          the approval gate; the server re-validates the enum. Pick a fate and
          type <span className="font-mono text-amber-300">{CONFIRM_PHRASE}</span>{" "}
          to arm.
        </p>

        <div className="mt-3 flex flex-wrap items-center gap-2">
          <select
            value={override}
            onChange={(e) => setOverride(e.target.value as EndingFate | "")}
            aria-label="Override fate"
            className="rounded-md border border-neutral-800 bg-ash px-3 py-2 font-mono text-sm text-neutral-100 outline-none focus:border-amber-700"
          >
            <option value="">— select a fate —</option>
            {FATES.map((f) => (
              <option key={f.fate} value={f.fate}>
                {f.label}
              </option>
            ))}
          </select>
          <input
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            placeholder={CONFIRM_PHRASE}
            aria-label="Confirmation phrase"
            className="w-32 rounded-md border border-neutral-800 bg-ash px-3 py-2 font-mono text-sm text-neutral-100 outline-none focus:border-amber-700"
          />
          <button
            type="button"
            onClick={fire}
            disabled={!armed || pending}
            className="rounded-md border border-amber-700 bg-amber-900/30 px-3 py-2 text-sm text-amber-100 transition-colors hover:bg-amber-900/50 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {pending ? "Queuing…" : "Queue override"}
          </button>
        </div>

        {override ? (
          <p className="mt-2 font-mono text-xs text-neutral-500">
            {FATES.find((f) => f.fate === override)?.blurb}
          </p>
        ) : null}

        {result ? (
          <p
            className={`mt-2 text-sm ${
              result.ok ? "text-emerald-400" : "text-red-400"
            }`}
          >
            {result.msg}
          </p>
        ) : null}
      </div>
    </section>
  );
}
