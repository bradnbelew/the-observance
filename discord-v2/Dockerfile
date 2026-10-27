# The Discord bridge on Railway (or any Docker host). Build context = the repository root, so Theo's
# voice notes (dashboard/private-media/theo) ship with the bot. See README.md, "Railway".
FROM node:22-alpine
WORKDIR /app
COPY discord-v2/package.json discord-v2/package-lock.json discord-v2/
RUN npm ci --omit=dev --prefix discord-v2
COPY discord-v2/bot.mjs discord-v2/canon.json discord-v2/
COPY dashboard/private-media/ dashboard/private-media/
ENV STATE_DIR=/data
CMD ["node", "discord-v2/bot.mjs"]
