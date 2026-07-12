"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

/**
 * Site chrome. The public listing (/) and Record routes (/record/*) are in-world artifacts,
 * so they render bare and full-bleed. Operator routes keep the dashboard header and frame.
 */
export default function SiteChrome({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  if (pathname === "/" || pathname?.startsWith("/record")) {
    return <>{children}</>;
  }

  // The Author link is spoiler-mode navigation. Hiding it while on /status is an immersion guard,
  // not a security boundary.
  const onSpoilerFreePage = pathname?.startsWith("/status");
  return (
    <>
      <header className="border-b border-neutral-800">
        <nav className="mx-auto flex max-w-5xl items-center gap-6 px-4 py-3">
          <Link href="/" className="font-mono text-sm tracking-wide text-neutral-300 hover:text-white">
            The Observance
          </Link>
          <div className="ml-auto flex items-center gap-4 text-sm">
            <Link href="/status" className="text-neutral-400 hover:text-white">
              Status
            </Link>
            {onSpoilerFreePage ? null : (
              <Link href="/author" className="text-neutral-400 hover:text-white">
                Author
              </Link>
            )}
          </div>
        </nav>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">{children}</main>
    </>
  );
}
