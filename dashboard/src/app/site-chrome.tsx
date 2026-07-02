"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

/**
 * Site chrome. The dashboard routes (/, /status, /author) get the control-surface header nav +
 * a constrained main. The public Record (/record/*) is an in-world artifact that "leaves the game"
 * — it must NOT show dashboard chrome, so it renders bare and full-bleed (its own record-root styles
 * it). Client-only so it can key off the pathname; the record page content itself stays server-rendered.
 */
export default function SiteChrome({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  if (pathname?.startsWith("/record")) {
    return <>{children}</>;
  }
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
            <Link href="/author" className="text-neutral-400 hover:text-white">
              Author
            </Link>
          </div>
        </nav>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">{children}</main>
    </>
  );
}
