/**
 * What the mirror answers when a request isn't what a page expects. Everything here looks like the
 * 2011 Apache box the site pretends to be, never a framework default (empty 405, stack trace, JSON).
 */

/** The posted form, or an empty one when the body isn't a form (curl, a JSON probe): never throws. */
export const readForm = (request: Request): Promise<FormData> => request.formData().catch(() => new FormData());

const esc = (s: string) => s.replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);

/** An Apache 2.2 error page (the same markup as the folders' 401/403 in proxy.ts). */
export function apachePage(status: number, title: string, message: string, headers: Record<string, string> = {}): Response {
  return new Response(
    `<!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN"><html><head><title>${status} ${title}</title></head>` +
      `<body><h1>${title}</h1><p>${message}</p>` +
      "<hr><address>Apache/2.2.16 (Debian) Server at copperlinehosting.com Port 80</address></body></html>",
    { status, headers: { "Content-Type": "text/html; charset=iso-8859-1", ...headers } },
  );
}

/** Apache's 401 for a password-protected folder (also the operator's /backstage). */
export const authRequired = (realm: string) =>
  apachePage(401, "Authorization Required", "This server could not verify that you are authorized to access the document requested.",
    { "WWW-Authenticate": `Basic realm="${realm.replace(/["\\\r\n]/g, "")}"`, "Cache-Control": "no-store" });

/** Apache's 404 for a file path. */
export function notFoundPage(request: Request): Response {
  let path = new URL(request.url).pathname;
  try {
    path = decodeURIComponent(path);
  } catch {
    /* keep it encoded */
  }
  return apachePage(404, "Not Found", `The requested URL ${esc(path)} was not found on this server.`);
}

/** A form handler opened directly (GET): back to the page that has the form, as the PHP did. */
export const seeOther = (request: Request, to: string) => Response.redirect(new URL(to, request.url), 303);
