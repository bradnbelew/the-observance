import { existsSync, statSync } from "node:fs";
import { join } from "node:path";
import Image from "next/image";
import Link from "next/link";

const HOLD_ZIP_PUBLIC_PATH = "/the-hold/the-hold.zip";

function holdZipInfo(): { available: boolean; sizeLabel: string } {
  const path = join(process.cwd(), "public", "the-hold", "the-hold.zip");
  if (!existsSync(path)) return { available: false, sizeLabel: "not recovered" };
  const bytes = statSync(path).size;
  const kib = Math.max(1, Math.round(bytes / 1024));
  return { available: true, sizeLabel: `${kib} KiB` };
}

function serverAddress(): string {
  const raw = process.env.NEXT_PUBLIC_OBSERVANCE_SERVER_ADDRESS?.trim();
  return raw && raw.length > 0 ? raw : "address withheld until the host is awake";
}

const archivedRows = [
  ["server", "The Observance", "unlisted"],
  ["version", "Java 1.21.11", "last compatible"],
  ["players", "0 / 7", "whitelist residue"],
  ["motd", "count first. walk low.", "unchanged"],
  ["world", "hold_recovered_03", "restored"],
  ["pack", "required", "runes"],
  ["staff", "no staff listed", "removed"],
];

const mirrorRows = [
  ["2009-10-31", "first report copied to mirror 03", "kept"],
  ["2011-02-06", "map file replaced after complaint", "kept"],
  ["2014-07-19", "staff list removed from public row", "kept"],
  ["2020-11-02", "world file restored from cold backup", "partial"],
  ["unknown", "seventh row erased from host cache", "not kept"],
];

const hostRows = [
  ["front door", "SNOIKERZ"],
  ["ending", "common web"],
  ["path", "/"],
  ["mirror", "03"],
];

const dnsRows = [
  ["host", "snoikerz", "front door"],
  ["web", "common", "root"],
  ["path", "/", "indexed"],
  ["mirror", "03", "last kept"],
  ["_minecraft", "removed", "staff access"],
  ["txt", "record kept in more than one place", "founder residue"],
];

const bulletinRows = [
  ["notice", "returning players should use the listed address, not the map copy."],
  ["claim", "map re-uploaded by m.kept after staff access was closed."],
  ["rule", "do not force shut rooms. report missing signs to no one."],
  ["billing", "free mirror account retained because the row still receives checks."],
];

const propertyRows = [
  ["online-mode", "true"],
  ["enforce-whitelist", "true"],
  ["max-players", "7"],
  ["spawn-protection", "16"],
  ["resource-pack-prompt", "required for alphabet"],
  ["view-distance", "8"],
];

const planRows = [
  ["host plan", "legacy free"],
  ["panel", "classic"],
  ["region", "us-central"],
  ["daily restart", "off"],
  ["backup slot", "mirror_03"],
  ["last invoice", "waived"],
];

const panelRows = [
  ["session", "expired"],
  ["last login", "m.kept / 2020-11-02"],
  ["console", "disabled on free plan"],
  ["address row", "public listing only"],
  ["restart", "queued, never acknowledged"],
  ["operator tab", "no verified staff"],
];

const billingRows = [
  ["2014-07-20", "staff removal request", "approved"],
  ["2014-07-21", "whitelist restore", "partial"],
  ["2020-11-02", "cold backup pull", "manual"],
  ["2020-11-03", "invoice waived", "free mirror"],
  ["unknown", "blank whitelist row", "not billable"],
];

const abuseRows = [
  ["#77", "players report a hidden address in the map", "closed: not in file"],
  ["#81", "guest asks for staff.txt restore", "denied"],
  ["#88", "server accepts pack but no staff answers", "left stale"],
  ["#91", "blank whitelist row counts toward seven", "locked"],
];

const fileRows = [
  ["the-hold.zip", "offline world", "recovered"],
  ["server-icon.png", "six-mark ring", "missing"],
  ["observance-pack.zip", "join pack", "served by server"],
  ["staff.txt", "operator names", "removed"],
  ["whitelist.txt", "seven rows", "locked"],
];

const ticketRows = [
  ["#1842", "player says black wall will not break", "closed: intentional"],
  ["#1848", "book opens but no one agrees what page matters", "closed: read aloud"],
  ["#1850", "third lamp marked ready without a lamp", "left open"],
  ["#1851", "seventh whitelist row has no matching name", "locked"],
];

const uptimeRows = [
  ["03:00", "timeout", "0 / 7"],
  ["09:00", "timeout", "0 / 7"],
  ["15:00", "timeout", "0 / 7"],
  ["21:00", "timeout", "0 / 7"],
];

