import { createHash } from "node:crypto";
import { cookies } from "next/headers";
import type { Bundle, DeskRule } from "./canon";
import { deskFast, eveningAfter, players, seal } from "./campaign";
import type { DeskEvent } from "./bridge";

/**
 * The Archive Desk. Theo is prerecorded: every reply is authored canon, chosen by the
 * evidence tokens in what players file. Tickets are signed tokens (WHMCS-style
 * viewticket.php?tid=&c= links), so there is no database to run.
 *
 * Correct evidence filed before its act is matched against the bundle's `held` rules (tokens only)
 * and stored as an ordinary rule/file ticket. Viewing it re-reads the current act's bundle: until
 * that act arrives the rule isn't there and Theo says "give me a bit"; afterwards the same link shows
 * the real reply. No state, just the signed ticket.
 */
export type Ticket = {
  tid: number;
  at: string;          // ISO submit time
  name: string;
  subject: string;
  message: string;     // truncated copy of what the player wrote
  kind: "rule" | "partial" | "file" | "brief" | "booking" | "choice" | "none";
  ref?: string;        // rule id / filename / choice key
  replyAt: string;     // when Theo's answer becomes visible
  brief?: { accepted: string[]; missing: string[] };  // case-brief field ids (older tickets: labels)
};

export type Reply = { text: string; voice?: string | null; attach?: string | null };

type HeldRule = { id: string; act: number; tokens: string[]; need: number; require?: string[] };
/** Generated fields newer than the shared Bundle type (morrow/canon/build.py site_bundle). */
type DeskExtras = { held?: HeldRule[]; held_files?: Record<string, number>; hint_hours?: number[] };
const extras = (b: Bundle) => b.desk as Bundle["desk"] & DeskExtras;

const HOLD_TEXT = "Give me a bit.  I'll look into that.";

const norm = (s: string) => s.toLowerCase().replace(/\s+/g, " ");
const escapeRe = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

/** A filename is asked for by its full name or its stem (moss_0518 or moss_0518.ogg). */
function mentionsFile(text: string, file: string): boolean {
  const f = file.toLowerCase();
  const stem = f.replace(/\.[a-z0-9]+$/, "");
  return text.includes(f) || new RegExp(`(^|[^a-z0-9_])${escapeRe(stem)}($|[^a-z0-9_])`).test(text);
}

/**
 * Whether the filed text carries a token. A purely alphabetic token must start a word, so "morrow"
 * isn't found inside "tomorrow" and "marked" isn't found inside "bookmarked"; anything else (times,
 * phrases, "@reboot") is a plain substring. "md5:<hex>" matches a filed word whose MD5 is <hex>
 * (Iona's key: the bundle only carries its hash, like the console's key_md5).
 */
