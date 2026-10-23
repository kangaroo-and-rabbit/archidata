# Objets embarqués écrits par leur type déclaré, projection jusqu'aux feuilles (2026-10-23)

Suite du point resté ouvert de l'audit du 2026-10-20 (SECURITY_AUDIT.md, « Points restant
ouverts », CS-01 polymorphisme). Objectif utilisateur : archidata ne doit plus **stocker** ce qu'il
ne sait pas relire, ni **ramener** de MongoDB ce qu'il ne doit pas exposer. Le résultat sera validé
sur un projet professionnel avant d'aller sur `develop` : la description de chaque changement de
comportement compte autant que le code.

## Constat (vérifié dans le code au commit acffd1f)

- `MongoCodecFactory.writeSubObject` encode un objet embarqué avec sa classe **réelle**
  (`data.getClass()`), alors que `readSubObject` le relit avec la classe **déclarée**. Les éléments
  de `List` / `Set` et les valeurs de `Map` suivent la même règle.
  - Une sous-classe stockée sous un champ déclaré avec la classe de base : ses champs en plus
    (y compris `@DataNotRead` / `@DataEncrypt`) sont écrits en base et jamais relus.
  - Un champ déclaré `Object`, interface, classe abstraite, ou `List` / `Set` / `Map` brut : tout est
    écrit, la lecture rend un `Object` vide ou échoue.
  - Un filtre client qui compare le sous-document entier (`{base: {$gt: {...}}}`) devient un oracle
    sur le secret stocké. `FieldPathCursor` refuse aujourd'hui ces positions (fail-closed) dans
    `Filters.checkFieldName`, le journal et le masquage TRACE, mais ne peut pas détecter la
    comparaison d'une classe de base concrète qui contient une sous-classe.
- La projection des lectures s'arrête au premier niveau (`DbClassModel.generateSelectFields` →
  `ReadFieldSelector.collectProjectionPaths`, `DBAccessMongo.getsRaw` → `Projections.include`) :
  un objet embarqué transite entier, sous-champs protégés compris, et `readSubObject` les jette en
  Java.

Vérifications faites avant de concevoir (test jetable, base de test locale, MongoDB du replica set
de dev) :

| Type de champ | Aujourd'hui |
| --- | --- |
| `BigDecimal`, `byte[]`, `String[]`, `Decimal128` | aller-retour en échec (« No default constructor ») : pas géré par le codec |
| `Collection<String>` | aller-retour en échec : le codec ne connaît que `List`, `Set` et `Map` |
| `JsonNode` | aller-retour en échec : classe abstraite, non gérée |
| `org.bson.Document` (imbriqué, listes, `null`) | aller-retour exact |

| Projection MongoDB | Résultat |
| --- | --- |
| `list.a` sur `[null, {a:1,s:2}, {a:3}, {s:4}, 5]` | `[{a:1}, {a:3}, {}]` : **les éléments `null` et scalaires disparaissent** |
| `e.a` sur `e: {}`, `sub.y` sur `sub: {x:1}` | `e: {}`, `sub: {}` (sous-document gardé, vide) |
| `n.a` sur `n: null`, `scalar.a` sur `scalar: 7` | champ absent du résultat |
| `ll.a` sur `[[{a:1,s:2}]]` | `[[{a:1}]]` (traverse les tableaux imbriqués) |
| `deep.m.k.a` | fonctionne à travers une clé de map nommée |
| `sub` et `sub.x` ensemble | erreur 31249 « Path collision » |

Applications (relevé fait) : karso, karideo, karusic, karentory, karanage et karoto n'ont aucun champ
d'entité typé `Object`, interface, classe abstraite, collection brute ou `Collection<X>`, ni de
getter de ce type, ni de sous-classe stockée sous un champ de classe de base, ni d'usage de
`OptionSpecifyType`. Seul karideo a des objets embarqués (`List<TvProgram>`, `List<TvRecordingPart>`,
records), tous concrets ; il filtre sur `programs.title` / `programs.subtitle` (déclarés) et lit
`Data.streams.type` en `org.bson.Document` brut (non concerné par la projection du codec).
`sample/archidata/basic` (`Task`) : aucun champ refusé.

## Conception

### 1. Écriture par le type déclaré

- Un objet embarqué est encodé avec le modèle de son type **déclaré** : type du champ, type
  d'élément résolu d'une `List` / `Set`, type de valeur d'une `Map`, à toute profondeur. Le writer
  pré-compilé connaît ce type dès sa construction (`buildWriter`), il ne regarde plus
  `value.getClass()` pour choisir le modèle.
- Couvert : insertion, mise à jour complète (y compris l'écriture champ par champ en notation pointée
  des objets embarqués qui ont des champs protégés, `writeEmbeddedVisibleFields`), mise à jour
  partielle (`FilterValue`), éléments et valeurs de map.
- Valeur d'une sous-classe du type déclaré : seuls les champs du type déclaré sont écrits. Un WARN
  est logué **une fois** par triplet (classe déclarante, champ, classe réelle).
- Valeur qui n'est pas une instance du type déclaré (pollution de tas via un type brut) : refus
  (`DataAccessException`), au lieu d'une `ClassCastException` plus loin.
- Type d'élément inconnu (un `TypeInfo` brut passé à l'API publique `buildWriter`) : refus explicite
  (`DataAccessException`). Inatteignable par un champ d'entité, refusé à l'analyse (point 2).

### 2. Refus à l'analyse du modèle

Au même moment que le refus des annotations JPA (première analyse de la classe par
`DbClassModel.of`, donc au premier accès à l'entité : insert, lecture, `IndexEngine`, migrations,
journal...), et avec la même forme d'exception (`IntrospectionException` « DB model creation failed
for X » dont la cause est une `DataAccessException`).

