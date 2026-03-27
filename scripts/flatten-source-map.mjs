/**
 * Flatten an indexed (sectioned) source map into a standard source map.
 *
 * Shadow-cljs emits indexed source maps (using the V3 "sections" format)
 * because it concatenates individually processed npm modules with the
 * Closure Compiler output.  Sentry's server-side source-map processing
 * has known issues with indexed source maps, leading to incorrect line
 * numbers in stack traces.  Flattening eliminates the sections and
 * produces a single mappings string that every tool handles correctly.
 *
 * Usage:  node scripts/flatten-source-map.mjs <path-to-source-map>
 */

import { readFileSync, writeFileSync } from "node:fs";
import { SourceMapConsumer, SourceMapGenerator } from "source-map";

const file = process.argv[2];
if (!file) {
    console.error(
        "Usage: node scripts/flatten-source-map.mjs <source-map-file>",
    );
    process.exit(1);
}

const raw = JSON.parse(readFileSync(file, "utf8"));

if (!raw.sections) {
    console.log(`${file}: already a flat source map, nothing to do.`);
    process.exit(0);
}

console.log(
    `${file}: indexed source map with ${raw.sections.length} sections — flattening…`,
);

const consumer = new SourceMapConsumer(raw);
const generator = new SourceMapGenerator({ file: raw.file || "" });

consumer.eachMapping((m) => {
    const mapping = {
        generated: { line: m.generatedLine, column: m.generatedColumn },
    };
    if (
        m.source != null &&
        m.originalLine != null &&
        m.originalColumn != null
    ) {
        mapping.source = m.source;
        mapping.original = { line: m.originalLine, column: m.originalColumn };
        if (m.name != null) mapping.name = m.name;
    }
    generator.addMapping(mapping);
});

consumer.sources.forEach((source) => {
    try {
        const content = consumer.sourceContentFor(source, true);
        if (content != null) generator.setSourceContent(source, content);
    } catch {
        // some sources (e.g. synthetic Closure modules) may lack content
    }
});

const flat = generator.toString();
writeFileSync(file, flat);
console.log(
    `${file}: flattened (${raw.sections.length} sections → flat, ${flat.length} bytes)`,
);
