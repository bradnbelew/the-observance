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
        <p className="eyebrow">Observance control / restricted channel</p>
        <div className="operator-seal" aria-hidden>VII</div>
        <h1>Director access</h1>
        <p className="operator-login-copy">
          This surface can alter the live world, release queued events, and expose unrecovered evidence.
          Access is limited to configured operators.
        </p>
        <LoginForm />
        <Link href="/" className="quiet-link">Return to public mirror</Link>
      </section>
    </main>
  );
}
