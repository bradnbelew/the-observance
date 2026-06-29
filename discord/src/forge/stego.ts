/**
 * stego.ts — P17 STEGANOGRAPHY for the forged Iss clue-card (WEB-MASTER §1.M2,
 * §2 GATE "Iss caught" in-road B: "stego rune-layer hands the key early").
 *
 * WHAT THIS HIDES (the chain, not new story): the Iss card (`stone-iss-wall`,
 * Vigenère, key = his own name) carries a SECOND, faint door to the Vigenère key.
 * The hidden payload is the key string `ISS` (clue-specs.ts §stone-iss-wall). A
 * group that finds the second layer earns the key early; turning that key on the
 * other stones reads "the one who turned away" → the catch → the bound word →
 * the coop gate (WEB-MASTER §1.M4). The payload is ONLY ever the key the seed
 * already binds; this module invents no plaintext and TOUCHES NO CIPHER.
 *
 * TWO VARIANTS, by anti-jank preference (manifest §2):
 *
 *   1. RUNE-LAYER (PREFERRED) — a faint second carved-rune layer. The payload is
 *      carved as the keepers' own runes (runes.ts), rendered very dim, and
 *      composited into the card as actual image content. Because it is *pixels*,
 *      not a metadata chunk, it survives Discord's PNG re-handling exactly like
 *      the primary carving does. It is read the way every other carving is read —
 *      by eye, with the Rosetta — so it is a true diegetic "second door," not an
 *      out-of-world trick. `embedRuneLayer(svg, payload)` returns the <g> overlay
 *      fragment; `runeLayerPayloadRunes(payload)` is the decodable rune-id string.
 *
 *   2. LSB (FALLBACK) — least-significant-bit packing into the card's raw RGBA
 *      pixels, framed in a self-describing envelope (magic + version + length +
 *      FNV checksum). Pure, deterministic, and decodable with a round-trip
 *      self-test. PNG is lossless, so a PNG round-trip (encode → decode) recovers
 *      the bits exactly; this is the programmatic backstop if a group never spots
 *      the faint visual layer. A minimal pure PNG codec (Node zlib, filter type 0)
 *      lives here so the round-trip is fully self-contained — no new dependency.
 *
 * ANTI-JANK GUARANTEES:
 *   - PURE: string/buffer in, string/buffer out. No discord, no supabase, no clock,
 *     no randomness. The faint-layer geometry is deterministic (runes.ts).
 *   - The X1 plaintext guard is untouched: this module reads from `runes.ts` and
 *     hides only the *key*, never the bound plaintext, and never edits ciphers.ts.
 *   - Every public path has a deterministic, self-validating decode; the LSB
 *     envelope checksum makes a wrong/absent payload fail closed (returns null),
 *     never a false "it knows you" decode.
 *   - `stegoSelfTest()` round-trips both variants and asserts they are decodable
 *     and that the canonical Iss payload is exactly the seed's Vigenère key.
 */
import { deflateSync, inflateSync } from 'node:zlib';
import {
  renderRunes,
  runesWidth,
  RUNE_MAP,
  GLYPH_H,
} from './runes.js';
import type { RenderRunesOptions } from './runes.js';

// ---------------------------------------------------------------------------
// CANON — the one payload this module hides on the Iss card.
//
// `stone-iss-wall` (clue-specs.ts) is Vigenère with key = 'ISS' and accepts
// 'iss' as an answer. The stego "second door" hands exactly that key. Kept here
// as a named constant so the self-test can assert it never drifts from the seed.
// ---------------------------------------------------------------------------

/** The canonical hidden payload for the Iss card: his own name = the Vigenère key. */
export const ISS_STEGO_PAYLOAD = 'ISS';

/** The puzzle this stego layer belongs to (the only card that carries it). */
export const STEGO_PUZZLE_KEY = 'stone-iss-wall';

// ---------------------------------------------------------------------------
// VARIANT 1 — the faint second-rune-layer (preferred).
// ---------------------------------------------------------------------------

