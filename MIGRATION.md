# Migration Guide

## Prochaine version (après v0.50.2) - Annotations propres à archidata à la place de JPA (BREAKING CHANGE)

archidata ne lit plus aucune annotation JPA (`jakarta.persistence`), ni le `@DefaultValue` de
JAX-RS posé sur un champ d'entité. Quatre annotations d'archidata, dans
`org.atriasoft.archidata.annotation`, les remplacent. Il n'y a **pas de période de
compatibilité** : les anciennes annotations ne sont plus lues, et une entité qui en porte encore
une est **refusée**.

Échec rapide : à la première analyse du modèle d'une entité (premier accès en base), archidata lève
une `DataAccessException` si la classe, l'une de ses classes mères, l'un de leurs champs ou l'une de
leurs méthodes porte une annotation `jakarta.persistence.*`, ou si l'un de leurs champs porte
`jakarta.ws.rs.DefaultValue`. Le message nomme la classe, l'élément, l'ancienne annotation et son
remplacement (tableau ci-dessous). Ce n'est pas un mode de compatibilité : une entité oubliée ne
change plus de collection, de nom de champ ni de valeur initiale en silence, elle bloque.

Rien ne change en base pour un modèle migré : mêmes noms de collection, mêmes noms de champs,
mêmes valeurs initiales. Sans annotation, la collection porte toujours le nom simple de la classe
(`getSimpleName()`).

### Correspondance

| Avant | Maintenant |
|---|---|
| `@jakarta.persistence.Table(name = "media")` (sur la classe) | `@Collection("media")` |
| `@jakarta.persistence.Column(name = "nom")` (sur un champ) | `@Field("nom")` |
| `@jakarta.persistence.Id` | `@Id` (`org.atriasoft.archidata.annotation.Id`) |
| `@jakarta.ws.rs.DefaultValue("'0'")` sur un champ d'entité | `@InitValue("'0'")` |
| `@ManyToOne` / `@OneToMany` / `@ManyToMany` (JPA) | `@ManyToOneDoc` / `@OneToManyDoc` / `@ManyToManyDoc` |
| `@Column` sans `name`, `@GeneratedValue`, `@Entity` | rien (à supprimer) |

`@BsonId` (`org.bson.codecs.pojo.annotations`) n'est pas une annotation JPA : il **reste** à côté
de `@Id @Field("_id")`. `DbIoMongo` enregistre le codec POJO du driver
(`PojoCodecProvider.automatic(true)`), qui le lit pour envoyer la propriété dans `_id` ;
`OIDGenericData`, `ChangeJournalEntry` et `ChangeJournalMarker` le gardent.

Le `@DefaultValue` de JAX-RS reste la bonne annotation sur les **paramètres** d'une ressource REST
(`@QueryParam("page") @DefaultValue("1") int page`) : c'est Jersey qui le gère, ne pas y toucher.
Le nom `@InitValue` est distinct exprès : un mauvais import compilerait et serait ignoré.

Collision de noms : dans un fichier où `Field` ou `Collection` désigne déjà un autre type (import
de `java.lang.reflect.Field` ou `java.util.Collection`, import `java.lang.reflect.*` ou
`java.util.*`, type de ce nom déclaré dans le fichier ou dans son package), écrire l'annotation avec
son nom complet (`@org.atriasoft.archidata.annotation.Field("nom")`).

### Ce qui disparaît

- Les attributs de `@Column` autres que `name` : `nullable`, `length`, `unique`, `insertable`,
  `updatable`. Ils n'avaient aucun effet (sauf `unique`, qui loguait un avertissement dans
  `IndexRegistry`, retiré lui aussi). Les remplacer, si la contrainte est voulue, par `@NotNull`,
  `@Size(max = ...)` ou `@Indexed(unique = true)` : attention, ces annotations-là **agissent**
  (validation, index unique).
- `@GeneratedValue` / `GenerationType` : sans effet, la génération de la clé dépend de son type
  (`ObjectId`, `Long` par la collection `counters`, `UUID`).
- Les relations JPA (`@ManyToOne`, `@OneToMany`, `@ManyToMany`) : elles ne servaient plus qu'au
  générateur d'API (lien `linkClass` des graphes Dot/draw.io). Les relations passent uniquement par
  `@ManyToOneDoc`, `@OneToManyDoc` et `@ManyToManyDoc`, dont le `targetEntity` trace désormais les
  arêtes de relation des graphes.
- La dépendance `jakarta.persistence-api`, retirée du pom d'archidata : une application qui importe
  encore `jakarta.persistence` ne compile plus. **Ne pas la rajouter dans l'application** : les
  anciennes annotations compileraient de nouveau, et l'entité serait refusée à l'exécution
  (`DataAccessException`, voir « Échec rapide » ci-dessus).
- API Java : `AnnotationTools.getLimitSize`, `getColumnNotNull`, `isUnique`, `getStrategy`,
  `getManyToOne`, `getManyToMany`, `getOneToMany` et `getDefault` (sans appelant) sont
  supprimées ; `DbPropertyDescriptor.isNullable`, `isUnique`, `getColumnLength`,
  `getGenerationStrategy` sont supprimées et `getDefaultValue` devient `getInitValue` ;
  `DBAccessMongo.insertPrimaryKey_test` (sans appelant) est supprimée.

### Comment migrer

Le script `tools/migrate-annotations.sh` du dépôt archidata fait les remplacements mécaniques
(sed, GNU) dans un dossier de sources, puis liste ce qui reste à traiter à la main. Il modifie les
fichiers en place : le lancer sur un arbre git propre et relire le `git diff`.

```bash
# depuis le dépôt de l'application, archidata étant cloné à côté
../archidata/tools/migrate-annotations.sh back/src
```

Ce qu'il fait :

- `@Table(name = "x")` → `@Collection("x")` (aussi avec une constante : `@Table(name = X.NAME)`) ;
- `@Column(name = "x")` → `@Field("x")` ; `@Column` sans attribut supprimé ;
- `import jakarta.persistence.Id` → `import org.atriasoft.archidata.annotation.Id` ;
- `@Entity` sans attribut, `@GeneratedValue(...)` et leurs imports supprimés ;
- `@DefaultValue(` → `@InitValue(` dans les fichiers **sans** paramètre JAX-RS (`@QueryParam`,
  `@PathParam`, `@HeaderParam`, `@FormParam`, `@MatrixParam`, `@CookieParam`, `@BeanParam`, et
  `@FormDataParam` du multipart Jersey) ;
