import { fileURLToPath } from 'node:url';

/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  distDir: process.env.OBSERVANCE_NEXT_DIST_DIR || '.next',
  turbopack: {
    root: fileURLToPath(new URL('..', import.meta.url)),
  },
  outputFileTracingIncludes: {
    '/the-hold/the-hold.zip': [
      './content/the-hold-v5/the-hold.zip',
      './content/the-hold-v5/the-hold.sha1',
    ],
    '/support/cases/mossfield-recovery/media/[mediaKey]': [
      './src/lib/morrow-media-assets/*.ogg',
    ],
  },
};

export default nextConfig;
