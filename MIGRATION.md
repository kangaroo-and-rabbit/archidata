# Migration Guide

## Prochaine version (après v0.52.0) - `rest-tools.ts` strict, sans `enum`, sans jeton dans la console

Le runtime du client TypeScript passe le mode `strict` de TypeScript et le format des fronts. Il suffit de
régénérer le client (`GenerateApi`) : le code qui utilise le client ne change pas.

- `HTTPMimeType` et `HTTPRequestModel` ne sont plus des `enum` mais des objets `as const` accompagnés d'un type du
  même nom : `HTTPMimeType.JSON` et `accept: HTTPMimeType` s'écrivent comme avant, et les valeurs sont identiques.
  Le type accepte en plus la chaîne littérale (`'application/json'`). Seul un membre utilisé **comme type** change :
  `HTTPMimeType.JSON` s'écrit alors `(typeof HTTPMimeType)['JSON']`, comme le fait le générateur pour `produce`.
- La console du navigateur et les journaux du client Python ne reçoivent plus l'URL complète : avec `tokenInUrl` ou
  `token_in_url`, elle contenait le jeton. `RESTError.url` du client Python perd aussi sa partie requête.
- Une valeur de requête `undefined` ou `null` n'est plus envoyée : `?limit=undefined` faisait échouer l'appel.
- `fetchProgress` termine sa promesse sur un 204 ou une réponse sans en-tête lisible, au lieu de rester en attente.

## Prochaine version (après v0.51.3) - Secrets lus depuis des fichiers

Les mots de passe restaient dans l'environnement, donc dans `docker compose config`, `docker inspect` et
`/proc/<pid>/environ`. `DB_PASSWORD`, `SSO_TOKEN` et `EMAIL_PASSWORD` acceptent désormais `<NOM>_FILE`, comme
`DATA_ENCRYPT_KEY`. Le mode direct continue de fonctionner, sans rien changer aux déploiements existants, à
part le WARN et le cas `DATA_ENCRYPT_KEY_FILE` ci-dessous. Détails : section Secrets de `doc/tools.md`.

### `DATA_ENCRYPT_KEY_FILE` illisible arrête le démarrage (CHANGEMENT DE COMPORTEMENT)

Avant : l'erreur était journalisée et la clé restait `null`, ce qui n'échouait qu'au premier champ chiffré.
Après : le chargement de `ConfigBaseVariable` échoue, de même pour un fichier vide. L'application reçoit une
`ExceptionInInitializerError`, dont la cause `IllegalStateException` nomme la variable, sans sa valeur ni le
chemin ; ce n'est pas une `Exception`, donc un `catch (Exception)` ne l'attrape pas. Il en va de même pour les
trois nouvelles variables `_FILE` ; aucun repli sur `<NOM>` n'est fait.

### Migrer un site

1. Écrire chaque secret dans son propre fichier, droits `root:<gid du site> 0440`.
2. Déclarer le fichier dans `secrets:`, ajouter `group_add`, remplacer `DB_PASSWORD` par `DB_PASSWORD_FILE`.
3. Vérifier qu'aucun WARN `Secret ... is set in the environment` ne reste au démarrage.
4. Ajouter `SECRETS_FILE_ONLY=true` : la variable directe, si elle revient, empêchera alors de démarrer. Toute
   autre valeur que `true` ou `false` empêche aussi de démarrer.

### Secrets propres aux applications

Remplacer `System.getenv` par `ConfigBaseVariable.readSecret` (`readSecretBytes` pour du binaire), au
démarrage, pour bénéficier des mêmes règles.

## Prochaine version (après v0.51.3) - Objets embarqués écrits par leur type déclaré, lectures projetées jusqu'aux feuilles (CHANGEMENT DE COMPORTEMENT)

Suite du point CS-01 resté ouvert (SECURITY_AUDIT.md) : archidata écrivait un objet embarqué avec la
classe **réelle** de la valeur et le relisait avec la classe **déclarée**. Une sous-classe stockée sous un
champ de classe de base, ou toute valeur d'un champ `Object` / interface / classe abstraite / collection
brute, écrivait en base des champs (y compris `@DataNotRead` / `@DataEncrypt`) qu'aucune lecture ne
rendait, mais qu'un filtre comparant le sous-document entier pouvait atteindre. Et toute lecture
transférait les objets embarqués entiers, sous-champs protégés compris, pour les jeter ensuite en Java.
Désormais archidata ne stocke que ce qu'il sait relire, et ne transfère que ce que la lecture rend ; et le
client MongoDB refuse d'encoder, par les API brutes, un objet de l'application qui porte un champ protégé.

### Un champ au type polymorphe ou non typé est refusé à l'analyse du modèle

Avant : accepté ; les données étaient écrites par la classe réelle et relues mal (objet vide, échec, ou
champs perdus). Après : le modèle de l'entité est refusé à sa première analyse (`DbClassModel.of`, donc au
premier insert, lecture, `IndexEngine.addClass`, `ChangeJournalEngine.addClass`, `Filters.checkFieldName`...),
comme une annotation JPA. Sont refusés, pour toute propriété persistée (champ public ou getter) qui n'est pas
une relation, à la racine et dans toute classe embarquée atteignable (champs, éléments de `List` / `Set`,
valeurs de `Map`, à toute profondeur, cycles compris) :

| Type déclaré | Remplacement |
| --- | --- |
| `Object`, une variable de type (`T`) (sauf la clé primaire, voir plus bas) | la classe concrète, ou `org.bson.Document` pour un contenu libre |
| une interface ou une classe abstraite, y compris du JDK (`Serializable`, `Number`, `CharSequence`, `Comparable`) et Jackson `JsonNode` | la classe concrète, ou `org.bson.Document` |
| `List` / `Set` / `Map` bruts, `List<?>`, `List<? extends X>`, `List<T>` | `List<MaClasse>`, `Set<MaClasse>`, `Map<String, MaClasse>` |
| `Collection<X>`, `Iterable<X>`, `Queue<X>`, `ArrayDeque<X>`... | `List<X>` ou `Set<X>` |
| un tableau d'une classe de l'application ou d'un des cas ci-dessus (`Point[]`, `Object[]`, `Serializable[][]`) | `List<Point>` ou `Set<Point>` |
| `List` / `Set` / `Map` dont l'élément ou la valeur est l'un des cas ci-dessus, dont les plus courants `List<Object>` et `Map<String, Object>` (aussi `List<List<Object>>`, `Set<MonInterface>`) | idem, sur l'élément : `List<MaClasse>`, `Map<String, MaClasse>` ; pour un contenu libre, un `org.bson.Document` (à la place d'une `Map<String, Object>`) ou une `List<Document>` |

L'exception est une `IntrospectionException` (`DB model creation failed for Account`) dont la cause,
une `DataAccessException`, dit tout :

