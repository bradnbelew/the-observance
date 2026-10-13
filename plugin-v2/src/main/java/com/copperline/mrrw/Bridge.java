package com.copperline.mrrw;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The bridge: a small signed HTTP API the website and the Discord bot use to follow the plugin.
 * Config: bridge.enabled (true), bridge.bind (0.0.0.0), bridge.port (25588).
 *
 * Every request carries X-Mrrw-Ts (unix seconds) and X-Mrrw-Sig = lowercase hex
 * HMAC-SHA256(key = stop-secret, METHOD + "\n" + PATH_WITH_QUERY + "\n" + TS + "\n" + BODY).
 * 401 on a bad signature or a clock more than 120 s off; 503 while stop-secret is the dev default.
 *
 *   GET  /state                       act, act_started, players, ready, stopped, usb, fingerprint, theo
 *   POST /event  {"kind":"desk",...}  a desk ticket was filed (see {@link Campaign})
 *   GET  /corpus?heard=&persona=&seed= a Morrow reply, biased to the persona's own fragments
 *
 * Requests are read on the HTTP threads and answered on the server thread (nothing races the game).
 */
public final class Bridge {
    private final Plugin plugin;
    private final State state;
    private final Canon canon;
    private final PlayerRoster roster;
    private final Finale finale;
    private final Campaign campaign;
    private final ChatListener chat;
    private HttpServer server;
    private ExecutorService pool;

