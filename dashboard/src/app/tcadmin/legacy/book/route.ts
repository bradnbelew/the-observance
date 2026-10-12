import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { currentAct, deskFast, eveningAfter, unseal } from "@/lib/morrow/campaign";
import { remember, ticketLink, type Ticket } from "@/lib/morrow/desk";

export const dynamic = "force-dynamic";

/** Act 10: booking the maintenance window. Theo answers on the desk (T-10-booked). */
export async function POST(request: Request) {
  if ((await currentAct()) < 10 || !unseal((await cookies()).get("tcadmin")?.value)) {
    return NextResponse.redirect(new URL("/tcadmin/legacy", request.url), 303);
  }
  const when = String((await request.formData()).get("when") ?? "").slice(0, 32);
  const now = new Date();
  const t: Ticket = {
    tid: 300000 + (now.getTime() % 100000), at: now.toISOString(), name: "rookery", subject: "maintenance window request",
    message: `Requested window: ${when}`, kind: "booking", replyAt: (deskFast() ? now : eveningAfter(now, 7)).toISOString(),
  };
  await remember(t);
  console.log(`MORROW_DESK booking when=${when}`);
  return NextResponse.redirect(new URL(ticketLink(t), request.url), 303);
}
