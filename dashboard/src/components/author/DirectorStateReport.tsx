import type { Beat, CustomCompliance, Player } from "@/lib/database.types";
import { SIDE_PROOF_FLAGS } from "./proofFlags";

type DirectorStateReportProps = {
  currentAct: number;
  flags: Record<string, unknown>;
  players: Player[];
  compliance: CustomCompliance[];
  beats: Beat[];
  watcherAsleep: boolean;
  activeRosterSize: number;
  hasHoldZip: boolean;
  serverAddressConfigured: boolean;
};

const THEORY_FLAGS = [
  ["vaun_theory", "Vaun"],
  ["mara_theory", "Mara"],
  ["sella_theory", "Sella"],
  ["orin_theory", "Orin"],
  ["brann_theory", "Brann"],
  ["iss_theory", "Iss"],
] as const;

const MEDIA_FLAGS = [
  ["media_clip_01_ready", "base footage"],
  ["media_clip_02_ready", "far-water footage"],
  ["media_clip_03_ready", "black-moon footage"],
  ["media_clip_04_ready", "release-room footage"],
  ["recovered_archive_ready", "recovered archive"],
] as const;

const PRIOR_FLAGS = [
  ["prior_absence_known", "absence"],
  ["prior_camp_read", "camp"],
  ["prior_vaun_corrected", "Vaun"],
  ["prior_mara_corrected", "Mara"],
  ["prior_sella_corrected", "Sella"],
  ["prior_orin_corrected", "Orin"],
  ["prior_brann_corrected", "Brann"],
  ["prior_iss_corrected", "Iss"],
  ["prior_witness_ready", "witness"],
] as const;

function isTrue(flags: Record<string, unknown>, key: string) {
  return flags[key] === true;
}

function StatusPill({
  children,
  tone,
}: {
  children: string;
  tone: "good" | "warn" | "bad" | "neutral";
}) {
  const styles = {
    good: "border-emerald-500/30 bg-emerald-500/10 text-emerald-100",
    warn: "border-amber-500/30 bg-amber-500/10 text-amber-100",
    bad: "border-red-500/30 bg-red-500/10 text-red-100",
    neutral: "border-neutral-700 bg-neutral-900/50 text-neutral-300",
  }[tone];

  return (
    <span className={`rounded-full border px-2 py-0.5 font-mono text-[11px] uppercase ${styles}`}>
      {children}
    </span>
  );
}

function CommandLine({ children }: { children: string }) {
  return (
    <code className="block max-w-full break-all rounded-md border border-neutral-800 bg-black/35 px-3 py-2 font-mono text-xs text-neutral-200">
      {children}
    </code>
  );
}

function buildOpenLeads(flags: Record<string, unknown>) {
  const theoryCount = THEORY_FLAGS.filter(([key]) => isTrue(flags, key)).length;
  const priorCount = PRIOR_FLAGS.filter(([key]) => isTrue(flags, key)).length;
  const leads: string[] = [];

  if (!isTrue(flags, "rosetta_known")) {
    leads.push("Cold open and Rosetta: players still need the seven-way literacy turn.");
  }
  if (isTrue(flags, "rosetta_known") && theoryCount < THEORY_FLAGS.length) {
    leads.push("Keeper investigations: evidence clusters are still open; do not reduce them to stone solves.");
  }
  if (theoryCount >= 3 && !isTrue(flags, "iss_caught")) {
    leads.push("Liar thread: enough context may exist to let Iss contradict himself.");
  }
  if (isTrue(flags, "iss_caught") && !isTrue(flags, "seventh_suspected")) {
    leads.push("Seventh suspicion: point the group back through Sella/far-water evidence.");
  }
  if (isTrue(flags, "seventh_suspected") && !isTrue(flags, "seventh_named")) {
    leads.push("Seventh name: the group should be comparing keeper fragments, not hunting a new random code.");
  }
  if (isTrue(flags, "seventh_named") && !isTrue(flags, "accepting_onramp_open")) {
    leads.push("Threshold and Unlit: open the late route only after the prior evidence has landed.");
  }
  if (isTrue(flags, "undercroft_open") && !isTrue(flags, "nether_forge_placed")) {
    leads.push("Nether forge: the story has earned the deep fire lane, but the physical forge is not placed yet.");
  }
  if (isTrue(flags, "seventh_named") && !isTrue(flags, "end_seventh_shrine_placed")) {
    leads.push("End shrine: the Seventh way-out can be pursued only after the End site is surveyed and stamped.");
  }
  if (isTrue(flags, "nether_forge_placed") && !isTrue(flags, "nether_forge_found")) {
    leads.push("Nether payoff: the forge is live; the group can still close the lent-fire receipt.");
  }
  if (isTrue(flags, "end_seventh_shrine_placed") && !isTrue(flags, "seventh_seen_out")) {
    leads.push("End payoff: the shrine is live; the out-of-record Seventh read is still unclaimed.");
  }
  if (theoryCount === THEORY_FLAGS.length && !isTrue(flags, "prior_absence_known")) {
    leads.push("Failed Accepting: the roster before the camp gate should make the missing condition answerable.");
  }
  if (isTrue(flags, "prior_absence_known") && !isTrue(flags, "prior_camp_read")) {
    leads.push("Prior camp: read the failed record and distinguish solved answers from witness.");
  }
  if (isTrue(flags, "prior_camp_read") && priorCount < PRIOR_FLAGS.length) {
    leads.push("Prior corrections: six repair files can be worked in parallel from camp barrels plus keeper/side evidence.");
  }
  if (isTrue(flags, "accepting_onramp_open") && !isTrue(flags, "prior_witness_ready")) {
    leads.push("Accepting is blocked by the failed-run witness condition; do not let token placement become a shortcut.");
  }
  if (isTrue(flags, "accepting_onramp_open") && isTrue(flags, "prior_witness_ready") && !isTrue(flags, "tokens_laid")) {
    leads.push("Accepting on-ramp: token work is open; keep it physical and group-owned.");
  }
  if (isTrue(flags, "tokens_laid") && !isTrue(flags, "bowed_as_one")) {
    leads.push("Finale readiness: synchronized bow is the live gate, not a typed phrase.");
  }
  if (isTrue(flags, "bowed_as_one")) {
    leads.push("Release: ending delivery and post-run receipts are the active concern.");
  }

  return leads.length > 0 ? leads : ["No live flag has opened a clear player lead yet; start with the cold-open proof route."];
}