function has(text: string, token: string): boolean {
  const t = token.toLowerCase();
  if (t.startsWith("md5:")) {
    const want = t.slice(4);
    return text.split(/[\s"'`,;()<>[\]{}]+/).some((w) => w && createHash("md5").update(w.replace(/[.!?]+$/, "")).digest("hex") === want);
  }
  return /^[a-z]+$/.test(t) ? new RegExp(`(^|[^a-z])${t}`).test(text) : text.includes(t);
}

function fullMatch(text: string, rule: { tokens: string[]; need: number; require?: string[] }): number | null {
  const hits = rule.tokens.filter((t) => has(text, t)).length;
  const required = (rule.require ?? []).every((t) => has(text, t));
  return hits >= rule.need && required ? hits : null;
}

export function evaluate(b: Bundle, message: string): { kind: Ticket["kind"]; ref?: string; delayHours?: number } {
  const text = norm(message);
  for (const file of Object.keys(b.desk.files)) {
    if (mentionsFile(text, file)) return { kind: "file", ref: file };
  }
  let best: { rule: DeskRule; hits: number } | null = null;
  let full: { rule: DeskRule; hits: number } | null = null;
  for (const rule of b.desk.rules) {
    const hits = rule.tokens.filter((t) => has(text, t)).length;
    const complete = fullMatch(text, rule);
    // the strongest complete match wins (Kit's date + phrase beats the phrase alone); ties go to the earlier rule
    if (complete !== null && (!full || rule.need > full.rule.need || (rule.need === full.rule.need && hits > full.hits))) full = { rule, hits };
    if (hits > 0 && (!best || hits > best.hits)) best = { rule, hits };
  }
  if (full) return { kind: "rule", ref: full.rule.id, delayHours: full.rule.delay_hours };
  // right evidence, too early: hold it (strongest held match, same ordering)
  let held: HeldRule | null = null;
  for (const rule of extras(b).held ?? []) {
    if (fullMatch(text, rule) !== null && (!held || rule.need > held.need)) held = rule;
  }
  if (held) return { kind: "rule", ref: held.id };
  for (const file of Object.keys(extras(b).held_files ?? {})) {
    if (mentionsFile(text, file)) return { kind: "file", ref: file };
  }
  if (best && best.rule.partial) return { kind: "partial", ref: best.rule.id };
  return { kind: "none" };
}

export function newTicket(b: Bundle, name: string, subject: string, message: string, now = new Date()): Ticket {
  const e = evaluate(b, `${subject}\n${message}`);
  const tid = 100000 + (Number.parseInt(createHash("md5").update(now.toISOString() + message).digest("hex").slice(0, 6), 16) % 900000);
  const replyAt = deskFast()
    ? now
    : e.delayHours
      ? new Date(now.getTime() + e.delayHours * 3_600_000)
      : eveningAfter(now, tid);
  return {
    tid, at: now.toISOString(), name: name.slice(0, 60), subject: subject.slice(0, 120), message: message.slice(0, 900),
    kind: e.kind, ref: e.ref, replyAt: replyAt.toISOString(),
  };
}

const lowerFirst = (s: string) => s.charAt(0).toLowerCase() + s.slice(1);

export function replyFor(b: Bundle, t: Ticket): Reply | null {
  if (Date.now() < Date.parse(t.replyAt)) return null;
  if (t.kind === "file" && t.ref) {
    const f = b.desk.files[t.ref];
    if (f) return { text: f.line, voice: f.voice, attach: t.ref };
    return t.ref in (extras(b).held_files ?? {}) ? { text: HOLD_TEXT } : null;
  }
  if ((t.kind === "rule" || t.kind === "partial") && t.ref) {
    const rule = b.desk.rules.find((r) => r.id === t.ref);
    if (!rule) return t.kind === "rule" && (extras(b).held ?? []).some((r) => r.id === t.ref) ? { text: HOLD_TEXT } : null;
    return t.kind === "rule"
      ? { text: rule.reply, voice: rule.voice, attach: rule.attach ?? null }
      : { text: rule.partial, voice: rule.partial_voice ?? null };
  }
  if (t.kind === "brief" && t.brief) {
    const label = (id: string) => b.case_brief?.find((f) => f.id === id)?.label ?? id;
    const r = b.brief_replies ?? BRIEF_REPLIES;
    return briefAccepted(t.brief)
      ? { text: r.accepted.text, voice: r.accepted.voice }
      : { text: `${r.missing.text}\n\n(I couldn't match: ${t.brief.missing.map(label).join(", ")})`, voice: r.missing.voice };
  }
  if (t.kind === "booking") {
    return { text: "Okay.  Saturday then.  I'll be on.  I'll cut the bridge when you've done your bit.", voice: "T-10-booked" };
  }
  if (t.kind === "choice" && t.ref && b.usb_choices?.[t.ref]) {
    const c = b.usb_choices[t.ref];
    return { text: `Took the drives out this morning.  Six-forty.  About when I turned it back on, back then.  The stick's got ${lowerFirst(c.line)}  Kit says hi, weirdly.  Thanks for coming.\n\nT`, voice: c.voice };
  }
  if (t.kind === "none") return { text: "Thanks!  I'm not sure I have anything on that, sorry.  If you've got a filename, that helps." };
  return null;
}

/** Fallback for bundles built before brief_replies existed (morrow/canon/later.py BRIEF_REPLIES is the source). */
const BRIEF_REPLIES = {
  accepted: { text: "Okay.  Okay.  I've read all of it.  I knew all of this.  I'd just never seen it in one place.  You're right.  Let's do it properly, the way Rowan wanted.\n\nI'm wiping the drives after.  I've got one USB stick in the drawer.  I'll keep one thing.  Just one, or I'll end up doing this again.  You pick.\n\nThe panel will let you book a maintenance window now.", voice: "T-09a" },
  missing: { text: "I hear you.  I just need it to be real.  Show me.", voice: "T-09b" },
};

/** Four of the five case-brief fields is enough. */
export const briefAccepted = (brief: { missing: string[] }) => brief.missing.length <= 1;

/**
 * What the plugin is told when a ticket is filed (POST /event), or null when it moves nothing.
 * Only evidence the current act's desk actually answers counts (a held rule waits for its act).
 * Refs are the desk rule ids (found_morrow, unmask, floor, restart, key, ingest, kit, ...), a
 * requested file's stem (moss_0518), "brief" / "brief_missing" for the case brief, and "booking"
 * for the Act 10 window. The plugin advances acts and queues Theo's big replies from these.
 */
export function deskEvent(b: Bundle, t: Ticket): Omit<DeskEvent, "kind"> | null {
  const base = { reply_at: t.replyAt, subject: t.subject || null };
  if (t.kind === "rule" && t.ref) {
    const rule = b.desk.rules.find((r) => r.id === t.ref);
    return rule ? { ...base, ref: rule.id, voice: rule.voice ?? null } : null;
  }
  if (t.kind === "file" && t.ref) {
    const f = b.desk.files[t.ref];
    return f ? { ...base, ref: t.ref.replace(/\.[a-z0-9]+$/i, ""), voice: f.voice ?? null } : null;
  }
  if (t.kind === "brief" && t.brief) {
    const r = b.brief_replies ?? BRIEF_REPLIES;
    return briefAccepted(t.brief)
      ? { ...base, ref: "brief", voice: r.accepted.voice }
      : { ...base, ref: "brief_missing", voice: r.missing.voice };
  }
  if (t.kind === "booking") return { ...base, ref: "booking", voice: "T-10-booked" };
  return null;
}

/** Theo's unprompted drops and the escalating hints, released by time since the act started. */
export function deskNotes(b: Bundle, clock: { hours: number; hintFactor: number }): { at: string; text: string; file: string | null; voice?: string }[] {
  const h = clock.hours;
  const notes: { at: string; text: string; file: string | null; voice?: string }[] = [
    { at: "2026-08-02", text: "This is a read-only mirror of the old Copperline site.  Records for case 6118 (Mossfield) are kept until 31 Dec 2026.  If you were a Mossfield player, the desk is open.  I check it in the evenings.  No worries at all.", file: null, voice: "T-01" },
  ];
  for (const d of b.desk.drops) {
    if (d.act < b.act || h >= d.after_hours) notes.push({ at: `Act ${d.act}`, text: d.note, file: d.file });
  }
  // hint timers scale with the group (bridge.ts hintFactor): 24/72/120/168 h for five or more, half that for a pair
  const hours = (extras(b).hint_hours ?? [24, 72, 120, 168]).map((x) => x * clock.hintFactor);
  b.desk.hints.forEach((hint, i) => {
    if (i < hours.length && h >= hours[i]) notes.push({ at: "", text: hint, file: null });
  });
  return notes;
}

/** One normal form per field: case, quotes, a trailing CDT/CST and stray punctuation don't matter. */
function normField(s: string): string {
  return s.toLowerCase()
    .replace(/['’`"]/g, "")
    .replace(/\b(cdt|cst)\b/g, " ")
    .replace(/[^a-z0-9:\- ]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

/** Exact (normalised) match per field: pasting every candidate into one box matches none of them. */
export function scoreBrief(b: Bundle, answers: Record<string, string>): { accepted: string[]; missing: string[] } {
  const accepted: string[] = [], missing: string[] = [];
  const backdated = players().map((p) => backdatedJoin(p));
  for (const f of b.case_brief ?? []) {
    const given = normField(answers[f.id] ?? "");
    const accept = f.accept
      .flatMap((a) => (a === "@clean_md5" ? [b.clean_md5 ?? ""] : a === "@backdated" ? backdated : [a]))
      .map(normField)
      .filter(Boolean);
    (given && accept.includes(given) ? accepted : missing).push(f.id);
  }
  return { accepted, missing };
}

/** Must match morrow/canon/later.py backdated_join(). */
export function backdatedJoin(player: string): string {
  const hex = createHash("md5").update(player.toLowerCase()).digest("hex");
  const n = BigInt(`0x${hex}`) % 28n;
  const d = new Date(Date.UTC(2010, 10, 1 + Number(n)));
  return d.toISOString().slice(0, 10);
}

export const fileLink = (file: string) => `/desk-file/${seal({ f: file })}`;
export const voiceLink = (id: string) => `/desk-voice/${seal({ v: id })}`;
export const ticketLink = (t: Ticket) => `/viewticket.php?tid=${t.tid}&c=${seal(t)}`;

const COOKIE_BUDGET = 3800;  // browsers drop a cookie over 4096 bytes, and every ticket with it
/** ISO to the minute ("2026-10-12T23:41Z"), rounded down or up. */
const minuteIso = (iso: string, up = false) => {
  const ms = Date.parse(iso), m = 60_000;
  return new Date((up ? Math.ceil(ms / m) : Math.floor(ms / m)) * m).toISOString().replace(/:\d\d\.\d{3}Z$/, "Z");
};

/**
 * Keep a compact copy of each request in a cookie so the desk can list "Your requests". Each entry is
 * the ticket number, a short subject, the reply time, and a slim signed ticket for the link (no name,
 * message or subject: the page falls back to "Guest" / "(no subject)"; the original link has them).
 * About 10 fit; the newest that fit the budget are kept, so the cookie can never overflow.
 */
export async function remember(t: Ticket) {
  const jar = await cookies();
  const subject = t.subject.slice(0, 24);
  const replyAt = minuteIso(t.replyAt, true);
  const slim = { tid: t.tid, at: minuteIso(t.at), kind: t.kind, ref: t.ref, replyAt, ...(t.brief ? { brief: t.brief } : {}) };
  const entry = seal({ tid: t.tid, subject, replyAt, c: seal(slim) });
  const list = [entry, ...(jar.get("desk_tickets")?.value ?? "").split("~").filter(Boolean)];
  const kept: string[] = [];
  let size = 0;
  for (const e of list) {
    if (size + e.length + 1 > COOKIE_BUDGET) break;
    kept.push(e);
    size += e.length + 1;
  }
  jar.set("desk_tickets", kept.join("~"), { httpOnly: true, sameSite: "lax", path: "/", maxAge: 60 * 60 * 24 * 120 });
}

/** Whether Theo's reply is visible yet. */
export function answered(replyAt: string, now = Date.now()): boolean {
  return now >= Date.parse(replyAt);
}
