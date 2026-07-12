import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "record key required",
  robots: { index: false, follow: false },
};

export default function BareRecordPage() {
  return (
    <main className="record-error-site">
      <div className="record-error-box">
        <p>recordsrv/0.7</p>
        <h1>400: record key required</h1>
        <pre>{`request rejected\nno public index is available at this path.`}</pre>
      </div>
    </main>
  );
}