function buildNextCommand({
  watcherAsleep,
  pendingBeats,
  failedBeats,
  theoryCount,
  priorCount,
  hasHoldZip,
  serverAddressConfigured,
  flags,
}: {
  watcherAsleep: boolean;
  pendingBeats: number;
  failedBeats: number;
  theoryCount: number;
  priorCount: number;
  hasHoldZip: boolean;
  serverAddressConfigured: boolean;
  flags: Record<string, unknown>;
}) {
  if (!hasHoldZip) return "tools\\rebuild_hold_invitation.ps1";
  if (!serverAddressConfigured) return "set NEXT_PUBLIC_OBSERVANCE_SERVER_ADDRESS before planting record-url";
  if (failedBeats > 0) return "/obs status, then inspect failed beat payloads";
  if (pendingBeats > 0) return "review pending beats in this dashboard";
  if (watcherAsleep) return "/obs sleep off when the rehearsal is ready";
  if (theoryCount < THEORY_FLAGS.length) return "/obs site todo, then /obs visit next";
  if (priorCount < PRIOR_FLAGS.length) return "/obs placehold sync, then audit prior_camp and failed_accepting";
  if (isTrue(flags, "undercroft_open") && !isTrue(flags, "nether_forge_placed")) {
    return "/obs site set nether_forge in the Nether, then /obs placeworld";
  }
  if (isTrue(flags, "seventh_named") && !isTrue(flags, "end_seventh_shrine_placed")) {
    return "/obs site set end_seventh_shrine in the End, then /obs placeworld";
  }
  return "/obs preflight, then /obs unlit ready";
}

