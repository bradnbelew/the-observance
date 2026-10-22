import { after, NextResponse } from "next/server";
import { bundle } from "@/lib/morrow/canon";
import { deskReplyAt, eveningAfter } from "@/lib/morrow/campaign";
import { deskEvent, remember, scoreBrief, ticketLink, type Ticket } from "@/lib/morrow/desk";
import { postDeskEvent } from "@/lib/morrow/bridge";

export const dynamic = "force-dynamic";

export async function POST(request: Request) {
  const form = await request.formData();
  const b = await bundle();
  if (!b.case_brief) return NextResponse.redirect(new URL("/support/index.php", request.url), 303);
  const answers = Object.fromEntries(b.case_brief.map((f) => [f.id, String(form.get(f.id) ?? "")]));
  const brief = scoreBrief(b, answers);
  const now = new Date();
  const tid = 200000 + (now.getTime() % 100000);
  const t: Ticket = {
    tid, at: now.toISOString(), name: String(form.get("name") ?? "").slice(0, 60), subject: "case 6118 brief",
    message: b.case_brief.map((f) => `${f.label}\n  ${answers[f.id]}`).join("\n"),
    kind: "brief", replyAt: deskReplyAt(now, () => eveningAfter(now, tid)).toISOString(), brief,
  };
  await remember(t);
  console.log(`MORROW_DESK brief=${tid} accepted=${brief.accepted.length}/${b.case_brief.length}`);
  const ev = deskEvent(b, t);
  if (ev) after(() => postDeskEvent(ev));
  return NextResponse.redirect(new URL(ticketLink(t), request.url), 303);
}
