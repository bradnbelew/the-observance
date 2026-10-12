import type { Metadata } from "next";
import "./globals.css";
import { DevActSwitcher } from "@/components/morrow/DevActSwitcher";

export const metadata: Metadata = {
  title: "Copperline Hosting Archive",
  description: "Preserved Copperline Hosting customer pages and public server directory.",
  robots: { index: false, follow: false },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="min-h-screen antialiased">
        {children}
        <DevActSwitcher />
      </body>
    </html>
  );
}
