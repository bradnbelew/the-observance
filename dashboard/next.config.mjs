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
  turbopack: {
    root: fileURLToPath(new URL('..', import.meta.url)),
  },
};

export default nextConfig;
