'use client';

import Link from 'next/link';

export default function MorrowCaseError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return <div className="legacy-site"><main className="morrow-route-error" role="alert">
    <p>copperline support / recovery cases</p><h1>The case page failed safely.</h1>
    <p>No receipt was submitted and no locked case material was rendered.</p>
    <button type="button" onClick={reset}>Retry case read</button><Link href="/support/index.php">Return to Support Center</Link>
  </main></div>;
}