- les imports remplacés (nom complet de l'annotation à la place de l'import en cas de collision de
  noms, voir ci-dessus).

Il ne touche pas aux fichiers qui importent `jakarta.persistence.*` : le joker cache les annotations
utilisées. Il les liste à part ; remplacer le joker par des imports explicites, puis relancer le
script.

Ce qu'il liste, à traiter à la main :

- les `@Column` avec d'autres attributs que `name` (`nullable`, `length`, `unique`...) : garder le
  nom dans `@Field("...")`, supprimer le reste ou le remplacer par une vraie contrainte (voir
  ci-dessus) ;
- les `@Table` avec d'autres attributs que `name` ;
- les relations JPA, à réécrire avec `@ManyToOneDoc` / `@OneToManyDoc` / `@ManyToManyDoc` ;
- les autres usages de `jakarta.persistence` (`@Transient`, `@Enumerated`, imports `*`...) ;
- les fichiers de ressources REST dont il n'a pas touché les `@DefaultValue` (paramètres JAX-RS) :
  vérifier qu'aucun n'est sur un champ d'entité ;
- les fichiers où il a écrit le nom complet de `@Field` / `@Collection` ;
- les fichiers non modifiés à cause de `import jakarta.persistence.*;`.

Ensuite : organiser les imports (IDE), lancer le formateur, vérifier que
`grep -rn "jakarta.persistence" src` ne trouve plus rien, puis lancer les tests. Voir
`doc/annotations.md` et `doc/data_model.md`.

## Prochaine version (après v0.50.2) - Corrections de bugs qui changent le comportement (CHANGEMENT DE COMPORTEMENT)

Ce lot corrige des bugs ; plusieurs corrections changent un comportement observable. À relire
avant de monter de version.

### Syntaxe cron (`CronScheduler`)

La syntaxe est désormais celle du cron Unix (Vixie). Le sens de certaines expressions déjà
déployées change :

| Motif | Avant | Maintenant |
|---|---|---|
| `*/n` sur le jour du mois, le mois ou le jour de semaine | valeurs multiples de `n` (`*/2` sur le jour du mois : 2, 4, 6...) | tous les `n` depuis le minimum du champ (`*/2` : 1, 3, 5...) |
| jour de semaine `0` | ne correspondait jamais (lundi = 1 .. dimanche = 7) | dimanche (0 ou 7) |
| jour du mois ET jour de semaine restreints tous les deux (`0 0 1 * 1`) | les deux devaient correspondre (ET) | l'un ou l'autre suffit (OU) |

`*/n` sur la minute ou l'heure, `*`, les valeurs et les intervalles (hors jour de semaine 0)
gardent leur sens. Nouveautés : dimanche = 7 ; les expressions invalides sont refusées par
`addTask` (`IllegalArgumentException`) au lieu d'échouer à chaque évaluation.

Des expressions qui faisaient échouer l'ancien parseur à chaque évaluation (la tâche ne tournait
jamais) **s'exécutent désormais** :

- un pas sur un intervalle `a-b/n` (par exemple `0-30/10`) ;
- des champs séparés par plusieurs espaces ou par une tabulation (`0  12 * * *`).

**Comment migrer :** aucun avertissement n'est logué. Relire chaque expression cron déployée avec
le tableau ci-dessus et la réécrire si le nouveau sens n'est pas celui voulu (par exemple `*/2` sur
le jour du mois pour les jours pairs devient `2-30/2`) ; vérifier aussi que les tâches qui ne
tournaient jamais pour les raisons ci-dessus doivent bien tourner. Voir `doc/cron.md`.

### `MigrationEngine`

- Si le dernier nom de migration enregistré dans `KAR_migration` n'est ni l'initialisation ni une
  migration ajoutée (migration renommée ou supprimée), le moteur lève une `MigrationException` et
  `migrateWaitAdmin` bloque le démarrage. Avant : rien n'était appliqué, sans erreur.
  **Comment migrer :** ne jamais renommer une migration déployée ; si c'est arrivé, rendre à
  `getName()` son ancien nom.
- `revertSingle()` lève `UnsupportedOperationException`, de même que `revertTo()` dès qu'une
  migration doit être annulée. Avant : l'annulation ne faisait rien en silence. Écrire une nouvelle
  migration « en avant » à la place.

### Statuts HTTP des catchers

- Un corps de requête JSON illisible (`StreamReadException`) ou qui ne correspond pas au type
  attendu (`MismatchedInputException`, `ValueInstantiationException`) répond **400** au lieu
  de 500 : `RequestBodyReaderInterceptor` l'enveloppe dans une `RequestBodyException`, à laquelle
  `RequestBodyExceptionCatcher` répond 400. Toute `JacksonException` brute (parsing fait par le
  serveur lui-même, réponse d'un service amont, document stocké, sérialisation) reste en 500
  (`JacksonExceptionCatcher`). `GenericCatcher.addAll(rc)` enregistre l'intercepteur et le
  mapper ; une application qui enregistre les catchers un par un doit ajouter ces deux classes.
- `InputExceptionCatcher` : le champ `status` du corps JSON reprend le statut réel de
  l'`InputException` (celui de la réponse HTTP) ; avant, il valait toujours 400.

**Comment migrer :** adapter les clients ou les tests qui attendaient un 500 sur un JSON invalide,
ou qui lisaient `status` dans le corps.

### `DbConfig` et `ConfigBaseVariable.getDBPort()` : port en `Integer`

Les ports au-delà de 32767 sont acceptés. Le constructeur de `DbConfig` prend un `Integer` ;
l'ancien constructeur `Short` est conservé, `@Deprecated`, et délègue. Un appel avec un `null`
littéral comme port est désormais ambigu entre les deux : écrire `(Integer) null`. Le port est
validé dans 1..65535 : les constructeurs de `DbConfig` lèvent sinon `DataAccessException`
(l'exception qu'ils déclarent déjà). `ConfigBaseVariable.getDBPort()` renvoie un `int` (au lieu de
`Short`) et lève `IllegalArgumentException` si `DB_PORT` n'est pas un entier entre 1 et 65535 ;
`new DbConfig()` la convertit en `DataAccessException`.

### `DB_ABLE_TO_CREATE` lu correctement

La variable était lue avec `Boolean.getBoolean` (qui lit une propriété système portant ce nom) :
toute valeur donnait `false`. Elle est maintenant lue avec `Boolean.parseBoolean` :
`DB_ABLE_TO_CREATE=true` donne enfin `true` (sans variable : `true`, inchangé).

### Change streams : modes `FullDocument` par listener dépréciés

Le mode `FullDocument` est global, choisi par `ChangeNotificationManager.start(...)`. Les surcharges
qui prennent un mode par listener ou par watch (`registerListener(..., FullDocument)`,
`ListenerRegistrationBuilder.withMode`, `CollectionWatchBuilder.fullDocument`) l'ignoraient déjà ;
elles sont `@Deprecated` et loguent un WARN dès qu'un mode leur est passé, avant ou après
`start()` : seul le mode de `start()` compte. Retirer le mode de ces appels et le passer à
`start()`.

### `tools.PublicKey` déprécié

`org.atriasoft.archidata.tools.PublicKey` est `@Deprecated(forRemoval = true)` : utiliser
`JWTWrapper.PublicKey`.

### `@ApiDoc(hidden = true)` respecté

Il était ignoré. Il retire désormais l'élément de tous les générateurs (OpenAPI, TypeScript,
Python, graphes) : sur une méthode (ou sur la méthode qu'elle surcharge ou implémente, dans une
superclasse ou une interface), l'endpoint ; sur une classe de ressource JAX-RS (ou une
superclasse ou interface), tous ses endpoints ; sur un champ, son getter ou son setter, la
propriété du modèle. L'endpoint répond toujours et le champ est toujours sérialisé en JSON.
**Comment migrer :** vérifier que les éléments marqués `hidden` ne sont pas utilisés par les
clients générés.