const joinRows = [
  ["1", "open java 1.21.11", "do not use bedrock"],
  ["2", "copy the address from this row only", "map files are not endpoints"],
  ["3", "accept the resource pack", "alphabet/runes"],
  ["4", "join on a whitelisted name", "seven rows counted"],
  ["5", "read signs before digging", "old staff rule"],
];

const packetRows = [
  ["srv", "_minecraft removed", "do not reconstruct a port"],
  ["a", "front door cached", "snoikerz"],
  ["txt", "record kept in more than one place", "founder line"],
  ["map", "mirror_03/the-hold.zip", "copy only"],
  ["pack", "observance-pack.zip", "served after join"],
];

const commentRows = [
  ["2011-02-07", "m.kept", "copy plays through. ending says the rest is kept here."],
  ["2011-02-08", "guest_1842", "black wall is not breakable. stop digging around it."],
  ["2014-07-20", "no staff", "staff file removed after the row was archived."],
  ["2020-11-02", "mirror", "restored file checksum differs only where signs were repaired."],
];

const whitelistRows = [
  ["vaun", "kept", "founder import"],
  ["mara", "kept", "file owner"],
  ["sella", "kept", "shore copy"],
  ["orin", "kept", "low door"],
  ["brann", "kept", "toll"],
  ["iss", "kept", "warm file"],
  ["", "withheld", "row exists"],
];

const moderationRows = [
  ["pin", "the map is a copy. the listing is the door."],
  ["pin", "do not paste the address into the map. it is stale there by design."],
  ["strike", "do not ask staff for the seventh row."],
  ["strike", "do not repair missing staff.txt from archive.org."],
];

const checksumRows = [
  ["hold_recovered_03.zip", "154181 bytes", "map copy"],
  ["manifest.txt", "six room names, one blank row", "kept"],
  ["level.dat", "last played by m.kept", "not staff"],
  ["region/r.-1.3.mca", "sign repairs only", "diff held"],
  ["resource pack", "served after join", "not bundled"],
];

const failedJoinRows = [
  ["2014-07-21 03:14", "guest_1842", "kicked: not whitelisted"],
  ["2014-07-21 03:16", "guest_1842", "kicked: missing pack"],
  ["2020-11-02 21:00", "mirror_check", "timeout"],
  ["2020-11-02 21:01", "mirror_check", "server row still resolves"],
  ["unknown", "blank row", "counted but no name returned"],
];

const maintenanceRows = [
  ["sign sweep", "do not rewrite low signs upward"],
  ["book sweep", "lecterns with no book are host errors, not clues"],
  ["lamp sweep", "ready mark is not the same as lit"],
  ["wall sweep", "black walls are not support tickets"],
  ["staff sweep", "removed names stay removed"],
];

