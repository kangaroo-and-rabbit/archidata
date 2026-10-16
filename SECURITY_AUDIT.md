# Audit de Sécurité - archidata (2026-04-15)

## Audit du 2026-10-16

Audit défensif en lecture seule au commit `edac0b99`, en trois volets : web et authentification (W-xx, S-01), données et services (D-xx), dépendances, build et scripts (D1-D3, W1-W5, R1, E1, S1-S2, P1-P2, T1-T2, A1-A2, V1). Chemins relatifs à `src/main/org/atriasoft/archidata/` sauf mention contraire. Aucun constat CRITIQUE, aucun secret réel trouvé, aucune régression de sécurité caractérisée depuis 2026-06-01.

Attention aux identifiants : D1, D2, D3 (dépendances) sont distincts de D-01 à D-20 (données) ; W1 à W5 (workflows GitHub) sont distincts de W-01 à W-24 (web).

Total : 64 constats, soit 0 CRITIQUE, 5 HAUTE, 17 MOYENNE, 25 BASSE, 17 INFO.

### Tableau récapitulatif

| ID | Sévérité | Titre | Emplacement | Statut |
|----|----------|-------|-------------|--------|
| W-01 | HAUTE | SSRF : redirections suivies sans revalidation, DNS rebinding, plages IP manquantes | `tools/SsrfGuard.java:47-63`, `tools/DataTools.java:425-434,487-495`, `api/ProxyResource.java:45-53` | à corriger |
| W-02 | HAUTE | XSS stocké via SVG servi en ligne sur l'origine de l'API, puis vol du jeton | `tools/DataTools.java:64-66,217`, `api/DataResource.java:465,611,797-803` | à corriger |
| D1 | HAUTE | Plugins formatter et checkstyle déclarés comme dépendances compile : environ 70 jars d'outillage chez les consommateurs, dont des versions vulnérables | `pom.xml` (fin de `<dependencies>`) | à corriger |
| D2 | HAUTE | jackson-databind/core 2.22.2 : 4 avis HIGH, corrigés en 2.22.3 | `pom.xml` (jackson) | à corriger |
| W1 | HAUTE | `assign-pr-author` : action tierce en `@latest` avec un `GITHUB_TOKEN` en écriture | `.github/workflows/assign-pr-author.yml` | à corriger |
| W-03 | MOYENNE | Audience JWT (`application`) jamais vérifiée : le point #7 était faussement coché | `filter/AuthenticationFilter.java:403` | à corriger (décision requise) |
| W-04 | MOYENNE | DoS mémoire sur les téléchargements distants : lecture sans borne, contrôle `== 50 Mio` inopérant, pas de timeout | `tools/DataTools.java:434,443,495,502`, `api/ProxyResource.java:53` | à corriger |
| W-05 | MOYENNE | Upload sans taille maximale, fichiers temporaires orphelins | `api/DataResource.java:334-365`, `tools/DataTools.java:448-457,506-515,554-579` | à corriger |
| W-06 | MOYENNE | Bombe de décompression et coût CPU des vignettes | `api/DataResource.java:521-531` | à corriger |
| W-07 | MOYENNE | `Data` lisible par tout USER sans contrôle de propriétaire, état du générateur d'ObjectId divulgué dans chaque erreur | `api/DataResource.java:441-470,587-613`, `catcher/RestErrorResponse.java:23` | à corriger (décision requise) |
| W-08 | MOYENNE | Mass assignment : `@ApiReadOnly` ignoré à l'insertion | `dataAccess/DBAccessMongo.java:1319-1344`, `model/User.java:44,47` | à corriger |
| W-09 | MOYENNE | Jeton en query string (`@PermitTokenInURI`), jeton à pleins pouvoirs (#8) | `filter/AuthenticationFilter.java:207-218`, `api/DataResource.java:443,485,589` | à corriger (décision requise) |
| W-10 | MOYENNE | Deux compteurs temporaires indépendants et non atomiques dans le même dossier | `api/DataResource.java:87,104`, `tools/DataTools.java:60,89,112` | à corriger |
| D-01 | MOYENNE | `updateById` sans `FilterValue` réécrit (efface ou écrase) les champs `@DataNotRead` et `@DataEncrypt` | `dataAccess/DBAccessMongo.java:580`, `dataAccess/model/codec/MongoFieldCodec.java:65-68`, `dataAccess/commonTools/ManyToManyTools.java:142-156` | à corriger |
| D-02 | MOYENNE | `@DataNotRead` ignoré dans les sous-objets embarqués | `dataAccess/model/codec/MongoCodecFactory.java:491-530` | à corriger |
| D-03 | MOYENNE | Le journal de changements copie les documents bruts (secrets) et les garde sans limite | `journal/ChangeJournalEngine.java:256,415-448` | à corriger |
| D-04 | MOYENNE | Archives de sauvegarde en clair, permissions par défaut, sans intégrité | `backup/BackupEngine.java:213,767-778` | à corriger (décision requise) |
| W2 | MOYENNE | Aucun bloc `permissions:` dans maven.yml, check-title.yml, assign-pr-author.yml alors que le défaut du dépôt est l'écriture | `.github/workflows/` | à corriger |
| W3 | MOYENNE | docs.yml : `pages: write` et `id-token: write` accordés au job `build` | `.github/workflows/docs.yml` | à corriger |
| R1 | MOYENNE | `release.bash` : `versions:use-latest-releases` sans revue (jalons possibles), puis déploiement | `.island/release.bash`, `deliver` | à corriger |
| E1 | MOYENNE | Environnement de dev : mongo-express sans authentification, Mongo root exposé sur toutes les interfaces | `env_dev/docker-compose.yml`, `env_dev_arch/docker-compose.yaml` | à corriger |
| D3 | MOYENNE | Dépendances en retard : mongodb-driver 5.3.1 (5.13.0), tika 4.0.0, twelvemonkeys 3.14.0 | `pom.xml` | à corriger |
| W-11 | BASSE | Path traversal par préfixe de répertoire frère dans `FrontGeneric` (#3 partiel) | `api/FrontGeneric.java:45-47` | à corriger |
| W-12 | BASSE | CORS `*` avec `Allow-Credentials: true` (#6) | `filter/CORSFilter.java:36,43`, `api/ProxyResource.java:53` | à corriger (décision requise) |
| W-13 | BASSE | Fuite d'informations dans les réponses d'erreur (messages bruts, noms de classes) | `catcher/ExceptionCatcher.java:36`, `catcher/RequestBodyExceptionCatcher.java:29`, `catcher/JacksonExceptionCatcher.java:36`, `filter/AuthenticationFilter.java:238,257`, `tools/DataTools.java:420-455` | à corriger |
| W-14 | BASSE | Clé publique SSO : HTTP accepté, pas de timeout, `SSO_TOKEN` en clair (#15) | `tools/JWTWrapper.java:99-134` | à corriger |
| W-15 | BASSE | Rechargement de la clé SSO déclenchable par un anonyme | `tools/JWTWrapper.java:283,291`, `UpdateJwtPublicKey.java:81-83` | à corriger |
| W-16 | BASSE | Validation JWT incomplète : `exp` absent donne une NPE, `nbf`/`typ`/`kid` non vérifiés, pas de révocation (#16) | `tools/JWTWrapper.java:294-309` | à corriger |
| W-17 | BASSE | `uploadCover` sans contrôle du contenu, type MIME déduit de l'extension (#13 partiel) | `tools/DataTools.java:593-633,205-223,567` | à corriger |
| W-18 | BASSE | Swagger / OpenAPI publics, Swagger UI chargé depuis un CDN sans SRI (#18) | `api/openApiResource.java`, `api/SwaggerUiExternal.java` | à corriger (décision requise) |
| W-19 | BASSE | Aucun en-tête de sécurité HTTP | global (`filter/CORSFilter.java` seul filtre de réponse) | à corriger |
| W-20 | BASSE | Fuite possible de descripteurs `RandomAccessFile` (à vérifier) | `api/DataResource.java:812-825` | à corriger |
| D-05 | BASSE | Restauration d'une archive non fiable : préfixe frère, pas de borne de taille, réécriture libre de la base | `backup/BackupEngine.java:720,738-745,435-439,466-500` | à corriger |
| D-06 | BASSE | Expansion des relations sans borne de profondeur ni détection de cycle | `dataAccess/addOn/AddOnManyToOneDoc.java:191`, `dataAccess/LazyGetterCollector.java:134` | à corriger |
| D-07 | BASSE | `Limit` non validé, `Skip`/`Limit` tronqués en `int` | `dataAccess/options/Limit.java:15`, `dataAccess/DBAccessMongo.java:1623,1630,2323` | à corriger |
| D-08 | BASSE | Primitives d'injection exposées sans garde (`eq` sans `$eq`, noms de champs et de tri libres, `regex`, `where`) | `dataAccess/Filters.java:79,495,564`, `dataAccess/options/OrderBy.java:139` | à corriger |
| D-09 | BASSE | Logs TRACE des valeurs de filtres et documents, `toJson` calculé même hors TRACE | `dataAccess/DBAccessMongo.java:1600,1645` | à corriger |
| D-10 | BASSE | Messages du driver (valeurs E11000) renvoyés au client | `dataAccess/DBAccessMongo.java:1356,1666,1782`, `catcher/ExceptionCatcher.java:35` | à corriger |
| D-11 | BASSE | QueryStatistics : fichier `.tmp` prévisible suivant les liens symboliques, table non bornée | `dataAccess/statistic/QueryStatistics.java:105,208-210` | à corriger |
| D-12 | BASSE | Migrations sans verrou distribué ni tri explicite de la dernière migration | `migration/MigrationEngine.java:68,85,139` | à corriger (décision requise) |
| D-13 | BASSE | Connexion DB : `root` et `authSource=admin` par défaut, URL mal échappée, pas de TLS | `tools/ConfigBaseVariable.java:478`, `db/DbConfig.java:203-216` | à corriger |
| D-14 | BASSE | Chiffrement de champ : pas d'AAD, pas d'identifiant de clé, phrase de passe acceptée comme clé | `crypto/CryptoTools.java:58-62`, `crypto/EncryptedEnvelope.java:41-49`, `crypto/KeyMaterialLoader.java:48-60` | à corriger (décision requise) |
| D-15 | BASSE | Logs INFO des en-têtes HTTP (`Authorization`), des corps et du principal | `tools/RESTApiRequest.java:485-488,629,645-650`, `filter/MySecurityContext.java:220-223` | à corriger |
| S1 | BASSE | `configure_precommit.bash` lie un fichier inexistant ; `pre-commit` utilise `eval` | `tools/` | à corriger |
| S2 | BASSE | jdtls : PID, log, socket, workspace prévisibles dans /tmp, `.pyc` versionné, chemins absolus | `tools/claude/jdtls/` | à corriger |
| P1 | BASSE | `maven-assembly-plugin` sans version, mainClass factice, `scm` en `git://` et `ssh:` mal formé | `pom.xml` | à corriger |
| P2 | BASSE | SpotBugs sans FindSecBugs, pas de scan de dépendances en CI | `pom.xml`, `.github/workflows/maven.yml` | à corriger |
| W-21 | INFO | Pas de limitation de débit (#12) | global | à corriger |
| W-22 | INFO | `RESTApiRequest` : jeton journalisé avec `showIOStream`, pas de timeouts, en-têtes multipart non échappés | `tools/RESTApiRequest.java:355,431,448,285-292,629` | à corriger |
| W-23 | INFO | Annotations de sécurité au niveau classe ignorées (sûr par défaut), `ProxyResource` toujours 403 | `filter/AuthenticationFilter.java:182-201`, `annotation/AnnotationTools.java:59-73` | à corriger |
| W-24 | INFO | Divers : `isSecure()` derrière un proxy, `@RightAllowed(access=NONE)` toujours vrai, champs JWK non `volatile` | `filter/MySecurityContext.java:342,366`, `tools/JWTWrapper.java:47-48` | à corriger |
| S-01 | INFO | Sample `basic` sûr dans l'ensemble, mais reprend CORS `*` ; `WebLauncherLocal` à ne jamais déployer | `sample/archidata/basic/` | à corriger |
| D-16 | INFO | Les change streams livrent le document brut (champs `@DataNotRead`) | `dataStreamEvent/ChangeStreamWorker.java:217`, `dataStreamEvent/ChangeEvent.java:252` | à corriger |
| D-17 | INFO | Pas de garde contre les écritures de masse (`Condition` vide accepté) | `dataAccess/DBAccessMongo.java:2391-2394`, `dataAccess/options/Condition.java` | à corriger |
| D-18 | INFO | Valeur `@InitValue` écrite sans codec : en clair sur un champ `@DataEncrypt` | `dataAccess/DBAccessMongo.java:1340` | à corriger |
| D-19 | INFO | Pagination : valeur brute dans un log WARN, en-tête `Link` construit depuis `Host` | `pagination/PaginationContextValueProvider.java:80,98`, `pagination/LinkHeaderBuilder.java:73` | à corriger |
| D-20 | INFO | Durcissements divers : verrou de configuration contournable, `EncryptionKeyStore.register`, clé en variable d'environnement, MGF1-SHA1, index unique sur champ chiffré | `tools/ConfigBaseVariable.java:30-31,79,394`, `crypto/EncryptionKeyStore.java:45`, `crypto/RsaCipherProvider.java:20` | à corriger |
| W4 | INFO | Actions GitHub non épinglées par SHA | tous les workflows | à corriger |
| W5 | INFO | maven.yml : mot de passe Mongo de CI en clair, image `mongo:latest` | `.github/workflows/maven.yml` | à corriger |
| T1 | INFO | Mot de passe de test `base_db_password` (dev et CI uniquement) | docker-compose, `AGENTS.md`, `ConfigureDb`, sample | à corriger |
| T2 | INFO | Fichiers parasites versionnés (`res.txt`, `.ts` de 18 Mo, `.pyc`, config IDE) | racine | à corriger |
| A1 | INFO | Fichiers pour IA (`llms.txt`, `AGENTS.md`, `context7.json`, plugin) : rien de dangereux | racine, `claude-plugin/` | à corriger |
| A2 | INFO | `SKILL.md` écrit dans un /tmp prévisible | `claude-plugin/archidata/skills/archidata/SKILL.md` | à corriger |
| V1 | INFO | Restauration tar : contrôle de chemin par `startsWith` sans séparateur (doublon de D-05) | `backup/BackupEngine.java:720` | à corriger |

Les constats marqués « décision requise » changent un comportement sur lequel s'appuient les applications : le propriétaire doit trancher avant la correction.

### Constats HAUTE

**W-01 : SSRF contournable.** `SsrfGuard` résout le nom une seule fois et ne teste que la première adresse ; le client HTTP suit les redirections sans revalider la cible, résout à nouveau le nom à la connexion (DNS rebinding) et ne couvre pas certaines plages (IPv6 ULA, NAT64, 6to4, CGNAT, multicast). Scénario : un utilisateur USER soumet à `uploadUri` l'URL d'un serveur qui redirige vers un service interne ; les codes de réponse servent d'oracle et un contenu de type image est stocké, donc exfiltré. Impact : cartographie du réseau interne, appels GET à effet de bord, lecture de métadonnées cloud. Le `ProxyResource` devient un proxy ouvert anonyme si une application l'enregistre sans filtre d'authentification. Correction : désactiver le suivi des redirections (ou les traiter à la main en revalidant), valider toutes les adresses résolues et se connecter à l'IP validée, couvrir les plages manquantes, ajouter des timeouts, protéger `ProxyResource` par un rôle et restreindre son `Content-Type`.

**W-02 : XSS stocké via SVG et vol de jeton.** Le SVG est accepté à l'upload (Tika le détecte comme `image/svg+xml`) puis servi en ligne, sans `nosniff` ni `Content-Disposition`, sur l'origine de l'API, qui est aussi celle du front. Scénario : un USER téléverse un SVG contenant du script et fait ouvrir le lien (jeton accepté en query string, voir W-09) à une victime ; le script lit le jeton stocké dans `localStorage`. Impact : prise de compte, administrateur compris. Correction : retirer le SVG des types acceptés ou l'assainir, ajouter systématiquement `X-Content-Type-Options: nosniff` et une CSP restrictive à la diffusion des médias, `Content-Disposition: attachment` pour le SVG, et à terme servir les médias depuis une origine distincte.

**D1 : outillage de build en dépendances compile.** `formatter-maven-plugin` et `maven-checkstyle-plugin` sont déclarés sans `scope` ni `optional`, ce qui livre environ 70 jars (checkstyle, saxon, jsoup, snappy, beanutils 1.8.3, plexus-utils...) dans le classpath de chaque application consommatrice. Le code n'en utilise aucun. Impact : CVE inutiles chez les consommateurs (beanutils : exécution de code via la propriété `class`, plexus-utils : traversée, jsoup, snappy) et surface de supply chain élargie. Correction : supprimer ces deux blocs `<dependency>` (les plugins restent dans `<build>`), vérifier `mvn dependency:tree`, passer `spotbugs-annotations` en `provided` ou `optional`.

**D2 : Jackson 2.22.2 vulnérable.** `jackson-databind` et `jackson-core` ont quatre avis HIGH (complétion quadratique de forward-reference, mémoire sur type ID inconnus, DoS sur un parseur, ReDoS sur la lecture de nombres), corrigés en 2.22.3. Scénario : un corps JSON hostile envoyé à une route `@PermitAll`. Impact : déni de service non authentifié. Les versions Jackson sont en outre mélangées (2.19, 2.22, 2.22.1). Correction : passer à 2.22.3 et importer `jackson-bom` dans `dependencyManagement`.

**W1 : action tierce mutable avec jeton en écriture.** `assign-pr-author.yml` appelle une action tierce en `@latest`, avec un `GITHUB_TOKEN` en écriture (aucun bloc `permissions`, défaut du dépôt en écriture). Scénario : compromission de l'action tierce. Impact : accès en écriture au dépôt. Le déclencheur `pull_request` et le garde sur les PR internes limitent l'exposition. Correction : épingler par SHA complet, ajouter `permissions` minimales au job, ou remplacer l'action par `gh pr edit --add-assignee`.

### Constats MOYENNE

**W-03 : audience JWT non vérifiée.** `AuthenticationFilter` passe `null` comme audience à `validateToken`, ligne inchangée depuis 2025-04 ; le point #7 était donc coché à tort. Scénario : un jeton émis pour une application est accepté par une autre qui partage l'émetteur et la clé. Impact : atténué car karso n'émet que les rôles de l'application cible, mais `isUserInRole("USER")` sans préfixe renvoie vrai pour tout jeton valide. Correction : passer le nom de l'application, après avoir vérifié que karso émet exactement ce nom (et non `xxx-dev`), et envisager le claim standard `aud`. Décision requise : cela invalide les jetons existants dont l'audience diffère.

**W-04 : DoS mémoire sur les téléchargements distants.** Le corps distant est lu en entier en mémoire, le contrôle de taille utilise `==` (seule la taille exacte de 50 Mio est rejetée) et il n'y a aucun timeout. Scénario : un USER pointe `uploadUri` vers un serveur renvoyant plusieurs Gio ou un flux très lent. Impact : OutOfMemoryError ou thread bloqué. Correction : lecture en flux avec compteur borné, rejet d'un `Content-Length` excessif, `>=` au lieu de `==`, timeouts.

**W-05 : uploads non bornés et fichiers temporaires orphelins.** Aucune taille maximale à l'upload, tampon de 50 Mio alloué par requête, fichier temporaire jamais supprimé sur doublon ou type refusé. Scénario : un USER renvoie en boucle le même fichier. Impact : saturation du disque et du tas. Correction : taille maximale, suppression dans un `finally`, tampon de 64 Kio.

**W-06 : bombe de décompression et vignettes.** La vignette décode l'image entière à chaque requête. Scénario : un PNG minuscule déclarant des dimensions gigantesques. Impact : OutOfMemoryError, indisponibilité, coût CPU amplifiable. Correction : lire les dimensions sans décoder et refuser au-delà d'un seuil, sous-échantillonner, mettre la vignette en cache sur disque.

**W-07 : `Data` lisible par tout USER, ObjectId prévisibles.** Les routes de lecture de `Data` ne vérifient que le rôle USER, et chaque réponse d'erreur (y compris 401/403 anonymes) contient un `ObjectId` produit par la même JVM que les `_id` insérés, ce qui révèle l'état du générateur. Scénario : encadrer deux erreurs dans le temps permet de retrouver les médias téléversés par les autres. Impact : IDOR si l'application considère les médias comme privés. Correction : identifiant de corrélation d'erreur aléatoire (UUID), documenter l'absence de cloisonnement ou ajouter un lien Data vers propriétaire et des URL signées. Décision requise : cloisonner par propriétaire change l'accès aux médias existants.

**W-08 : mass assignment à l'insertion.** `@ApiReadOnly` n'est filtré qu'à la mise à jour ; à l'insertion les champs sont écrits tels que désérialisés. Scénario : un `POST` sans `@ValidGroup(GroupCreate)` laisse le client fixer un champ en lecture seule (par exemple `User.blocked`). Impact : altération de champs métier ; la protection dépend de la vigilance de chaque développeur. Correction : ignorer les champs `@ApiReadOnly` à l'insertion sauf `ForceReadOnlyField`, marquer `User.blocked` et `lastConnection`, noter le changement cassant dans `MIGRATION.md`.

**W-09 : jeton dans l'URL.** Les routes `@PermitTokenInURI` acceptent le JWT complet en query string, ensuite visible dans l'historique, les journaux d'accès et un cache `immutable` d'un an. Scénario : poste partagé ou journaux consultés. Impact : fuite d'un jeton à pleins pouvoirs, et vecteur de W-02. Correction : jeton dédié aux médias, de courte durée et à portée limitée, seul accepté en query string. Décision requise : les fronts existants doivent émettre et utiliser ce jeton.

**W-10 : collisions de fichiers temporaires.** Deux compteurs statiques indépendants et non atomiques écrivent dans le même dossier. Scénario : un upload et un démultiplexage simultanés (karideo) obtiennent le même identifiant. Impact : contenu corrompu ou échangé, déduplication empoisonnée. Correction : un seul générateur (`Files.createTempFile` ou `AtomicLong` avec UUID).

**D-01 : `updateById` réécrit les champs protégés.** Sans `FilterValue`, `updateById` inclut les champs `@DataNotRead` et `@DataEncrypt`, et une valeur `null` produit un `$unset`. Scénario : une lecture standard laisse ces champs à `null`, la mise à jour suivante les efface (`ManyToManyTools.updateRemoteLinks` le fait lui-même sur toute la collection), ou un client écrit un champ caché via un `PUT`. Impact : perte de secrets ou de champs chiffrés, ou écrasement par le client. Correction : retirer ces champs de la liste éditable par défaut, ne jamais transformer un `null` en `$unset` pour eux, corriger `ManyToManyTools`.

**D-02 : `@DataNotRead` ignoré dans les sous-objets.** `readSubObject` lit toutes les propriétés sans contrôle. Scénario : un objet embarqué dont un champ porte `@DataNotRead` est renvoyé au client sans avertissement. Impact : fuite de données déclarées protégées (aucune occurrence aujourd'hui dans karso, karideo, karusic). Correction : appliquer la même règle dans `readSubObject`, ou rejeter l'annotation au démarrage dans une classe utilisée comme sous-objet.

**D-03 : le journal copie les secrets.** `captureDocuments` sérialise le document entier sans projection, `runAll()` couvre toutes les collections. Scénario : un mot de passe ou un jeton révoqué reste lisible dans `changeJournal`, un document supprimé pour effacement RGPD y survit. Impact : durée de vie excessive des secrets, cible de valeur. Correction : exclure les champs `@DataNotRead` par projection, documenter l'exclusion des collections de jetons, offrir une purge par `sourceId`.

**D-04 : sauvegardes en clair.** Les archives contiennent toute la base (champs `@DataNotRead`, jetons, journal, médias) avec les permissions de l'umask et sans intégrité. Scénario : un compte local d'un hôte partagé ou un volume Docker monté ailleurs lit les archives. Impact : perte de confidentialité de toute la base. Correction : permissions `0600`/`0700`, chiffrement optionnel de l'archive (AES-GCM en flux ou sealed box), manifeste signé. Décision requise : le chiffrement impose une gestion de clé de sauvegarde et un nouveau format.

**W2 : pas de `permissions:` explicite.** Trois workflows héritent du défaut du dépôt (écriture). Scénario : un push ou une PR interne exécute du code Maven avec un jeton en écriture. Impact : un plugin compromis peut modifier le dépôt. Correction : `permissions: contents: read` au niveau du workflow, écriture seulement pour la soumission des dépendances, et défaut du dépôt en lecture.

**W3 : droits Pages et OIDC trop larges dans `docs.yml`.** `pages: write` et `id-token: write` sont au niveau du workflow, donc le job `build` (pip, mvn) les détient. Scénario : un paquet piégé installé par pip déploie du contenu. Impact : publication de contenu arbitraire sur Pages. Correction : déplacer ces droits dans le seul job `deploy`.

**R1 : release sans revue des versions.** `versions:use-latest-releases` met à jour toutes les dépendances au moment du tag, sans règle d'exclusion des jalons, puis `deliver --deploy` publie sans pause. Scénario : un jalon instable ou une version fraîchement compromise est embarqué dans la release. Impact : release instable ou compromise. Correction : retirer `use-latest-releases` du script, fournir des règles de versions, `set -euo pipefail`, `mvn verify` avant `deploy`.

**E1 : environnement de dev exposé.** Mongo (27017) et mongo-express (4077) sont publiés sur toutes les interfaces, mongo-express sans authentification et avec le compte root. Scénario : une machine de dev sur un réseau partagé. Impact : accès root complet à la base. Correction : lier à `127.0.0.1`, activer l'authentification de mongo-express ou le rendre optionnel, épingler les images.

**D3 : dépendances en retard.** mongodb-driver-sync/bson 5.3.1 (dernière 5.13.0), tika-core 4.0.0 (4.1.0), twelvemonkeys 3.14.0 mélangé avec 3.15.x (3.15.3). Pas de CVE connue, mais les décodeurs d'images traitent des entrées non fiables. Correction : mise à jour contrôlée et alignement des versions twelvemonkeys.

### Constats BASSE

- **W-11** : `FrontGeneric` teste `startsWith` sans séparateur ; un répertoire frère au préfixe commun est lisible sans authentification. Utiliser `Path.normalize().startsWith(base)`.
- **W-12** : CORS `*` avec `Allow-Credentials: true`. Pas de vol de session aujourd'hui (jeton en en-tête), mais tout site peut lire les endpoints `@PermitAll` d'une API interne ; liste blanche d'origines configurable.
- **W-13** : messages bruts, noms de classes Java et listes de propriétés renvoyés dans les erreurs (contredit #14). Message générique et `oid`, détail dans les logs.
- **W-14** : clé publique SSO récupérée en HTTP possible, sans timeout, `SSO_TOKEN` en clair. Imposer `https://`, timeouts, taille de clé minimale.
- **W-15** : un anonyme peut déclencher en boucle le rechargement de la clé SSO avec des JWT invalides. Limiter la fréquence, log WARN sans stack trace.
- **W-16** : `exp` absent donne une NPE, `nbf`, `typ`, `kid` non vérifiés, pas de révocation. Utiliser `DefaultJWTClaimsVerifier` de Nimbus.
- **W-17** : `uploadCover` sans Tika, type stocké déduit de l'extension. Factoriser la validation et stocker le type détecté.
- **W-18** : Swagger et OpenAPI publics, Swagger UI chargé depuis un CDN sans SRI. Réserver au développement ou à ADMIN, ajouter `integrity`. Décision requise.
- **W-19** : aucun en-tête de sécurité (nosniff, CSP, frame-ancestors, Referrer-Policy). Fournir un `SecurityHeadersFilter`.
- **W-20** : descripteurs `RandomAccessFile` potentiellement non fermés si `write()` n'est jamais appelé (à vérifier). Ouvrir le fichier dans `write()`.
- **D-05** : restauration d'une archive non fiable (préfixe frère, pas de borne de taille ni de ligne, collections arbitraires dont `system.js`, pas d'intégrité). Exploitable seulement avec une archive contrôlée par l'attaquant.
- **D-06** : expansion des relations sans profondeur maximale ni détection de cycle ; récursion jusqu'au `StackOverflowError` possible sur une entité auto-référente.
- **D-07** : `Limit` non validé, conversions `(int)` qui transforment un grand offset en valeur négative ou nulle (limite 0 signifie illimité).
- **D-08** : `eq` sans `$eq`, noms de champs et de tri libres (oracle sur un champ caché), `regex` non cité, `where` exposé. Aucune entrée HTTP n'atteint ces primitives dans archidata.
- **D-09** : TRACE écrit valeurs de filtres et documents ; `toJson` est calculé même hors TRACE (coût CPU gratuit).
- **D-10** : messages du driver (valeurs de clés dupliquées E11000) recopiés dans `DataAccessException` puis renvoyés au client ; énumération de comptes possible.
- **D-11** : fichier `.tmp` de QueryStatistics prévisible et suivant les liens symboliques, permissions par défaut, table non bornée.
- **D-12** : migrations sans verrou distribué (deux instances simultanées), dernière migration choisie sans tri. Décision requise.
- **D-13** : défaut `root` avec `authSource=admin`, URL de connexion mal échappée, pas de TLS.
- **D-14** : chiffrement de champ sans AAD (chiffré transplantable), sans identifiant de clé (rotation impossible), phrase de passe acceptée comme clé. Les primitives sont saines. Décision requise : passer à une enveloppe v2 demande une migration des données.
- **D-15** : `showIOStream` journalise `Authorization` et les corps en INFO, `getRole` journalise le principal en INFO. Masquer les en-têtes sensibles.
- **S1** : `configure_precommit.bash` lie `pre-commit-hook` qui n'existe pas (le hook n'est jamais installé), `eval` inutile dans `pre-commit`.
- **S2** : démon jdtls avec PID, log, socket et workspace prévisibles dans /tmp, `kill -9` sur un PID lu dans un fichier, `.pyc` et chemins absolus versionnés. Outil de dev, non livré dans le jar.
- **P1** : `maven-assembly-plugin` sans version et `mainClass` factice, `scm` en `git://` et `ssh:` mal formé, pas de `--strict-checksums`.
- **P2** : FindSecBugs commenté, aucun scan de dépendances (osv-scanner, dependency-check) en CI qui aurait détecté D1 et D2.

### Constats INFO

- **W-21** : pas de limitation de débit (#12) ; atténuerait W-04, W-05, W-06, W-15.
- **W-22** : `RESTApiRequest` sans timeouts, en-têtes multipart non échappés, `header()` ajoute au lieu de remplacer ; CRLF et TLS sains.
- **W-23** : les annotations de classe sont ignorées (refus par défaut, sûr mais surprenant) ; `ProxyResource` sans annotation répond toujours 403.
- **W-24** : `isSecure()` faux derrière un proxy TLS, `@RightAllowed(access=NONE)` toujours vrai, `rsaPublicJWK` non `volatile`, RSA 2048 bits.
- **S-01** : sample `basic` sûr, mais il reprend CORS `*` et n'a pas d'en-têtes de sécurité ; `WebLauncherLocal` journalise un jeton ADMIN de 24 h et code un mot de passe en dur (ne jamais déployer).
- **D-16** : les change streams livrent le document brut (champs `@DataNotRead` inclus) ; ne pas relayer `toJson()` vers un client.
- **D-17** : `new Condition()` vide accepté, `updateBsonDocuments` sans condition : écriture de masse possible par erreur applicative.
- **D-18** : `@InitValue` écrit sans codec, en clair sur un champ `@DataEncrypt`, puis lecture en échec.
- **D-19** : valeur brute d'un paramètre de pagination dans un log WARN (forgerie de lignes), en-tête `Link` dépendant de `Host`.
- **D-20** : verrou de configuration contournable, `EncryptionKeyStore.register` hors verrou, clé en variable d'environnement, MGF1-SHA1 d'OAEP, index unique inopérant sur champ chiffré.
- **W4** : actions GitHub épinglées par tag et non par SHA (déjà fait pour `maven-dependency-submission-action`).
- **W5** : mot de passe Mongo de CI en clair dans maven.yml (conteneur éphémère), `mongo:latest` non reproductible.
- **T1** : mot de passe de test `base_db_password` documenté (dev et CI) ; `getDBPassword()` échoue bien si `DB_PASSWORD` manque.
- **T2** : fichiers parasites versionnés (`res.txt`, `.test_partialtar.gz_tmp`, vidéo `.ts` de 18 Mo, `.pyc`, config IDE) ; `git rm --cached` et `.gitignore`.
- **A1** : fichiers pour IA relus, aucun secret ni instruction affaiblissant la sécurité.
- **A2** : `SKILL.md` écrit dans un /tmp prévisible ; utiliser `mktemp` ou un pipe.
- **V1** : doublon de D-05 (contrôle de chemin de restauration sans séparateur final), signalé par l'audit des dépendances.

### Points positifs

- Refus par défaut : tout endpoint sans `@PermitAll`, `@RolesAllowed` ou `@RightAllowed` reçoit 403, `@DenyAll` est respecté.
- JWT RSA vérifié par `RSASSAVerifier` : `alg=none` et confusion RS/HS impossibles, émetteur contrôlé, structure des rôles et droits validée strictement. Rôles cloisonnés par application.
- Jeton en query string accepté seulement sur les méthodes `@PermitTokenInURI` et en l'absence d'en-tête.
- `DataResource` construit les chemins depuis un ObjectId typé et un SHA-256 (pas de traversée), `Range` robuste, MIME détecté par Tika à l'upload générique, déduplication par filtres typés (pas d'injection NoSQL).
- `RESTApiRequest` : TLS vérifié, redirections non suivies, en-têtes validés contre le CRLF.
- Jackson sans default typing, champs inconnus rejetés. Aucune désérialisation polymorphe exploitable.
- Chiffrement de champ sain dans ses primitives (AES-256-GCM, X25519 + HKDF, RSA-OAEP, `SecureRandom`), échec franc plutôt qu'écriture en clair, déchiffrement désactivé par défaut, données chiffrées conservées chiffrées dans sauvegardes, journal et change streams.
- Pagination bornée à 500, suppressions logiques exclues des lectures, `update`/`delete` exigent une condition, `@ApiReadOnly` respecté à la mise à jour.
- Configuration verrouillable (setters gardés), mot de passe DB obligatoire et masqué dans `toString`, secrets lisibles depuis des fichiers, écoute en loopback par défaut.
- Migrations qui refusent de démarrer sur un état inconnu ; sauvegardes écrites dans un fichier caché puis déplacées, liens symboliques non recréés.
- Dépôt propre : aucun secret, clé privée ou `.env` versionné (historique depuis 2026-01 relu), aucune URL `http://` dans les poms, publication signée, Dependabot actif, `docs.yml` bien structuré (déclencheurs sûrs, job `deploy` séparé).
- Sample : `@Valid` + `@ValidGroup`, niveau d'accès sur chaque endpoint, échec sûr sans clé SSO.

### Statut revérifié des points de l'audit d'avril 2026

Rouverts le 2026-10-16 : #3 (partiel, W-11), #7 (jamais corrigé, W-03), #10 et #11 (garde SSRF contournable, W-01), #13 (partiel, W-02 et W-17), #14 (partiel, W-13). Restent ouverts #6 (W-12), #8 (W-09), #12 (W-21), #15 (W-14), #16 (W-16), #18 (W-18). Confirmés corrigés : #1, #2, #4, #5, #9, #17 (dans le filtre ; reste le cas opt-in de `RESTApiRequest`, voir W-22), #19.

---

## Audit d'avril 2026 (historique)

## Statut des vulnérabilités

### Corrigées
- [x] #2 - `Math.random()` pour génération de tokens (CRITIQUE)
- [ ] #3 - Path Traversal dans FrontGeneric.java (HIGH) (rouvert le 2026-10-16 : voir W-11)
- [x] #5 - Mot de passe DB dans DbConfig.toString() (HIGH)
- [ ] #7 - Validation audience JWT jamais faite (HIGH) (rouvert le 2026-10-16 : voir W-03)
- [ ] #10 - SSRF sans protection dans uploadDataFromUri (MEDIUM) (rouvert le 2026-10-16 : voir W-01)
- [ ] #11 - SSRF bypass dans ProxyResource (MEDIUM) (rouvert le 2026-10-16 : voir W-01)
- [ ] #13 - Upload fichiers sans validation type (MEDIUM) (rouvert le 2026-10-16 : voir W-02, W-17)
- [x] #17 - Tokens JWT complets dans les logs (LOW)

### Corrigées (cette itération)
- [x] #1 - Mode test bypass JWT (CRITIQUE) - voir MIGRATION.md pour le breaking change
- [x] #9 - Champs ConfigBaseVariable privés + setters avec lock/unlock sécurisé - voir MIGRATION.md

- [x] #4 - Mot de passe DB par défaut supprimé, crash si non configuré
- [ ] #14 - Noms de classes Java supprimés des erreurs client (message conservé, OID pour corrélation logs) (rouvert le 2026-10-16 : voir W-13)
- [x] #19 - Défaut API changé de 0.0.0.0:80 à localhost:80

### À faire (prochaine itération)
- [ ] #6 - CORS wildcard * - CORSFilter.java:33-38
- [ ] #8 - JWT dans les query parameters - AuthenticationFilter.java:145-155
- [ ] #12 - Pas de rate limiting sur l'authentification
- [ ] #15 - SSO public key potentiellement en HTTP - JWTWrapper.java:108-143
- [ ] #16 - Pas de révocation JWT (pas de jti) - JWTWrapper.java
- [ ] #18 - Swagger UI exposé en prod - openApiResource.java

### Dépendances
- [x] webp-imageio:0.2.2 - SUPPRIMÉ (libwebp native de 2021, vulnérable CVE-2023-4863). Lecture WebP couverte par TwelveMonkeys. Écriture WebP (thumbnails) à réintégrer quand une alternative safe existe.