### `OffsetDateTime` persisté en UTC

Un champ d'entité `OffsetDateTime` est stocké comme un `Instant` (date BSON, précision
milliseconde) et relu **en UTC** : l'offset d'origine est perdu (`2025-06-15T14:30+02:00` revient
`2025-06-15T12:30Z`, même instant). Avant, ce type n'était pas pris en charge comme champ
persisté. Stocker l'offset dans un champ séparé si besoin.

### `RESTApiRequest.header()`

Nouvelle méthode `header(key, value)` pour ajouter un en-tête à la requête. `fetchString()` et
`fetchByte()` restent des accesseurs bruts qui ne lèvent pas d'exception sur 4xx/5xx ;
`fetch(Class)`, `fetch()` et `fetchList` lèvent `RESTErrorResponseException`.

---

## Prochaine version (après v0.50.2) - Corrections de sécurité de la couche web et de l'authentification (CHANGEMENT DE COMPORTEMENT)

Ce lot corrige les constats de l'audit de sécurité de la couche web (W-xx). Plusieurs corrections
changent un comportement observable ; chaque point dit qui est concerné.

### Téléchargements côté serveur : `SafeHttpFetcher` (W-01)

`uploadDataFromUri`, `uploadCoverFromUri` et `ProxyResource` ne passent plus par le client Jersey
mais par `SafeHttpFetcher` : nom résolu une seule fois, **toutes** les adresses vérifiées, connexion
à l'adresse validée, aucune redirection suivie automatiquement (chaque `Location` est revalidée, 3 au
plus), délais de 5 s (connexion), 30 s (lecture) et 120 s (échange complet). `SsrfGuard` refuse en
plus CGNAT (`100.64.0.0/10`), `0.0.0.0/8`, le multicast, les plages réservées, les ULA IPv6
(`fc00::/7`), NAT64, 6to4, Teredo, les adresses IPv4 mappées ou compatibles IPv6, et une URL avec
identifiants (`user:pass@`).