export default function HomePage() {
  const zip = holdZipInfo();
  const address = serverAddress();

  return (
    <main className="min-h-screen bg-[#070809] px-4 py-6 text-neutral-400">
      <div className="mx-auto max-w-5xl">
        <header className="border-b border-neutral-900 pb-5">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div>
              <p className="font-mono text-[10px] uppercase text-neutral-700">
                archived server listing / snoikerz mirror 03
              </p>
              <h1 className="mt-2 font-mono text-3xl uppercase text-neutral-200">
                SNOIKERZ
              </h1>
              <p className="mt-2 max-w-xl font-mono text-xs lowercase leading-relaxed text-neutral-700">
                old java server host. free map mirrors, staff rows, and uptime checks. most outbound links are gone.
              </p>
            </div>
            <div className="border border-neutral-900 px-3 py-2 font-mono text-[11px] lowercase text-neutral-700">
              ping: no reply
              <br />
              last checked: unknown
            </div>
          </div>
        </header>

        <section className="grid gap-6 py-6 lg:grid-cols-[1fr_340px]">
          <div className="space-y-6">
            <section className="border border-neutral-900 bg-black/30">
              <div className="border-b border-neutral-900 px-4 py-2 font-mono text-[10px] uppercase text-neutral-600">
                hosted server row
              </div>
              <div className="px-4 py-5">
                <p className="font-mono text-sm leading-relaxed text-neutral-300">
                  the observance is listed here for returning players and for anyone who found the
                  offline copy. the map is not the server. the map only proves which row was moved.
                </p>
                <div className="mt-5 grid gap-2 border-y border-neutral-900 py-3 font-mono text-xs">
                  {archivedRows.map(([label, value, note]) => (
                    <div key={label} className="grid grid-cols-[82px_1fr_120px] gap-3">
                      <span className="text-neutral-700">{label}</span>
                      <span className="text-neutral-300">{value}</span>
                      <span className="text-right text-neutral-700">{note}</span>
                    </div>
                  ))}
                </div>
              </div>
            </section>

            <section className="border border-neutral-900 bg-black/30">
              <div className="border-b border-neutral-900 px-4 py-2 font-mono text-[10px] uppercase text-neutral-600">
                map mirror
              </div>
              <div className="px-4 py-5">
                <p className="font-mono text-xs lowercase leading-relaxed text-neutral-600">
                  offline copy. single player. no mods. the last room points back to this host because
                  the address was moved off the map after the staff list disappeared.
                </p>
                <div className="mt-4 flex flex-wrap items-center gap-x-5 gap-y-2 font-mono text-sm">
                  {zip.available ? (
                    <a
                      href={HOLD_ZIP_PUBLIC_PATH}
                      download
                      rel="noopener"
                      className="text-neutral-200 underline decoration-neutral-700 underline-offset-4 hover:text-white"
                    >
                      download the-hold.zip
                    </a>
                  ) : (
                    <span className="text-neutral-700 line-through decoration-neutral-800">
                      the-hold.zip
                    </span>
                  )}
                  <span className="text-xs lowercase text-neutral-700">{zip.sizeLabel}</span>
                </div>
              </div>
            </section>

            <section className="border border-neutral-900 bg-black/30">
              <div className="border-b border-neutral-900 px-4 py-2 font-mono text-[10px] uppercase text-neutral-600">
                live server address
              </div>
              <div className="px-4 py-5">
                <p className="font-mono text-xs lowercase leading-relaxed text-neutral-600">
                  copied from the last unstruck server row. if the row is withheld, the operator has
                  not woken the host.
                </p>
                <p className="mt-3 break-all border-y border-neutral-900 py-4 font-mono text-xl text-neutral-200">
                  {address}
                </p>
                <p className="mt-3 font-mono text-xs lowercase leading-relaxed text-neutral-700">
                  java edition. accept the pack or the carved alphabet will look wrong.
                </p>
              </div>
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">join packet</div>
              {joinRows.map(([step, action, note]) => (
                <div key={`${step}-${action}`} className="grid grid-cols-[36px_1fr_132px] gap-3 border-t border-neutral-900 pt-2">
                  <span className="text-neutral-700">{step}</span>
                  <span className="text-neutral-400">{action}</span>
                  <span className="text-right text-neutral-700">{note}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">address fragments</div>
              {hostRows.map(([label, value]) => (
                <div key={label} className="grid grid-cols-[92px_1fr] gap-3 border-t border-neutral-900 pt-2">
                  <span className="text-neutral-700">{label}</span>
                  <span className="text-neutral-400">{value}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">dns cache</div>
              {dnsRows.map(([label, value, note]) => (
                <div key={`${label}-${value}`} className="grid grid-cols-[92px_1fr_104px] gap-3 border-t border-neutral-900 pt-2">
                  <span className="text-neutral-700">{label}</span>
                  <span className="text-neutral-400">{value}</span>
                  <span className="text-right text-neutral-700">{note}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">packet trace</div>
              {packetRows.map(([kind, value, note]) => (
                <div key={`${kind}-${value}`} className="grid grid-cols-[72px_1fr_128px] gap-3 border-t border-neutral-900 pt-2">
                  <span className="text-neutral-700">{kind}</span>
                  <span className="text-neutral-400">{value}</span>
                  <span className="text-right text-neutral-700">{note}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">checksum ledger</div>
              {checksumRows.map(([name, value, note]) => (
                <div key={`${name}-${value}`} className="grid grid-cols-[132px_1fr_96px] gap-3 border-t border-neutral-900 pt-2">
                  <span className="text-neutral-700">{name}</span>
                  <span className="text-neutral-400">{value}</span>
                  <span className="text-right text-neutral-700">{note}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">mirror log</div>
              {mirrorRows.map(([date, event, state]) => (
                <div key={`${date}-${event}`} className="grid grid-cols-[92px_1fr_72px] gap-3 border-t border-neutral-900 pt-2">
                  <span className="text-neutral-700">{date}</span>
                  <span className="text-neutral-400">{event}</span>
                  <span className="text-right text-neutral-700">{state}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">failed join log</div>
              {failedJoinRows.map(([time, name, state]) => (
                <div key={`${time}-${name}`} className="grid gap-1 border-t border-neutral-900 pt-2 sm:grid-cols-[132px_96px_1fr] sm:gap-3">
                  <span className="text-neutral-700">{time}</span>
                  <span className={name === "blank row" ? "text-neutral-800" : "text-neutral-500"}>{name}</span>
                  <span className="text-neutral-400">{state}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">uptime checks</div>
              {uptimeRows.map(([time, state, players]) => (
                <div key={`${time}-${state}`} className="grid grid-cols-[72px_1fr_72px] gap-3 border-t border-neutral-900 pt-2">
                  <span className="text-neutral-700">{time}</span>
                  <span className="text-neutral-400">{state}</span>
                  <span className="text-right text-neutral-700">{players}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">maintenance notes</div>
              {maintenanceRows.map(([label, value]) => (
                <div key={label} className="grid gap-1 border-t border-neutral-900 pt-2 sm:grid-cols-[112px_1fr] sm:gap-3">
                  <span className="text-neutral-700">{label}</span>
                  <span className="text-neutral-400">{value}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-6 md:grid-cols-2">
              <div className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
                <div className="text-[10px] uppercase text-neutral-600">server.properties</div>
                {propertyRows.map(([label, value]) => (
                  <div key={label} className="grid grid-cols-[132px_1fr] gap-3 border-t border-neutral-900 pt-2">
                    <span className="text-neutral-700">{label}</span>
                    <span className="text-neutral-400">{value}</span>
                  </div>
                ))}
              </div>

              <div className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
                <div className="text-[10px] uppercase text-neutral-600">host account</div>
                {planRows.map(([label, value]) => (
                  <div key={label} className="grid grid-cols-[104px_1fr] gap-3 border-t border-neutral-900 pt-2">
                    <span className="text-neutral-700">{label}</span>
                    <span className="text-neutral-400">{value}</span>
                  </div>
                ))}
              </div>
            </section>

            <section className="grid gap-6 md:grid-cols-2">
              <div className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
                <div className="text-[10px] uppercase text-neutral-600">control panel residue</div>
                {panelRows.map(([label, value]) => (
                  <div key={label} className="grid grid-cols-[104px_1fr] gap-3 border-t border-neutral-900 pt-2">
                    <span className="text-neutral-700">{label}</span>
                    <span className="text-neutral-400">{value}</span>
                  </div>
                ))}
              </div>

              <div className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
                <div className="text-[10px] uppercase text-neutral-600">billing ledger</div>
                {billingRows.map(([date, action, state]) => (
                  <div key={`${date}-${action}`} className="grid grid-cols-[92px_1fr_72px] gap-3 border-t border-neutral-900 pt-2">
                    <span className="text-neutral-700">{date}</span>
                    <span className="text-neutral-400">{action}</span>
                    <span className="text-right text-neutral-700">{state}</span>
                  </div>
                ))}
              </div>
            </section>

            <section className="grid gap-6 md:grid-cols-2">
              <div className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
                <div className="text-[10px] uppercase text-neutral-600">public files</div>
                {fileRows.map(([name, purpose, state]) => (
                  <div key={name} className="grid grid-cols-[96px_1fr_72px] gap-3 border-t border-neutral-900 pt-2">
                    <span className="text-neutral-700">{name}</span>
                    <span className="text-neutral-400">{purpose}</span>
                    <span className="text-right text-neutral-700">{state}</span>
                  </div>
                ))}
              </div>
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">download comments</div>
              {commentRows.map(([date, user, body]) => (
                <div key={`${date}-${user}`} className="grid gap-1 border-t border-neutral-900 pt-2 sm:grid-cols-[92px_96px_1fr] sm:gap-3">
                  <span className="text-neutral-700">{date}</span>
                  <span className="text-neutral-500">{user}</span>
                  <span className="text-neutral-400">{body}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">support ticket cache</div>
              {ticketRows.map(([id, subject, state]) => (
                <div key={id} className="grid gap-1 border-t border-neutral-900 pt-2 sm:grid-cols-[72px_1fr_128px] sm:gap-3">
                  <span className="text-neutral-700">{id}</span>
                  <span className="text-neutral-400">{subject}</span>
                  <span className="text-neutral-700 sm:text-right">{state}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">abuse queue</div>
              {abuseRows.map(([id, subject, state]) => (
                <div key={id} className="grid gap-1 border-t border-neutral-900 pt-2 sm:grid-cols-[72px_1fr_128px] sm:gap-3">
                  <span className="text-neutral-700">{id}</span>
                  <span className="text-neutral-400">{subject}</span>
                  <span className="text-neutral-700 sm:text-right">{state}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">whitelist queue</div>
              {whitelistRows.map(([name, state, note], index) => (
                <div key={`${name}-${index}`} className="grid grid-cols-[72px_88px_1fr] gap-3 border-t border-neutral-900 pt-2">
                  <span className={name ? "text-neutral-500" : "text-neutral-800"}>
                    {name || "--------"}
                  </span>
                  <span className="text-neutral-700">{state}</span>
                  <span className="text-neutral-400">{note}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">staff bulletin cache</div>
              {bulletinRows.map(([label, value]) => (
                <div key={label} className="grid gap-1 border-t border-neutral-900 pt-2 sm:grid-cols-[92px_1fr] sm:gap-3">
                  <span className="text-neutral-700">{label}</span>
                  <span className="text-neutral-400">{value}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-3 border border-neutral-900 bg-black/20 p-4 font-mono text-xs">
              <div className="text-[10px] uppercase text-neutral-600">moderation cache</div>
              {moderationRows.map(([label, value], index) => (
                <div key={`${label}-${index}`} className="grid gap-1 border-t border-neutral-900 pt-2 sm:grid-cols-[72px_1fr] sm:gap-3">
                  <span className="text-neutral-700">{label}</span>
                  <span className="text-neutral-400">{value}</span>
                </div>
              ))}
            </section>

            <section className="grid gap-4 sm:grid-cols-3">
              <Link
                href="/record/the-record-keeps"
                className="border border-neutral-900 bg-black/20 p-4 transition-colors hover:border-neutral-700"
              >
                <h2 className="font-mono text-sm uppercase text-neutral-300">
                  recovered file
                </h2>
                <p className="mt-2 font-mono text-xs lowercase leading-relaxed text-neutral-600">
                  the same map row, filed under the old hand.
                </p>
              </Link>
              <Link
                href="/record/the-record"
                className="border border-neutral-900 bg-black/20 p-4 transition-colors hover:border-neutral-700"
              >
                <h2 className="font-mono text-sm uppercase text-neutral-300">
                  record
                </h2>
                <p className="mt-2 font-mono text-xs lowercase leading-relaxed text-neutral-600">
                  a colder host file, mostly struck, until the stones are read.
                </p>
              </Link>
              <Link
                href="/record/archive"
                className="border border-neutral-900 bg-black/20 p-4 transition-colors hover:border-neutral-700"
              >
                <h2 className="font-mono text-sm uppercase text-neutral-300">
                  archive
                </h2>
                <p className="mt-2 font-mono text-xs lowercase leading-relaxed text-neutral-600">
                  recovered material appears here only after it is found in play.
                </p>
              </Link>
            </section>
          </div>

          <aside className="border border-neutral-900 bg-black/30">
            <div className="border-b border-neutral-900 px-4 py-2 font-mono text-[10px] uppercase text-neutral-600">
              listing notes
            </div>
            <div className="px-4 py-5">
              <Image
                src="/keeper-eye.svg"
                alt="keeper eye server sigil"
                width={144}
                height={144}
                className="mx-auto mb-5 h-36 w-36 border border-neutral-900 object-cover opacity-70"
              />
              <dl className="space-y-3 font-mono text-xs">
                <div className="flex justify-between gap-4 border-t border-neutral-900 pt-3">
                  <dt className="text-neutral-700">server</dt>
                  <dd className="text-neutral-500">The Observance</dd>
                </div>
                <div className="flex justify-between gap-4 border-t border-neutral-900 pt-3">
                  <dt className="text-neutral-700">version</dt>
                  <dd className="text-neutral-500">java 1.21.11</dd>
                </div>
                <div className="flex justify-between gap-4 border-t border-neutral-900 pt-3">
                  <dt className="text-neutral-700">mode</dt>
                  <dd className="text-neutral-500">survival / record kept</dd>
                </div>
                <div className="flex justify-between gap-4 border-t border-neutral-900 pt-3">
                  <dt className="text-neutral-700">rules</dt>
                  <dd className="text-right text-neutral-500">read what is placed. do not force what is shut.</dd>
                </div>
                <div className="flex justify-between gap-4 border-t border-neutral-900 pt-3">
                  <dt className="text-neutral-700">contact</dt>
                  <dd className="text-neutral-500">no staff listed</dd>
                </div>
              </dl>
            </div>
          </aside>
        </section>

        <footer className="flex flex-wrap justify-between gap-3 border-t border-neutral-900 py-4 font-mono text-[11px] lowercase text-neutral-700">
          <span>mirror 03 keeps the old listing. the file is only the copy.</span>
          <span className="space-x-4">
            <Link href="/status" className="hover:text-neutral-500">
              status
            </Link>
            <Link href="/author" className="hover:text-neutral-500">
              panel
            </Link>
          </span>
        </footer>
      </div>
    </main>
  );
}
