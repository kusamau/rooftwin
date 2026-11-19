# dtdl-validator

Validates the DTDL v3 models in `models/dtdl/` with Microsoft's reference parser
(`DTDLParser`, compiled to WebAssembly and packaged as `@digicatapult/dtdl-parser`).

```bash
docker build -t dtdl-validator docker/dtdl-validator
docker run --rm -v "$PWD/models/dtdl:/w:ro" dtdl-validator /w          # a directory
docker run --rm -v "$PWD/models/dtdl:/w:ro" dtdl-validator '/w/*.json' # a glob (quote it)
```

All files are parsed **together**, so Component schemas and `extends` must resolve within the set.
On top of the parser, `validate.mjs` adds one repo-specific rule: every Relationship `target` must
be an Interface defined in the set. DTDL itself allows dangling targets (relationships are
"by reference"), which is right for a platform and wrong for a closed model like ours.

Why quote the glob: an unquoted `/w/*.json` is expanded by the *host* shell, where `/w` does not
exist. The script expands simple `dir/*.json` patterns itself.

Exit codes: `0` valid, `1` invalid model, `2` usage or I/O error.
