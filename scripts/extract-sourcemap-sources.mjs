/**
 * Extract source files from a source map's sourcesContent.
 *
 * Reads a (flat) source map and writes each entry from the `sources` /
 * `sourcesContent` arrays to disk under the given output directory,
 * preserving the original relative paths.
 *
 * Usage:  node scripts/extract-sourcemap-sources.mjs <source-map-file> <output-dir>
 */

import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";

const file = process.argv[2];
const outDir = process.argv[3];

if (!file || !outDir) {
    console.error(
        "Usage: node scripts/extract-sourcemap-sources.mjs <source-map-file> <output-dir>",
    );
    process.exit(1);
}

const raw = JSON.parse(readFileSync(file, "utf8"));
const sources = raw.sources || [];
const sourcesContent = raw.sourcesContent || [];

let count = 0;
for (let i = 0; i < sources.length; i++) {
    const content = sourcesContent[i];
    if (content == null) continue;

    const outPath = join(outDir, sources[i]);
    mkdirSync(dirname(outPath), { recursive: true });
    writeFileSync(outPath, content);
    count++;
}

console.log(`${file}: extracted ${count} source files to ${outDir}`);
