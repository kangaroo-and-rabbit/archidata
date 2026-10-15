Graph generation (Graphviz DOT and draw.io)
===========================================

Archidata can draw your API: the REST resources, the data models they exchange,
the enums, the inheritance between models and the foreign keys. Two generators
produce a diagram from the same introspection (`AnalyzeApi`) as the
[OpenAPI](openapi_generation.md), [TypeScript](typescript_api_generation.md) and
[Python](python_api_generation.md) generators:

| Generator | Output | Use |
|---|---|---|
| `DotGenerateApi` | a Graphviz `.dot` text file | rendered to SVG/PNG/PDF by the `dot` tool, for a documentation site or a CI artifact |
| `DrawioGenerateApi` | a `.drawio` file (mxGraph XML) | opened and edited in draw.io / diagrams.net |

Neither is needed at runtime. They are documentation tools.


When to use them
----------------

Use the draw.io output to give a newcomer a map of the back-end that they can
also move around and annotate, and to commit an `api.drawio` next to the code
(karideo regenerates `../doc/api.drawio` at each run of its `GenerateApi`).
Use the DOT output when the diagram must be produced by a script without any
GUI.

Do not use them to document a field-by-field contract: the generated
OpenAPI/TypeScript/Python artefacts are the contract, the diagram is a picture
of it. A big API produces a big picture; split it by registering only a subset
of the resources in a dedicated `AnalyzeApi`.


Minimal example
---------------

```java
final AnalyzeApi api = new AnalyzeApi();
api.addAllApi(List.of(UserResource.class, ArticleResource.class));   // resources and the models they use
api.addModel(PartRight.class);                                       // a model no endpoint references

DrawioGenerateApi.generateApi(api, "doc/api.drawio");
DotGenerateApi.generateApi(api, "target/api.dot");
```

```bash
dot -Tsvg target/api.dot -o target/api.svg
```

The `AnalyzeApi` is the one you already build for the other generators: reuse
the same instance. Karideo, karentory and karoto call `DrawioGenerateApi` from the
`generateObjects()` method of their `Initialization` class, run by a small
`GenerateApi` main. No application calls `DotGenerateApi`; only a test does.


draw.io: `DrawioGenerateApi`
----------------------------

| Method | Content |
|---|---|
| `generateApi(AnalyzeApi, String path)` | models and REST endpoints |
| `generateModels(AnalyzeApi, String path)` | data models and enums only |
| `generateRestApi(AnalyzeApi, String path)` | REST resources only |
| `generateApiString(AnalyzeApi)` | the XML of the combined diagram as a `String`, nothing written (used by the tests) |

All methods are `static` and may throw `Exception`; the file variants create the
missing parent directories.

What you get:

- Blue boxes for the data models (name, then one line per field), green boxes
  for the enums (their values), red boxes for the REST resources (one line per
  endpoint: `GET /path -> ReturnType`).
- Edges: inheritance (hollow triangle arrow), relations between models (filled
  diamond, drawn from the `targetEntity` of `@ManyToOneDoc`, `@OneToManyDoc`
  and `@ManyToManyDoc`, labelled with the field name), associations, REST endpoint to the model it takes or returns
  (dashed red), and `@CheckForeignKey` links (dashed purple); a foreign key
  without a relation annotation also shows ` -> Target` on the field.
- The layout is computed for you: inheritance trees are laid out as trees, then
  the blocks (a tree, a standalone class, an enum, a REST group) are placed with
  a force-directed algorithm and pushed apart to remove overlaps. The result is
  readable but not final; move boxes in draw.io, or accept that a regeneration
  overwrites the manual layout.
- Primitive wrappers, `String`, `Object`, `void` are not drawn as boxes.


Graphviz: `DotGenerateApi`
--------------------------

One method: `DotGenerateApi.generateApi(AnalyzeApi api, String pathDotFile)`
(`static`, throws `Exception`).

The file is a `digraph UML_Class_diagram`, titled "Rest API server Model",
where each REST resource and each model is a table-shaped node (red border for a
resource, black for a model). The edges go from an endpoint or a field port to
the type it uses, inheritance is a hollow arrow, and a field that refers to a
model is an edge to that model. Render it with any layout engine of Graphviz:

```bash
dot -Tsvg api.dot -o api.svg        # hierarchical layout
neato -Tpng api.dot -o api.png      # spring layout, often better for many models
```

The generator also adds `RestErrorResponse` to the analyzed API (the
[error body](error_handling.md) of every endpoint), so it appears in the
diagram.


Reference
---------

| Class | Visibility | Role |
|---|---|---|
| `DrawioGenerateApi` | public, static utility | `generateApi`, `generateModels`, `generateRestApi`, `generateApiString` |
| `DotGenerateApi` | public, static utility | `generateApi` |
| `AnalyzeApi` | public | `addApi`, `addAllApi`, `addModel`, `addAllModel`: what to draw |
| `externalRestApi.dot.*` (`DotApiGeneration`, `DotClassElement`, `DotClassElementGroup`) | internal | the DOT rendering of resources and models. Not meant to be called directly |
| `externalRestApi.model.*` | internal | the neutral model shared by every generator |

The `externalRestApi.dot` package is purely internal: it is the implementation
of `DotGenerateApi`. Use the facade.


Pitfalls
--------

- `DotGenerateApi.generateApi` opens the file with a plain `FileWriter` and
  does **not** create the parent directory: create it first or you get a
  `FileNotFoundException`. `DrawioGenerateApi` does create it.
- Both overwrite the target without asking, and neither is incremental: the
  files are not protected by the output guard (`GenerationWriter`) that the
  TypeScript and Python generators use against system directories.
- `DotGenerateApi` modifies the `AnalyzeApi` you pass (it adds
  `RestErrorResponse`). If you reuse the same instance for a later generator,
  that model is already registered; no harm, but not neutral.
- Only the models reachable from the registered resources, plus the ones you add
  with `addModel` / `addAllModel`, are drawn. A model used nowhere has to be
  added explicitly.
- A very large API gives a draw.io file that is slow to open and a DOT graph
  that Graphviz lays out slowly: draw only the resources you want to show.
- The graphs are regenerated from the code at every run. Do not hand-edit a
  generated `.dot`; hand edits of a `.drawio` are lost at the next generation.
