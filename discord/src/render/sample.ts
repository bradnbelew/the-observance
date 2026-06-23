/**
 * `npm run sample` — render sample clue PNGs into ./out so the card designs can
 * be eyeballed without running the bot or touching Discord/Supabase. Pure local
 * render: no network.
 *
 * It covers BOTH card families:
 *   1. The plain brand-frame whisper cards (`clueCard`, src/render/cards.ts) —
 *      the simple tiered hint card the bot attaches to a /whisper reply.
 *   2. The forged rune-cipher artifacts (forge/templates) — the five themed
 *      clue templates, each carving real ciphertext as the keepers' runes.
 *
 * The forge self-tests run first (decode(encode(x)) === x for every cipher, and
 * the rune alphabet is a clean 1:1 substitution); a failure aborts before any
 * PNG is written, so a broken cipher can never ship a misleading sample.
 */
import { writeFile, mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';
import { brand } from '../brand.js';
import { clueCard, type ClueCardData } from './cards.js';
import { OUT_DIR, renderToFile } from './render.js';
import { forgeSelfTest } from '../forge/index.js';
import { renderClueDetailed, type ClueRenderSpec } from '../forge/templates/index.js';

// --- 1. Plain brand-frame whisper cards -----------------------------------

const WHISPER_CARDS: Array<{ filename: string; data: ClueCardData }> = [
  {
    filename: 'whisper-tier1.png',
    data: {
      eyebrow: 'Whisper · Act I',
      puzzleKey: 'obsidian-gate',
      tier: 1,
      body: 'you are tired. look again at what repeats — those are not stone. they are sounds.',
      footer: '2 whispers remain',
    },
  },
  {
    filename: 'whisper-tier2.png',
    data: {
      eyebrow: 'Whisper · Act I',
      puzzleKey: 'obsidian-gate',
      tier: 2,
      body: 'four corners, four pillars. the frame is older than the door.',
      footer: '1 whisper remains',
    },
  },
  {
    filename: 'whisper-tier3.png',
    data: {
      eyebrow: 'Whisper · Act II',
      puzzleKey: 'the-ledger',
      tier: 3,
      body: 'read the ledger backward. the first name was written last.',
      footer: 'no whispers remain',
    },
  },
];

// --- 2. Forged rune-cipher artifacts (the five themed templates) ----------

const CLUE_ARTIFACTS: Array<{ filename: string; spec: ClueRenderSpec }> = [
  {
    filename: 'clue-rune-cipher.png',
    spec: {
      template: 'runeCipherCard',
      clue: { cipher: 'caesar', text: 'BOW AT THE MARKER', shift: 7, namespace: 'act1' },
      eyebrow: 'Cipherwork · Act I',
      title: 'The Keepers’ Script',
    },
  },
  {
    filename: 'clue-redacted-dossier.png',
    spec: {
      template: 'redactedDossier',
      clue: { cipher: 'vigenere', text: 'THE GATE OPENS AT DUSK', key: 'OBSIDIAN', namespace: 'act2' },
      eyebrow: 'Archive · Restricted',
      title: 'Recovered Dossier',
      dossier: [
        'SUBJECT: [[the keeper]] — last seen near the [[obsidian gate]].',
        'NOTE: the marks below name the [[hour]]. Do not read them aloud.',
        'CLEARANCE: [[Archivist]] only. Everything else is struck.',
      ],
      footer: 'stamped by the Archivist',
    },
  },
  {
    filename: 'clue-map-fragment.png',
    spec: {
      template: 'mapFragment',
      clue: { cipher: 'coord', coord: { x: -1280, z: 64 }, namespace: 'act2' },
      eyebrow: 'Surveyor’s Fragment · Act II',
      title: 'Charted Ground',
      place: 'where the river forgets its name',
      lines: ['The marks below number a place. Sign first, then count.'],
    },
  },
  {
    filename: 'clue-parchment.png',
    spec: {
      template: 'parchmentCard',
      clue: { cipher: 'atbash', text: 'FIRST IS LAST', namespace: 'act1' },
      eyebrow: 'Field Note · Act I',
      title: 'A mark in the stone',
      lines: ['What follows was carved, not written.', 'Read the script reversed.'],
    },
  },
  {
    filename: 'clue-journal.png',
    spec: {
      template: 'journalPage',
      clue: {
        cipher: 'book',
        text: 'COUNT THE PAGES',
        book: 'count the silent pages where the keepers hid their oldest customs and watch the doors',
        namespace: 'act3',
      },
      eyebrow: 'From the Keeper’s Diary · Act III',
      title: 'An entry, half-burned',
      lines: ['I write what I dare not say aloud.', 'They watch the doors, not the page.'],
    },
  },
];

async function main(): Promise<void> {
  // Prove the forge before rendering anything that depends on it.
  const tests = forgeSelfTest();
  console.log(`forge self-tests passed (${tests.passed}):`);
  for (const c of tests.cases) console.log(`  ✓ ${c}`);

  await mkdir(OUT_DIR, { recursive: true });

  // 1. plain brand-frame whisper cards
  const { clueWidth, clueHeight } = brand.canvas;
  for (const { filename, data } of WHISPER_CARDS) {
    const path = await renderToFile(clueCard(data), clueWidth, clueHeight, filename);
    console.log(`rendered ${path}`);
  }

  // 2. forged rune-cipher artifacts
  for (const { filename, spec } of CLUE_ARTIFACTS) {
    const { png, template, forged } = await renderClueDetailed(spec);
    const outPath = resolve(OUT_DIR, filename);
    await writeFile(outPath, png);
    console.log(
      `rendered ${outPath}  [${template}]  solution="${forged.solution}"  key=${forged.puzzleKey}`,
    );
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
