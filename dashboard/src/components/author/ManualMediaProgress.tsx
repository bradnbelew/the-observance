type FlagMap = Record<string, unknown>;

type ManualMediaProgressProps = {
  flags: FlagMap;
  hasHoldZip: boolean;
};

function truthy(v: unknown): boolean {
  return v === true || v === "true" || v === 1 || v === "1";
}

type MediaItem = {
  key: string;
  readyFlag?: string;
  readFlag?: string;
  title: string;
  detail: string;
  status: string;
};

const ITEMS: MediaItem[] = [
  {
    key: "resource-pack",
    title: "Resource pack",
    detail: "Hosted URL/SHA and all rehearsal clients loaded",
    status: "live check",
  },
  {
    key: "media_clip_01",
    readyFlag: "media_clip_01_ready",
    readFlag: "media_prior_base_read",
    title: "Clip 1",
    detail: "Prior base payload: ASH-13",
    status: "manual",
  },
  {
    key: "media_clip_02",
    readyFlag: "media_clip_02_ready",
    readFlag: "media_far_water_read",
    title: "Clip 2",
    detail: "Far water payload: where the reeds fold back",
    status: "manual",
  },
  {
    key: "media_clip_03",
    readyFlag: "media_clip_03_ready",
    readFlag: "media_black_moon_read",
    title: "Clip 3",
    detail: "Black moon payload: stay awake",
    status: "manual",
  },
  {
    key: "media_clip_04",
    readyFlag: "media_clip_04_ready",
    readFlag: "media_release_room_read",
    title: "Clip 4",
    detail: "Late payload: six return, one is not kept",
    status: "manual",
  },
  {
    key: "the-hold",
    title: "the-hold.zip",
    detail: "Record lure download",
    status: "file",
  },
  {
    key: "recovered-archive",
    title: "Recovered archive",
    detail: "Spectrogram payload: i was not kept",
    status: "flag",
  },
];

export function ManualMediaProgress({
  flags,
  hasHoldZip,
}: ManualMediaProgressProps) {
  const recoveredReady = truthy(flags.recovered_archive_ready);
  const seventhSuspected = truthy(flags.seventh_suspected);

  const stateFor = (key: string): { label: string; tone: string } => {
    if (key === "the-hold") {
      return hasHoldZip
        ? { label: "present", tone: "text-emerald-300" }
        : { label: "withheld", tone: "text-neutral-600" };
    }
    if (key === "recovered-archive") {
      if (recoveredReady && seventhSuspected) {
        return truthy(flags.media_spectrogram_read)
          ? { label: "read", tone: "text-emerald-300" }
          : { label: "armed", tone: "text-amber-300" };
      }
      if (recoveredReady) return { label: "ready, gated", tone: "text-amber-300" };
      return { label: "withheld", tone: "text-neutral-600" };
    }
    if (key.startsWith("media_clip_")) {
      const item = ITEMS.find((candidate) => candidate.key === key);
      const ready = truthy(flags[item?.readyFlag ?? ""]);
      if (truthy(flags[item?.readFlag ?? ""])) return { label: "read", tone: "text-emerald-300" };
      return ready
        ? { label: "ready, gated", tone: "text-amber-300" }
        : { label: "withheld", tone: "text-neutral-600" };
    }
    if (key === "resource-pack") {
      return { label: "verify live", tone: "text-amber-300" };
    }
    return { label: "unplanted", tone: "text-neutral-600" };
  };

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-mono text-lg text-neutral-100">
            Manual Media
          </h2>
          <p className="mt-1 text-sm text-neutral-400">
            external artifacts stay withheld until the real file or live proof exists
          </p>
        </div>
        <div className="font-mono text-xs text-neutral-500">
          no placeholder trails
        </div>
      </div>

      <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
        {ITEMS.map((item) => {
          const state = stateFor(item.key);
          return (
            <div
              key={item.key}
              className="rounded-md border border-neutral-800 bg-ash px-3 py-2"
            >
              <div className="flex items-center justify-between gap-3">
                <span className="text-sm text-neutral-300">{item.title}</span>
                <span className={`font-mono text-xs ${state.tone}`}>
                  {state.label}
                </span>
              </div>
              <p className="mt-1 text-xs text-neutral-500">{item.detail}</p>
            </div>
          );
        })}
      </div>
    </section>
  );
}
