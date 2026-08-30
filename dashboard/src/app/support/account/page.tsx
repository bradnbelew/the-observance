import type { Metadata } from 'next';
import Link from 'next/link';
import { redirect } from 'next/navigation';
import { Breadcrumbs, LegacyShell } from '@/components/legacy/LegacyShell';
import { MORROW_CASE_ROUTE } from '@/lib/morrow-copperline-case';
import { createClient } from '@/lib/supabase/server';
import { PlayerLoginForm } from './PlayerLoginForm';

export const dynamic = 'force-dynamic';
export const metadata: Metadata = {
  title: 'Recovery case access - Copperline Support',
  robots: { index: false, follow: false },
};

export default async function PlayerAccountPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const params = await searchParams;
  const supabase = await createClient();
  const { data } = await supabase.auth.getUser();
  if (data.user) redirect(MORROW_CASE_ROUTE);

  return <LegacyShell active="support">
    <Breadcrumbs><Link href="/support/index.php">Support</Link> &raquo; Account access</Breadcrumbs>
    <article className="player-login-card">
      <p className="morrow-case-kicker">Private recovery assignment</p>
      <h1>Open your assigned case</h1>
      <p>Use the email already linked to your Minecraft identity. Copperline sends a short-lived link;
        this preserved support desk never asks for a password.</p>
      {params.error === 'link'
        ? <p className="player-login-alert" role="alert">That link was invalid or expired. Request a fresh one below.</p>
        : null}
      <PlayerLoginForm />
      <aside><b>Privacy boundary</b><p>Unknown addresses receive the same response. A valid session can
        read only its own release-bound projection; direct links reveal no assignment.</p></aside>
      <Link href={MORROW_CASE_ROUTE}>Return to the recovery case &raquo;</Link>
    </article>
  </LegacyShell>;
}
