import { cookies } from "next/headers";
import { currentAct, DEV, DEV_ACT_COOKIE } from "@/lib/morrow/campaign";

/** Rendered only when MORROW_DEV=1 (local testing). Lets the owner jump between acts. */
export async function DevActSwitcher() {
  if (!DEV) return null;
  const act = await currentAct();
  const pinned = Boolean((await cookies()).get(DEV_ACT_COOKIE)?.value);
  return (
    <div className="dev-acts" aria-label="Testing: act switcher">
      <b title={pinned ? "pinned by the switcher" : "MORROW_ACT or the bridge"}>TEST act</b>
      {Array.from({ length: 10 }, (_, i) => i + 1).map((n) => (
        <a key={n} href={`/dev-act?n=${n}&back=/`} className={n === act ? "on" : ""}>{n}</a>
      ))}
    </div>
  );
}
