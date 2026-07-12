import type { Metadata } from "next";
import "./globals.css";
import SiteChrome from "./site-chrome";

export const metadata: Metadata = {
  title: "The Observance",
  description: "Archived Minecraft server listing and recovered record.",
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
        <SiteChrome>{children}</SiteChrome>
      </body>
    </html>
  );
}
