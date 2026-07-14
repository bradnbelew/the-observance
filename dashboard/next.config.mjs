/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  outputFileTracingIncludes: {
    '/the-hold/the-hold.zip': [
      './content/the-hold-v5/the-hold.zip',
      './content/the-hold-v5/the-hold.sha1',
    ],
  },
};

export default nextConfig;