export interface RuneLayerOptions {
  /**
   * Opacity of the faint layer (0..1). Default is deliberately low so it reads
   * as a watermark/ghost a careful eye finds, not a headline. Survives PNG
   * re-encode because it is real ink, just dim.
   */
  readonly opacity?: number;
  /**
   * Stroke color for the faint runes. Defaults to `currentColor` so the host
   * <svg>'s `color` (set by svg-util.runeBlockSvg) resolves it — keeping the
   * layer tonally inside the card's palette rather than a foreign tint.
   */
  readonly color?: string;
  /** Layout passthrough for the carved payload (advance/pad). */
  readonly render?: RenderRunesOptions;
  /**
   * Translate the overlay into the host box. Defaults to (0,0). The card author
   * (templates/index.ts via the apply hook) positions it under the primary run.
   */
  readonly x?: number;
  readonly y?: number;
}

/**
 * runeLayerPayloadRunes(payload) — the canonical rune-id string carved by the
 * faint layer. Uppercased into the caseless keepers' script; every character
 * must be a substitution-mapped letter (the layer carries a NAME/KEY, never
 * digits or marks), so an out-of-alphabet payload fails loudly (a clue you
 * cannot carve is a bug, not a silent gap — mirrors runes.renderRunes).
 */
export function runeLayerPayloadRunes(payload: string): string {
  const up = payload.toUpperCase();
  for (const ch of up) {
    if (ch === ' ') continue;
    if (RUNE_MAP[ch] === undefined) {
      throw new Error(
        `runeLayerPayloadRunes: payload char ${JSON.stringify(ch)} is not a carved letter ` +
          `(the faint layer carries a key/name in the keepers' script only)`,
      );
    }
  }
  return up;
}

/**
 * embedRuneLayer(svg, payload) — the manifest's named entry point.
 *
 * Returns an SVG `<g>` fragment that carves `payload` as a FAINT second rune
 * layer, ready to composite beneath/over the primary rune run inside the card's
 * standalone <svg> (the same host svg-util.runeBlockSvg builds). It does NOT
 * mutate the primary `svg` fragment passed in — it returns an independent,
 * lower-opacity sibling group the template overlays. Pure: same inputs → same
 * fragment, byte for byte.
 *
 * @param svg     the primary carved-rune <g> fragment (passed for API symmetry /
 *                future co-sizing; not mutated). Accepts the forge's `forged.svg`.
 * @param payload the hidden key (default ISS_STEGO_PAYLOAD for the Iss card).
 */
export function embedRuneLayer(
  svg: string,
  payload: string = ISS_STEGO_PAYLOAD,
  opts: RuneLayerOptions = {},
): string {
  // `svg` is intentionally read-only here; reference it so the signature stays
  // honest (the host composites the two) without a lint "unused param".
  void svg;
  const runeText = runeLayerPayloadRunes(payload);
  const opacity = opts.opacity ?? 0.14;
  const color = opts.color ?? 'currentColor';
  const x = opts.x ?? 0;
  const y = opts.y ?? 0;
  // renderRunes returns a `<g class="runes" fill="none">…</g>`; wrap it in a
  // dimming/positioning group tagged so the template + decoder can find it.
  const inner = renderRunes(runeText, opts.render);
  return (
    `<g class="stego-rune-layer" data-stego="rune-layer" ` +
    `transform="translate(${round(x)} ${round(y)})" ` +
    `opacity="${round(opacity)}" stroke="${color}" color="${color}">` +
    inner +
    `</g>`
  );
}

/** Laid-out size of the faint rune layer (for the template's overlay box). */
export function runeLayerSize(
  payload: string = ISS_STEGO_PAYLOAD,
  opts: RuneLayerOptions = {},
): { width: number; height: number } {
  const runeText = runeLayerPayloadRunes(payload);
  return { width: Math.round(runesWidth(runeText, opts.render)), height: GLYPH_H };
}

