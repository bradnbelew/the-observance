# Fonts

satori cannot read system fonts — it needs raw font files. Drop these here:

- `body-regular.ttf` — serif, weight 400 (maps to `brand.fonts.body`)
- `body-bold.ttf` — serif, weight 700
- `mono-regular.ttf` — monospace, weight 400 (maps to `brand.fonts.mono`)

Filenames are configured in `src/render/fonts.ts` (`SPECS`). Change them there if
you ship different files. Any `.ttf`/`.otf` works.

Font files are **not** git-ignored by default (this `.gitignore` only ignores
`.env`, `node_modules/`, `out/`, `dist/`). Commit the fonts if the renders must be
reproducible in CI; otherwise add `assets/fonts/*.ttf` to `.gitignore` yourself
(mind any license that forbids redistribution).
