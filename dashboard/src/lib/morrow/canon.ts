
/**
 * Server-side access to the generated canon bundles (dashboard/src/lib/canon/act-NN.json).
 * Bundles are produced by `python -m morrow.canon.build`; never hand-edit them.
 * Each bundle only contains what a player could reach by that act, so a page can't
 * leak a later act by accident.
 */

export type Post = { id: number; author: string; at: string; body: string; edited?: string };
export type Thread = { id: number; title: string; act: number; posts: Post[] };
export type Member = {
  title: string; joined: string; location: string; local_offset: string; posts: number; sig: string;
  interests?: string; import_note?: string;
};
export type LogRow = { date: string; player: string; action: string; block: string; x: number; y: number; z: number; extra: string };
export type Shot = { filename: string; server_mtime?: string; chicago?: string; act: number; shows: string };
export type Marker = { label: string; x: number; y: number; z: number; type: string };

export type Bundle = {
  act: number;
  members: Record<string, Member>;
  threads: Thread[];
  archivist_notes: { id: number; at: string; act: number; body: string }[];
  client_account: {
    username: string; security_question: string; security_answer: string;
    service: { product: string; label: string; node: string; status: string; players_online: number; address: string };
    invoices: { no: number; date: string; amount: string; status: string }[];
    ftp: { host: string; user: string; note_act1: string };
  };
  ticket_6118: { subject: string; opened: string; client: string; dept: string; status_public: string;
    messages: { from: string; at: string; body: string }[] };
  status_incident: {
    id: string;
    original: { title: string; posted: string; body: string; update: string };
    morrow_rewrite?: { title: string; posted: string; body: string };
    raw_export: string;
  };
  heartbeat: Record<string, string>;
  robots_txt: string;
  motd: string;
  server_logs: Record<string, string>;
  logblock: LogRow[];
  june_shots: Shot[];
  dynmap_markers: Marker[];
  asset_sale?: { title: string; date: string; lots: { lot: number; item: string; buyer: string }[] };
  checklist?: string;
  tcadmin_log?: string[][];
  irc_log?: string;
};

/** The campaign act. Set MORROW_ACT on the deployment; defaults to Act 1. */
export function currentAct(): number {
  const n = Number.parseInt(process.env.MORROW_ACT ?? "1", 10);
  return Number.isFinite(n) && n >= 1 && n <= 10 ? n : 1;
}

const loaders: Record<number, () => Promise<{ default: unknown }>> = {
  1: () => import("@/lib/canon/act-01.json"), 2: () => import("@/lib/canon/act-02.json"),
  3: () => import("@/lib/canon/act-03.json"), 4: () => import("@/lib/canon/act-04.json"),
  5: () => import("@/lib/canon/act-05.json"), 6: () => import("@/lib/canon/act-06.json"),
  7: () => import("@/lib/canon/act-07.json"), 8: () => import("@/lib/canon/act-08.json"),
  9: () => import("@/lib/canon/act-09.json"), 10: () => import("@/lib/canon/act-10.json"),
};

export async function bundle(): Promise<Bundle> {
  return (await loaders[currentAct()]()).default as Bundle;
}

/** vBulletin-style date: "05-18-2011, 04:16 AM". */
export function vbDate(at: string): string {
  const [d, t] = at.split(" ");
  const [y, m, day] = d.split("-");
  const [hh, mm] = t.split(":").map(Number);
  const h12 = ((hh + 11) % 12) + 1;
  return `${m}-${day}-${y}, ${String(h12).padStart(2, "0")}:${String(mm).padStart(2, "0")} ${hh < 12 ? "AM" : "PM"}`;
}

export function threadById(b: Bundle, id: number): Thread | undefined {
  return b.threads.find((t) => t.id === id);
}

export function postsBy(b: Bundle, handle: string): { thread: Thread; post: Post }[] {
  return b.threads.flatMap((thread) => thread.posts.filter((p) => p.author === handle).map((post) => ({ thread, post })));
}