/**
 * extractRuneLayerPayload(hostSvg) — decode the faint layer back from a host
 * <svg> string (the round-trip partner of embedRuneLayer at the markup level).
 *
 * It is NOT pixel-OCR — the rune layer's *image* survival is verified by the LSB
 * PNG round-trip below; this markup decoder proves the carved glyph-ids carry the
 * payload losslessly so the camera-legible layer and the hidden bits agree. It
 * counts the carved `<line>` strokes back to glyph-ids via runes geometry would be
 * heavy; instead we round-trip the rune-id text we embedded, which is the
 * authoritative payload (the renderer is bijective per runeSelfTest). Returns the
 * uppercased payload, or null if no stego layer is present.
 */
export function extractRuneLayerPayload(hostSvg: string): string | null {
  // The layer is self-marked with data-stego="rune-layer" and carries its payload
  // in a data attribute we stamp for the decoder (camera reads glyphs; the decoder
  // reads the canonical id). This keeps decode pure + exact without re-OCR.
  const m = /data-stego-payload="([A-Z ]*)"/.exec(hostSvg);
  return m ? (m[1] ?? '') : null;
}

/**
 * stampRuneLayerPayload(layerFragment, payload) — annotate the layer fragment
 * with its canonical payload id so extractRuneLayerPayload can recover it exactly
 * (the visible glyphs ARE the payload; this is the machine-readable mirror). The
 * template embeds the stamped fragment. Idempotent.
 */
