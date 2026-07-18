import nextCoreWebVitals from "eslint-config-next/core-web-vitals";

const config = [
  ...nextCoreWebVitals,
  {
    ignores: [".next/**", ".next-*/**", "node_modules/**", "public/**"],
  },
];

export default config;
