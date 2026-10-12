import type { Metadata } from "next";
import Link from "next/link";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";
import { bundle } from "@/lib/morrow/canon";
import { TEMP_PASSWORD } from "@/lib/morrow/session";

export const metadata: Metadata = { title: "Lost Password Reset - Copperline Hosting" };
export const dynamic = "force-dynamic";

type Search = Promise<{ step?: string; username?: string; error?: string }>;

export default async function PwResetPage({ searchParams }: { searchParams: Search }) {
  const { step, username, error } = await searchParams;
  const b = await bundle();
  return (
    <LegacyShell active="support">
      <Breadcrumbs><Link href="/clientarea.php">Client Area</Link> » Lost Password Reset</Breadcrumbs>
      <OldPageTitle sub="Forgotten your password? Enter your details below to begin the reset process.">Lost Password Reset</OldPageTitle>
      {step === "done" ? (
        <>
          <div className="old-alert"><b>Password reset.</b> Email delivery is disabled while this site runs as an archive mirror, so your new password is shown here once.</div>
          <table className="old-data-table"><tbody><tr><th>New password</th><td><code>{TEMP_PASSWORD}</code></td></tr></tbody></table>
          <p><Link href="/clientarea.php">Continue to the client area »</Link></p>
        </>
      ) : step === "question" && username ? (
        <form method="post" action="/dopwreset.php" className="old-form">
          {error === "answer" ? <div className="old-message error">The answer you entered did not match our records.</div> : null}
          <input type="hidden" name="username" value={username} />
          <p>Please answer your security question to continue.</p>
          <label>{b.client_account.security_question}<input name="answer" autoComplete="off" aria-label="Security answer" /></label>
          <button type="submit">Submit</button>
        </form>
      ) : (
        <form method="post" action="/dopwreset.php" className="old-form">
          {error === "nouser" ? <div className="old-message error">No client account was found with that username.</div> : null}
          <label>Username<input name="username" autoComplete="off" aria-label="Username" /></label>
          <button type="submit">Submit</button>
        </form>
      )}
    </LegacyShell>
  );
}