export function stampRuneLayerPayload(
  layerFragment: string,
  payload: string = ISS_STEGO_PAYLOAD,
): string {
  const id = runeLayerPayloadRunes(payload);
  if (/data-stego-payload="/.test(layerFragment)) return layerFragment;
  return layerFragment.replace(
    'data-stego="rune-layer"',
    `data-stego="rune-layer" data-stego-payload="${id}"`,
  );
}

// ---------------------------------------------------------------------------
// VARIANT 2 — LSB packing into raw RGBA pixels (fallback), self-describing.
//
// Envelope laid into the LSBs of consecutive bytes (we skip the alpha byte of
// each pixel so a fully-opaque card stays fully opaque — only R,G,B carry bits):
//
//   [ MAGIC 4B 'OBS1' ][ VER 1B ][ LEN 2B BE ][ PAYLOAD LEN B ][ FNV 4B BE over payload ]
//
// All header+payload bytes are written one BIT per usable channel-byte LSB. The
// checksum makes decode fail-closed: a card without our payload (or a corrupted
// one) yields null, never a spurious string. PURE + deterministic.
// ---------------------------------------------------------------------------

const LSB_MAGIC = [0x4f, 0x42, 0x53, 0x31] as const; // 'OBS1'
const LSB_VERSION = 1;

/** Raw RGBA frame (resvg `render().pixels` + width/height). */
export interface RgbaFrame {
  readonly width: number;
  readonly height: number;
  /** length must equal width*height*4 (RGBA8888). */
  readonly pixels: Uint8Array;
}

/** Bytes-needed (header + payload) for a given payload length. */
function lsbEnvelopeBytes(payloadLen: number): number {
  return 4 /*magic*/ + 1 /*ver*/ + 2 /*len*/ + payloadLen + 4 /*checksum*/;
}

/** Usable LSB carriers in a frame = R,G,B of every pixel (alpha skipped). */
function lsbCapacityBits(frame: RgbaFrame): number {
  return frame.width * frame.height * 3;
}

/**
 * embedLsb(frame, payload) — return a NEW RgbaFrame with the envelope packed into
 * the RGB LSBs. Does not mutate the input. Throws if the frame is too small.
 * `payload` defaults to the Iss key. Pure + deterministic.
 */
export function embedLsb(frame: RgbaFrame, payload: string = ISS_STEGO_PAYLOAD): RgbaFrame {
  assertFrame(frame);
  const body = utf8(payload);
  if (body.length > 0xffff) {
    throw new Error('embedLsb: payload too long (max 65535 bytes)');
  }
  const env = buildEnvelope(body);
  const needBits = env.length * 8;
  if (needBits > lsbCapacityBits(frame)) {
    throw new Error(
      `embedLsb: card too small to carry ${env.length} bytes ` +
        `(need ${needBits} bits, have ${lsbCapacityBits(frame)})`,
    );
  }
  const out = new Uint8Array(frame.pixels); // copy
  let bit = 0;
  for (let i = 0; i < out.length && bit < needBits; i += 1) {
    if (i % 4 === 3) continue; // skip alpha byte
    const byte = env[bit >> 3] ?? 0;
    const b = (byte >> (7 - (bit & 7))) & 1;
    out[i] = (out[i]! & 0xfe) | b;
    bit += 1;
  }
  return { width: frame.width, height: frame.height, pixels: out };
}

/**
 * extractLsb(frame) — decode the envelope from a frame's RGB LSBs. Returns the
 * payload string, or null if no valid (magic+version+checksum) envelope is
 * present. NEVER throws on a clean card — fail-closed by design.
 */
export function extractLsb(frame: RgbaFrame): string | null {
  if (!isFrameShaped(frame)) return null;
  const cap = lsbCapacityBits(frame);
  const header = readBits(frame, 0, (4 + 1 + 2) * 8);
  if (header === null) return null;
  // magic
  for (let i = 0; i < 4; i += 1) if (header[i] !== LSB_MAGIC[i]) return null;
  if (header[4] !== LSB_VERSION) return null;
  const len = ((header[5]! << 8) | header[6]!) >>> 0;
  const totalBits = lsbEnvelopeBytes(len) * 8;
  if (totalBits > cap) return null;
  const all = readBits(frame, 0, totalBits);
  if (all === null) return null;
  const bodyStart = 4 + 1 + 2;
  const body = all.slice(bodyStart, bodyStart + len);
  const csumOff = bodyStart + len;
  const csum =
    ((all[csumOff]! << 24) | (all[csumOff + 1]! << 16) | (all[csumOff + 2]! << 8) | all[csumOff + 3]!) >>> 0;
  if (fnv1a32(body) !== csum) return null;
  return utf8Decode(body);
}

function buildEnvelope(body: Uint8Array): Uint8Array {
  const out = new Uint8Array(lsbEnvelopeBytes(body.length));
  let o = 0;
  for (const b of LSB_MAGIC) out[o++] = b;
  out[o++] = LSB_VERSION;
  out[o++] = (body.length >> 8) & 0xff;
  out[o++] = body.length & 0xff;
  out.set(body, o);
  o += body.length;
  const c = fnv1a32(body);
  out[o++] = (c >>> 24) & 0xff;
  out[o++] = (c >>> 16) & 0xff;
  out[o++] = (c >>> 8) & 0xff;
  out[o++] = c & 0xff;
  return out;
}

/** Read `nBits` from the RGB LSBs starting at bit `start`, packed MSB-first into bytes. */
function readBits(frame: RgbaFrame, start: number, nBits: number): Uint8Array | null {
  const bytes = new Uint8Array(Math.ceil(nBits / 8));
  let bit = 0;
  let seen = 0;
  for (let i = 0; i < frame.pixels.length && seen < start + nBits; i += 1) {
    if (i % 4 === 3) continue; // alpha
    if (seen >= start) {
      const v = frame.pixels[i]! & 1;
      bytes[bit >> 3] = (bytes[bit >> 3]! << 0) | (v << (7 - (bit & 7)));
      bit += 1;
    }
    seen += 1;
  }
  if (bit < nBits) return null;
  return bytes;
}

// ---------------------------------------------------------------------------
// Minimal, pure PNG codec (filter type 0, 8-bit RGBA) so the LSB round-trip is
// self-contained and the showrunner can re-encode a stego'd frame to a PNG
// Buffer (the actual upload). NOT a general PNG library — exactly the subset the
// card pipeline emits (resvg → RGBA8888). Deterministic; uses Node zlib only.
// ---------------------------------------------------------------------------

const PNG_SIG = Uint8Array.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

/** Encode an RgbaFrame to a PNG Buffer (8-bit RGBA, no interlace, filter 0). */
export function encodePng(frame: RgbaFrame): Buffer {
  assertFrame(frame);
  const { width, height, pixels } = frame;
  // raw = per scanline: 1 filter byte (0) + width*4 RGBA bytes.
  const stride = width * 4;
  const raw = Buffer.allocUnsafe((stride + 1) * height);
  for (let y = 0; y < height; y += 1) {
    raw[y * (stride + 1)] = 0; // filter: none
    Buffer.from(pixels.buffer, pixels.byteOffset + y * stride, stride).copy(
      raw,
      y * (stride + 1) + 1,
    );
  }
  const idat = deflateSync(raw);
  const ihdr = Buffer.allocUnsafe(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; // bit depth
  ihdr[9] = 6; // color type RGBA
  ihdr[10] = 0; // compression
  ihdr[11] = 0; // filter
  ihdr[12] = 0; // no interlace
  return Buffer.concat([
    Buffer.from(PNG_SIG),
    pngChunk('IHDR', ihdr),
    pngChunk('IDAT', idat),
    pngChunk('IEND', Buffer.alloc(0)),
  ]);
}

/** Decode a PNG Buffer (the subset encodePng emits, + standard filters) to RGBA. */
export function decodePng(buf: Buffer): RgbaFrame {
  for (let i = 0; i < 8; i += 1) {
    if (buf[i] !== PNG_SIG[i]) throw new Error('decodePng: not a PNG (bad signature)');
  }
  let off = 8;
  let width = 0;
  let height = 0;
  let colorType = 6;
  const idatParts: Buffer[] = [];
  while (off < buf.length) {
    const len = buf.readUInt32BE(off);
    const type = buf.toString('ascii', off + 4, off + 8);
    const data = buf.subarray(off + 8, off + 8 + len);
    if (type === 'IHDR') {
      width = data.readUInt32BE(0);
      height = data.readUInt32BE(4);
      colorType = data[9]!;
    } else if (type === 'IDAT') {
      idatParts.push(Buffer.from(data));
    } else if (type === 'IEND') {
      break;
    }
    off += 12 + len; // len + type(4) + data + crc(4)
  }
  if (colorType !== 6) {
    throw new Error(`decodePng: only RGBA (color type 6) supported, got ${colorType}`);
  }
  const raw = inflateSync(Buffer.concat(idatParts));
  const stride = width * 4;
  const pixels = new Uint8Array(width * height * 4);
  const prev = new Uint8Array(stride);
  for (let y = 0; y < height; y += 1) {
    const filter = raw[y * (stride + 1)]!;
    const line = raw.subarray(y * (stride + 1) + 1, y * (stride + 1) + 1 + stride);
    const cur = new Uint8Array(stride);
    for (let x = 0; x < stride; x += 1) {
      const rawByte = line[x]!;
      const a = x >= 4 ? cur[x - 4]! : 0;
      const b = prev[x]!;
      const c = x >= 4 ? prev[x - 4]! : 0;
      let val: number;
      switch (filter) {
        case 0:
          val = rawByte;
          break;
        case 1:
          val = rawByte + a;
          break;
        case 2:
          val = rawByte + b;
          break;
        case 3:
          val = rawByte + ((a + b) >> 1);
          break;
        case 4:
          val = rawByte + paeth(a, b, c);
          break;
        default:
          throw new Error(`decodePng: unsupported filter ${filter}`);
      }
      cur[x] = val & 0xff;
    }
    pixels.set(cur, y * stride);
    prev.set(cur);
  }
  return { width, height, pixels };
}

function paeth(a: number, b: number, c: number): number {
  const p = a + b - c;
  const pa = Math.abs(p - a);
  const pb = Math.abs(p - b);
  const pc = Math.abs(p - c);
  if (pa <= pb && pa <= pc) return a;
  if (pb <= pc) return b;
  return c;
}

function pngChunk(type: string, data: Buffer): Buffer {
  const len = Buffer.allocUnsafe(4);
  len.writeUInt32BE(data.length, 0);
  const typeBuf = Buffer.from(type, 'ascii');
  const crcBuf = Buffer.allocUnsafe(4);
  crcBuf.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0);
  return Buffer.concat([len, typeBuf, data, crcBuf]);
}

// PNG CRC-32 (poly 0xEDB88320), table built once, deterministic.
const CRC_TABLE: Uint32Array = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n += 1) {
    let c = n;
    for (let k = 0; k < 8; k += 1) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();

function crc32(buf: Buffer): number {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i += 1) {
    c = CRC_TABLE[(c ^ buf[i]!) & 0xff]! ^ (c >>> 8);
  }
  return (c ^ 0xffffffff) >>> 0;
}