    public Bridge(Plugin plugin, State state, Canon canon, PlayerRoster roster, Finale finale, Campaign campaign, ChatListener chat) {
        this.plugin = plugin;
        this.state = state;
        this.canon = canon;
        this.roster = roster;
        this.finale = finale;
        this.campaign = campaign;
        this.chat = chat;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("bridge.enabled", true)) {
            plugin.getLogger().info("bridge disabled (bridge.enabled: false)");
            return;
        }
        String bind = plugin.getConfig().getString("bridge.bind", "0.0.0.0");
        int port = plugin.getConfig().getInt("bridge.port", 25588);
        try {
            server = HttpServer.create(new InetSocketAddress(bind, port), 16);
        } catch (IOException e) {
            plugin.getLogger().warning("bridge could not listen on " + bind + ":" + port + ": " + e);
            return;
        }
        pool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "mrrw-bridge");
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(pool);
        server.createContext("/", this::handle);
        server.start();
        plugin.getLogger().info("bridge listening on " + bind + ":" + port + (finale.devSecret() ? " (refusing every request until stop-secret is set)" : ""));
    }

    public void stop() {
        if (server != null) server.stop(0);
        if (pool != null) pool.shutdownNow();
        server = null;
    }

    private record Resp(int status, JsonObject body) { }

    private void handle(HttpExchange ex) {
        try (ex) {
            byte[] body = ex.getRequestBody().readNBytes(65_537);
            if (body.length > 65_536) {
                send(ex, new Resp(413, error("body too large")));
                return;
            }
            String method = ex.getRequestMethod().toUpperCase(java.util.Locale.ROOT);
            URI uri = ex.getRequestURI();
            String pathWithQuery = uri.getRawPath() + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
            String ts = ex.getRequestHeaders().getFirst("X-Mrrw-Ts");
            String sig = ex.getRequestHeaders().getFirst("X-Mrrw-Sig");
            Resp r;
            try {
                r = Bukkit.getScheduler().callSyncMethod(plugin, () -> route(method, uri, pathWithQuery, ts, sig, body))
                        .get(10, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                r = new Resp(503, error("server busy"));
            } catch (Exception e) {
                plugin.getLogger().warning("bridge request failed: " + e);
                r = new Resp(500, error("internal error"));
            }
            send(ex, r);
        } catch (IOException ignored) {
            // the client went away
        }
    }

    private static void send(HttpExchange ex, Resp r) throws IOException {
        byte[] out = r.body().toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(r.status(), out.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(out);
        }
    }

    private static JsonObject error(String why) {
        JsonObject o = new JsonObject();
        o.addProperty("ok", false);
        o.addProperty("error", why);
        return o;
    }

    /** Server thread. */
    private Resp route(String method, URI uri, String pathWithQuery, String ts, String sig, byte[] body) {
        if (finale.devSecret()) return new Resp(503, error("stop-secret is the dev default"));
        String secret = plugin.getConfig().getString("stop-secret");
        if (!BridgeLogic.verify(secret, method, pathWithQuery, ts, body, sig, System.currentTimeMillis() / 1000)) {
            return new Resp(401, error("bad signature"));
        }
        String path = uri.getRawPath();
        return switch (path) {
            case "/state" -> method.equals("GET") ? new Resp(200, state()) : new Resp(405, error("GET only"));
            case "/event" -> method.equals("POST") ? event(body) : new Resp(405, error("POST only"));
            case "/corpus" -> method.equals("GET") ? corpus(uri.getRawQuery()) : new Resp(405, error("GET only"));
            default -> new Resp(404, error("no such endpoint"));
        };
    }

    private JsonObject state() {
        JsonObject o = new JsonObject();
        o.addProperty("act", state.act());
        o.addProperty("act_started", Campaign.iso(campaign.actStarted()));
        o.addProperty("players", roster.names().size());
        o.addProperty("ready", finale.ready());
        o.addProperty("stopped", finale.stopped());
        o.add("usb", usb().json());
        o.addProperty("fingerprint", finale.fingerprint());
        o.add("theo", campaign.theo());
        o.addProperty("auto_advance", campaign.autoAdvance());
        List<String> log = finale.finalLog();
        if (log != null) {
            JsonArray a = new JsonArray();
            log.forEach(a::add);
            o.add("stop_log", a);
        }
        return o;
    }

    BridgeLogic.Usb usb() {
        return BridgeLogic.readUsb(new File(plugin.getDataFolder(), "usb-votes.json"), roster.names().size());
    }

    private Resp event(byte[] body) {
        JsonObject in;
        try {
            JsonElement e = JsonParser.parseString(new String(body, StandardCharsets.UTF_8));
            if (!e.isJsonObject()) return new Resp(400, error("expected a JSON object"));
            in = e.getAsJsonObject();
        } catch (Exception e) {
            return new Resp(400, error("bad JSON"));
        }
        String kind = str(in, "kind");
        if (!"desk".equals(kind)) return new Resp(400, error("kind must be \"desk\""));
        String ref = str(in, "ref");
        if (ref == null || ref.isBlank()) return new Resp(400, error("ref is required"));
        Long replyAt;
        try {
            replyAt = Campaign.parseIso(str(in, "reply_at"));
        } catch (Exception e) {
            return new Resp(400, error("reply_at must be ISO-8601 or null"));
        }
        Campaign.DeskResult r = campaign.desk(ref, replyAt, str(in, "voice"), str(in, "subject"));
        JsonObject o = new JsonObject();
        o.addProperty("ok", true);
        o.addProperty("act", r.act());
        if (r.duplicate()) o.addProperty("duplicate", true);
        return new Resp(200, o);
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || !e.isJsonPrimitive() ? null : e.getAsString();
    }

    private Resp corpus(String rawQuery) {
        Map<String, String> q = query(rawQuery);
        String heard = q.getOrDefault("heard", "");
        String persona = q.get("persona");
        int act = state.act();
        long seed;
        try {
            seed = q.containsKey("seed") ? Long.parseLong(q.get("seed").trim()) : heard.hashCode() * 31L + act;
        } catch (NumberFormatException e) {
            return new Resp(400, error("seed must be an integer"));
        }
        JsonObject o = new JsonObject();
        JsonArray sources = new JsonArray();
        if (finale.stopped()) {  // silent after the stop
            o.addProperty("reply", "");
            o.add("sources", sources);
            o.addProperty("obeys_law", true);
            return new Resp(200, o);
        }
        chat.loadKit();
        Corpus.Reply r = canon.corpus.reply(heard, act, seed, System.currentTimeMillis() - ChatListener.FRESH_WINDOW_MS, persona);
        r.fragments().forEach(f -> sources.add(f.words() + " <- " + f.source()));
        o.addProperty("reply", r.text());
        o.add("sources", sources);
        o.addProperty("obeys_law", canon.corpus.obeysLaw(r, act));
        return new Resp(200, o);
    }

    static Map<String, String> query(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null || raw.isEmpty()) return out;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            String v = eq < 0 ? "" : pair.substring(eq + 1);
            out.putIfAbsent(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return out;
    }
}
