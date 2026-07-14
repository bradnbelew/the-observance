import { NextRequest, NextResponse } from "next/server";

function constantTimeEqual(left: string, right: string): boolean {
  const length = Math.max(left.length, right.length);
  let difference = left.length ^ right.length;
  for (let i = 0; i < length; i += 1) {
    difference |= (left.charCodeAt(i) || 0) ^ (right.charCodeAt(i) || 0);
  }
  return difference === 0;
}

function readBasicCredentials(request: NextRequest): [string, string] | null {
  const authorization = request.headers.get("authorization");
  if (!authorization?.startsWith("Basic ")) return null;

  try {
    const decoded = atob(authorization.slice(6));
    const separator = decoded.indexOf(":");
    if (separator < 0) return null;
    return [decoded.slice(0, separator), decoded.slice(separator + 1)];
  } catch {
    return null;
  }
}

export function proxy(request: NextRequest) {
  const expectedUser = process.env.AUTHOR_USERNAME?.trim();
  const expectedPassword = process.env.AUTHOR_PASSWORD;

  // The Author surface holds a service-role client. Missing protection must
  // fail closed instead of silently deploying a public control plane.
  if (!expectedUser || !expectedPassword) {
    return new NextResponse("Author access is not configured.", {
      status: 503,
      headers: { "Cache-Control": "no-store" },
    });
  }

  const credentials = readBasicCredentials(request);
  if (
    !credentials ||
    !constantTimeEqual(credentials[0], expectedUser) ||
    !constantTimeEqual(credentials[1], expectedPassword)
  ) {
    return new NextResponse("Authentication required.", {
      status: 401,
      headers: {
        "Cache-Control": "no-store",
        "WWW-Authenticate": 'Basic realm="The Observance Director", charset="UTF-8"',
      },
    });
  }

  const response = NextResponse.next();
  response.headers.set("Cache-Control", "no-store");
  response.headers.set("Vary", "Authorization");
  return response;
}

export const config = {
  matcher: ["/author/:path*"],
};