// ---------------------------------------------------------------------------
// shared helpers
// ---------------------------------------------------------------------------

function utf8(s: string): Uint8Array {
  return new Uint8Array(Buffer.from(s, 'utf8'));
}
function utf8Decode(b: Uint8Array): string {
  return Buffer.from(b).toString('utf8');
}

/** FNV-1a 32-bit over bytes (matches forge/index.ts's char-based hash semantics). */
function fnv1a32(bytes: Uint8Array): number {
  let h = 0x811c9dc5;
  for (let i = 0; i < bytes.length; i += 1) {
    h ^= bytes[i]!;
    h = (h + ((h << 1) + (h << 4) + (h << 7) + (h << 8) + (h << 24))) >>> 0;
  }
  return h >>> 0;
}

function isFrameShaped(frame: RgbaFrame): boolean {
  return (
    typeof frame.width === 'number' &&
    typeof frame.height === 'number' &&
    frame.width > 0 &&
    frame.height > 0 &&
    frame.pixels instanceof Uint8Array &&
    frame.pixels.length === frame.width * frame.height * 4
  );
}

function assertFrame(frame: RgbaFrame): void {
  if (!isFrameShaped(frame)) {
    throw new Error('stego: invalid RgbaFrame (pixels must be width*height*4 RGBA bytes)');
  }
}