`ProxyResource` renvoie le type amont seulement pour une image matricielle (sinon
`application/octet-stream`), ajoute `X-Content-Type-Options: nosniff` et n'envoie plus
`Access-Control-Allow-Origin: *` (le `CORSFilter` s'en charge).

**Qui est impacté** : une application qui télécharge depuis une URL interne (réseau Docker, IP
privée) par ces méthodes : elle est refusée, y compris via une redirection. Utiliser un appel direct
au service interne, pas une URL fournie par l'utilisateur.

### Fichiers servis par `DataResource` : `nosniff`, sandbox, pièce jointe (W-02)

Toutes les réponses de fichiers (`/data/{oid}`, `/data/{oid}/{name}`, `/data/thumbnail/{oid}`)
portent `X-Content-Type-Options: nosniff` et `Content-Security-Policy: sandbox; default-src 'none'`.
Un type actif (SVG, HTML, XHTML, XML, JavaScript, voir `DataTools.isActiveMimeType`) est en plus servi
en `Content-Disposition: attachment`. Le SVG reste accepté à l'upload.

**Qui est impacté** : un front qui ouvre l'URL d'un SVG dans un onglet (il est maintenant
téléchargé). L'affichage par `<img>`, `<video>` ou `<audio>` ne change pas. Une ressource applicative
qui sert elle-même des fichiers stockés doit appeler `DataTools.addSafeServingHeaders`.

### Audience des JWT vérifiée (W-03)

`generateJWToken` écrit le claim standard `aud` (nom de l'application), en plus de `application`.
`AuthenticationFilter` refuse (401) un jeton dont `aud` ne contient pas le nom d'application passé à
son constructeur. Un jeton **sans** `aud` (émis par un archidata plus ancien) est accepté seulement si
son claim `application` vaut ce nom.

**Qui est impacté** : un jeton émis pour une autre application, ou pour un nom différent (suffixe
`-dev`, majuscules), est maintenant refusé. Les jetons de test doivent porter le nom d'application du
filtre du serveur de test. Vérifié : les jetons de karso (`users/get_token`, `oauth2/token`,
`application/get_token`, qui retirent `-dev`) et les jetons de test d'archidata, karso, karideo et
karusic sont compatibles.

### Taille des téléchargements distants bornée (W-04)

Le corps téléchargé par `uploadDataFromUri`, `uploadCoverFromUri` et `ProxyResource` est lu en flux
et borné par `DataTools.setMaxRemoteDataSize(long)` (50 Mio par défaut) : au-delà, HTTP 413. Avant,
seule une taille d'exactement 50 Mio était refusée, et le corps entier était chargé en mémoire.

### Taille maximale d'upload et fichiers temporaires supprimés (W-05)

`uploadData` et `uploadCover` (donc `POST /data/upload`) refusent un fichier de plus de
`DataTools.setMaxUploadSize(long)` (1 Gio par défaut) avec HTTP 413. Le fichier temporaire est
supprimé dans tous les cas, y compris quand la donnée existe déjà ou que le type est refusé (avant, il
restait dans `DATA_TMP_FOLDER`). `DataTools.CHUNK_SIZE_IN` est déprécié.

**Qui est impacté** : une application qui téléverse plus de 1 Gio par ces méthodes doit relever la
limite au démarrage. Les variantes non bornées de `saveTemporaryFile` restent disponibles pour les
flux produits par le serveur lui-même (démultiplexage…).

### Vignettes : dimensions bornées avant décodage (W-06)

`GET /data/thumbnail/{oid}` lit les dimensions dans l'en-tête de l'image et répond 415 au-delà de
20 000 pixels de côté ou de 100 Mpx (`DataResource.MAX_THUMBNAIL_SOURCE_DIMENSION`,
`MAX_THUMBNAIL_SOURCE_PIXELS`), au lieu de décoder l'image entière. L'image est décodée
sous-échantillonnée ; une image très fine donne une vignette d'au moins un pixel de haut (avant : 500).

**Qui est impacté** : un front qui demande la vignette d'une image géante reçoit 415 au lieu d'une
vignette ; il peut afficher l'image d'origine.

### Identifiants temporaires aléatoires et réservés (W-10)

`DataTools.getTmpDataId()` et `DataResource.getTmpDataId()` partagent maintenant un seul générateur :
un identifiant aléatoire (`SecureRandom`, supérieur à 2^40) dont le fichier est créé atomiquement dans
`DATA_TMP_FOLDER`. Avant, deux compteurs partant de 1 distribuaient les mêmes numéros (fichiers
écrasés entre deux uploads simultanés). `getTmpFolder()` renvoie aussi un nom aléatoire.

**Qui est impacté** : le code qui supposait des identifiants petits ou croissants. Un identifiant
obtenu et jamais utilisé laisse un fichier vide : appeler `removeTemporaryFile(id)` dans un
`finally`.

### `FrontGeneric` : comparaison de chemins normalisés (W-11)

Le fichier demandé doit rester dans `baseFrontFolder` au sens de `Path.startsWith(Path)` : un dossier
frère au préfixe commun (`/data/front-old` pour `/data/front`) n'est plus accessible. Les messages 404
et 415 ne recopient plus le nom demandé. Aucun impact pour un front servi normalement.

### CORS : liste d'origines `CORS_ALLOWED_ORIGINS` (W-12)

Nouvelle variable `CORS_ALLOWED_ORIGINS` (liste séparée par des virgules,
`ConfigBaseVariable.setCorsAllowedOrigins`). Sans elle, `CORSFilter` envoie
`Access-Control-Allow-Origin: *` mais **plus** `Access-Control-Allow-Credentials: true`. Avec elle,
seule une origine de la liste reçoit les en-têtes CORS (origine renvoyée, `Vary: Origin`,
`Access-Control-Allow-Credentials: true`) ; une autre origine n'en reçoit aucun.

**Qui est impacté** : un front servi depuis une autre origine que l'API doit figurer dans la liste dès
qu'elle est définie. Un front qui envoyait des cookies ou `credentials: 'include'` vers une API sans
liste ne reçoit plus `Allow-Credentials` (les navigateurs refusaient déjà `*` avec des identifiants).
Les appels serveur à serveur ne sont pas concernés : CORS ne s'applique qu'aux navigateurs.

### Messages d'erreur sans détail interne (W-13, D-10)

Ne renvoient plus le message brut de l'exception (il reste dans le log, sous l'`oid` de la réponse) :
`ExceptionCatcher` (toute exception non gérée, dont `DataAccessException` et les erreurs du driver
Mongo comme `E11000 … dup key`), `WebApplicationExceptionCatcher` pour un statut 5xx, et
`JacksonExceptionCatcher` : `message` vaut `Internal server error`. `RequestBodyExceptionCatcher`
renvoie `Unknown field 'x'`, `Invalid value for field 'a.b'` ou `Malformed JSON at line L, column C`
au lieu du message Jackson (noms de classes, liste des propriétés). Les 401 d'`AuthenticationFilter`
disent toujours `Missing or invalid authentication`. Les messages de `DataTools` ne contiennent plus de
nom de classe. Les messages volontaires (`FailException`, `InputException`, `SystemException`,
validation, `WebApplicationException` 4xx) ne changent pas.

**Qui est impacté** : un client ou un test qui lisait le texte d'une erreur 500, d'une erreur de corps
JSON ou d'un 401. Pour un message destiné au client, lever une `FailException`.

### Clé publique SSO : https obligatoire, délais, rechargement limité (W-14, W-15)

`JWTWrapper.initLocalTokenRemote` (donc `UpdateJwtPublicKey` avec `SSO_ADDRESS`) refuse une adresse
`http://` sauf vers `localhost` / une adresse de bouclage, ou après
`JWTWrapper.setAllowInsecureSsoAddress(true)`. Délais de 5 s (connexion) et 10 s (lecture), réponse
bornée à 64 Kio, redirections non suivies, clé RSA de moins de 2048 bits refusée. Un jeton à signature
inconnue ne déclenche plus qu'un rechargement de la clé par minute au plus. Les jetons invalides sont
journalisés en WARN, sans trace de pile.

**Qui est impacté** : un déploiement dont `SSO_ADDRESS` est en `http://` vers un autre hôte (par
exemple `http://karso:80/...` dans un réseau Docker) : passer en `https://` ou appeler
`JWTWrapper.setAllowInsecureSsoAddress(true)` au démarrage, avant de lancer `UpdateJwtPublicKey`.

### JWT : `exp` obligatoire, `nbf` vérifié (W-16)

Un jeton sans `exp` est refusé proprement (avant : `NullPointerException` attrapée par le filtre). Un
claim `nbf` présent doit être antérieur à maintenant plus 30 s. Les jetons de `generateJWToken` ont
toujours un `exp` : aucun impact pour eux.

### `uploadCover` : type détecté par le contenu (W-17)

`DataTools.uploadCover` détecte le type avec Tika, refuse (HTTP 406) un contenu qui n'est pas une
image de `SUPPORTED_IMAGE_MIME_TYPE`, et stocke le type détecté au lieu de celui déduit de
l'extension du nom de fichier. **Qui est impacté** : un appelant qui envoyait une image avec une
mauvaise extension (elle passe maintenant avec le bon type) ou un contenu qui n'est pas une image
(refusé).

### Swagger UI et spécification OpenAPI désactivés par défaut (W-18)

`openApiResource` (`/openapi/swagger.json`) et `SwaggerUiExternal` répondent 404 tant que
`API_DOC_ENABLED` ne vaut pas `true` (`ConfigBaseVariable.setApiDocEnabled("true")`). La page Swagger UI
charge ses fichiers cdnjs avec SRI (`integrity` + `crossorigin`).

**Qui est impacté** : un environnement de développement, ou un outil qui lit la spécification à chaud
(génération de client), doit définir `API_DOC_ENABLED=true`. La génération de clients TypeScript /
Python par `AnalyzeApi` ne passe pas par cet endpoint et n'est pas concernée.

### En-têtes de sécurité HTTP : `SecurityHeadersFilter` (W-19)

Nouveau filtre de réponse à enregistrer comme `CORSFilter` (`rc.register(SecurityHeadersFilter.class)`) :
`X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `X-Frame-Options: DENY`, et
`Strict-Transport-Security: max-age=31536000` seulement pour une requête reçue en https. Un en-tête déjà
posé par la ressource est conservé. **À faire** dans chaque application : l'enregistrer dans le
lanceur (le sample le fait). Un front qui doit être affiché dans une iframe pose son propre
`X-Frame-Options`.

### `MediaStreamer` ouvre le fichier à l'écriture (W-20)

Nouveau constructeur `MediaStreamer(File, long from, long length)` : le fichier n'est ouvert que dans
`write()` et y est fermé. `DataResource` l'utilise ; une requête `HEAD` ou un client qui coupe ne
laisse plus de descripteur ouvert. L'ancien constructeur `MediaStreamer(long, RandomAccessFile)` est
déprécié.

### Journaux sans en-tête `Authorization` ni corps au niveau INFO (D-15)

`RESTApiRequest.showIOStream()` masque `Authorization`, `ApiKey`, `Cookie`, `Set-Cookie` et les
paramètres de requête `Authorization` / `ApiKey` (`Bearer ***`), et ne journalise plus les corps
qu'au niveau TRACE (la taille reste en INFO). `MySecurityContext.getRole` journalise en TRACE.
**Qui est impacté** : un test qui lisait les corps dans les logs INFO : passer le logger
`org.atriasoft.archidata.tools.RESTApiRequest` en TRACE.

---

## Prochaine version (après v0.50.2) - Corrections de sécurité de la couche données (CHANGEMENT DE COMPORTEMENT)

Ce lot corrige les constats de l'audit de sécurité de la couche données (D-xx) et le mass
assignment à l'insertion (W-08). Plusieurs corrections changent un comportement observable.

### `updateById` sans `FilterValue` ne touche plus `@DataNotRead` ni `@DataEncrypt` (D-01)

Une mise à jour complète (`updateById(data, id)` sans `FilterValue`) écrivait tous les champs, y
compris `@DataNotRead` et `@DataEncrypt`. Comme une lecture standard laisse ces champs à `null` (non
projetés, ou non déchiffrés quand `DATA_DECRYPT_ENABLE` est désactivé), un « lire puis mettre à jour »
les effaçait (`$unset`) ; un corps JSON client pouvait aussi les écraser.

Désormais `FilterValue.getEditableFieldsNames(...)` (liste par défaut de `updateById`) exclut ces
champs protégés : ils ne sont écrits que si un `FilterValue` les nomme
(`new FilterValue("passwordHash")`). `FilterValue.getAllFieldsNames(...)` les contient toujours.
`ManyToManyTools.updateRemoteLinks` ne réécrit plus que le champ du lien.

**Comment migrer** : chercher les `updateById` sans `FilterValue` qui doivent écrire un champ
`@DataNotRead` ou `@DataEncrypt` (changement de mot de passe, de secret, de valeur chiffrée) et
nommer ce champ dans un `FilterValue`.

### Les champs `@ApiReadOnly` d'un corps de requête sont ignorés (W-08)

Le provider JSON du serveur (`ContextGenericTools.addJsr310(rc)`) jette en silence toute propriété
`@ApiReadOnly` d'un corps de requête, comme `@JsonProperty(access = READ_ONLY)`. Avant, un endpoint
`POST` qui passait l'entité désérialisée à `insert` sans `@ValidGroup(GroupCreate.class)` laissait le
client écrire un champ en lecture seule (mass assignment). Le client reçoit toujours ces champs dans
les réponses ; `RESTApiRequest` et `createObjectMapper()` ne filtrent rien. `insert` et `update`
gardent leur comportement côté serveur (`ForceReadOnlyField` reste nécessaire pour un update).

`User.blocked` porte maintenant `@ApiReadOnly` : un client ne peut plus l'envoyer, et un `updateById`
qui doit l'écrire passe `ForceReadOnlyField` (karso le fait déjà).

**Qui est impacté** : un endpoint qui lit un champ `@ApiReadOnly` dans le corps de la requête (la
valeur arrive maintenant `null`), et un corps qui faisait échouer `@Null(groups = GroupCreate.class)`
sur un champ en lecture seule (la requête passe maintenant, la valeur étant ignorée). Les propriétés
d'un constructeur (records, `@JsonCreator`) ne sont pas filtrées. Pour recevoir une valeur du client,
utiliser un DTO d'entrée sans `@ApiReadOnly`.

### `@InitValue` passe par le codec du champ (D-18)

La valeur `@InitValue` écrite à l'insertion passe maintenant par le codec du champ, comme une valeur
fournie. Sur un champ `@DataEncrypt`, elle était stockée en clair, et la lecture suivante échouait
(`Failed to decrypt`) ; elle est désormais chiffrée. Pour les autres types, la forme BSON est celle
d'une valeur normale du même champ. Les documents déjà écrits en clair par une `@InitValue` sur un
champ `@DataEncrypt` restent illisibles : les réécrire avec un `updateById` nommant le champ.

### `@DataNotRead` appliqué dans les sous-objets embarqués (D-02)

Un champ `@DataNotRead` d'un objet embarqué (POJO ou record, seul ou dans une liste) était lu et
renvoyé comme un champ normal. Il est maintenant caché par une lecture standard, comme au premier
niveau, et rendu avec `ReadAllColumn`. Le code qui lisait un tel champ sans `ReadAllColumn` le reçoit
désormais `null` (ou la valeur par défaut du constructeur).

### `Limit` et `Skip` validés (D-07)

`new Limit(n)` lève `IllegalArgumentException` si `n <= 0` ou si `n > Limit.getMaxValue()` (10 000 par
défaut, réglable avec `Limit.setMaxValue(long)`, au plus `Integer.MAX_VALUE`). Avant, `0` voulait dire
« sans limite » pour MongoDB et une valeur de 2^32 devenait `0` par troncature en `int`. `new Skip(n)`
refuse aussi `n > Integer.MAX_VALUE` (2^31 devenait négatif). L'offset de pagination
(`X-Pagination-Offset`) est borné à `Skip.MAX_VALUE`.

**Comment migrer** : borner toute valeur client avant de construire `Limit` (`Math.clamp`), et appeler
`Limit.setMaxValue(...)` au démarrage si une requête interne dépasse 10 000 documents.

### En-tête `Link` de pagination relatif (D-19)

Les liens `first` / `prev` / `next` / `last` de l'en-tête `Link` sont maintenant des références
relatives (`</foos?X-Pagination-Offset=50&X-Pagination-Limit=25>`) : l'hôte et le schéma venaient de
l'en-tête `Host` du client. Un client HTTP standard les résout contre l'URL de la requête ; un client
qui utilisait le lien tel quel comme URL absolue doit le résoudre (`URI.resolve`). Le WARN d'une valeur
de pagination non numérique ne contient plus la valeur brute, seulement sa longueur.

### Logs TRACE des lectures (D-09)

La trace TRACE de chaque document reçu n'est plus calculée quand TRACE est désactivé (le JSON indenté
était construit pour chaque document lu, même en production). Dans la trace du filtre et des
documents, les valeurs des champs `@DataNotRead` et `@DataEncrypt` sont remplacées par `***`. Les
autres valeurs restent écrites : TRACE sur `DBAccessMongo` reste interdit en production.

### Écritures de masse refusées sans `AllowMassOperation` (D-17)

Une écriture (`update`, `delete`, `deleteHard`, `deleteSoft`, `restore`, `updateBsonDocuments`) dont la
condition est absente ou vide (`new Condition()`, `new Condition(Filters.empty())`) lève maintenant une
`DataAccessException`. Avant, `new Condition()` passait le contrôle et visait toute la collection, et
`updateBsonDocuments` sans condition modifiait tous les documents. Pour une opération de masse voulue,
ajouter l'option `new AllowMassOperation()`. Les lectures ne changent pas.

**Comment migrer** : chercher les écritures dont la condition est construite conditionnellement
(`filters.isEmpty() ? Filters.empty() : ...`) et les `updateBsonDocuments` sans `Condition` (migrations).

### Expansion des relations bornée (D-06)

L'expansion d'une relation typée entité (`@ManyToOneDoc Node parent`...) s'arrête après
`RelationExpansion.getMaxDepth()` niveaux imbriqués (5 par défaut, réglable avec
`RelationExpansion.setMaxDepth(int)`), et une entité déjà en cours d'expansion plus haut sur la même
chaîne (cycle, auto-référence) est relue sans que ses relations soient chargées. Avant, un cycle
provoquait une récursion jusqu'au `StackOverflowError`. Au-delà de ces limites, le champ de relation
reste `null`.

### `Filters` : `$eq` explicite, noms de champs vérifiés, `regexLiteral` (D-08)

- `Filters.eq(champ, valeur)` produit `{champ: {$eq: valeur}}` au lieu de `{champ: valeur}` : une
  valeur `Map` ou `Document` venue d'un JSON client est comparée telle quelle, sans être lue comme des
  opérateurs. Le résultat est le même pour les valeurs typées, et les index partiels restent utilisés.
  Un test qui compare le JSON d'un filtre doit attendre la nouvelle forme.
- Tout nom de champ passé en chaîne à `Filters` ou à `new OrderItem(String, Order)` est vérifié :
  ni `$` en tête, ni caractère nul (`IllegalArgumentException`).
- Nouveaux : `Filters.checkFieldName(Classe.class, nom)` et `OrderItem.ofClientField(...)` pour un
  nom de champ choisi par le client (le champ doit exister et ne pas être `@DataNotRead` ni
  `@DataEncrypt`), et `Filters.regexLiteral(champ, texte[, options])` qui cite le texte
  (`Pattern.quote`). `Filters.regex` et `Filters.where` ne doivent jamais recevoir de valeur client.

### Journal de changements : champs `@DataNotRead` exclus (D-03)

`ChangeJournalEngine` copiait chaque document en entier dans `changeJournal`, champs `@DataNotRead`
compris (empreintes, jetons). Pour une classe enregistrée avec `addClass`, ces champs ne sont plus
copiés ; `createdAt`, `updatedAt` et `deleted` restent. Les champs `@DataEncrypt` sont copiés sous leur
forme stockée, donc chiffrés. Pour une collection ajoutée par nom ou capturée par `runAll()`, déclarer
les champs sensibles avec `excludeFields(collection, champs...)`. Les entrées déjà écrites ne sont pas
modifiées : les purger si elles contiennent des secrets.

### Sauvegardes : permissions 0600 et somme SHA-256 (D-04, en partie)

Les archives sont créées en `rw-------` (et le dossier de stockage en `rwx------` quand le moteur le
crée) au lieu des permissions de l'umask. Une somme SHA-256 est écrite à côté de chaque archive
(`<archive>.sha256`) et vérifiée avant toute restauration : une archive modifiée est refusée
(`IOException`). Une archive sans fichier `.sha256` (version précédente) reste restaurable avec un
WARN, sauf `setRequireChecksum(true)`. `clean(...)` supprime aussi le fichier `.sha256`.

**Attention** : les archives restent **en clair** (toute la base, champs `@DataNotRead` compris ;
seuls les champs `@DataEncrypt` restent chiffrés). Le chiffrement des sauvegardes attend une décision.
Un outil qui copie ou synchronise les archives doit aussi copier les fichiers `.sha256`.

### Restauration d'une archive non fiable bornée (D-05)

- Les chemins des médias sont comparés en `Path` normalisé : un dossier frère au même préfixe
  (`/data/media-autre` pour `/data/media`) est refusé, et un lien symbolique existant du dossier
  média qui pointe ailleurs n'est jamais suivi. Les entrées lien (symbolique ou physique) sont
  ignorées avec un WARN ; avant, elles devenaient des fichiers vides.
- Taille déclarée par entrée (`setRestoreMaxEntryBytes`, 64 Gio par défaut), taille totale
  (`setRestoreMaxTotalBytes`, 1 Tio) et longueur d'une ligne de document (`setRestoreMaxLineChars`,
  64 Mi caractères) bornées : au-delà, `IOException`.
- Un nom de collection doit respecter `[A-Za-z0-9_-][A-Za-z0-9_.-]{0,119}` et ne pas commencer par
  `system.` : sinon la restauration s'arrête (`IOException`).
- Une séquence contenant `/`, `\`, `..` ou un caractère nul est refusée (`IllegalArgumentException`).

**Qui est impacté** : une application dont une collection a un nom hors de ce motif, ou dont un
fichier média dépasse 64 Gio, doit régler les bornes avant de restaurer.

---

## v0.49.0 - Filtre soft-delete simplifié en égalité (CHANGEMENT DE COMPORTEMENT)

### Ce qui change

Le filtre de lecture des entités soft-delete passe de :

```javascript
{$or: [{deleted: false}, {deleted: {$exists: false}}]}
```

à une simple égalité :

```javascript
{deleted: false}
```

En contrepartie, archidata écrit désormais **systématiquement** le champ à l'insertion, même quand
le modèle ne déclare pas de `@DefaultValue` (avant, le champ restait absent dans ce cas).

### Pourquoi

MongoDB n'utilise un **index partiel** que si le prédicat de la requête implique son filtre. Un
`$or` acceptant aussi les documents sans le champ n'implique rien : la requête retombait en
`COLLSCAN`, ce qui annulait l'index censé la servir. Mesuré sur 20 000 documents : `COLLSCAN` avec
le `$or`, `IXSCAN` avec l'égalité.

L'index partiel est exactement l'outil adapté au soft-delete — il n'indexe que les documents
vivants. Il est maintenant utilisable :

```java
@Index(value = {"companyId", "-createdAt"}, partialFilter = "{\"deleted\": false}")
```

### Qui est impacté

Les documents **sans** le champ `deleted` (écrits hors archidata, ou antérieurs à l'ajout du champ
dans le modèle) deviennent invisibles aux lectures normales. Ils restent accessibles avec
`AccessDeletedItems`.

### Comment migrer

Sur chaque collection soft-delete contenant d'éventuels documents anciens :

```javascript
db.<collection>.updateMany({deleted: {$exists: false}}, {$set: {deleted: false}})
```

À exécuter côté application (migration applicative) : archidata ne connaît pas la liste des
collections concernées de ses dépôts consommateurs.

## v0.49.0 - `FilterValue` / `FilterOmit` deviennent effectifs en lecture (CHANGEMENT DE COMPORTEMENT)

### Ce qui change

`FilterValue` et `FilterOmit` n'agissaient que sur les écritures : passés à un `gets` / `get` /
`getById`, ils étaient **ignorés silencieusement** (alors que `doc/database_access.md` décrivait
déjà l'inverse). Ils pilotent désormais la **projection MongoDB** de la lecture : un champ non
sélectionné n'est ni transféré ni mappé, et un champ de relation exclu ne déclenche plus la requête
supplémentaire de sa résolution.

Un `FilterValue` accepte en plus les **chemins pointés** (`"address.city"`) pour ne transférer
qu'une partie d'un sous-document embarqué.

### Qui est impacté

Uniquement le code qui passait déjà un `FilterValue` / `FilterOmit` à une **lecture** en croyant
qu'il filtrait : les objets retournés n'ont plus que les champs demandés, les autres restent à leur
valeur par défaut. Les écritures (`update`, `updateById`) sont inchangées.

**Avant :** `da.gets(Article.class, new FilterValue("title"))` renvoyait tous les champs.
**Après :** seuls `title` et la clé primaire sont lus.

### Comment migrer

- Pour retrouver l'ancien comportement, retirer l'option de l'appel de lecture.
- La clé primaire reste toujours lue, même sous `FilterOmit` : un objet sans identifiant ne pourrait
  plus être mis à jour ni supprimé.
- Deux `FilterValue` (ou deux `FilterOmit`) sur une même requête lèvent maintenant une
  `DataAccessException` au lieu d'un choix arbitraire.
- Un nom de champ inconnu (faute de frappe) lève une `DataAccessException` : sans ça, une liste
  blanche erronée ne lirait plus rien du tout, en silence.
- Les chemins pointés sont refusés dans un `FilterOmit` (une projection ne peut pas mélanger
  inclusions et exclusions) : lister ce qu'on garde avec `FilterValue`.
- `@DataNotRead` reste prioritaire : `ReadAllColumn` lève cette exclusion mais ne contourne pas une
  liste blanche explicite.

Voir [doc/database_access.md](doc/database_access.md#restricting-what-a-read-transfers).

## v0.43.1 - Suppression du mode test JWT (BREAKING CHANGE)

### Ce qui change

Le mécanisme `TEST_MODE` / `TestSigner` / `createJwtTestToken()` a été **supprimé** de `JWTWrapper`.
La variable d'environnement `TEST_MODE` n'a plus d'effet sur la validation JWT.

**Supprimés :**
- Classe `TestSigner` (signature statique hardcodée)
- Méthode `JWTWrapper.createJwtTestToken()`
- Bypass de vérification de signature JWT en mode test
- Bypass d'expiration JWT en mode test

### Pourquoi

La signature test `TEST_SIGNATURE_FOR_LOCAL_TEST_AND_TEST_E2E` était hardcodée dans le code source.
Si `TEST_MODE=true` était activé en production (par erreur ou attaque), n'importe qui pouvait forger
des tokens JWT valides sans clé cryptographique.

### Comment migrer vos tests

**Avant (ancien code) :**
```java
// WebLauncherTest.java
ConfigBaseVariable.testMode = "true";

// Common.java
public static final String TOKEN = JWTWrapper.createJwtTestToken(
    16512, "test_user", "Karso", "myapp",
    Map.of("myapp", Map.of("USER", Boolean.TRUE)), null);
```

**Après (nouveau code) :**
```java
// Common.java - initialiser les clés RSA et générer de vrais tokens
public class Common {
    static {
        try {
            JWTWrapper.initLocalToken(null); // Génère une paire RSA éphémère
        } catch (final Exception e) {
            throw new RuntimeException("Failed to init JWT keys for tests", e);
        }
    }

    public static final String USER_TOKEN = JWTWrapper.generateJWToken(
        16512L, "test_user_login", "Karso", "myapp",
        Map.of("myapp", Map.of("USER", Boolean.TRUE)),
        null, 3600);

    public static final String ADMIN_TOKEN = JWTWrapper.generateJWToken(
        16512L, "test_admin_login", "Karso", "myapp",
        Map.of("myapp", Map.of("USER", Boolean.TRUE, "ADMIN", Boolean.TRUE)),
        null, 3600);
}

// WebLauncherTest.java - supprimer la ligne testMode
// ConfigBaseVariable.testMode = "true";  // SUPPRIMER CETTE LIGNE
```

### Points clés

1. `JWTWrapper.initLocalToken(null)` doit être appelé **avant** `generateJWToken()`
2. Les tokens générés sont maintenant signés avec une vraie clé RSA éphémère
3. La validation JWT fonctionne identiquement en test et en production
4. Le timeout est en minutes (3600 = 60 heures, largement suffisant pour les tests)
5. Le paramètre `roles` de `generateJWToken()` utilise `Map<String, Object>` (pas `Map<String, Map<String, Object>>`)

---

## v0.43.1 - ConfigBaseVariable : champs privés + lock/unlock (BREAKING CHANGE)

### Ce qui change

Tous les champs de `ConfigBaseVariable` sont maintenant **privés**. L'accès se fait via des setters
et des getters. La configuration peut être **verrouillée** après le démarrage du serveur.

**Supprimé :** accès direct `ConfigBaseVariable.apiAdress = "..."` (et tous les autres champs)

### Pourquoi

Les champs étaient `public static` (non-final). N'importe quel code dans le classpath pouvait
modifier la configuration à runtime, y compris des valeurs sensibles comme `dbPassword` ou `testMode`.

### Comment migrer

**Écriture — remplacer les accès directs par les setters :**

| Ancien code | Nouveau code |
|---|---|
| `ConfigBaseVariable.apiAdress = "..."` | `ConfigBaseVariable.setApiAddress("...")` |
| `ConfigBaseVariable.bdDatabase = "..."` | `ConfigBaseVariable.setBdDatabase("...")` |
| `ConfigBaseVariable.dbPort = "..."` | `ConfigBaseVariable.setDbPort("...")` |
| `ConfigBaseVariable.dataFolder = "..."` | `ConfigBaseVariable.setDataFolder("...")` |
| `ConfigBaseVariable.testMode = "..."` | `ConfigBaseVariable.setTestMode("...")` |
| *(idem pour tous les autres champs)* | |

**Lecture — utiliser les getters existants :**

| Ancien code | Nouveau code |
|---|---|
| `ConfigBaseVariable.apiAdress` | `ConfigBaseVariable.getLocalAddress()` |
| `ConfigBaseVariable.bdDatabase` | `ConfigBaseVariable.getDBName()` |

### Verrouillage de la configuration

Après le démarrage du serveur, appelez `ConfigBaseVariable.lock()` pour interdire toute modification.

```java
// Dans votre WebLauncher.process() après le démarrage :
ConfigBaseVariable.lock();
```

En production, `unlock()` est interdit et **crashe immédiatement le programme** avec la backtrace
du code appelant (traité comme une violation de sécurité).

### Reconfiguration entre tests

Pour les tests, appelez `allowReconfiguration(true)` une seule fois au début. Cela permet
`unlock()` / `clearAllValue()` entre les tests.

```java
@BeforeAll
static void setup() {
    ConfigBaseVariable.allowReconfiguration(true);
    ConfigBaseVariable.setApiAddress("http://127.0.0.1:12345/test/api/");
    // ...
}

@AfterAll
static void cleanup() {
    ConfigBaseVariable.clearAllValue(); // unlock implicite + reset
}
```

---

## v0.43.1 - Suppression du mot de passe DB par défaut (BREAKING CHANGE)

### Ce qui change

`ConfigBaseVariable.getDBPassword()` ne retourne plus `"base_db_password"` par défaut.
Si `DB_PASSWORD` n'est pas configuré (variable d'environnement ou `setDbPassword()`), une
`IllegalStateException` est levée au démarrage.

### Comment migrer

**Production :** s'assurer que la variable d'environnement `DB_PASSWORD` est définie.

**Tests :** ajouter `ConfigBaseVariable.setDbPassword("base_db_password")` dans le `ConfigureDb.configure()`.

---

## v0.43.1 - Suppression de webp-imageio (CVE-2023-4863)

### Ce qui change

La dépendance `com.github.gotson:webp-imageio:0.2.2` a été supprimée. Elle embarquait une
version native de libwebp datant de 2021, vulnérable au heap buffer overflow CVE-2023-4863.

### Impact

- **Lecture WebP** : toujours fonctionnelle via TwelveMonkeys (`imageio-webp`)
- **Écriture WebP** : plus disponible. Si `THUMBNAIL_FORMAT=webp` était configuré, les thumbnails
  échoueront. Le format par défaut `png` n'est pas affecté.

### Comment migrer

Si vous utilisiez `THUMBNAIL_FORMAT=webp`, passez à `png` ou `jpg`.

---

## v0.43.1 - Adresse API par défaut changée à localhost

### Ce qui change

L'adresse API par défaut passe de `http://0.0.0.0:80/api/` à `http://localhost:80/api/`.

### Impact

`0.0.0.0` écoutait sur toutes les interfaces réseau (y compris l'extérieur).
`localhost` n'écoute que sur l'interface locale. En production, c'est le reverse proxy
(nginx/traefik) qui expose le service vers l'extérieur.

### Comment migrer

Si vous avez besoin d'écouter sur toutes les interfaces, définissez explicitement
`API_ADDRESS=http://0.0.0.0:80/api/` dans vos variables d'environnement ou via
`ConfigBaseVariable.setApiAddress("http://0.0.0.0:80/api/")`.