```
Unsupported field type on com.app.Account: field Wrapper.inner (path wrapper.inner) is declared java.lang.Object, which can hold any class (Object, an interface or an abstract class), and archidata stores and reads an embedded value by its declared type only; declare a concrete class (or a List, Set or Map of one), or org.bson.Document for free-form content (see MIGRATION.md)
```

Variantes : `..., whose element type java.lang.Object can hold any class ...` (élément d'une liste),
`(path free.<key>) ... whose value type ...` (valeur d'une map), `which is a raw type whose element type is
unknown`, `which has an element type that cannot be resolved (a wildcard or a type variable)`, `which is a
collection that is neither a List nor a Set: only List, Set and Map are stored as collections; declare a List
or a Set`, `which is an array of a class of the application (or of a type that can hold any class), which
archidata does not store element by element: ...`. Une classe refusée n'est pas gardée en cache : chaque appel
relève la même erreur. Une classe abstraite (ou une interface) de l'application analysée comme racine (entité
abstraite passée à `Filters.checkFieldName`, au masquage TRACE ou au journal) est vérifiée comme une classe
concrète.

**Clé primaire déclarée `Object`** : exemptée du refus, et toujours gérée comme avant (`@Id public Object
_id`, son type donné à chaque appel par `OptionSpecifyType("_id", ObjectId.class)`, insertion avec
`DirectPrimaryKey`). Nouveau : sa valeur est vérifiée à l'écriture, elle doit être un `ObjectId`, un `UUID`,
une `String`, un `Long`, un `Integer` ou un `Short` ; sinon `DataAccessException` `Field com.app.Keyed._id is a
primary key declared java.lang.Object but holds a com.app.Credentials: such a key only takes an ObjectId, a
UUID, a String, a Long, an Integer or a Short`. Le refus reste entier pour **tout autre champ** déclaré
`Object`, interface, classe abstraite ou générique non résolu (`List<T>`), même lu avec `OptionSpecifyType`
(simple ou liste) : l'option ne lève pas le refus, le modèle est refusé avant qu'elle soit lue.

**Le refus suit la déclaration, y compris dans les classes de bibliothèques.** Toute classe qui n'est ni du
JDK (`java.`, `javax.`, `jdk.`, `sun.`, `com.sun.`), ni de Jackson (`com.fasterxml.`), ni de BSON (`org.bson.`)
est parcourue comme un objet embarqué : `jakarta.*`, JTS, les classes GeoJSON du driver
(`com.mongodb.client.model.geojson.Point`), Joda-Time... Une seule propriété (champ public ou getter) déclarée
`Object`, interface ou classe abstraite dans une telle classe bloque le démarrage, même si elle vaut toujours
`null`. Exemple vérifié : un champ `com.mongodb.client.model.geojson.Point location` donne `field
Point.coordinateReferenceSystem (path location.coordinateReferenceSystem) is declared
com.mongodb.client.model.geojson.CoordinateReferenceSystem, which can hold any class ...`. Contournement : un
`org.bson.Document` (pour un point GeoJSON : `new Document("type", "Point").append("coordinates", List.of(lon,
lat))`, ou l'objet GeoJSON du driver placé **dans** un `Document`, écrit par le codec du driver), ou une classe
concrète de l'application (DTO) convertie à l'entrée et à la sortie.

Non refusés, comportement inchangé : les types valeur (primitifs et boxés, `String`, `Character`,
`ObjectId`, `UUID`, `Date`, `Instant`, `OffsetDateTime`, `LocalDate`, `LocalTime`, enums), les types BSON
(`org.bson.Document`, contenu libre relu à l'identique, mais voir « Le contenu d'un `Document` est vérifié
par une liste d'autorisation » plus bas), les relations (`@ManyToOneDoc`, `@OneToManyDoc`, `@ManyToManyDoc`), les classes
concrètes de l'application. Les autres classes du JDK, de BSON et de
Jackson (`BigDecimal`, `LocalDateTime`, `Optional`, `Decimal128`, `ObjectNode`) et les tableaux de valeurs
ou de ces classes (`byte[]`, `String[]`, `BigDecimal[]`) ne sont ni refusés ni gérés : le codec ne les convertit
pas (aller-retour en échec avant comme après). Deux changements les concernent :

- y écrire une valeur d'une classe de l'application (sous-classe de `BigDecimal`, implémentation de
  `org.bson.conversions.Bson`, tableau de telles classes) échoue (`DataAccessException` `Field
  com.app.Account.price is declared java.math.BigDecimal, a type archidata does not model, but holds a
  com.app.Money, a class of the application: it cannot be stored there; declare the field with that class`) :
  elle était écrite avec son propre modèle, champs protégés compris, à une position que le garde des filtres
  croyait libre ;
- tout ce qui est stocké **sous** un tel champ (`Locale`, `URI`, `BigDecimal`, `LocalDateTime`...) est
  traité comme inconnu : `Filters.checkFieldName` / `OrderItem.ofClientField` refusent le chemin
  (`price.scale`), le journal retire les clés (le champ y est copié vide), le masquage TRACE les masque. Le
  champ lui-même reste accepté par `checkFieldName`.

Qui est concerné : une entité (ou un objet embarqué) qui déclare un tel champ ou un getter de ce type. Les
applications relevées (karso, karideo, karusic, karentory) et `sample/archidata/basic` n'en ont pas. Pour
le vérifier sans attendre un appel : `DbClassModel.of(MonEntite.class)` dans un test, pour chaque entité.

### Un objet embarqué est écrit par son type déclaré

Avant : `insert`, `updateById` complet (y compris l'écriture champ par champ des objets embarqués à champs
protégés) et partiel (`FilterValue`) écrivaient un objet embarqué, un élément de `List` / `Set` ou une
valeur de `Map` avec le modèle de sa classe réelle. Après : avec le modèle de son type déclaré, à toute
profondeur. Une valeur d'une sous-classe ne stocke que les champs du type déclaré ; les champs ajoutés par
la sous-classe sont perdus, et un WARN est écrit une fois par champ et par classe réelle (noms seulement,
jamais de valeur), logger `org.atriasoft.archidata.dataAccess.model.codec.MongoCodecFactory` :

```
Field com.app.Holder.base is declared com.app.Base but holds a com.app.Derived: only the fields of Base are stored, the fields Derived adds are dropped ([extra, secret])
```

(`An element of field ...` pour un élément de liste ou de set, `A value of field ...` pour une valeur de
map.) Une valeur qui n'est pas une instance du type déclaré (glissée par un type brut) fait échouer
l'écriture : `DataAccessException` `An element of field com.app.Holder.bases is declared com.app.Base but
holds a java.lang.String, which is not one: it cannot be stored`.

Qui est concerné : une application qui range une sous-classe sous un champ déclaré avec la classe de base.
La lecture rendait déjà la classe de base sans les champs ajoutés : seul le contenu de la base change.
Action : chercher `but holds a` dans les logs ; si les champs de la sous-classe doivent être gardés,
déclarer la sous-classe sur le champ. Une sous-classe anonyme ou « double accolade » (`new Base() {{ label
= "x"; }}`) est une sous-classe : même WARN (classe réelle `com.app.Service$1`), seuls les champs de `Base`
sont écrits, comme pour toute sous-classe. Une valeur dont la classe a été chargée par un autre classloader
que la classe déclarée (rechargement à chaud, plugin ; atteignable par un élément de `List` / `Set` ou une
valeur de `Map`, pas par un champ typé) n'est pas une instance de la classe déclarée : l'écriture échoue
(`... which is not one: it cannot be stored`) au lieu d'écrire la valeur par sa propre classe.

**Le contenu d'un `Document` est vérifié par une liste d'autorisation.** Avant : un `org.bson.Document`
(champ, élément de `List<Document>`, valeur de `Map<String, Document>`) était passé tel quel au driver, qui
encode un objet de l'application qu'il contient avec son codec POJO automatique, **tous ses champs**,
`@DataNotRead` compris et `@DataEncrypt` **en clair**. Après : le contenu est parcouru à l'écriture (`insert`,
mise à jour complète ou partielle, à toute profondeur) et n'est accepté que s'il est fait de :

- `null` et des valeurs : `String`, les primitifs boxés (`Character` compris), `BigDecimal`, `ObjectId`,
  `UUID`, `Date`, `Instant`, `OffsetDateTime`, `LocalDate`, `LocalTime`, `LocalDateTime`, `Pattern`, les enums
  (écrits par leur nom), les tableaux de primitifs (`byte[]`...) ; la classe exacte compte (une sous-classe de
  `Date` comme `java.sql.Timestamp` est refusée) ;
- des types BSON : tout `BsonValue` (`BsonDocument`, `BsonArray`...), `Binary`, `Decimal128`, `Code`, `Symbol`,
  `MinKey`, `MaxKey`, `BSONTimestamp`, `JsonObject`, une `Geometry` GeoJSON du driver (`Point`...) ;
- parcourus : une `Map` à clés `String` (`Document` compris), une `Collection` (`List`, `Set`...), un tableau
  d'objets, la portée d'un `CodeWithScope`, l'id d'un `DBRef` ; au plus 100 niveaux (la limite de MongoDB).

Tout le reste fait échouer l'écriture, rien n'est écrit. Une classe de l'application (POJO, record,
implémentation de `Bson`) : `DataAccessException` `Field com.app.Account.free is a free-form org.bson.Document
holding, under 'list.1.deep.k', a com.app.Credentials, a class of the application: the driver would store all
its fields (@DataNotRead and @DataEncrypt ones included, in clear); put only values, Documents, Lists and Maps
in a Document, or declare a field with that class` (`An element of field ...` pour un élément de liste). Toute
autre classe (une enveloppe du JDK comme `AtomicReference`, `Optional` ou `Map.Entry`, un `StringBuilder`, un
nœud Jackson, une `Map` à clé non `String`), dont le contenu ne serait pas vérifié : `Field
com.app.Account.free is a free-form org.bson.Document holding, under 'list.0.x', a
java.util.concurrent.atomic.AtomicReference, which is not a value archidata accepts in a Document (a BSON or
JDK value, an enum, a Document or a Map with String keys, a List, a Collection or an array of them): what it
holds would not be checked; put only values, Documents, Lists and Maps in a Document, or declare a field with
the class it holds` (`a java.util.HashMap with a java.lang.Integer key` pour une clé non `String`). Qui est
concerné : un `Document` qui contient une autre classe que celles listées ; la convertir en valeur, `Document`,
`List` ou `Map` avant l'écriture. Les API brutes (`insertBsonDocument`, `updateBsonDocuments`, la collection
du driver) ne passent pas par ce contrôle ; elles refusent en revanche un objet de l'application qui porte un
champ `@DataNotRead` / `@DataEncrypt` (voir « Le driver refuse d'encoder un objet qui porte un champ protégé »).

API :

- `MongoCodecFactory.writeEmbeddedVisibleFields(String, Object, Document, Document)` est dépréciée (elle
  écrit par la classe réelle) : utiliser `writeEmbeddedVisibleFields(String, DbPropertyDescriptor, Object,
  Document, Document)`.
- `MongoCodecFactory.isEmbeddedObjectType` ne répond plus `true` pour une classe du JDK, de BSON ou de
  Jackson, ni pour une classe abstraite : seulement pour une classe concrète de l'application.
- `MongoCodecFactory.buildWriter(TypeInfo)` d'un type `Object` / interface / abstrait, ou d'une collection
  sans type d'élément, rend un writer qui refuse toute valeur (`DataAccessException`).
- `OptionSpecifyType` n'est pas dépréciée : elle sert à la clé primaire déclarée `Object` (seul usage
  restant). Pour un champ `Object`, interface ou abstrait, elle ne lève pas le refus (le modèle est refusé
  avant). Sur un champ `List<X>` d'une classe concrète, `OptionSpecifyType("champ", Y.class, true)` fait
  toujours relire les éléments comme des `Y` (inchangé) : les éléments sont écrits par `X`, et un sous-champ
  que `X` protège n'est rendu que si la lecture le rend (`ReadAllColumn`, déchiffrement activé), même si `Y`
  le déclare sans protection (voir « Les lectures projettent les objets embarqués jusqu'aux feuilles »). Sur
  tout autre champ, la lecture ignore l'option.
- `MongoCodecFactory.isWrittenByRuntimeClass` (ajoutée après v0.51.3, dans aucune version publiée) est
  dépréciée et répond toujours `false` : plus rien n'est écrit par la classe de la valeur. Les règles de
  type sont dans `org.atriasoft.archidata.dataAccess.model.DeclaredTypeRules`.
- `MongoCodecFactory.resolveElementTypeInfo` (publique) rend `null` quand le type des éléments est inconnu :
  joker (`List<?>`, `List<? extends X>`), variable de type (`List<T>`) ou tableau générique, où elle levait
  une `IllegalArgumentException` (`Cannot resolve Type`), et pour un type tableau, où elle rendait le type
  de ses composants.

### Les lectures projettent les objets embarqués jusqu'aux feuilles

Avant : la projection MongoDB d'une lecture nommait les champs de premier niveau ; un objet embarqué était
transféré entier (sous-champs `@DataNotRead`, `@DataEncrypt` et clés inconnues du modèle compris) puis
filtré en Java. Après : il est projeté champ par champ, par son type déclaré (`credentials.user`), pour
toute lecture (`get*`, relations développées, `readPreviousDataForUpdate`, pagination) :

| Type déclaré | Avant | Après |
| --- | --- | --- |
| valeur, `org.bson.Document`, classe JDK, relation | entier | entier (inchangé) |
| objet embarqué | entier | ses sous-champs rendus par la lecture, à toute profondeur |
| objet embarqué dont aucun sous-champ n'est rendu | entier | `champ._archidata_presence_` : relu vide, `null` reste `null` |
| `List` / `Set` d'objets embarqués | entière | ses chemins feuilles **seulement si** la lecture cache un sous-champ des éléments ; sinon entière |
| `Map` | entière | entière (clés dynamiques) : les sous-champs protégés de ses valeurs transitent, sans log, et la lecture Java les jette |
| `FilterValue("obj.secret")` sans `ReadAllColumn` | `obj.secret` transféré puis jeté | non transféré (`obj` relu vide, comme avant) |
| `FilterValue("obj.cleInconnue")` | poussé tel quel | non transféré (`obj` relu vide, comme avant) |
| `FilterValue("obj")` | `obj` entier | `obj` jusqu'aux feuilles |
| `List<X>` lu en `List<Y>` (`OptionSpecifyType("champ", Y.class, true)`) | entière | les feuilles de `Y`, moins les sous-champs que `X` ou `Y` protège (liste développée dès que `X` protège un sous-champ des éléments, que `Y` le déclare ou non) |

Un sous-champ est laissé de côté quand la lecture Java le laisserait vide : `@DataNotRead` sans
`ReadAllColumn`, `@DataEncrypt` quand `DATA_DECRYPT_ENABLE` n'est pas `true`, relation d'un objet embarqué
(jamais relue). Un `FilterValue` ne le fait pas relire (il restreint la lecture, il n'ouvre pas un champ
protégé) : seuls `ReadAllColumn` et le déchiffrement activé le rendent. La liste ne contient jamais `a` et
`a.b` (le plus large gagne).

**Bornes.** Au-delà de 8 niveaux sous le champ (type récursif) ou de 256 chemins pour un champ, la position
est projetée entière ; la lecture Java jette toujours ce qu'elle ne rend pas. Quand la partie projetée
entière contient un sous-champ que la lecture ne rend pas, ce sous-champ est **transféré** depuis la base :
un WARN le dit, une fois par champ d'entité (logger `org.atriasoft.archidata.dataAccess.model.LeafProjection`) :

```
Field com.app.Account.node (declared com.app.Node) is projected whole at 'node.child.child.child.child.child.child.child.child' because it is deeper than 8 levels, and it holds sub-fields this read does not return (@DataNotRead, @DataEncrypt): they are transferred from the database, then dropped by the Java read
```

(`because it develops into more than 256 projection paths` pour la borne des chemins, la position étant
alors le champ lui-même.) Sans sous-champ protégé dans la partie coupée, un log DEBUG seulement (message
contenant `projected whole`). Le WARN ne couvre que ces coupes : une `Map` est transférée entière sans log. Action : chercher `is projected whole at` dans les logs ; pour que rien de
protégé ne soit transféré, réduire la profondeur ou la largeur du type, ou sortir les champs protégés de la
partie coupée.

**`OptionSpecifyType`.** Un champ que la lecture décode avec une autre classe (la clé primaire déclarée
`Object`, ou `isList` sur un champ `List<X>` lu en `List<Y>`) est projeté par les feuilles de `Y`, moins tout
sous-champ que `Y` **ou `X`** (la classe qui a écrit les valeurs) protège, sauf si la lecture le rend
(`ReadAllColumn`, déchiffrement activé). Les deux classes sont parcourues côte à côte sur les clés du
document : un sous-champ que seul `Y` connaît est projeté par `Y` ; une position que `Y` prend entière (une
valeur, une `Map`, un `Document`) alors que `X` y protège un sous-champ est projetée par `X` ; la liste est
développée dès que `X` protège un sous-champ des éléments, que `Y` le déclare ou non. Ce que la projection
doit transférer entier (une `Map`, une partie coupée par les bornes) est filtré en Java par `X` avant que `Y`
le décode : un sous-champ que `X` protège et que la lecture ne rend pas n'atteint jamais `Y`, même si `Y` le
déclare sans protection. Avant, le champ était transféré entier et décodé par `Y`, qui rendait un tel
sous-champ. Quand la lecture le rend, `Y` décode la valeur stockée avec son propre type : un sous-champ
`@DataEncrypt` de `X` est stocké chiffré, donc, déchiffrement activé, `Y` reçoit la valeur en clair seulement
s'il le marque aussi `@DataEncrypt` ; déclaré `String` simple, il reçoit l'enveloppe stockée (le chiffré en
Base64) ; déclaré d'un autre type, la lecture échoue (`DataAccessException`) ; comme avant. Partout ailleurs
la lecture ignore l'option, la projection aussi.

Les chemins d'un champ lu entier ne dépendent que de son type déclaré (et de la classe d'un
`OptionSpecifyType`), de `ReadAllColumn` et de `DATA_DECRYPT_ENABLE` : ils sont calculés une fois par clé et
gardés en cache (borné à 4096 entrées, vidé avec `DbClassModel.clearCache()` / `setAddOns`) ; seuls les
chemins pointés d'un `FilterValue` sont résolus à chaque lecture. Conséquence : les logs DEBUG d'un champ
entier sont écrits une seule fois par clé, au premier champ rencontré de ce type ; le WARN est écrit une fois
par champ d'entité (sa mémoire est vidée avec le cache).

Ce qui change pour l'application :

- **Éléments `null` d'une liste, indices décalés** : une `List` / `Set` d'objets embarqués projetée jusqu'aux
  feuilles perd ses éléments `null` **et ses éléments qui ne sont pas des sous-documents** (une chaîne
  stockée par erreur) : MongoDB les retire de la réponse, donc les indices de la liste relue ne sont plus
  ceux de la base dès qu'un tel élément précède. Concerne seulement les listes dont l'élément a un sous-champ
  caché par la lecture (une lecture `ReadAllColumn` avec le déchiffrement activé les relit entières, `null`
  compris). Vérifié par `TestLeafProjectionCases.testNullListElementsAgainstTheUpdates` :
  - une mise à jour complète (`updateById` sans `FilterValue`) n'écrit pas ces listes : le document est
    inchangé ;
  - `updateById(..., new FilterValue(List.of("liste")))` après une lecture par défaut réécrit la liste telle
    qu'elle a été relue : sans ses `null` (un élément passe de l'indice 2 à 1) et, comme avant, sans les
    sous-champs cachés des éléments (jamais relus) ;
  - **risque** : un indice calculé sur la liste relue (par le serveur, ou par un client qui a affiché la
    liste relue) puis utilisé pour une mise à jour positionnelle en base (`updateBsonDocuments` avec
    `liste.N.champ` en `$set` / `$unset`) vise un autre élément dès qu'un `null` le précède en base.
    Désigner l'élément par un de ses champs (filtre `{"liste.id": ...}` et `liste.$.champ`), ou calculer
    l'indice sur le document brut (`getBsonDocument`).
- **Valeur d'un type inattendu** : une chaîne (ou un nombre) stockée là où un objet embarqué est déclaré est
  relue `null` au lieu de faire échouer la lecture.
- **`DBAccessMongo.generateSelectField(...)`** et `DbClassModel.generateSelectFields(...)` rendent les
  chemins feuilles (et le chemin sentinelle), plus les seuls noms de premier niveau.
- Sinon, l'objet Java relu est le même qu'avant.

### Le driver refuse d'encoder un objet qui porte un champ protégé

Avant : le client MongoDB ouvert par `DbIoMongo` enregistre le codec POJO automatique du driver
(`PojoCodecProvider.automatic(true)`) et le codec des records. Ces codecs ignorent `@DataNotRead` et
`@DataEncrypt` : un objet de l'application passé directement au driver (collection typée
`getCollection("account", Account.class)`, valeur d'un `Document`, de `Updates.set(...)` ou de
`Filters.eq(...)`, document de `insertBsonDocument`, mise à jour de `updateBsonDocuments`) était écrit avec
tous ses champs, `@DataNotRead` compris et `@DataEncrypt` **en clair**.

Après : l'encodage d'un tel objet lève `ProtectedFieldEncodingException` (`org.atriasoft.archidata.exception`,
une `RuntimeException`) ; `insertBsonDocument` et `updateBsonDocuments` la rendent en `DataAccessException` avec
le même message. Rien n'est écrit. La règle suit les types déclarés : une classe est refusée dès qu'elle porte
un tel champ à une profondeur quelconque atteinte par les types déclarés des propriétés que le driver écrit (à
travers `List`, `Map`, tableaux et arguments de type), même si la valeur sur le chemin est `null` ou la liste
vide. Ces propriétés sont celles du modèle de classe du driver : champ public, champ ou propriété avec un getter
public (vérifiés par le champ et le type de retour du getter), de la classe ou d'une superclasse, et chaque
composant d'un record. Un champ que le driver n'écrit pas (`transient`, marqué `@BsonIgnore`, privé sans getter)
n'est pas vérifié. Une valeur dont la classe réelle en porte un (sous-classe sous un champ de sa classe
de base) est refusée aussi, comme la valeur d'un champ typé par une variable de type (`T value` d'un
`Wrapper<T>`) dont la classe en porte un.

```
The MongoDB driver refuses to encode com.app.Holder: its field 'inner.secret' (com.app.Secretive.secret) is marked @DataNotRead, which the codecs of the driver ignore (the value would be written like any other field). Write the object through DataAccess (insert, updateById...), which applies these annotations, or pass a Document of plain values.
```

Qui est concerné : une application qui écrit un objet de l'application par le driver directement. **Toute
entité sur un modèle de base d'archidata l'est** : `GenericTiming` (donc `GenericData`, `OIDGenericData`,
`UUIDGenericData` et leurs sous-classes) porte `@DataNotRead createdAt` et `updatedAt`, les bases à
suppression douce `@DataNotRead deleted`. L'écrire par `DataAccess` (`insert`, `updateById`...), ou construire
un `Document` de valeurs. Un objet sans champ protégé reste encodé par le driver, tous champs compris. La
lecture par le driver (collection typée) est inchangée : elle décode tous les champs présents, protégés
compris (le driver ignore ces annotations). Les écritures d'archidata lui-même ne passent au driver que des
`Document` de valeurs : elles ne changent pas (aucune classe de l'application n'atteint les codecs du driver
dans la suite de tests d'archidata, hors le test de ce refus). La garde est sur le client de `DbIoMongo` ; le
client que `ChangeNotificationManager` ouvre pour ses change streams n'en a pas : il ne fait que lire.

### Données déjà en base

Les documents écrits avant gardent les champs de sous-classe et le contenu des anciens champs `Object` :
rien n'est réécrit automatiquement. Une écriture de la nouvelle version en retire une partie, selon la
position de l'ancienne clé (règles de `DBAccessMongo` et `MongoCodecFactory.writeEmbeddedVisibleFields`) :

| Où est l'ancienne clé | Mise à jour complète (`updateById` sans `FilterValue`) | `FilterValue` qui nomme le champ de premier niveau |
| --- | --- | --- |
| dans un objet embarqué qui n'a **aucun** sous-champ `@DataNotRead` / `@DataEncrypt` (à aucune profondeur), ou dans une liste / map de tels objets | retirée : le champ est réécrit entier | retirée |
| directement dans un objet embarqué qui a un sous-champ protégé (à une profondeur quelconque) | **reste** : seuls ses sous-champs visibles sont écrits, en notation pointée (`credentials.user`) | retirée : le champ est réécrit entier (ses sous-champs protégés aussi, avec la valeur de l'objet passé) |
| dans un élément d'une `List` / `Set`, ou une valeur d'une `Map`, dont la classe a un sous-champ protégé | **reste** : la liste ou la map n'est pas écrite | retirée (même remarque) |
| au premier niveau du document, hors modèle | reste | reste |

Ce qui reste se repère et se nettoie à la main. Les relire ne les expose pas : ils ne sont plus transférés
pour un objet embarqué (projection jusqu'aux feuilles), et sous une `Map`, une liste sans sous-champ caché ou
au-delà de 8 niveaux, la lecture Java les ignore ; le masquage TRACE et le journal retirent les clés inconnues
du modèle. Exception : une lecture `OptionSpecifyType("champ", Y.class, true)` dont la classe `Y` déclare la
clé la relit (comme avant). Et un filtre qui compare le sous-document entier peut encore les atteindre. Pour lister les clés
présentes (mongosh ; comparer au modèle) :

```js
// objet embarqué
db.Account.aggregate([
  { $project: { keys: { $objectToArray: "$credentials" } } }, { $unwind: "$keys" },
  { $group: { _id: "$keys.k", count: { $sum: 1 } } }
])
// éléments d'une liste
db.Account.aggregate([
  { $unwind: "$history" }, { $match: { history: { $type: "object" } } },
  { $project: { keys: { $objectToArray: "$history" } } }, { $unwind: "$keys" },
  { $group: { _id: "$keys.k", count: { $sum: 1 } } }
])
// valeurs d'une map
db.Account.aggregate([
  { $project: { values: { $objectToArray: "$byKey" } } }, { $unwind: "$values" },
  { $match: { "values.v": { $type: "object" } } },
  { $project: { keys: { $objectToArray: "$values.v" } } }, { $unwind: "$keys" },
  { $group: { _id: "$keys.k", count: { $sum: 1 } } }
])
```

Pour retirer une clé inconnue du modèle (ici `extra`) aux trois positions qui la gardent (vérifié sur
MongoDB 8.3, avec des éléments `null` et des valeurs `null` présents, qui restent intacts) :

```js
// objet embarqué à sous-champ protégé
db.Account.updateMany({ "credentials.extra": { $exists: true } }, { $unset: { "credentials.extra": "" } })
// chaque élément d'une liste
db.Account.updateMany({ "history.extra": { $exists: true } }, { $unset: { "history.$[].extra": "" } })
// chaque valeur d'une map (pipeline : les clés de la map sont dynamiques)
db.Account.updateMany({ byKey: { $type: "object" } }, [
  { $set: { byKey: { $arrayToObject: { $map: {
    input: { $objectToArray: "$byKey" }, as: "e",
    in: { k: "$$e.k", v: { $cond: [ { $eq: [ { $type: "$$e.v" }, "object" ] },
      { $unsetField: { field: "extra", input: "$$e.v" } }, "$$e.v" ] } } } } } } }
])
```

Les deux premiers s'écrivent aussi en Java :
`DataAccess.updateBsonDocuments("Account", new Document("$unset", new Document("history.$[].extra", "")), new Condition(Filters.exists("history.extra")))`.

### Retour arrière

Les documents écrits par la nouvelle version se relisent à l'identique avec l'ancien jar : elle écrit les
mêmes encodages, seulement moins de clés (champs de sous-classe retirés, valeurs refusées), et la sentinelle
`_archidata_presence_` n'existe que dans la projection, jamais en base. Vérifié : un document écrit par la
nouvelle version (sous-classe sous un champ de base, objet à `@DataNotRead`, liste avec un `null`, map,
`Document`, record, `Set`, enum, `Instant`) relu par le jar de acffd1f (`getById` par défaut et
`ReadAllColumn`, `gets`) donne les mêmes valeurs Java que relu par la nouvelle version, sauf l'élément `null`
de la liste, que l'ancien jar rend (lecture par défaut). Revenir en arrière rouvre les fuites corrigées ici.

## Prochaine version (après v0.51.3) - Corrections de sécurité de l'audit du 2026-10-20 (CHANGEMENT DE COMPORTEMENT)

Ce lot corrige des constats de l'audit de sécurité du 2026-10-20 (CS-xx). Chaque point dit qui est
concerné.

### Effets de bord des relations limités au document atteint par `update()` (CS-04)

`update()` (et donc `updateById`) lisait l'état stocké du document par sa seule clé primaire, avec
`AccessDeletedItems`, puis exécutait les effets de bord des champs de relation après le `updateMany`,
que celui-ci ait trouvé un document ou non. Une `Condition` de l'appelant (filtre de propriétaire
`updateById(body, id, new Condition(Filters.eq("ownerId", moi)))`) ne protégeait donc que le document
lui-même : `PUT /parent/<idVictime>` avec `{"children": []}` supprimait (`cascadeUpdate = DELETE`) ou
détachait (`SET_NULL`) les enfants de la victime, rattachait à son document les ids listés, et
ajoutait ou retirait son id des documents distants (`@ManyToManyDoc`, `@ManyToOneDoc`). Même chose sur
un document supprimé logiquement, que la mise à jour principale ignore.

Désormais l'état stocké est lu avec le filtre de la mise à jour (conditions de l'appelant, clé
primaire, exclusion des documents supprimés sauf `AccessDeletedItems`, `OverrideTableName`), et les
effets de bord ne s'exécutent que si le `updateMany` a trouvé un document (`matchedCount > 0`). Une
mise à jour qui ne trouve rien renvoie `0` et ne modifie aucun document distant. `updateById` sur un
id inexistant d'une entité à relations renvoie `0` au lieu de lever une `IntrospectionException`.

Aucune transaction n'est ouverte automatiquement : appeler `update` dans un `TransactionContext` rend
la mise à jour et ses effets de bord atomiques (replica set requis).

**Qui est impacté** : une application qui comptait sur les effets de bord d'une mise à jour qui ne
trouvait pas le document (par exemple `updateById` sur un document supprimé logiquement sans
`AccessDeletedItems`, pour nettoyer ses liens) : ajouter `AccessDeletedItems` à l'appel. Les ids
qu'un client liste dans un champ de relation ne sont toujours pas vérifiés par archidata (un enfant
listé est rattaché quel que soit son propriétaire) : l'application doit les valider.

### Les clients TypeScript et Python encodent les paramètres de chemin (CS-03)

Le runtime TypeScript généré (`rest-tools.ts`, `RESTUrl`) encode désormais chaque valeur de
`params` avec `encodeURIComponent` avant de la placer dans l'URL (le runtime Python le faisait déjà
avec `quote(..., safe="")`). Une valeur contenant `../`, `/`, `?` ou `#` ne peut plus modifier le
chemin ou la requête appelés avec l'en-tête `Authorization` de l'utilisateur.

Changement de comportement :

- un appelant qui passait volontairement un `/` brut dans un paramètre de chemin envoie maintenant
  `%2F` (le serveur le décode dans la valeur du paramètre, mais le routage ne voit plus un segment
  de plus) ;
- une valeur exactement égale à `.` ou `..` est refusée : `Error` en TypeScript, `ValueError` en
  Python, avec le nom du paramètre (les navigateurs et urllib3 normalisent ces segments).

Régénérer le client pour récupérer le nouveau `rest-tools.ts` / `rest_tools.py`. Les paramètres de
requête (`queries`) sont déjà encodés (`URLSearchParams` côté TypeScript) et ne changent pas.

### Dimensions de sortie des miniatures bornées (CS-02)

`GET /data/thumbnail/{oid}` bornait la source mais pas la sortie : une image 1 × 20 000 donnait une
miniature de 256 × 5 120 000 pixels (~5 Go de raster). La hauteur de la miniature est maintenant au plus
`4 ×` la largeur configurée (`THUMBNAIL_WIDTH`, 256 par défaut, donc 1024 pixels). Au-delà, les deux côtés
sont réduits en gardant le rapport d'aspect (largeur d'au moins 1 pixel). Aucun changement pour les images
de rapport hauteur/largeur inférieur ou égal à 4.

### Caractères de contrôle neutralisés dans les logs (CS-07)

Le chemin de requête, le préfixe de token, le nom de fichier et l'URL fournis par le client sont écrits dans
les logs de `AuthenticationFilter` et `DataTools` via `LogSanitizer.clean(...)` : CR, LF, TAB et les autres
caractères de contrôle et les caractères de format Unicode (surcharges bidi comme U+202E, caractères de
largeur nulle) deviennent `\r`, `\n`, `\t`, `\uXXXX`, et le backslash est doublé (`\\`) pour qu'une séquence
d'échappement ne se confonde pas avec un texte littéral. Le format des logs ne change que pour ces
valeurs ; aucune migration n'est nécessaire.

### `Filters.checkFieldName(Class, String)` résout tous les segments du chemin (CS-01)

Jusqu'ici la garde ne vérifiait que le premier segment d'un nom pointé : `credentials.passwordHash`,
`history.0.passwordHash` ou `key.secret` passaient, et un filtre `gt` / `lt` / `regexLiteral` ou un tri
permettait de retrouver par dichotomie la valeur d'un champ `@DataNotRead` / `@DataEncrypt` d'un objet
embarqué. Désormais chaque segment est résolu dans le modèle (objets embarqués, éléments de `List` /
`Set`, valeurs de `Map`) et le nom est refusé (`IllegalArgumentException`) si :

- un segment est inconnu pour une classe modélisée (comme pour la racine) ;
- un segment est un champ protégé, à n'importe quelle profondeur ;
- le chemin se termine sur un objet embarqué, une liste, un set ou une map d'objets dont la classe a des
  champs protégés (`credentials`, `history`...) : comparer ou trier le sous-document entier les atteindrait ;
- le chemin dépasse 64 segments, ou est ambigu (segment numérique après une liste de maps dont les valeurs
  ont des champs protégés : indice de tableau ou clé de map) ;
- le chemin dépend d'un champ déclaré `Object`, interface, classe abstraite ou collection brute : un tel
  modèle est désormais refusé dès son analyse (section « Objets embarqués écrits par leur type déclaré »
  ci-dessus), `checkFieldName` lève alors `IllegalArgumentException("Cannot analyse the model of ...")`.

Après une liste, un segment numérique est un indice (ignoré) ; après une map, le segment suivant est une
clé (n'importe quelle chaîne). Sous un type BSON (`org.bson.Document`...), une valeur, une relation ou une clé
primaire déclarée `Object`, la suite du chemin est acceptée comme avant. Sous un type du JDK ou de Jackson qui
n'est pas une valeur (`Locale`, `URI`, `BigDecimal`, `LocalDateTime`, `Optional`...), la suite du chemin est
**refusée** (`Field path 'price.scale' of Account is too deep, ambiguous or typed by a polymorphic type for a
client filter or sort`) ; le journal retire aussi ces clés et le masquage TRACE les masque (voir « Objets
embarqués écrits par leur type déclaré »). La protection suit le type **déclaré**, comme
l'écriture : un champ ajouté par une sous-classe n'est plus stocké, et refusé comme inconnu.
`OrderItem.ofClientField` passe par la même vérification.

Action : si une application acceptait un nom client pointé vers un sous-champ protégé (ou vers un
sous-document qui en contient), elle reçoit maintenant une `IllegalArgumentException` (à traduire en 400).
Les noms de champs visibles (`credentials.user`, `history.0.user`, `map.k.label`) sont inchangés. Seule
la garde `checkFieldName` change : `Filters.eq(...)` et les autres helpers à nom de champ libre ne
connaissent toujours pas le modèle, c'est à l'appelant de vérifier un nom venant d'un client.

### Les logs TRACE masquent les champs protégés des objets embarqués (CS-05)

Le masquage des logs TRACE de `DBAccessMongo` (filtre et document reçu) ne connaissait que les champs
protégés de premier niveau, par leur nom : `credentials.passwordHash` (clé pointée d'un filtre) et
`credentials: {passwordHash: ...}` (document reçu) étaient écrits en clair. Le masquage se fait maintenant
par position, en parcourant le document avec le modèle : champs `@DataNotRead` / `@DataEncrypt` des objets
embarqués, des éléments de `List` / `Set` et des valeurs de `Map`, clés pointées de filtre
(`history.0.passwordHash`, `map.k.secret`), y compris sous `$and` / `$or` / `$elemMatch`, et colonne
renommée par `OptionRenameColumn`. Sous la racine, une clé inconnue du modèle (champ ajouté par une
sous-classe du type déclaré) est masquée aussi, de même que toute la valeur d'un opérateur de filtre qui ne
s'applique pas à la position (`$expr`, `$where`, `$function`, `$jsonSchema`, `$text`...). Aucune action requise ; les valeurs visibles restent loguées telles
quelles, et TRACE ne doit toujours pas être activé en production. Les anciens fichiers de log TRACE
peuvent contenir ces valeurs en clair : à purger.

### Le journal des modifications omet les champs `@DataNotRead` des objets embarqués (CS-06)

`ChangeJournalEngine.addClass` ne retirait que les champs `@DataNotRead` de premier niveau : ceux d'un
objet embarqué, des éléments d'une `List` / `Set` ou des valeurs d'une `Map` étaient copiés tels quels
dans le champ `data` de chaque entrée du journal, contrairement à ce que promettent la Javadoc et
`doc/change_journal.md`. Désormais ils sont retirés à tout niveau (parcours du document avec le modèle,
en Java : la clé d'une `Map` est dynamique, une projection fixe ne peut pas l'exprimer). Les champs
`@DataEncrypt` d'un objet embarqué restent copiés sous leur forme chiffrée, comme ceux du premier niveau ;
les champs système (`createdAt`, `updatedAt`, `deleted`) restent copiés. Par prudence, sous la racine,
une clé inconnue du modèle (champ ajouté par une sous-classe du type déclaré, valeur d'un champ déclaré
`Object` / interface / classe abstraite) est retirée aussi, et toutes les classes enregistrées pour une
même collection sont appliquées.

Action : **les entrées déjà écrites par les versions précédentes peuvent contenir ces secrets en clair**
(et les sauvegardes de la base aussi). Purger les entrées du journal des collections concernées, puis
changer les secrets exposés si le journal ou ses sauvegardes ont été accessibles à des tiers.

## Prochaine version (après v0.51.1) - Change streams : un `stop()` suivi d'un `start()` ne perd plus d'écriture (CHANGEMENT DE COMPORTEMENT)

- `start(...)`, `registerListener(...)`, `register()` d'un `ListenerRegistrationBuilder`,
  `watchCollection(...)` et `watch(name).start()` rendent la main une fois ouverts les change streams
  des collections qu'ils démarrent (10 s au plus, puis un WARN nomme la collection). Avant, ils
  rendaient la main avant l'`aggregate` qui ouvre le stream : une écriture faite juste après
  n'était jamais notifiée. Chaque enregistrement coûte donc quelques millisecondes (une connexion
  par stream) ; `registerListener(listener, Set<String>)` ouvre les streams en parallèle.
- `stop()` rend la main une fois ses workers terminés : environ 1 s (le temps d'attente d'un
  `getMore` côté serveur), au lieu de 1,5 s avec les workers encore vivants. Un worker encore actif
  après 5 s est interrompu, et une ERROR nomme ceux qui restent 5 s plus tard. Un événement lu par
  un worker arrêté n'est plus jamais transmis aux listeners d'un `start()` suivant.
- `start(DbConfig)` sur un manager déjà démarré dans le même mode ferme le client qu'il vient de
  créer et garde le sien (avant, il remplaçait la référence et le client d'origine n'était jamais
  fermé).

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
`OIDGenericData`, `ChangeJournalEntry` et `ChangeJournalMarker` le gardent. Depuis la version qui suit
v0.51.3, ce codec refuse d'encoder un objet qui porte un champ `@DataNotRead` / `@DataEncrypt`, ce que fait
une entité sur `OIDGenericData` (`createdAt`, `updatedAt`) : voir « Le driver refuse d'encoder un objet qui
porte un champ protégé ».

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
`Access-Control-Allow-Credentials: true`) ; une autre origine n'en reçoit aucun. Toutes les
réponses portent alors `Vary: Origin`, origine acceptée, refusée ou absente.

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
`http://` sauf vers `localhost` / une adresse IP littérale de bouclage (`127.x.y.z`, `[::1]`, sans
résolution DNS : `127.evil.example` est refusé), ou avec la variable d'environnement
`SSO_ALLOW_INSECURE=true` (en Java : `JWTWrapper.setAllowInsecureSsoAddress(true)`). Délais de 5 s (connexion) et 10 s (lecture), réponse
bornée à 64 Kio, redirections non suivies, clé RSA de moins de 2048 bits refusée. Un jeton à signature
inconnue ne déclenche plus qu'un rechargement de la clé par minute au plus. Les jetons invalides sont
journalisés en WARN, sans trace de pile.

**Qui est impacté** : un déploiement dont `SSO_ADDRESS` est en `http://` vers un autre hôte, par
exemple `http://karso_back_service:80/karso/api/` dans un réseau Docker interne. Sans changement,
l'application **ne démarre plus** : `new UpdateJwtPublicKey()` lève une `IllegalStateException` dont
le message nomme `SSO_ALLOW_INSECURE` (avant : la clé n'était jamais chargée, et chaque requête
répondait 401). Passer en `https://`, ou, si le réseau est de confiance, ajouter
`SSO_ALLOW_INSECURE=true` (un WARN unique est alors écrit au démarrage) :

```yaml
services:
  karideo_back_service:
    environment:
      SSO_ADDRESS: http://karso_back_service:80/karso/api/
      SSO_ALLOW_INSECURE: "true"  # réseau Docker interne uniquement
```

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

### `RESTApiRequest` : délais par défaut (W-22)

Connexion bornée à 10 s et requête à 120 s par défaut (`RESTApiRequest.timeout(Duration)` pour changer
la seconde). Avant, un appel pouvait rester bloqué indéfiniment. **Qui est impacté** : un appel légitime
de plus de 120 s (export, sauvegarde) doit passer `.timeout(...)`.

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

Pour qu'un « lire puis mettre à jour » n'efface pas ces champs cachés, une mise à jour complète
(`updateById` sans `FilterValue`, ou avec `FilterValue.getEditableFieldsNames`) ne réécrit plus en bloc
un champ dont le type contient, à n'importe quelle profondeur, des champs `@DataNotRead` ou
`@DataEncrypt` :

- objet embarqué (POJO, record) : chaque sous-champ visible est écrit en notation pointée
  (`credentials.user`, `null` = `$unset`), les sous-champs protégés restent tels qu'en base ; un objet
  mis à `null` est supprimé en entier (`$unset`) ;
- `List`, `Set` ou `Map` de tels objets : le champ n'est **pas** écrit par une mise à jour complète.

**Comment migrer** : pour remplacer une telle liste (ou un objet en entier, champs cachés compris),
nommer le champ dans un `FilterValue` (`new FilterValue("history")`). Une modification d'un élément
d'une telle liste n'est plus enregistrée par un `updateById` sans `FilterValue`.

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

### `QueryStatistics` : fichier privé et table bornée (D-11)

Le fichier de statistiques est écrit via un fichier temporaire au nom imprévisible créé dans le même
dossier (`Files.createTempFile`), puis déplacé de façon atomique ; l'ancien `<fichier>.tmp` prévisible
suivait un lien symbolique posé à l'avance. Le fichier est en `rw-------`. Le nombre de formes de
requête distinctes est borné (`QueryStatistics.setMaxEntries`, 10 000 par défaut) : au-delà, elles
sont comptées ensemble sous la clé `__other__`.

### URL de connexion échappée, `DB_TLS`, `DB_AUTH_SOURCE` (D-13)

- `DbConfig.getUrl()` encode l'utilisateur, le mot de passe et le nom de base (pourcentage, RFC 3986).
  Avant, seul `@` était échappé : un mot de passe contenant `: / ? # %` cassait l'URL ou y ajoutait
  des options. Un mot de passe déjà encodé à la main (`%40` écrit dans `DB_PASSWORD`) doit être remis
  en clair.
- Nouvelles variables : `DB_TLS=true` ajoute `tls=true` ; `DB_AUTH_SOURCE` fixe la base
  d'authentification (sinon `admin` pour `root`, comme avant). Aussi `DbConfig.setTls` /
  `setAuthSource`.
- `DB_USER` vaut toujours `root` par défaut, mais un WARN est écrit une fois : préférer un compte
  `readWrite` limité à la base de l'application.

### `EncryptionKeyStore.register` et paramètres RSA-OAEP (D-20)

- Une fois la configuration verrouillée (`ConfigBaseVariable.lock()`), `EncryptionKeyStore.register`
  refuse de remplacer une clé existante, `default` comprise (`IllegalStateException`). Un nouveau nom
  reste accepté. Enregistrer les clés avant `lock()`.
- `RsaCipherProvider` fixe explicitement ses paramètres OAEP : SHA-256 et MGF1-SHA-1, c'est-à-dire
  ce que SunJCE utilisait déjà. Les données ne changent pas et restent lisibles par les versions
  précédentes ; elles le restent aussi avec un autre fournisseur JCE. Le passage à MGF1-SHA-256 n'est
  **pas** fait : il rendrait illisibles les données déjà chiffrées, et l'enveloppe n'a pas de version
  pour distinguer les deux formats (attend la décision sur le format de chiffrement).

### Change streams : document brut (D-16, documentation)

Pas de changement de code : `ChangeEvent.getFullDocument()` et `toJson()` livrent le document tel qu'il
est stocké, champs `@DataNotRead` compris (et `@DataEncrypt` chiffrés). Ne jamais les relayer tels
quels à un client (WebSocket, SSE) : n'envoyer que l'identifiant ou un DTO.

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