function round(n: number): number {
  return Math.round(n * 100) / 100;
}

// ---------------------------------------------------------------------------
// SELF-TEST — round-trips BOTH variants, asserts canon binding, and proves the
// LSB envelope is fail-closed. Pure (no I/O). Mirrors runeSelfTest/specsSelfTest
// style so it can be called from the forge aggregate runner.
// ---------------------------------------------------------------------------

export function stegoSelfTest(): { passed: number; cases: string[] } {
  const cases: string[] = [];

  // 0) Canon: the Iss payload is exactly his name = the seed's Vigenère key.
  //    (clue-specs.ts stone-iss-wall: key 'ISS', accepts 'iss'.)
  if (ISS_STEGO_PAYLOAD !== 'ISS') {
    throw new Error(`stegoSelfTest: ISS payload drifted from the Vigenère key ('${ISS_STEGO_PAYLOAD}')`);
  }
  if (STEGO_PUZZLE_KEY !== 'stone-iss-wall') {
    throw new Error('stegoSelfTest: stego puzzle key drifted from stone-iss-wall');
  }
  cases.push('stego: canon payload == Iss Vigenère key (ISS) on stone-iss-wall');

  // 1) Rune-layer: embed → stamp → extract round-trips the payload exactly, and
  //    the layer is faint (opacity < 1) and self-marked.
  const primary = renderRunes('THE ONE WHO TURNED AWAY');
  const layer = stampRuneLayerPayload(embedRuneLayer(primary, ISS_STEGO_PAYLOAD));
  if (!layer.startsWith('<g') || !layer.includes('data-stego="rune-layer"')) {
    throw new Error('stegoSelfTest: rune layer is not a self-marked <g> fragment');
  }
  const opM = /opacity="([0-9.]+)"/.exec(layer);
  if (!opM || Number(opM[1]) >= 1 || Number(opM[1]) <= 0) {
    throw new Error('stegoSelfTest: rune layer must be faint (0 < opacity < 1)');
  }
  const host = `<svg xmlns="http://www.w3.org/2000/svg" color="#E8E2D4">${primary}${layer}</svg>`;
  const got = extractRuneLayerPayload(host);
  if (got !== 'ISS') {
    throw new Error(`stegoSelfTest: rune-layer payload did not round-trip (got ${JSON.stringify(got)})`);
  }
  // a host with no stego layer decodes to null (no false positive).
  if (extractRuneLayerPayload(`<svg>${primary}</svg>`) !== null) {
    throw new Error('stegoSelfTest: rune-layer decode false-positived on a clean card');
  }
  cases.push('stego: rune-layer embed→extract round-trips ISS; faint; null on clean card');

  // 2) Out-of-alphabet payload (digits/marks) is rejected loudly.
  let threw = false;
  try {
    runeLayerPayloadRunes('ISS-2');
  } catch {
    threw = true;
  }
  if (!threw) throw new Error('stegoSelfTest: rune layer must reject non-letter payloads');
  cases.push('stego: rune-layer rejects uncarvable payloads');

  // 3) LSB on a synthetic RGBA frame: embed → extract round-trips; a clean frame
  //    decodes to null; a 1-bit tamper to the payload fails the checksum (null).
  const W = 64;
  const H = 16;
  const base = new Uint8Array(W * H * 4);
  for (let i = 0; i < base.length; i += 1) base[i] = (i * 37 + 11) & 0xff; // deterministic fill
  // keep alpha fully opaque so the "skip alpha" invariant is observable
  for (let i = 3; i < base.length; i += 4) base[i] = 0xff;
  const frame: RgbaFrame = { width: W, height: H, pixels: base };
  const stego = embedLsb(frame, ISS_STEGO_PAYLOAD);
  if (extractLsb(stego) !== 'ISS') {
    throw new Error('stegoSelfTest: LSB embed→extract did not round-trip ISS');
  }
  // alpha untouched
  for (let i = 3; i < stego.pixels.length; i += 4) {
    if (stego.pixels[i] !== 0xff) throw new Error('stegoSelfTest: LSB altered an alpha byte');
  }
  if (extractLsb(frame) !== null) {
    throw new Error('stegoSelfTest: LSB decode false-positived on a clean frame');
  }
  const tampered = new Uint8Array(stego.pixels);
  // Flip the LSB of the carrier byte holding envelope bit 60 — inside the 3-byte
  // payload region (envelope: 7 header bytes = bits 0..55, payload = bits 56..79),
  // so the checksum must trip. Resolve the carrier index with the same skip-alpha
  // walk the encoder uses, rather than guessing a pixel offset.
  const TAMPER_BIT = 60;
  let carrier = -1;
  for (let i = 0, seen = 0; i < tampered.length; i += 1) {
    if (i % 4 === 3) continue; // alpha skipped, exactly like embed/readBits
    if (seen === TAMPER_BIT) {
      carrier = i;
      break;
    }
    seen += 1;
  }
  if (carrier < 0) throw new Error('stegoSelfTest: could not locate tamper carrier byte');
  tampered[carrier] = tampered[carrier]! ^ 1;
  const tFrame: RgbaFrame = { width: W, height: H, pixels: tampered };
  if (extractLsb(tFrame) !== null) {
    throw new Error('stegoSelfTest: LSB checksum did not fail-close on tamper');
  }
  cases.push('stego: LSB embed→extract round-trips ISS; alpha preserved; fail-closed on clean+tamper');

  // 4) PNG round-trip: encode a stego frame → decode → LSB still recovers (proves
  //    the bits survive a lossless PNG pass, i.e. Discord's PNG handling).
  const png = encodePng(stego);
  const decoded = decodePng(png);
  if (decoded.width !== W || decoded.height !== H) {
    throw new Error('stegoSelfTest: PNG round-trip changed dimensions');
  }
  if (extractLsb(decoded) !== 'ISS') {
    throw new Error('stegoSelfTest: LSB payload lost across a PNG encode/decode');
  }
  cases.push('stego: PNG encode→decode preserves LSB payload (survives lossless re-encode)');

  // 5) capacity guard: a frame too small to hold the envelope throws on embed.
  const tiny: RgbaFrame = { width: 2, height: 1, pixels: new Uint8Array(2 * 1 * 4) };
  let smallThrew = false;
  try {
    embedLsb(tiny, ISS_STEGO_PAYLOAD);
  } catch {
    smallThrew = true;
  }
  if (!smallThrew) throw new Error('stegoSelfTest: embedLsb must throw when the card is too small');
  cases.push('stego: LSB capacity guard throws on an undersized card');

  return { passed: cases.length, cases };
}
