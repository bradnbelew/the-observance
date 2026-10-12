import { fileURLToPath } from 'node:url';

/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  distDir: process.env.OBSERVANCE_NEXT_DIST_DIR || '.next',
  typescript: { tsconfigPath: process.env.OBSERVANCE_TSCONFIG || 'tsconfig.json' },
  async rewrites() {
    return [
      { source: '/~:user', destination: '/u/:user' },
      { source: '/~:user/:path*', destination: '/u/:user/:path*' },
    ];
  },
  // Gated routes read private-media/ at runtime; make sure it ships with them.
  outputFileTracingIncludes: {
    '/desk-file/[token]': ['./private-media/files/**'],
    '/desk-voice/[token]': ['./private-media/theo/**'],
    '/media/[...path]': ['./private-media/**'],
    '/ftp/[[...path]]': ['./private-media/**'],
  },
  turbopack: {
    root: fileURLToPath(new URL('..', import.meta.url)),
  },
};

export default nextConfig;
