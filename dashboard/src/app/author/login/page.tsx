import type { Metadata } from "next";
import { redirect } from "next/navigation";
import Link from "next/link";
import { getAuthor } from "@/lib/author-auth";
import { LoginForm } from "./LoginForm";

export const metadata: Metadata = { title: "Operator access — The Observance" };

export default async function AuthorLoginPage() {
  if (await getAuthor()) redirect("/author");
  return (
    <main className="operator-login-shell">
      <section className="operator-login-card">
        <p className="eyebrow">Production operations · authenticated</p>
        <div className="operator-seal" aria-hidden>OPS</div>
        <h1>Operator sign-in</h1>
        <p className="operator-login-copy">
          This console can alter live story state, release queued events, and expose unrecovered evidence.
          Passwordless access is restricted to the configured operator allowlist.
        </p>
        <LoginForm />
        <Link href="/" className="quiet-link">Return to Copperline archive</Link>
      </section>
    </main>
  );
}
