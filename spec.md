# archidata — spécifications

## Livraison (`./deliver`)

- La version est pilotée par `version.txt` : `X.Y.Z-dev` sur `develop`, `X.Y.Z` sur `main`.
- `./deliver` : fast-forward de `main` sur `develop` (jamais de commit de merge), commit
  `[RELEASE] Release vX.Y.Z`, tag `vX.Y.Z`, puis ouverture de la version `-dev` suivante sur
  `develop`. Rien n'est poussé. L'état de la release en cours est dans `.git/deliver-state.json`.
- `./deliver deploy` : lance `mvn deploy` sur `main` (le commit de release) et note
  `deployed_at` dans l'état.
- `./deliver --deploy` : release, puis `mvn deploy`, puis push **uniquement si le deploy a
  réussi** (un tag poussé sans artefact publié annoncerait une version inutilisable).
  - Échec du deploy : la release reste locale, rien n'est poussé.
  - Préconditions vérifiées avant toute modification : `pom.xml`, `mvn` dans le PATH, remote.
- Une release déployée ne se `revert` plus sans `--force` : le dépôt Maven (Maven Central via
  `central-publishing-maven-plugin`, signature GPG) détient déjà ce numéro de version.
- Le script `deliver` est dupliqué à l'identique dans les autres dépôts kangaroo_and_rabbit
  (jimbe, karentory, karideo, karoto, karso, karusic).
