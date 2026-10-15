# Possible improvements

Problems found in archidata while overhauling the fronts that use it (October 2026).

## TypeScript client generator

- **Empty PATCH models.** The generated `NV…Update` models of the PATCH requests are `zod.object({})`:
  the fronts can not type their update bodies. karentory works around it with `src/utils/patch.ts`
  (`patchBody`), to remove once the generator emits the fields.
- **The global error callback gets the `Response` only** (`rest-tools.ts`, `setErrorApiGlobalCallback`).
  The fronts can not tell which token a failing request carried, so a late 401 for an old token looks
  like a 401 for the current one; they ask the server again about the current token before dropping it.
  Passing the request (or at least the token it carried) to the callback would remove that re-check.

## Java

- archidata (and jimbe) target Java 25 while the applications are on Java 26; Java 27 is out (September
  2026) and Java 26 gets no more updates.
