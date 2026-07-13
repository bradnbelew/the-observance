type DirectorRunPanelProps = {
  watcherAsleep: boolean;
  pendingBeats: number;
  approvedBeats: number;
  failedBeats: number;
};

const COMMAND_GROUPS = [
  {
    title: "Server + Web",
    commands: [
      "/obs status",
      "tools\\check_hold_invitation.ps1",
      "tools\\set_resource_pack_config.ps1 -Url <hosted-https-zip-url>",
      "tools\\prepare_server_test.ps1 -ResourcePackUrl <hosted-https-zip-url> -Force",
    ],
  },
  {
    title: "Production Hold",
    commands: [
      "/obs placehold build",
      "/obs placehold audit",
      "/obs placehold sync",
    ],
  },
  {
    title: "Placement Proof",
    commands: [
      "/obs director world",
      "/obs site todo",
      "/obs site plan lanes",
      "/obs site next human",
      "/obs site set <siteId>",
      "/obs placeworld",
      "/obs visit next",
      "/obs coverage",
    ],
  },
  {
    title: "Rehearsal + Audits",
    commands: [
      "/obs director lab",
      "rehearsal only: /obs prepworld",
      "/obs rehearse start",
      "/obs puzzlepass",
      "/obs puzzlepass gates",
      "/obs sidepass",
      "/obs dreadpass run",
      "/obs runbook spine",
      "/obs audit",
      "/obs repair",
      "/obs test gauntlet",
    ],
  },
];

function CountTile({
  label,
  value,
  tone,
}: {
  label: string;
  value: string | number;
  tone: "neutral" | "amber" | "sky" | "red";
}) {
  const toneClass = {
    neutral: "border-neutral-700 bg-neutral-900/40 text-neutral-100",
    amber: "border-amber-500/30 bg-amber-500/10 text-amber-100",
    sky: "border-sky-500/30 bg-sky-500/10 text-sky-100",
    red: "border-red-500/30 bg-red-500/10 text-red-100",
  }[tone];

  return (
    <div className={`rounded-md border px-3 py-2 ${toneClass}`}>
      <p className="font-mono text-[11px] uppercase opacity-70">
        {label}
      </p>
      <p className="mt-1 font-mono text-xl">{value}</p>
    </div>
  );
}

function CommandLine({ children }: { children: string }) {
  return (
    <code className="block rounded-md border border-neutral-800 bg-black/35 px-3 py-2 font-mono text-xs text-neutral-200">
      {children}
    </code>
  );
}

export function DirectorRunPanel({
  watcherAsleep,
  pendingBeats,
  approvedBeats,
  failedBeats,
}: DirectorRunPanelProps) {
  const mode = watcherAsleep ? "Manual" : "Auto";

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(280px,420px)]">
        <div>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-mono text-xs uppercase text-neutral-500">
                Director run
              </p>
              <h2 className="mt-1 font-mono text-lg text-neutral-100">
                Live test pass
              </h2>
            </div>
            <span className="rounded-full border border-neutral-700 bg-neutral-900 px-3 py-1 font-mono text-xs uppercase text-neutral-300">
              {mode}
            </span>
          </div>

          <div className="mt-4 grid gap-2 sm:grid-cols-4">
            <CountTile label="Mode" value={mode} tone="neutral" />
            <CountTile label="Approvals" value={pendingBeats} tone="amber" />
            <CountTile label="Armed" value={approvedBeats} tone="sky" />
            <CountTile label="Failed" value={failedBeats} tone="red" />
          </div>
        </div>

        <div className="space-y-3">
          <p className="font-mono text-xs uppercase text-neutral-500">
            Run order
          </p>
          <p className="text-sm text-neutral-400">
            Copperline service row, hosted pack, and Supabase proof come before
            non-op Hold testing.
          </p>
          <div className="grid gap-3">
            {COMMAND_GROUPS.map((group) => (
              <div
                key={group.title}
                className="rounded-md border border-neutral-800 bg-black/20 p-3"
              >
                <h3 className="font-mono text-xs uppercase text-neutral-500">
                  {group.title}
                </h3>
                <div className="mt-2 space-y-2">
                  {group.commands.map((command) => (
                    <CommandLine key={command}>{command}</CommandLine>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
