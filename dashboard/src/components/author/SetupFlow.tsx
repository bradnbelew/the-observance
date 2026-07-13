const STEPS = [
  {
    label: "1",
    title: "Rehearsal Lab",
    body: "Prove mechanics and pacing in a disposable route before choosing final terrain.",
    commands: ["/obs director lab", "/obs preflight", "/obs rehearse start"],
  },
  {
    label: "2",
    title: "Proof Packets",
    body: "Create the placement worksheet and rehearsal packet before surveying real anchors.",
    commands: [
      "tools\\new_launch_placement_packet.ps1",
      "tools\\new_rehearsal_packet.ps1",
    ],
  },
  {
    label: "3",
    title: "Web Door + Pack",
    body: "Make the abandoned host row, pushed resource pack, and live database true before normal-player proof.",
    commands: [
      "tools\\check_hold_invitation.ps1",
      "tools\\set_resource_pack_config.ps1 -Url <hosted-https-zip-url>",
      "tools\\prepare_server_test.ps1 -ResourcePackUrl <hosted-https-zip-url> -Force",
      "discord\\supabase\\apply-all.sql",
    ],
  },
  {
    label: "4",
    title: "Encased Hold",
    body: "Run the surface-mouth descent into controlled stone, then prove the city shell, gates, ceilings, and player route before treating any clue as placed.",
    commands: [
      "/obs placehold build",
      "/obs placehold audit",
      "/obs placehold sync",
    ],
  },
  {
    label: "5",
    title: "Outside Anchors",
    body: "Place the launch anchors that intentionally remain outside the Hold.",
    commands: [
      "/obs site todo",
      "/obs site plan lanes",
      "/obs site next human",
      "/obs site set <siteId>",
      "/obs placeworld",
    ],
  },
  {
    label: "6",
    title: "Launch Proof",
    body: "Clear live proof: Hold audit, hosted pack, coordinates, Supabase, Unlit, rehearsal, and attestations.",
    commands: [
      "/obs placehold audit",
      "/obs preflight",
      "/obs unlit ready",
      "check_launch_manual_blockers.ps1 -Launch",
    ],
  },
];

function Command({ children }: { children: string }) {
  return (
    <code className="max-w-full break-all rounded border border-neutral-800 bg-black/35 px-2 py-1 font-mono text-[11px] text-neutral-300">
      {children}
    </code>
  );
}

export function SetupFlow() {
  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-mono text-xs uppercase text-neutral-500">
            Setup flow
          </p>
          <h2 className="mt-1 font-mono text-lg text-neutral-100">
            Host, build, then prove
          </h2>
        </div>
        <div className="font-mono text-xs text-neutral-500">
          web door first, world second
        </div>
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-6">
        {STEPS.map((step) => (
          <div
            key={step.label}
            className="rounded-md border border-neutral-800 bg-ash p-3"
          >
            <div className="flex items-center gap-2">
              <span className="grid h-6 w-6 place-items-center rounded border border-neutral-700 font-mono text-xs text-neutral-300">
                {step.label}
              </span>
              <h3 className="font-mono text-sm text-neutral-100">
                {step.title}
              </h3>
            </div>
            <p className="mt-2 text-sm text-neutral-400">{step.body}</p>
            <div className="mt-3 flex flex-wrap gap-1.5">
              {step.commands.map((command) => (
                <Command key={command}>{command}</Command>
              ))}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