export function DirectorStateReport({
  currentAct,
  flags,
  players,
  compliance,
  beats,
  watcherAsleep,
  activeRosterSize,
  hasHoldZip,
  serverAddressConfigured,
}: DirectorStateReportProps) {
  const theoryCount = THEORY_FLAGS.filter(([key]) => isTrue(flags, key)).length;
  const priorCount = PRIOR_FLAGS.filter(([key]) => isTrue(flags, key)).length;
  const mediaReady = MEDIA_FLAGS.filter(([key]) => isTrue(flags, key)).length;
  const sideProofs = SIDE_PROOF_FLAGS.filter((proof) => isTrue(flags, proof.key)).length;
  const pendingBeats = beats.filter((beat) => beat.status === "pending").length;
  const failedBeats = beats.filter((beat) => beat.status === "failed").length;
  const openLeads = buildOpenLeads(flags);
  const nextCommand = buildNextCommand({
    watcherAsleep,
    pendingBeats,
    failedBeats,
    theoryCount,
    priorCount,
    hasHoldZip,
    serverAddressConfigured,
    flags,
  });

  const playerById = new Map(players.map((player) => [player.id, player]));
  const hottest = compliance
    .filter((row) => row.violation_count > 0)
    .slice()
    .sort((a, b) => b.violation_count - a.violation_count)
    .slice(0, 5);

  const risks = [
    !hasHoldZip ? "Hold zip is missing from the deployed public path." : null,
    !serverAddressConfigured ? "Public listing is still withholding the live server address." : null,
    watcherAsleep ? "Watcher is asleep; automatic run beats are muted." : null,
    pendingBeats > 0 ? `${pendingBeats} beat approval(s) waiting.` : null,
    failedBeats > 0 ? `${failedBeats} beat failure(s) need inspection.` : null,
    activeRosterSize === 0 ? "No active roster detected in the current activity window." : null,
  ].filter(Boolean) as string[];

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-mono text-xs uppercase text-neutral-500">
            Director state
          </p>
          <h2 className="mt-1 font-mono text-lg text-neutral-100">
            Open leads and run risk
          </h2>
        </div>
        <StatusPill tone={risks.length > 0 ? "warn" : "good"}>
          {risks.length > 0 ? "watch" : "clear"}
        </StatusPill>
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-5">
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <p className="font-mono text-[11px] uppercase text-neutral-500">Act</p>
          <p className="mt-1 font-mono text-2xl text-neutral-100">{currentAct}</p>
        </div>
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <p className="font-mono text-[11px] uppercase text-neutral-500">Roster</p>
          <p className="mt-1 font-mono text-2xl text-neutral-100">{activeRosterSize}/{players.length}</p>
        </div>
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <p className="font-mono text-[11px] uppercase text-neutral-500">Theories</p>
          <p className="mt-1 font-mono text-2xl text-neutral-100">{theoryCount}/6</p>
        </div>
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <p className="font-mono text-[11px] uppercase text-neutral-500">Prior</p>
          <p className="mt-1 font-mono text-2xl text-neutral-100">{priorCount}/{PRIOR_FLAGS.length}</p>
        </div>
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <p className="font-mono text-[11px] uppercase text-neutral-500">Side/media</p>
          <p className="mt-1 font-mono text-2xl text-neutral-100">{sideProofs}/{SIDE_PROOF_FLAGS.length} / {mediaReady}/{MEDIA_FLAGS.length}</p>
        </div>
      </div>

      <div className="mt-4 rounded-md border border-neutral-800 bg-black/20 p-4">
        <h3 className="font-mono text-sm text-neutral-100">Opening web</h3>
        <div className="mt-3 flex flex-wrap gap-2">
          <StatusPill tone={hasHoldZip ? "good" : "warn"}>
            {hasHoldZip ? "the-hold.zip present" : "the-hold.zip missing"}
          </StatusPill>
          <StatusPill tone={serverAddressConfigured ? "good" : "warn"}>
            {serverAddressConfigured ? "server address listed" : "server address withheld"}
          </StatusPill>
        </div>
        <p className="mt-3 text-sm text-neutral-400">
          Plant the Record URL only after the deployed public root works in a private browser,
          the recovered file is downloadable, and the listing shows the live address intentionally.
        </p>
      </div>

      <div className="mt-4 rounded-md border border-neutral-800 bg-black/20 p-4">
        <h3 className="font-mono text-sm text-neutral-100">Production placement</h3>
        <div className="mt-3 grid gap-2 md:grid-cols-3">
          <CommandLine>/obs placehold build</CommandLine>
          <CommandLine>/obs placehold audit</CommandLine>
          <CommandLine>/obs placehold sync</CommandLine>
        </div>
        <p className="mt-3 text-sm text-neutral-400">
          Use the Deep Hold for the clustered underground city. Use site placement for the
          remaining surface, Nether, End, Unlit, media, and bespoke anchors; keep prepworld as
          a disposable rehearsal board.
        </p>
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-[minmax(0,1.25fr)_minmax(280px,0.75fr)]">
        <div className="rounded-md border border-neutral-800 bg-black/20 p-4">
          <h3 className="font-mono text-sm text-neutral-100">Open player leads</h3>
          <ul className="mt-3 space-y-2 text-sm text-neutral-300">
            {openLeads.map((lead) => (
              <li key={lead}>{lead}</li>
            ))}
          </ul>
        </div>

        <div className="space-y-3">
          <div className="rounded-md border border-neutral-800 bg-black/20 p-4">
            <h3 className="font-mono text-sm text-neutral-100">Next operator move</h3>
            <div className="mt-3">
              <CommandLine>{nextCommand}</CommandLine>
            </div>
          </div>

          <div className="rounded-md border border-neutral-800 bg-black/20 p-4">
            <h3 className="font-mono text-sm text-neutral-100">Risk lights</h3>
            <div className="mt-3 flex flex-wrap gap-2">
              {risks.length === 0 ? (
                <StatusPill tone="good">no immediate run risks</StatusPill>
              ) : (
                risks.map((risk) => (
                  <StatusPill key={risk} tone={risk.includes("failure") ? "bad" : "warn"}>
                    {risk}
                  </StatusPill>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="mt-4 rounded-md border border-neutral-800 bg-black/20 p-4">
        <h3 className="font-mono text-sm text-neutral-100">Customs heat</h3>
        {hottest.length === 0 ? (
          <p className="mt-2 text-sm text-neutral-400">
            No standing custom violations are visible in the dashboard views.
          </p>
        ) : (
          <div className="mt-3 grid gap-2 md:grid-cols-2 xl:grid-cols-3">
            {hottest.map((row) => {
              const name = row.player_id ? playerById.get(row.player_id)?.name : null;
              return (
                <div
                  key={`${row.player_id ?? "unknown"}-${row.custom_key}-${row.id}`}
                  className="rounded border border-neutral-800 bg-ash px-3 py-2"
                >
                  <p className="font-mono text-xs text-neutral-300">
                    {name ?? "unknown player"}
                  </p>
                  <p className="mt-1 text-sm text-neutral-400">
                    {row.custom_key} / {row.violation_count}x / {row.status}
                  </p>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
