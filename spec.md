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

## Générateur de client TypeScript/Zod (`externalRestApi`)

- Éléments de conteneur : un élément de `List<X>` et une valeur de `Map<K, V>` sont
  `.nullable()` par défaut (Java et Jackson acceptent un `null` dans une liste : `[null]` se
  désérialise en élément `null`, et `@Valid` ne le rejette pas). Un `@NotNull` en type-use
  (`List<@NotNull X>`, `Map<K, @NotNull V>`) le rend non nul, à chaque niveau d'imbrication ;
  la clé de map n'est jamais nullable. Lu sur les champs et sur les types de retour des
  méthodes REST, pas encore sur les paramètres (corps de requête `List<…>`). Choix confirmé
  le 2026-10-15 (commit d'origine 2ff5f12, v0.47.0).
- `@ApiOptionalIsNullable` n'agit que sur une classe en `@JsonInclude(NON_NULL)` (sinon un
  champ optionnel est déjà `.nullable()`). Avec `groups`, les champs optionnels sont
  `.nullable().optional()` dans les groupes listés ; sans `groups`, dans tous les usages.
- Nom des types : `@ValidGroup(GroupX.class)` ajoute `X` au nom du type généré ; `@Valid`
  seul donne le nom de la classe ; sans `@Valid`, suffixe `NV`. Un DTO de requête dédié à un
  endpoint ne prend donc ni groupe ni `@ValidGroup` (sinon `UserCreateCreate`).
- `@jakarta.annotation.Nullable` (seule annotation `@Nullable` reconnue par le générateur)
  rend le champ optionnel même avec `@NotNull`, avec le suffixe ci-dessus : `.nullable()`, ou
  `.optional()` sous `@JsonInclude(NON_NULL)`.

## Secrets (`SecretReader`, `ConfigBaseVariable.readSecret`)

- Une variable secrète a deux formes distinctes : `<NOM>` contient une valeur, `<NOM>_FILE` un chemin. Pas de
  détection par le contenu (`/` au début, préfixe) : le nom seul doit dire si une variable est sensible, pour que
  les hooks et les contrôles n'aient jamais à lire les valeurs, et un mot de passe peut commencer par `/`.
- Le fichier gagne. Illisible ou vide : échec au démarrage, jamais de repli sur `<NOM>`. L'erreur nomme la variable
  sans sa valeur ni la cause, qui la répète : un mot de passe laissé dans `_FILE` pendant une migration ne doit pas
  atteindre les journaux.
- `SECRETS_FILE_ONLY` n'accepte que `true` ou `false` : une faute de frappe ne doit pas couper le contrôle.
- Valeur dans `<NOM>` : WARN avec le nom seul ; refus de démarrer si `SECRETS_FILE_ONLY=true`, même à côté d'un
  fichier, car elle reste exposée dans l'environnement.
- Texte : retours à la ligne de fin retirés. Binaire (`DATA_ENCRYPT_KEY`) : octets inchangés. `<NOM>` vide reste
  une valeur pour le texte, comme avant, et « non défini » pour le binaire.
