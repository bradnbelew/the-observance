"use client";

/** When even the site frame fails: Apache's 500, as the 2011 box would have shown it. */
export default function GlobalError() {
  return (
    <html lang="en">
      <body>
        <title>500 Internal Server Error</title>
        <h1>Internal Server Error</h1>
        <p>The server encountered an internal error or misconfiguration and was unable to complete your request.</p>
        <p>Please contact the server administrator, support@copperlinehosting.com and inform them of the time the error occurred.</p>
        <hr />
        <address>Apache/2.2.16 (Debian) Server at copperlinehosting.com Port 80</address>
      </body>
    </html>
  );
}
