import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "The Observance — Dashboard",
  description: "Control surface for The Observance.",
  robots: { index: false, follow: false },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-ash text-neutral-100 antialiased">
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
      </body>
    </html>
  );
}
