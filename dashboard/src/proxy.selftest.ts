import { NextRequest } from "next/server";
import { config, proxy } from "./proxy";

function check(condition: boolean, message: string): void {
  if (!condition) throw new Error(`proxy self-test failed: ${message}`);
}

function request(authorization?: string): NextRequest {
  return new NextRequest("https://observance.invalid/author", {
    headers: authorization ? { authorization } : undefined,
  });
}

function basic(user: string, password: string): string {
  return `Basic ${Buffer.from(`${user}:${password}`, "utf8").toString("base64")}`;
}

const previousUser = process.env.AUTHOR_USERNAME;
const previousPassword = process.env.AUTHOR_PASSWORD;

try {
  check(JSON.stringify(config.matcher) === JSON.stringify(["/author/:path*"]),
    "proxy matcher must protect only /author and leave public/static assets untouched");
  delete process.env.AUTHOR_USERNAME;
  delete process.env.AUTHOR_PASSWORD;
  check(proxy(request()).status === 503, "missing credentials must fail closed");

  process.env.AUTHOR_USERNAME = "director";
  process.env.AUTHOR_PASSWORD = "correct horse battery staple";

  const missing = proxy(request());
  check(missing.status === 401, "missing Authorization must be rejected");
  check(missing.headers.get("cache-control") === "no-store", "rejections must not be cached");
  check(missing.headers.has("www-authenticate"), "rejection must issue a Basic challenge");

  check(proxy(request("Bearer nope")).status === 401, "non-Basic schemes must be rejected");
  check(proxy(request("Basic !!!")).status === 401, "malformed Basic values must be rejected");
  check(proxy(request(basic("director", "wrong"))).status === 401, "wrong password must be rejected");
  check(proxy(request(basic("wrong", "correct horse battery staple"))).status === 401,
    "wrong username must be rejected");

  const accepted = proxy(request(basic("director", "correct horse battery staple")));
  check(accepted.status === 200, "valid credentials must pass through");
  check(accepted.headers.get("cache-control") === "no-store", "authorized responses must not be cached");
  check(accepted.headers.get("vary") === "Authorization", "authorized responses must vary by credentials");

  console.log("proxy self-test: OK - /author fails closed and accepts only exact Basic credentials");
} finally {
  if (previousUser === undefined) delete process.env.AUTHOR_USERNAME;
  else process.env.AUTHOR_USERNAME = previousUser;
  if (previousPassword === undefined) delete process.env.AUTHOR_PASSWORD;
  else process.env.AUTHOR_PASSWORD = previousPassword;
}
