/**
 * satori (JSX-free) -> SVG -> PNG (resvg) rendering core.
 *
 * We build satori's virtual nodes with plain object literals (the
 * `{ type, props }` shape satori accepts) so this project needs no JSX/TSX
 * toolchain. Each helper returns a PNG Buffer ready to attach to a Discord
 * message or write to ./out.
 */
import { writeFile, mkdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import satori from 'satori';
import { Resvg } from '@resvg/resvg-js';
import { brand } from '../brand.js';
import { loadFonts } from './fonts.js';

/** A satori virtual node (no JSX). */
export interface VNode {
  type: string;
  props: Record<string, unknown> & { children?: VNode | string | Array<VNode | string> };
}

/** Tiny element helper — keeps card markup readable without JSX. */
export function el(
  type: string,
  style: Record<string, unknown>,
  children?: VNode | string | Array<VNode | string>,
): VNode {
  return { type, props: { style, children } };
}

const here = dirname(fileURLToPath(import.meta.url));
/** discord/out (src/render -> ../../out). */
export const OUT_DIR = resolve(here, '../../out');

/** Render a satori node tree to a PNG Buffer at the given pixel size. */
export async function renderPng(
  node: VNode,
  width: number,
  height: number,
): Promise<Buffer> {
  const fonts = await loadFonts();
  const svg = await satori(node as never, { width, height, fonts });
  const resvg = new Resvg(svg, {
    fitTo: { mode: 'width', value: width },
    background: brand.colors.ink,
  });
  return resvg.render().asPng();
}

/** Render and write a PNG into ./out, creating the dir if needed. Returns path. */
export async function renderToFile(
  node: VNode,
  width: number,
  height: number,
  filename: string,
): Promise<string> {
  const png = await renderPng(node, width, height);
  const outPath = resolve(OUT_DIR, filename);
  await mkdir(dirname(outPath), { recursive: true });
  await writeFile(outPath, png);
  return outPath;
}
