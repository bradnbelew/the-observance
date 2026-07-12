"use client";

import { usePathname } from "next/navigation";

/** Route-aware framing. Player artifacts remain full-bleed; authenticated
 * production tools receive a wider, neutral workspace. */
export default function SiteChrome({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  if (pathname?.startsWith("/author") && pathname !== "/author/login") {
    return <main className="director-frame">{children}</main>;
  }
  return <>{children}</>;
}