Champs vérifiés : toute propriété persistée (elle a un getter ou est un champ public, comme le codec
l'écrit), qui n'est pas une relation (`@ManyToOneDoc`, `@OneToManyDoc`, `@ManyToManyDoc` prises par
un add-on), à la racine et dans toute classe embarquée atteignable (champs des classes embarquées,
type d'élément des `List` / `Set`, type de valeur des `Map`, à toute profondeur, cycles compris).

Refusé, avec le chemin du champ :

- `Object` ;
- une interface (sauf `List`, `Set`, `Map` et leurs sous-types, traités comme collections) ou une
  classe abstraite, y compris du JDK (`Number`, `Serializable`, `CharSequence`, `Comparable`...) et
  `JsonNode` (vérifié non géré par le codec) ;
- `List` / `Set` / `Map` bruts (sans type générique), ou dont le type d'élément / de valeur ne peut
  pas être résolu (`List<?>`, `List<T>`, classe qui étend `ArrayList<String>` sans paramètre) ;
- une collection qui n'est ni une `List` ni un `Set` (`Collection<X>`, `Iterable<X>`, `Queue<X>`,
  `ArrayDeque<X>`) : **écart à la demande initiale** (qui exemptait `Collection`), car le codec ne
  la gère pas (vérifié : aller-retour en échec aujourd'hui) ;
- une `List` / `Set` / `Map` dont l'élément / la valeur est l'un des cas ci-dessus.

Non refusé :

- les types valeur que le codec gère : primitifs et boxés, `String`, `Character`, `ObjectId`,
  `UUID`, `Date`, `Instant`, `OffsetDateTime`, `LocalDate`, `LocalTime`, les enums ;
- `org.bson.Document` (contenu libre, aller-retour vérifié) ;
- les relations (champ avec add-on) ;
- la clé primaire déclarée `Object` (seule exemption, voir §6 B) ;
- les classes concrètes du JDK, de BSON et de Jackson qui ne sont pas des types valeur
  (`BigDecimal`, `LocalDateTime`, `Decimal128`, `ObjectNode`...) et les tableaux (`byte[]`...) : ni
  parcourues ni refusées. **Elles ne sont pas gérées par le codec aujourd'hui** (aller-retour en
  échec) et ne le sont pas davantage après : comportement inchangé, hors périmètre ;
- les clés de `Map` (écrites en chaîne, visibles).

Le refus suit la déclaration, quel que soit le paquet : une classe de bibliothèque hors JDK, Jackson et
BSON (`com.mongodb.client.model.geojson.Point`, `jakarta.*`, JTS, Joda...) est parcourue comme une classe
embarquée, et une seule de ses propriétés `Object` / interface / abstraite bloque le démarrage, même toujours
`null` (vérifié : `Point.coordinateReferenceSystem`). Contournement : la stocker dans un `Document`, ou
déclarer un DTO concret.

Message (`DataAccessException`, cause de l'`IntrospectionException`) :

```
Unsupported field type on com.app.Account: field Wrapper.inner (path wrapper.inner) is declared
java.lang.Object, which can hold any class (Object, an interface or an abstract class), and archidata
stores and reads an embedded value by its declared type only; declare a concrete class (or a List,
Set or Map of one), or org.bson.Document for free-form content (see MIGRATION.md)
```

Variantes de la partie centrale (texte exact de l'implémentation) :

- collection : `is declared java.util.List<java.lang.Object>, whose element type java.lang.Object
  can hold any class (...)` (`value type` pour une `Map`) ;
- type brut : `is declared java.util.Map, which is a raw type whose value type is unknown, and
  archidata stores and reads a value by its declared type only; declare the value type as a concrete
  class (List<MyClass>, Set<MyClass>, Map<String, MyClass>), or use org.bson.Document for free-form
  content` ;
- joker ou variable de type : `is declared java.util.List<?>, which has an element type that cannot
  be resolved (a wildcard or a type variable), and archidata stores ...` (même fin) ;
- autre collection : `is declared java.util.Collection<java.lang.String>, which is a collection
  that is neither a List nor a Set: only List, Set and Map are stored as collections; declare a List
  or a Set`.

Toutes se terminent par ` (see MIGRATION.md)`. Une classe embarquée dont l'introspection échoue
fait maintenant échouer le modèle racine (« DB model creation failed for X: an embedded class
cannot be analysed »), au lieu d'un échec à la première écriture.

Une classe refusée est retirée du cache des modèles : chaque nouvel appel relève la même erreur.

### 3. Projection des lectures jusqu'aux feuilles

La projection est calculée depuis le modèle déclaré :

- champ valeur, `Document`, `Map`, tableau, classe JDK, relation (add-on) : inclus entier, comme
  avant ;
- objet embarqué (classe de l'application) : remplacé par ses sous-champs, récursivement ;
- `List` / `Set` d'objets embarqués (listes imbriquées comprises, MongoDB traverse les tableaux
  imbriqués, vérifié sur 8.3.9) : projetée par ses sous-champs (`history.user`) **seulement si la
  lecture cache un sous-champ des éléments** (à toute profondeur) ; sinon incluse entière. *Implémenté
  ainsi plutôt que « la classe a des champs protégés »* : une lecture `ReadAllColumn` avec déchiffrement
  (`readPreviousDataForUpdate`, relecture complète) ne cache rien et relit la liste entière, `null`
  compris, comme avant. **Écart volontaire** : la projection
  `liste.champ` fait disparaître les éléments `null` (vérifié) ; la limiter aux listes qui ont
  quelque chose à cacher garde les autres listes exactement comme avant, et une mise à jour complète
  ne réécrit justement pas ces listes-là (règle D-02), donc aucun `null` n'est perdu en base par un
  cycle lecture / mise à jour complète ;
- sous-champs laissés de côté, exactement quand la lecture Java les jette aujourd'hui :
  `@DataNotRead` sans `ReadAllColumn` ; `@DataEncrypt` quand le déchiffrement est désactivé
  (`DATA_DECRYPT_ENABLE` faux : la valeur serait laissée `null`) ; relations d'un objet embarqué (jamais
  relues par `readSubObject`) ;
- objet embarqué dont aucun sous-champ ne peut être rendu (tous protégés) : projeté par un chemin
  sentinelle `champ._archidata_presence_`, que MongoDB rend comme un sous-document vide
  (vérifié) : l'objet est relu vide, comme avant, et non `null` ;
- `Map<String, X>` : incluse entière (la clé est dynamique, une projection fixe ne peut pas la
  nommer) ; les sous-champs protégés de ses valeurs transitent sans log (le WARN ne couvre que les
  coupes) ; le filtre Java de `readSubObject` reste la barrière ;
- type récursif ou très profond : coupé à 8 niveaux sous le champ de premier niveau, inclus entier
  en dessous ; au-delà de 256 chemins pour un même champ de premier niveau, le champ est inclus
  entier. Quand la partie incluse entière contient un sous-champ que la lecture ne rend pas, il
  transite : WARN une fois par champ d'entité (§6 E) ; sinon log DEBUG ;
- collisions : la liste finale ne contient jamais `a` et `a.b` (le plus large gagne, comme le
  « whole field wins » actuel), ni de doublon ;
- cache : les chemins d'un champ lu entier ne dépendent que de (type déclaré, classe d'un
  `OptionSpecifyType`, `ReadAllColumn`, déchiffrement activé) ; ils sont calculés une fois par clé, relatifs au champ, dans une
  `ConcurrentHashMap` bornée à 4096 entrées (au-delà, calculés à chaque lecture), vidée avec le cache des
  modèles. Seuls les chemins pointés d'un `FilterValue` sont résolus à chaque lecture.

`FilterValue` (lecture partielle) : un chemin pointé est résolu segment par segment dans le modèle
(objet embarqué, élément de liste, clé de map puis type de valeur) :

- un chemin qui finit sur un objet embarqué est développé jusqu'aux feuilles, avec les mêmes règles ;
- un segment protégé que la lecture ne doit pas rendre, ou inconnu sous une classe modélisée, retire
  ce chemin ; s'il ne reste rien pour l'objet, le chemin sentinelle le garde présent et vide (même
  objet Java qu'avant) ;
- sous un `Document`, une valeur ou une clé de map, la suite du chemin est poussée telle quelle,
  comme avant ;
- précision d'implémentation : une clé de map consomme un segment, puis la suite est résolue dans le
  type de valeur (développée jusqu'aux feuilles si c'est un objet embarqué) ; un segment numérique après
  une liste est cherché comme un champ de l'élément, donc remplacé par la sentinelle : même résultat
  qu'avant (MongoDB traite `liste.0.x` comme un nom de champ dans chaque élément, vérifié) ;
- un champ que la lecture décode avec une autre classe à cause d'un `OptionSpecifyType` (clé primaire
  `Object`, ou `isList` sur une `List`) : le chemin est résolu dans la classe lue, un segment que la
  classe déclarée protège donnant la sentinelle, puis développé comme un champ entier (§6) ; ailleurs la
  lecture ignore l'option, la projection aussi ;
- un `FilterValue` ne fait pas relire un champ protégé (règle 2 de `ReadFieldSelector` avant la liste
  blanche) : seuls `ReadAllColumn` et le déchiffrement le rendent ;
- inchangé : le premier segment est contrôlé (champ inconnu = `DataAccessException`), la clé
  primaire est toujours lue, `FilterOmit` reste limité aux champs de premier niveau, un nom renommé
  par `OptionRenameColumn` garde ses sous-chemins.

`readSubObject` garde son filtre Java (seconde barrière). Lectures internes vérifiées : add-ons de
relation (champs de premier niveau, inclus entiers), `readPreviousDataForUpdate` (CS-04,
`ReadAllColumn`), expansions de relations (lecture des entités cibles avec leur propre projection),
`getRaw` / `getsRaw` et TRACE, `count` (pas de projection), pagination (`gets` + `count`).

### 4. Défense en profondeur

`FieldPathCursor` garde son traitement fail-closed (positions polymorphes ou brutes `UNSAFE`,
profondeur maximale, ambiguïté indice / clé) mais un modèle qui en contient ne peut plus être
construit : ces branches deviennent du code mort gardé comme garde. Simplification : le pré-parcours
`holdsUnsafeContent` (recherche d'une position polymorphe sous un sous-document) est supprimé, le
refus à l'analyse le remplace ; `MongoCodecFactory.isWrittenByRuntimeClass` (non publié) est gardé
déprécié et répond toujours `false`, les règles de type sont regroupées dans une classe (`DeclaredTypeRules`) partagée par le
refus, le curseur et la projection. Le masquage TRACE et le journal continuent de retirer, sous la
racine, une clé inconnue du modèle : c'est ce qui protège les **données écrites avant**.

### 5. Ajouts après la revue de sécurité automatique

- **Racine abstraite** : le contrôle sautait une racine abstraite (ou une interface) de l'application, alors
  que le curseur des filtres la parcourt (fail-open). Elle est maintenant vérifiée comme une classe concrète
  (`DeclaredTypeRules.isModeledRoot`).
- **Filet fail-closed** : `MongoCodecFactory.hasProtectedSubFields` répond `true` pour un type refusé
  (inatteignable depuis un modèle accepté) : comparer ou trier une telle position en entier est refusé.
- **Types non modélisés (`OPAQUE`)** : le writer écrivait la valeur par sa propre classe ; une valeur d'une
  classe de l'application (sous-classe de `BigDecimal`, implémentation de `org.bson.conversions.Bson`, tableau
  de telles classes) était donc écrite avec son modèle, champs protégés compris, à une position que le curseur
  croyait libre. Elle est maintenant refusée à l'écriture (`DataAccessException` `... is declared
  java.math.BigDecimal, a type archidata does not model, but holds a com.app.Money, a class of the
  application: it cannot be stored there; declare the field with that class`). Le curseur répond `UNSAFE`
  sous une position `OPAQUE` (au lieu de libre) : `checkFieldName("price.scale")` refusé, clés masquées en
  TRACE et retirées du journal.
- **Tableaux** : un tableau d'une classe de l'application ou d'un type refusé (`Point[]`, `Object[]`,
  `Serializable[][]`, `List<Point[]>`) est refusé à l'analyse (le codec ne l'écrit pas élément par élément).
  Les tableaux de valeurs ou de classes JDK (`byte[]`, `String[]`, `BigDecimal[]`) restent `OPAQUE`
  (non gérés, inchangés). Aucune entité des applications relevées ne déclare de tableau.

### 6. Ajouts après la revue du 2026-10-23

- **A. `Document` contenant un objet de l'application** : le driver l'encodait par son
  `PojoCodecProvider.automatic(true)` (`DbIoMongo`), tous champs compris (`@DataNotRead`, `@DataEncrypt` en
  clair). L'écriture d'un champ `Document` (et de ses listes / maps, à toute profondeur) parcourt maintenant
  son contenu par une **liste d'autorisation qui échoue fermée** (`FreeFormContentGuard`) :
  - acceptés (classe exacte) : `String`, primitifs boxés, `BigDecimal`, `ObjectId`, `UUID`, `Date`,
    `Instant`, `OffsetDateTime`, `LocalDate`, `LocalTime`, `LocalDateTime`, `Pattern`, `Binary`,
    `Decimal128`, `Code`, `Symbol`, `MinKey`, `MaxKey`, `BSONTimestamp`, `JsonObject` ; et les enums, tout
    `BsonValue`, une `Geometry` GeoJSON du driver, les tableaux de primitifs ;
  - parcourus : `Map` à clés `String` (`Document` compris), `Collection`, tableau d'objets, portée d'un
    `CodeWithScope`, id d'un `DBRef` ; profondeur bornée à 100 ;
  - tout le reste est refusé (`DataAccessException` qui nomme le champ et le chemin dans le `Document`) :
    une classe de l'application (message d'origine), et toute autre classe, enveloppe JDK
    (`AtomicReference`, `Optional`, `Map.Entry`), `StringBuilder`, nœud Jackson, sous-classe d'une valeur
    (`java.sql.Timestamp`), `Map` à clé non `String` (message `which is not a value archidata accepts in a
    Document`).
  Les API brutes (`updateBsonDocuments`, collection du driver) ne passent pas par ce contrôle (voir F).
- **B. Clé primaire déclarée `Object`** : exemptée du refus (le support par `OptionSpecifyType` de
  `getTableIdCondition` est gardé) ; sa valeur est vérifiée à l'écriture (insert, `DirectPrimaryKey`,
  codec) : `ObjectId`, `UUID`, `String`, `Long`, `Integer` ou `Short`, sinon `DataAccessException`. Tout
  autre champ `Object` / interface / abstrait / variable de type reste refusé, avec ou sans
  `OptionSpecifyType`. `OptionSpecifyType` n'est donc plus dépréciée (écart à la version précédente du
  plan) : son usage restant est cette clé.
- **C. `FieldPathCursor`** : une exception à l'analyse d'une classe embarquée donnait une position libre
  (fail-open) ; elle donne `UNSAFE`. Inatteignable en pratique (une classe embarquée qui ne s'analyse pas
  fait échouer son modèle racine), d'où l'absence de test ; `hasProtectedSubFields` répond de même `true`.
- **D. Cache de la projection par défaut** : voir §3 (clé = type déclaré, classe d'un `OptionSpecifyType`,
  `ReadAllColumn`, déchiffrement ; borné, thread-safe ; vidé par `DbClassModel.clearCache` / `setAddOns`,
  puisque les add-ons changent les champs relus).
- **Projection et `OptionSpecifyType`** : la projection suit exactement la lecture (`ReadFieldSelector.
  overriddenReadType`, partagé par la projection et `DBAccessMongo`) : l'option n'a d'effet que sur la clé
  primaire `Object` et, avec `isList`, sur un champ `List<X>` lu en `List<Y>` ; une option posée sur un champ
  ordinaire ne change rien. Quand elle s'applique, le champ est projeté par les feuilles de `Y` **moins tout
  sous-champ que `Y` ou `X` protège** (sauf si la lecture le rend : `ReadAllColumn`, déchiffrement) : `Y` et
  `X` sont parcourus côte à côte sur les clés du document ; une clé que seul `Y` connaît est projetée par
  `Y` ; une position que `Y` prend entière (valeur, `Map`, `Document`) alors que `X` y protège un sous-champ
  est projetée par `X` ; une liste est développée dès que `X` protège un sous-champ de ses éléments, même
  sous une clé que `Y` ne déclare pas (`storedHidesOutside` ; sans cela la liste, que `Y` seul ne fait pas
  développer, transitait entière). Ce que la projection doit transférer entier (une `Map`, une partie coupée) est
  ensuite filtré en Java par `X` (`LeafProjection.restrict` : retire, à toute profondeur, les sous-champs que
  `X` protège ; une classe qui ne s'analyse pas donne un document vide) avant que `Y` le décode. Avant :
  champ transféré entier et décodé par `Y`, qui rendait un sous-champ protégé de `X` déclaré sans protection
  dans `Y`.
- **E. WARN des coupes** : la coupe à 8 niveaux et la borne de 256 chemins restent. Quand la partie
  incluse entière contient réellement un sous-champ que la lecture ne rend pas (`hidesBelow`, qui suit les
  drapeaux de la lecture ; une classe qui ne s'analyse pas compte comme en contenant un), un WARN est écrit
  une fois par champ d'entité (`Field com.app.Account.node`), mémoire vidée avec le cache ; sinon DEBUG,
  comme avant.
- **F. API brutes du driver** : le codec POJO automatique et le codec des records du driver ignorent
  `@DataNotRead` / `@DataEncrypt`. `DbIoMongo` les garde (`db/ProtectedFieldCodecGuard`) : une classe de
  l'application qui porte un tel champ à une profondeur quelconque atteinte par les types déclarés des
  propriétés que le driver écrit (`List`, `Map`, tableaux, arguments de type) refuse d'être encodée, valeur
  `null` comprise, `ProtectedFieldEncodingException`, rien n'est écrit.
  - Propriétés parcourues : celles du modèle de classe du driver (`ClassModel.builder` + conventions par
    défaut, propriétés lisibles) : champ public, champ ou propriété avec getter public (type du champ et type
    de retour du getter), superclasses comprises ; chaque composant d'un record. Un champ `transient`, marqué
    `@BsonIgnore` ou privé sans getter n'est pas écrit par le driver, donc pas vérifié. Une propriété lisible
    sans champ ni getter public (setter seul) : l'accesseur du driver lève avant d'écrire, rien à vérifier. Un
    modèle qui ne se construit pas : refus (fail-closed).
  - POJO : une `Convention` ajoutée après celles par défaut remplace la `PropertySerialization` de chaque
    propriété (sauf une clé primaire qui ne mène à aucun champ protégé) par une qui lève ; le drapeau
    `inline` (`@BsonExtraElements`, lu au décodage) est repris de l'ancienne. Une sous-classe à l'exécution,
    ou la valeur d'un champ typé par une variable de type, est encodée par son propre modèle, donc refusée.
  - Record : enveloppe du registre (`CodecProvider` qui délègue les deux `get`, arguments de type compris) ;
    le codec d'un record qui porte un champ protégé lève à l'encodage et délègue le décodage.
  - Écarté : envelopper aussi le codec des POJO. `LazyPropertyModelCodec` du driver ne spécialise un type
    générique (`Box<String>`) ni n'applique `@BsonProperty(useDiscriminator)` que si le codec est un
    `PojoCodec` (classe du paquet du driver) : l'enveloppe aurait changé le décodage. La convention ne touche
    que le test de sérialisation, utilisé par l'encodeur seul (vérifié par `javap` sur bson 5.13.0).
  - Décodage inchangé (test : un document à champs protégés relu par des collections typées, POJO imbriqué,
    liste, record, générique spécialisé ; le même test passe sans la garde). Une classe dont les champs ne
    s'analysent pas est refusée.

## Changements de comportement (avant → après, qui est concerné)

| # | Avant | Après | Qui est concerné |
| --- | --- | --- | --- |
| 1 | Entité avec un champ (ou getter) `Object`, interface, abstrait, collection brute, `Collection<X>`, à toute profondeur embarquée : acceptée, données écrites mais mal relues | Refusée à l'analyse du modèle (exception au premier accès) | Aucune des applications relevées ; tests d'archidata qui en avaient (adaptés) |
| 2 | Sous-classe sous un champ de classe de base : tous ses champs stockés | Seuls les champs du type déclaré sont stockés, WARN une fois | Aucune application relevée |
| 3 | Objet embarqué projeté entier | Projeté par ses sous-champs ; sous-champs protégés non transférés | Toutes les lectures d'entités à objets embarqués ; résultat Java identique |
| 4 | Liste / set d'objets à champs protégés : éléments `null` relus | Les éléments `null` (et non-documents) ne sont plus rendus quand la lecture cache un sous-champ des éléments | Listes d'objets embarqués à champs protégés contenant des `null` |
| 5 | Valeur scalaire stockée là où un objet embarqué est déclaré : lecture en échec | Relue `null` | Données incohérentes |
| 6 | `FilterValue("obj")` : `obj` projeté entier | Développé jusqu'aux feuilles | Lecture partielle ; résultat Java identique |
| 7 | `FilterValue("obj.secret")` : le secret transitait, jeté en Java | Non transféré (sentinelle : objet vide comme avant) | idem |
| 8 | `FilterValue("obj.inconnu")` poussé tel quel | Retiré (sentinelle) ; plus de transfert de clés inconnues du modèle | idem |
| 9 | `generateSelectField(...)` renvoyait les noms de premier niveau | Renvoie les chemins feuilles (et la sentinelle) | Code qui inspectait cette liste (aucun relevé) |
| 10 | `OptionSpecifyType` sur un champ `Object` | Seule la clé primaire `Object` reste acceptée (valeur vérifiée) ; tout autre champ `Object` est refusé avec ou sans l'option ; option non dépréciée ; projection suivant la lecture (#18) | Aucun usage relevé |
| 11 | Racine abstraite de l'application : champs non vérifiés | Vérifiée comme une classe concrète | Entité abstraite passée à `checkFieldName` / TRACE / journal |
| 12 | Valeur d'une classe de l'application sous un type JDK non modélisé : écrite par son modèle | Refusée à l'écriture | Aucune application relevée |
| 13 | Chemin sous un type JDK non modélisé (`price.scale`, `Locale`, `URI`...) : libre | `UNSAFE` (refusé par `checkFieldName`, masqué en TRACE, retiré du journal) | Filtres clients sur un tel sous-chemin (aucun relevé) |
| 14 | Tableau d'une classe de l'application ou de type refusé : accepté, non relu | Refusé à l'analyse | Aucune application relevée |
| 15 | `Document` contenant un objet de l'application (écrit par le driver, champs protégés en clair) ou toute autre classe hors liste d'autorisation (enveloppe JDK, nœud Jackson, `java.sql.Timestamp`, `Map` à clé non `String`) : passé tel quel au driver | Refusé à l'écriture, rien n'est écrit | Aucun champ `Document` relevé dans les applications |
| 16 | Classe de bibliothèque (GeoJSON, `jakarta.*`, JTS...) comme type de champ, avec une propriété polymorphe | Refusée à l'analyse (démarrage), même si la propriété est toujours `null` | À vérifier (checklist) |
| 17 | Données déjà stockées (champs de sous-classe, contenu de champs `Object`) | Restent en base ; plus projetées pour un objet embarqué ; retirées par une écriture seulement là où le champ est réécrit entier (MIGRATION.md, « Données déjà en base ») ; à nettoyer | Bases qui ont stocké des sous-classes |
| 18 | `OptionSpecifyType(champ, Y, true)` sur un `List<X>` : liste transférée entière, décodée par `Y`, un sous-champ protégé de `X` rendu si `Y` le déclare sans protection | Projetée par les feuilles de `Y` moins ce que `X` ou `Y` protège, filtrée par `X` en Java : un sous-champ protégé de `X` n'est jamais rendu (sauf `ReadAllColumn` / déchiffrement) | Aucun usage relevé |
| 19 | Objet de l'application à champ `@DataNotRead` / `@DataEncrypt` (à toute profondeur, valeur `null` comprise) passé au driver directement (collection typée, `Document`, `Updates`, `Filters`, `insertBsonDocument`, `updateBsonDocuments`) : écrit avec tous ses champs, `@DataEncrypt` en clair | Refusé à l'encodage (`ProtectedFieldEncodingException`, `DataAccessException` par `insertBsonDocument` / `updateBsonDocuments`), rien n'est écrit ; décodage inchangé | Toute entité sur un modèle de base (`GenericTiming` : `createdAt`, `updatedAt`) écrite par le driver ; aucune écriture de ce genre relevée dans les applications (le driver n'y reçoit que des `Document` de valeurs) |

Ce qui ne change pas : l'objet Java rendu par une lecture, hors #4 (éléments `null` et non-documents
d'une liste projetée jusqu'aux feuilles, indices décalés) et #5 (scalaire relu `null`) ; le document écrit
pour une entité dont chaque valeur est une instance exacte de son type déclaré et dont les `Document` ne
contiennent que des valeurs (hors #2, #12, #15) ; la mise à jour complète des objets embarqués à champs
protégés (notation pointée, listes / sets / maps de tels objets non réécrits) ; les filtres, le journal et
le masquage TRACE, hors chemins sous un type JDK non modélisé (#13) et hors clés inconnues du modèle (déjà
retirées avant). Les documents écrits par la nouvelle version se relisent avec l'ancien jar (vérifié,
MIGRATION.md « Retour arrière »).

## Logs

- WARN (une fois par classe déclarante, champ, classe réelle), logger
  `org.atriasoft.archidata.dataAccess.model.codec.MongoCodecFactory` :
  `Field com.app.Holder.base is declared com.app.Base but holds a com.app.Derived: only the fields of
  Base are stored, the fields Derived adds are dropped ([extra, secret])` (noms seulement, jamais de
  valeur). Pour un élément de liste ou une valeur de map, le préfixe devient `An element of field
  com.app.Holder.bases` / `A value of field com.app.Holder.byKey`. Le WARN est émis pour toute
  sous-classe, même si elle n'ajoute aucun champ (liste vide `[]`).
- Erreur (`DataAccessException`) pour une valeur qui n'est pas une instance du type déclaré :
  `An element of field com.app.Account.bases is declared com.app.Base but holds a java.lang.String,
  which is not one: it cannot be stored`.
- WARN, logger `org.atriasoft.archidata.dataAccess.model.LeafProjection`, quand une partie coupée contient
  un sous-champ que la lecture ne rend pas (une fois par champ d'entité, mémoire vidée avec le cache) :
  `Field com.app.Account.node (declared com.app.Node) is projected whole at
  'node.child.child.child.child.child.child.child.child' because it is deeper than 8 levels, and it holds
  sub-fields this read does not return (@DataNotRead, @DataEncrypt): they are transferred from the
  database, then dropped by the Java read` (`because it develops into more than 256 projection paths` pour
  la borne des chemins).
- DEBUG, même logger, pour une coupe sans sous-champ protégé : `Field 'x' (declared T) develops into more
  than 256 projection paths: projected whole`, `Embedded object 'x.y...' is deeper than 8 levels: projected
  whole`, `Path 'x.y' develops into more than 256 projection paths: projected whole`. Une fois par clé du
  cache pour un champ lu entier, au premier champ rencontré ; à chaque lecture pour un chemin de
  `FilterValue`.
- Erreur à l'écriture d'un `Document` (#15) : `Field com.app.Account.free is a free-form
  org.bson.Document holding, under 'list.1.deep.k', a com.app.Secret, a class of the application: the
  driver would store all its fields (@DataNotRead and @DataEncrypt ones included, in clear); put only
  values, Documents, Lists and Maps in a Document, or declare a field with that class` ; pour une autre
  classe hors liste : `... holding, under 'list.0.x', a java.util.Optional, which is not a value archidata
  accepts in a Document (a BSON or JDK value, an enum, a Document or a Map with String keys, a List, a
  Collection or an array of them): what it holds would not be checked; put only values, Documents, Lists
  and Maps in a Document, or declare a field with the class it holds`.
- Erreur à l'écriture d'une clé primaire `Object` (#10) : `Field com.app.Keyed._id is a primary key
  declared java.lang.Object but holds a java.util.Date: such a key only takes an ObjectId, a UUID, a
  String, a Long, an Integer or a Short`.
- Pas de nouveau log au refus : l'exception porte le message.
- Encodage refusé par le driver (#19) : `The MongoDB driver refuses to encode com.app.Holder: its field
  'inner.secret' (com.app.Secretive.secret) is marked @DataNotRead, which the codecs of the driver ignore
  (the value would be written like any other field). Write the object through DataAccess (insert,
  updateById...), which applies these annotations, or pass a Document of plain values` (`the value would be
  written in clear` pour `@DataEncrypt`). DEBUG, logger `org.atriasoft.archidata.db.ProtectedFieldCodecGuard`,
  une fois par classe de l'application qui atteint les codecs du driver : `MongoDB driver codec for
  application class com.app.Holder: refuses to encode it (inner.secret)` ou `...: no protected field`
  (`application record` pour un record).
- TRACE inchangé (`filter = ...`, ` - receive data from DB: ...`) : le document reçu ne contient plus
  les sous-champs protégés.

## Limites

- Données écrites avant : les champs de sous-classe et le contenu d'anciens champs `Object` restent
  en base. Ils ne transitent plus pour un objet embarqué (projection par feuilles) mais transitent
  encore sous une `Map` ou une liste d'objets sans champ protégé (jetés en Java), et un filtre qui
  compare le sous-document entier peut encore les atteindre tant qu'ils ne sont pas nettoyés.
  Une mise à jour complète ne les retire que là où elle réécrit le champ entier. Repérage et nettoyage
  (mongosh, vérifiés) dans MIGRATION.md « Données déjà en base ».
- `Map<String, X>` dont les valeurs ont des champs protégés : transfert entier, filtre Java, sans log.
- `OptionSpecifyType(champ, Y, true)`, déchiffrement activé : un sous-champ `@DataEncrypt` de `X` est
  décodé par `Y` tel que stocké : en clair seulement si `Y` le marque aussi `@DataEncrypt`, l'enveloppe
  chiffrée (Base64) pour une `String` simple, une lecture en échec pour un autre type (comme avant).
- Type récursif / très profond (8 niveaux) ou très large (256 chemins pour un champ) : la partie coupée
  est transférée entière ; un sous-champ protégé qu'elle contient transite (jeté en Java), signalé par un
  WARN une fois par champ d'entité (§6 E).
- Éléments `null` d'une liste d'objets à champs protégés : plus rendus (changement #4).
- Types concrets non gérés par le codec (`BigDecimal`, tableaux, `LocalDateTime`...) : ni refusés ni
  gérés, inchangé.
- `Document` : les API brutes (`updateBsonDocuments`, `getInterface().getDatabase()`) ne passent pas par la
  liste d'autorisation ; elles refusent seulement un objet de l'application qui porte un champ protégé (§6 F).
  Un objet sans champ protégé y est encodé par le driver, tous champs compris.
- Garde du driver (§6 F) : le champ typé par une variable de type n'est vérifié que par la classe de sa
  valeur (une valeur `null` passe, rien de protégé n'est écrit). Un autre `MongoClient` construit par
  l'application n'a pas la garde, ni celui que `ChangeNotificationManager.createMongoClient` ouvre pour ses
  change streams (registre par défaut du driver ; il ne fait que lire).
- `OptionSpecifyType(champ, Y, true)` : une clé que la classe déclarée ne connaît pas mais que `Y` déclare
  est transférée et relue (données anciennes d'une sous-classe comprises), comme avant.
- Mise à jour positionnelle (`liste.N`) calculée sur une liste relue jusqu'aux feuilles : indices décalés
  par les éléments `null` / non-documents (#4).

## Validation sur une application (checklist)

Préparer une copie de la base de l'application (dump / restore) pour comparer l'ancienne et la
nouvelle version sur les mêmes données.

1. **Démarrage** : lancer l'application (ou sa suite de tests back) avec le SNAPSHOT. Chercher dans
   la sortie `DB model creation failed for` / `Unsupported field type on`. Pour forcer l'analyse de
   toutes les entités sans dépendre des appels, un test d'une ligne par entité :
   `DbClassModel.of(MonEntite.class)` (lève l'`IntrospectionException` si refus).
   - Chercher dans les entités et leurs classes embarquées les types de bibliothèque (`geojson`,
     `jakarta.`, `org.locationtech`, `org.joda`...) : refusés si une de leurs propriétés est polymorphe (#16).
   - Chercher les types JDK non modélisés (`BigDecimal`, `Locale`, `URI`, `URL`, `LocalDateTime`,
     `Duration`, `Optional`...) : leurs sous-chemins sont refusés par `checkFieldName` (#13), et ils ne
     relisent toujours pas.
   - Chercher les champs `Document` (`List<Document>`, `Map<String, Document>`) et ce qu'on y met : un
     POJO, un record, une enveloppe JDK (`Optional`...), un nœud Jackson, un `java.sql.Timestamp` ou une
     `Map` à clé non `String` dedans est refusé à l'écriture (#15) ; grep `but holds a` / `free-form
     org.bson.Document holding` dans les logs.
2. **WARN** : rechercher `but holds a` dans les logs après les écritures (aucune attendue). Penser aux
   sous-classes anonymes ou « double accolade » (`new Base() {{ ... }}`, classe réelle `...$1`) : WARN,
   seuls les champs de `Base` sont écrits ; une valeur chargée par un autre classloader (rechargement à
   chaud, plugin) est refusée (`which is not one`).
3. **Écritures** : pour une entité à objets embarqués (et listes, maps), insérer et mettre à jour
   (complète puis `FilterValue`), puis comparer le document brut (`mongosh`
   `db.<coll>.findOne({_id: ...})`) avec l'ancienne version : identique.
4. **Lectures** : pour chaque entité à objets embarqués, comparer le JSON rendu par l'API (ou
   `getById`) ancien / nouveau jar, sur la même base :
   - lecture par défaut ;
   - `ReadAllColumn` (champs `@DataNotRead` des sous-objets rendus) ;
   - `FilterValue` d'un champ de premier niveau, d'un chemin pointé, d'un objet embarqué entier ;
   - listes et maps d'objets embarqués (attention aux éléments `null`, changement #4) ;
   - `DATA_DECRYPT_ENABLE` vrai et faux si l'application a des `@DataEncrypt` embarqués.
5. **Projection envoyée** : soit `DataAccess`/`DBAccessMongo.generateSelectField(MonEntite.class,
   new QueryOptions(...))` dans un test, soit le profiler MongoDB :
   `db.setProfilingLevel(2)`, une lecture, puis
   `db.system.profile.find({ns: "<base>.<coll>", op: "query"}).sort({ts: -1}).limit(1)` → champ
   `command.projection` : chemins feuilles, aucun sous-champ protégé (sauf `ReadAllColumn`).
   Remettre `db.setProfilingLevel(0)`.
6. **TRACE** (environnement de dev seulement) : logger
   `org.atriasoft.archidata.dataAccess.DBAccessMongo` en TRACE, faire une lecture, vérifier que les
   lignes ` - receive data from DB:` ne contiennent plus les sous-champs protégés (même pas `***`).
7. **Mises à jour** : `updateById` complet d'une entité dont un objet embarqué a un `@DataNotRead` :
   le secret reste en base ; `updateById` avec `FilterValue` qui le nomme : il est écrit.
8. **Pagination et comptage** : une page `gets` + `count` donnent les mêmes résultats qu'avant.
9. **Données anciennes** : repérer les clés inconnues des sous-documents (pipelines de MIGRATION.md),
   décider du nettoyage ; une mise à jour complète ne les retire que là où le champ est réécrit entier.
10. **Projection entière** : faire les lectures habituelles et chercher dans les logs le WARN
    `is projected whole at` (logger `org.atriasoft.archidata.dataAccess.model.LeafProjection`, niveau WARN,
    pas besoin de DEBUG) : chaque ligne nomme un champ d'entité dont une partie coupée (8 niveaux / 256
    chemins) transfère des sous-champs protégés (jetés en Java) ; aucune attendue sans type récursif ou très
    large. En DEBUG, `projected whole` sans WARN signale une coupe sans sous-champ protégé (sans risque).
    Si l'application utilise `OptionSpecifyType(..., true)`, vérifier que la lecture ne rend plus les
    sous-champs protégés de la classe déclarée (#18).
11. **Indices de liste** : chercher les mises à jour positionnelles en base (`updateBsonDocuments` avec
    `.N.`, `$[]` exclu) dont l'indice vient d'une liste relue (par le serveur ou par un client), sur des
    listes d'objets à champs protégés pouvant contenir des `null` : l'indice relu peut viser un autre
    élément (#4). Un `FilterValue("liste")` après une lecture par défaut réécrit la liste sans ses `null`.
12. **Retour arrière** : sur la copie de la base, écrire avec le SNAPSHOT puis relire avec l'ancien jar
    (mêmes valeurs attendues, éléments `null` des listes compris) ; vérifié sur archidata, voir
    MIGRATION.md « Retour arrière ».
13. **API brutes du driver** (#19) : chercher dans le code les écritures directes
    (`getInterface().getDatabase()`, `getCollection(..., X.class)`, `Updates.set(`, `Filters.eq(` avec un
    objet, `insertBsonDocument`, `updateBsonDocuments`) et ce qu'elles reçoivent ; puis faire tourner la
    suite de tests back et chercher `ProtectedFieldEncodingException` / `refuses to encode` dans la sortie.
    En DEBUG (logger `org.atriasoft.archidata.db.ProtectedFieldCodecGuard`), les lignes `MongoDB driver codec
    for application` listent les classes de l'application qui atteignent les codecs du driver.

## Essayer le jar SNAPSHOT

```bash
# dans archidata (branche de travail)
JAVA_HOME=/usr/lib/jvm/java-27-openjdk mvn install -DskipTests -Dgpg.skip=true
# dans le pom de l'application
#   <dependency><groupId>org.atria-soft</groupId><artifactId>archidata</artifactId>
#   <version>0.51.4-SNAPSHOT</version></dependency>
mvn -U package -Dexec.skip=true
```

## Étapes (commits)

1. `[DOC] (plans) ...` : ce fichier.
2. `[FIX] (dataAccess) an embedded object is written by its declared type; a polymorphic or untyped
   entity field is refused when the model is analysed` : `DeclaredTypeRules`, refus dans
   `DbClassModel.of`, writer par type déclaré + WARN, `FieldPathCursor` simplifié (et `UNSAFE` sur une
   classe embarquée qui ne s'analyse pas), contenu des `Document` vérifié par liste d'autorisation, clé primaire `Object` exemptée
   et vérifiée (`OptionSpecifyType` gardée pour elle), tests (refus, formes exemptées, sous-classe,
   `Document`, classe de bibliothèque, clé `Object`), tests existants adaptés, doc (`data_model.md`,
   `database_access.md`, `change_journal.md`, MIGRATION, AGENTS.md).
3. `[FEAT] (dataAccess) reads project the embedded objects down to their leaf fields, protected
   sub-fields left out` : projection, cache des chemins par défaut, `OptionSpecifyType` projeté par la
   classe lue moins les champs protégés de la classe déclarée et filtré par elle en Java, WARN des coupes
   qui contiennent un sous-champ protégé, tests (projection envoyée, `ReadAllColumn` / `FilterValue`,
   listes, `null` et mises à jour, records, `@Field`, héritage, `Set`, clé de map, expansion de relation,
   `getsRaw`, pagination et `OptionSpecifyType` via le profiler, collisions, récursion, WARN / DEBUG des
   coupes, cache), doc (`database_access.md`, `annotations.md`, MIGRATION, AGENTS.md, SECURITY_AUDIT.md
   point CS-01).
4. `[FIX] (db) the MongoDB driver refuses to encode an application object holding @DataNotRead or
   @DataEncrypt fields` : `ProtectedFieldCodecGuard` (convention du codec POJO, enveloppe des records),
   `ProtectedFieldEncodingException`, tests (`TestProtectedFieldEncoding` : `updateBsonDocuments`,
   `insertBsonDocument`, driver brut à toute profondeur, sous-classe, variable de type, entité sur modèle de
   base, objets sans champ protégé, décodage inchangé), doc (`database_access.md`, `security.md`,
   `data_model.md`, `annotations.md`, MIGRATION, AGENTS.md, SECURITY_AUDIT.md).
