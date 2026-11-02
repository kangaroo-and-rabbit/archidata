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

## Client TypeScript (`src/resources/rest-tools.ts`)

- Copié tel quel dans `src/back-api/` de chaque front, où le lint et le format l'excluent : il se vérifie ici. Il doit
  passer `tsc --strict` et l'`oxfmt` des fronts, seule protection contre la dérive.
- TypeScript effaçable uniquement (pas d'`enum`) : `TestRestToolsPathEncoding` l'exécute directement avec node.
- Ne jamais journaliser une URL complète, ni ici ni dans `python/rest_tools.py` : avec `tokenInUrl`, elle contient le jeton.
- Le champ `message` de l'erreur 999 (panne réseau) reste un objet `Error` : `isRestErrorResponse` le refuse, et les fronts
  s'en servent pour distinguer une panne réseau d'une erreur du serveur.

## Analyse statique (SpotBugs)

- `mvn verify` lance `spotbugs:check` (seuil par défaut, tests exclus) : tout constat non listé dans
  `spotbugs-exclude.xml` fait échouer le build, en local comme en CI. L'exécution est déclarée avant `maven-gpg-plugin`,
  pour que le contrôle passe avant la signature, donc avant `mvn deploy`.
- La CI passe par ce contrôle natif (`mvn -B verify -Dgpg.skip=true`), sans étape dédiée, sans `continue-on-error` ni
  `failOnError=false`.
- Pas de filtre d'inclusion : un include vide n'autorise aucun constat, ce qui a fait passer ce contrôle sans rien
  vérifier jusqu'à la v0.52.2.
- Une exclusion nomme une classe et un motif, avec un champ ou une méthode sauf si le motif porte sur la classe
  elle-même (nommage), et donne sa raison. Pas de catégorie entière.
  Elle n'est acceptable que si la correction casse un comportement ou une API utilisée, ou si le constat est faux.
- Un 0 ne prouve rien tout seul. Pour vérifier le contrôle : injecter un bug évident sur une copie (`new String()`), le
  build doit échouer dessus ; retirer `excludeFilterFile`, SpotBugs doit remonter le nombre exact de constats exclus.
- Exposition d'état : une écriture dans une copie se perd sans bruit. Quand rien n'écrit, une vue en lecture seule est
  préférée ; quand les appelants écrivent (`UserByToken.getRoles()`, listes des beans), le constat est exclu. Les
  tableaux et les `Date` sont copiés. Tri complet : `plans/2026-11-01-spotbugs.md`.
- `UserByToken.hasRight` suit le masque de bits de `PartRight`, comme `hasResourceRight`.
  `true` demande `READ_WRITE` ; demander `NONE` (`false`, `0`) répond toujours `false`, toute valeur qui ne décrit pas
  un droit aussi.

## Sauvegardes (`BackupService`)

- Les applications ne déclarent que leur dossier, leurs classes et leurs collections en plus : six copies d'un
  `BackupJob` divergeraient. Plan : `../plans/2026-11-02-sauvegardes-unifiees.md`.
- Base seule, `counters` et `KAR_migration` toujours inclus. `BackupEngine` garde ses collections dans un ensemble : une
  archive qui contient deux fois une collection échoue à la restauration.
- `store`, jamais `storeAll` : karvox laisse exprès ses jetons de worker hors des archives.
- Une seule sauvegarde à la fois, par processus, complète ou delta : `startAsync` et `startDeltaAsync` décident sous le
  même verrou. Une complète pendant une complète passe son tour : celle qui tourne est aussi récente. Un delta pendant
  une sauvegarde aussi, sans WARN : le suivant contiendra tout. Une complète pendant un delta attend, répond `STARTED` et
  part sans trou dans le même thread : le cron ne fixe pas l'ordre de deux tâches de la même minute (`ConcurrentHashMap`)
  et `0 0 * * *` croise `*/15 * * * *` chaque nuit ; sans cela le delta pouvait coûter sa complète à la nuit.
- Delta : `since` est la date de la dernière complète lue sur le disque, moins 120 s (`DELTA_MARGIN_MILLIS`). `updatedAt`
  est posé à l'écriture, pas au commit, et une transaction Mongo vit jusqu'à 60 s par défaut : un document daté juste
  avant la complète peut devenir visible derrière le curseur du dump. Pas de `since` en mémoire : il survit au redémarrage, et les deltas d'une même complète restent cumulatifs, seul
  le dernier sert à `restoreLatest`.
- Sans complète sur le disque, `startDeltaAsync` lance une complète plutôt que de passer son tour : serveur neuf ou
  dossier vidé, sinon rien de restaurable jusqu'à la nuit. Contrepartie acceptée : tant qu'elle échoue, elle est retentée
  tous les quarts d'heure.
- Une complète datée après l'heure courante fait échouer le delta (tentative en échec, ERROR) : il serait vide et une
  restauration s'arrêterait à cette complète. Pas de correction automatique : l'ordre des archives est faux, il faut un
  humain.
- Delta sans changement : `storeDeltaIfChanged` compare la somme de l'archive à celle recalculée du fichier du dernier delta de
  la chaîne, et jette l'archive si elles sont égales, jamais d'après le `.sha256` : une archive abîmée n'empêche pas de
  réécrire. Une erreur de lecture du précédent garde le nouveau delta. Une comparaison de contenu, car `counters` et les collections sans date rendent
  tout delta non vide, et un simple comptage des dates perdrait une modification en place d'un document sans date. Pour
  cela l'archive est déterministe : `setModTime(0)` sur les entrées, le gzip de commons-compress écrit 0 par défaut. Le
  premier delta après une complète reste écrit, faute de référence : un fichier par complète au lieu de 96 par jour.
- Le service garde séparément la dernière tentative de complète et la dernière de delta. `lastAttempt` expose la complète
  en échec s'il y en a une, sinon la plus récente : sans cela, 96 deltas par jour effaçaient l'échec en 15 minutes. Forme
  JSON inchangée, à dessein.
- Le dump tourne dans son propre thread : le cron n'a qu'un thread consommateur, qu'il tiendrait pendant des minutes.
  `BackupService` n'est donc plus un `Runnable`, on branche `backup::startAsync` : l'ancien branchement ne compile plus.
- Le dossier est créé `rwx------`, par l'engine, et non par `Files.createDirectories` comme les `BackupJob`.
- La date d'une tentative est celle de son archive, à la milliseconde. Cet état reste en mémoire et se perd au
  redémarrage.
- Pas de limite de débit : l'utilisateur lance quand il veut. Le garde-fou est l'espace libre : refus sous deux fois la
  dernière archive complète plus `SPACE_MARGIN_BYTES` (1 Gio), contrôlé verrou pris. Un refus, ou un contrôle impossible,
  compte comme une tentative en échec dans les deux cas, manuel ou nuit : la tâche de nuit jette la réponse de
  `startAsync`, seul l'état en garde la trace. Un delta passe le même contrôle.
- `listArchives` échoue si le dossier a disparu, et `startAsync` ne le recrée pas : un volume démonté doit se voir, sans
  écrire l'archive sur ce qui se trouve dessous.
- Un type `PARTIAL` s'ajoute à complète et delta : ranger une archive `_partial` dans l'un des deux serait faux. Aucune
  application n'en écrit aujourd'hui.
- Rétention (décidée le 2026-11-02) : après chaque complète réussie, `engine.clean(policy)`, `new RetentionPolicy()` par
  défaut, dernier paramètre du constructeur sinon. Jamais après un échec ou un delta. Un refus pour manque d'espace lance
  aussi `clean` puis recontrôle une fois : la rétention ne tournant qu'après une complète, un disque plein se serait
  bloqué pour toujours. Un échec de `clean` se
  journalise et laisse la sauvegarde en succès. Pas de route : aucun client ne choisit ce qui part.
- `null` comme politique coupe la rétention : plus rien dans le service ne supprime d'archive, ni après une complète, ni à
  un refus, ni les deltas datés après une complète. La chaîne de restauration les écarte quand même.
- `clean` ne supprime jamais la dernière complète, quels que soient la politique et l'horloge, même si un `_partial` plus
  récent gagne la déduplication de sa journée. Il retire tout delta antérieur à la dernière complète, quel que soit son âge, et garde les
  suivants ; sans complète il n'en retire aucun. Le `.sha256` part avec son archive. `keepAllDays` ne vaut donc pas pour
  les deltas : on ne remonte au quart d'heure près qu'après la dernière complète.
- Le nom d'un delta de `BackupService` porte la date de sa complète (`_base<yyyyMMddHHmmssSSS>_delta`) : après un saut
  d'horloge, une date seule ne distingue pas un delta écrit avant une complète, et `restoreLatest` réécrirait de vieux
  documents par-dessus. La chaîne refuse une archive datée après l'heure courante, complète comme delta, puis un delta
  plus ancien que la complète ou fondé sur une autre, avec un WARN, et se rabat sur la dernière complète et le dernier delta
  valides. Sans complète valide, elle est vide et rien n'est restauré. Une complète réussie retire les deltas datés
  après elle. Un delta d'une version antérieure ne porte pas ce nom : seule sa date le juge.
- Les archives se trient par date et heure, pas par le texte du nom : après une migration, les formats cohabitent.

## Route des sauvegardes (`BackupResource`)

- `GET /backups` liste, `POST /backups` lance toujours une complète : 202, y compris derrière un delta en cours, 409 si
  une complète tourne ou attend, 507 si l'espace manque.
  `Response.Status` n'a pas de 507 et `WebApplicationExceptionCatcher` masque le message d'un 5xx : la ressource construit
  cette réponse elle-même, avec le corps d'erreur habituel. **Aucune route de téléchargement,
  de restauration ni de suppression, aucun paramètre de chemin ou de nom** : une archive contient les hachages de mots de
  passe. `TestBackupResource` vérifie par réflexion que la ressource ne compte que ces deux routes, sans paramètre.
- `GET` demande `ADMIN:r`, `POST` demande `ADMIN:rw`, pas de `@PermitTokenInURI`. Attention : `ADMIN` sans suffixe exige
  `READ_WRITE`, il refuse aussi bien `READ` que `WRITE` seuls (`MySecurityContext.checkRightInGroup`). Le contrôle passe
  avant le service : sans jeton 401, sans rôle 403, même si aucun service n'est configuré. Sans service : 503.
- Défense en profondeur : chaque route revérifie son rôle, `@RolesAllowed` ne faisant rien sans filtre. Le contrôle se fait
  sur `<application>/ADMIN:x`, comme le filtre : sans groupe, `isUserInRole` cherche dans `?system?` et refuserait tous les
  vrais administrateurs. Le nom vient de `GenericContext.getApplicationName()`, renseigné par `AuthenticationFilter`, et
  pas de `MySecurityContext` : le `SecurityContext` reçu par `@Context` est un proxy, un `instanceof` y échoue.
- Le service est donné par `BackupResource.setService` : Jersey construit la ressource, les applications l'enregistrent
  par classe et `AnalyzeApi` a besoin de cette classe. C'est le même usage que les réglages statiques de `DataTools`.
- Les modèles (`Instant`, records, enum) passent dans `TsGenerateApi` ; `@ApiAsyncType(Void.class)` donne
  `Promise<void>` à `POST`, qui retourne un `Response` pour pouvoir répondre 202.
