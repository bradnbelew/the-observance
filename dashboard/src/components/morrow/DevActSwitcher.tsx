import { cookies } from "next/headers";
import { DEV, DEV_ACT_COOKIE } from "@/lib/morrow/campaign";

/** Rendered only when MORROW_DEV=1 (local testing). Lets the owner jump between acts. */
export async function DevActSwitcher() {
  if (!DEV) return null;
  const act = Number((await cookies()).get(DEV_ACT_COOKIE)?.value || process.env.MORROW_ACT || 1);
  return (
    <div className="dev-acts" aria-label="Testing: act switcher">
      <b>TEST act</b>
      {Array.from({ length: 10 }, (_, i) => i + 1).map((n) => (
        <a key={n} href={`/dev-act?n=${n}&back=/`} className={n === act ? "on" : ""}>{n}</a>
      ))}
    </div>
  );
}
