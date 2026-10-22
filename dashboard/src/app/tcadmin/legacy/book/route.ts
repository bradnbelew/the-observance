import { after, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { currentAct, deskReplyAt, eveningAfter, seal, unseal, upcomingSaturdays } from "@/lib/morrow/campaign";
import { deskEvent, remember, ticketLink, type Ticket } from "@/lib/morrow/desk";
import { postDeskEvent } from "@/lib/morrow/bridge";
import { bundle } from "@/lib/morrow/canon";

export const dynamic = "force-dynamic";

/** Act 10: booking the maintenance window, Saturdays only (Theo: "Saturday then"). Theo answers on the desk (T-10-booked). */
export async function POST(request: Request) {
  if ((await currentAct()) < 10 || !unseal((await cookies()).get("tcadmin")?.value)) {
    return NextResponse.redirect(new URL("/tcadmin/legacy", request.url), 303);
  }
  const form = await request.formData();
  const day = String(form.get("day") ?? "").slice(0, 10);
  const hour = Number(form.get("hour"));
  const saturday = upcomingSaturdays().find((s) => s.value === day);
  if (!saturday || !Number.isInteger(hour) || hour < 0 || hour > 23) {
    const r = seal({ book: "Maintenance windows can only be booked on an upcoming Saturday." });
    return NextResponse.redirect(new URL(`/tcadmin/legacy?r=${r}`, request.url), 303);
  }
  const when = `${saturday.label} ${String(hour).padStart(2, "0")}:00 (client time)`;
  const now = new Date();
  const t: Ticket = {
    tid: 300000 + (now.getTime() % 100000), at: now.toISOString(), name: "rookery", subject: "maintenance window request",
    message: `Requested window: ${when}`, kind: "booking", replyAt: deskReplyAt(now, () => eveningAfter(now, 7)).toISOString(),
  };
  await remember(t);
  console.log(`MORROW_DESK booking when=${when}`);
  const ev = deskEvent(await bundle(), t);
  if (ev) after(() => postDeskEvent(ev));
  return NextResponse.redirect(new URL(ticketLink(t), request.url), 303);
}
