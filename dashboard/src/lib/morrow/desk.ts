import { createHash } from "node:crypto";
import { cookies } from "next/headers";
import type { Bundle, DeskRule } from "./canon";
import { deskFast, eveningAfter, hoursIntoAct, players, seal } from "./campaign";

/**
 * The Archive Desk. Theo is prerecorded: every reply is authored canon, chosen by the
 * evidence tokens in what players file. Tickets are signed tokens (WHMCS-style
 * viewticket.php?tid=&c= links), so there is no database to run.
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
  brief?: { accepted: string[]; missing: string[] };
};

export type Reply = { text: string; voice?: string | null; attach?: string | null };

const norm = (s: string) => s.toLowerCase().replace(/\s+/g, " ");

export function evaluate(b: Bundle, message: string): { kind: Ticket["kind"]; ref?: string; delayHours?: number } {
  const text = norm(message);
  for (const [file, f] of Object.entries(b.desk.files)) {
    if (text.includes(file.toLowerCase())) return { kind: "file", ref: file };
  }
  let best: { rule: DeskRule; hits: number } | null = null;
  let full: { rule: DeskRule; hits: number } | null = null;
  for (const rule of b.desk.rules) {
    const hits = rule.tokens.filter((t) => text.includes(t.toLowerCase())).length;
    const required = (rule.require ?? []).every((t) => text.includes(t.toLowerCase()));
    // the strongest complete match wins (Kit's date + phrase beats the phrase alone)
    if (hits >= rule.need && required && (!full || rule.need > full.rule.need || (rule.need === full.rule.need && hits > full.hits))) full = { rule, hits };
    if (hits > 0 && (!best || hits > best.hits)) best = { rule, hits };
  }
  if (full) return { kind: "rule", ref: full.rule.id, delayHours: full.rule.delay_hours };
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

export function replyFor(b: Bundle, t: Ticket): Reply | null {
  if (Date.now() < Date.parse(t.replyAt)) return null;
  if (t.kind === "file" && t.ref) {
    const f = b.desk.files[t.ref];
    return f ? { text: f.line, voice: f.voice, attach: t.ref } : null;
  }
  if ((t.kind === "rule" || t.kind === "partial") && t.ref) {
    const rule = b.desk.rules.find((r) => r.id === t.ref);
    if (!rule) return null;
    return t.kind === "rule"
      ? { text: rule.reply, voice: rule.voice, attach: rule.attach ?? null }
      : { text: rule.partial, voice: rule.partial_voice ?? null };
  }
  if (t.kind === "brief" && t.brief) {
    return t.brief.missing.length <= 1
      ? { text: "Okay.  Okay.  I've read all of it.  You're right.  Let's do it properly, the way Rowan wanted.  I've got one USB stick in the drawer.  Pick what you want on it, and I'll keep it.\n\nThe panel will let you book a maintenance window now.", voice: "T-09a" }
      : { text: `I hear you.  I just need it to be real.  Show me.\n\n(I couldn't match: ${t.brief.missing.join(", ")})`, voice: "T-09b" };
  }
  if (t.kind === "booking") {
    return { text: "Okay.  Saturday then, or whenever you said.  I'll be on.  I'll cut the bridge when you've done your bit.", voice: "T-10-booked" };
  }
  if (t.kind === "choice" && t.ref && b.usb_choices?.[t.ref]) {
    const c = b.usb_choices[t.ref];
    return { text: `Took the drives out at six-forty.  The stick's got ${c.line.charAt(0).toLowerCase()}${c.line.slice(1)}  Kit says hi, weirdly.  Thanks for coming.\n\nT`, voice: c.voice };
  }
  if (t.kind === "none") return { text: "Thanks!  I'm not sure I have anything on that, sorry.  If you've got a filename, that helps." };
  return null;
}

/** Theo's unprompted drops and the escalating hints, released by time since the act started. */
export function deskNotes(b: Bundle): { at: string; text: string; file: string | null; voice?: string }[] {
  const h = hoursIntoAct();
  const notes: { at: string; text: string; file: string | null; voice?: string }[] = [
    { at: "2026-08-02", text: "This is a read-only mirror of the old Copperline site.  Records for case 6118 (Mossfield) are kept until 31 Dec 2026.  If you were a Mossfield player, the desk is open.  I check it in the evenings.  No worries at all.", file: null, voice: "T-01" },
  ];
  for (const d of b.desk.drops) {
    if (d.act < b.act || h >= d.after_hours) notes.push({ at: `Act ${d.act}`, text: d.note, file: d.file });
  }
  b.desk.hints.forEach((hint, i) => {
    if (h >= [24, 72, 120][i]) notes.push({ at: "", text: hint, file: null });
  });
  return notes;
}

export function scoreBrief(b: Bundle, answers: Record<string, string>): { accepted: string[]; missing: string[] } {
  const accepted: string[] = [], missing: string[] = [];
  const backdated = players().map((p) => backdatedJoin(p));
  for (const f of b.case_brief ?? []) {
    const given = (answers[f.id] ?? "").trim().toLowerCase();
    const accept = f.accept.flatMap((a) => (a === "@clean_md5" ? [b.clean_md5 ?? ""] : a === "@backdated" ? backdated : [a])).map((a) => a.toLowerCase());
    (given && accept.some((a) => given === a || given.includes(a)) ? accepted : missing).push(f.label);
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

/** Keep a compact, message-free copy in a cookie so the desk can list "Your requests". */
export async function remember(t: Ticket) {
  const jar = await cookies();
  const bare = { ...t, message: "" };
  const compact = seal({ tid: t.tid, subject: t.subject, replyAt: t.replyAt, c: seal(bare) });
  const list = (jar.get("desk_tickets")?.value ?? "").split("~").filter(Boolean);
  jar.set("desk_tickets", [compact, ...list].slice(0, 6).join("~"), { httpOnly: true, sameSite: "lax", path: "/", maxAge: 60 * 60 * 24 * 120 });
}

/** Whether Theo's reply is visible yet. */
export function answered(replyAt: string, now = Date.now()): boolean {
  return now >= Date.parse(replyAt);
}
