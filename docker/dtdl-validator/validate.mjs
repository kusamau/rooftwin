// Validates a set of DTDL models with Microsoft's DTDLParser (compiled to WebAssembly and
// packaged by @digicatapult/dtdl-parser). All models are parsed together, so cross-references
// (Component schemas, Relationship targets) are resolved and checked as well.
//
//   node validate.mjs <file|directory|glob> ...
//
// Exit codes: 0 = all valid, 1 = invalid model(s), 2 = usage or I/O error.
import fs from "node:fs";
import path from "node:path";
import { getInterop } from "@digicatapult/dtdl-parser";

function expand(arg) {
  // The shell on the host cannot expand a glob that only exists inside the container
  // (e.g. /w/*.json), so it arrives here verbatim. Support the simple "dir/*.json" form.
  if (arg.includes("*")) {
    const dir = path.dirname(arg);
    const re = new RegExp("^" + path.basename(arg).replace(/[.+^${}()|[\]\\]/g, "\\$&").replace(/\*/g, ".*") + "$");
    return fs.existsSync(dir) ? fs.readdirSync(dir).filter((f) => re.test(f)).map((f) => path.join(dir, f)) : [];
  }
  if (fs.existsSync(arg) && fs.statSync(arg).isDirectory()) {
    return fs.readdirSync(arg).filter((f) => f.endsWith(".json")).map((f) => path.join(arg, f));
  }
  return fs.existsSync(arg) ? [arg] : [];
}

const args = process.argv.slice(2);
if (args.length === 0) {
  console.error("usage: dtdl-validator <file|directory|glob> ...");
  process.exit(2);
}
const files = [...new Set(args.flatMap(expand))].sort();
if (files.length === 0) {
  console.error(`no .json files found in: ${args.join(" ")}`);
  process.exit(2);
}

const models = [];
for (const f of files) {
  try {
    models.push(JSON.parse(fs.readFileSync(f, "utf-8")));
  } catch (e) {
    console.error(`✗ ${f}: not valid JSON (${e.message})`);
    process.exit(1);
  }
}

const parser = await getInterop();
console.log(parser.parserVersion());
try {
  const entities = JSON.parse(parser.parse(JSON.stringify(models)));
  const interfaces = Object.values(entities).filter((e) => e.EntityKind === "Interface");

  // DTDL Relationships are "by reference": the parser accepts a target that names an Interface
  // nobody defined. In this repo every target must be one of our own models, so check it here.
  const known = new Set(interfaces.map((i) => i.Id));
  const dangling = Object.values(entities)
    .filter((e) => e.EntityKind === "Relationship" && e.target && !known.has(e.target))
    .map((e) => `${e.DefinedIn} -> ${e.name} -> ${e.target}`);
  if (dangling.length > 0) {
    console.error("✗ Relationship targets that no model in this set defines:");
    for (const d of dangling) console.error(`  ${d}`);
    process.exit(1);
  }

  for (const f of files) console.log(`✓ ${f}`);
  console.log(`OK: ${files.length} files, ${interfaces.length} interfaces, ${Object.keys(entities).length} elements`);
  process.exit(0);
} catch (e) {
  let detail = e;
  try {
    detail = JSON.parse(e.message ?? String(e));
  } catch {
    /* not JSON: print as is */
  }
  console.error("✗ DTDL validation failed:");
  console.error(typeof detail === "string" ? detail : JSON.stringify(detail, null, 2));
  process.exit(1);
}
