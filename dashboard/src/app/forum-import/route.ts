export const dynamic = "force-dynamic";

/** Apache with Indexes off: the directory exists, but you have to know a filename. */
export async function GET() {
  const html = `<!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">
<html><head>
<title>403 Forbidden</title>
</head><body>
<h1>Forbidden</h1>
<p>You don't have permission to access /forum-import/
on this server.</p>
<hr>
<address>Apache/2.2.16 (Debian) Server at copperlinehosting.com Port 80</address>
</body></html>
`;
  return new Response(html, { status: 403, headers: { "Content-Type": "text/html; charset=iso-8859-1" } });
}
