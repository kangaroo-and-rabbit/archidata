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
- les classes concrètes du JDK, de BSON et de Jackson qui ne sont pas des types valeur
  (`BigDecimal`, `LocalDateTime`, `Decimal128`, `ObjectNode`...) et les tableaux (`byte[]`...) : ni
  parcourues ni refusées. **Elles ne sont pas gérées par le codec aujourd'hui** (aller-retour en
  échec) et ne le sont pas davantage après : comportement inchangé, hors périmètre ;
- les clés de `Map` (écrites en chaîne, visibles).

Message (`DataAccessException`, cause de l'`IntrospectionException`) :

```
Unsupported field type on com.app.Account: field Wrapper.inner (path wrapper.inner) is declared
java.lang.Object, which can hold any class (Object, an interface or an abstract class), and archidata
stores and reads an embedded value by its declared type only; declare a concrete class (or a List,
Set or Map of one), or org.bson.Document for free-form content (see MIGRATION.md)
```

Variantes de la partie centrale : `..., whose element type java.lang.Object can hold any class (...)`
pour une collection ; `is declared java.util.Map, a raw type whose element type is unknown` ;
`is declared java.util.List<?>, whose element type cannot be resolved` ;
`is declared java.util.Collection<java.lang.String>: only List, Set and Map are stored as
collections; declare a List or a Set of it`.

Une classe refusée est retirée du cache des modèles : chaque nouvel appel relève la même erreur.

### 3. Projection des lectures jusqu'aux feuilles

La projection est calculée depuis le modèle déclaré :

- champ valeur, `Document`, `Map`, tableau, classe JDK, relation (add-on) : inclus entier, comme
  avant ;
- objet embarqué (classe de l'application) : remplacé par ses sous-champs, récursivement ;
- `List` / `Set` d'objets embarqués **dont la classe a des champs protégés** (`@DataNotRead` ou
  `@DataEncrypt`, à toute profondeur) : projetée par ses sous-champs (`history.user`) ; une liste
  d'objets **sans** champ protégé reste incluse entière. **Écart volontaire** : la projection
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
  nommer) ; le filtre Java de `readSubObject` reste la barrière ;
- type récursif ou très profond : coupé à 8 niveaux sous le champ de premier niveau, inclus entier
  en dessous ; au-delà de 256 chemins pour un même champ de premier niveau, le champ est inclus
  entier (log DEBUG) ;
- collisions : la liste finale ne contient jamais `a` et `a.b` (le plus large gagne, comme le
  « whole field wins » actuel), ni de doublon.

`FilterValue` (lecture partielle) : un chemin pointé est résolu segment par segment dans le modèle
(objet embarqué, élément de liste, clé de map puis type de valeur) :

- un chemin qui finit sur un objet embarqué est développé jusqu'aux feuilles, avec les mêmes règles ;
- un segment protégé que la lecture ne doit pas rendre, ou inconnu sous une classe modélisée, retire
  ce chemin ; s'il ne reste rien pour l'objet, le chemin sentinelle le garde présent et vide (même
  objet Java qu'avant) ;
- sous un `Document`, une valeur ou une clé de map, la suite du chemin est poussée telle quelle,
  comme avant ;
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
refus à l'analyse le remplace ; `MongoCodecFactory.isWrittenByRuntimeClass` (non publié) est
supprimé, les règles de type sont regroupées dans une classe (`DeclaredTypeRules`) partagée par le
refus, le curseur et la projection. Le masquage TRACE et le journal continuent de retirer, sous la
racine, une clé inconnue du modèle : c'est ce qui protège les **données écrites avant**.

## Changements de comportement (avant → après, qui est concerné)

| # | Avant | Après | Qui est concerné |
| --- | --- | --- | --- |
| 1 | Entité avec un champ (ou getter) `Object`, interface, abstrait, collection brute, `Collection<X>`, à toute profondeur embarquée : acceptée, données écrites mais mal relues | Refusée à l'analyse du modèle (exception au premier accès) | Aucune des applications relevées ; tests d'archidata qui en avaient (adaptés) |
| 2 | Sous-classe sous un champ de classe de base : tous ses champs stockés | Seuls les champs du type déclaré sont stockés, WARN une fois | Aucune application relevée |
| 3 | Objet embarqué projeté entier | Projeté par ses sous-champs ; sous-champs protégés non transférés | Toutes les lectures d'entités à objets embarqués ; résultat Java identique |
| 4 | Liste / set d'objets à champs protégés : éléments `null` relus | Les éléments `null` (et non-documents) ne sont plus rendus | Listes d'objets embarqués à champs protégés contenant des `null` |
| 5 | `FilterValue("obj")` : `obj` projeté entier | Développé jusqu'aux feuilles | Lecture partielle ; résultat Java identique |
| 6 | `FilterValue("obj.secret")` : le secret transitait, jeté en Java | Non transféré (sentinelle : objet vide comme avant) | idem |
| 7 | `FilterValue("obj.inconnu")` poussé tel quel | Retiré (sentinelle) ; plus de transfert de clés inconnues du modèle | idem |
| 8 | `generateSelectField(...)` renvoyait les noms de premier niveau | Renvoie les chemins feuilles (et la sentinelle) | Code qui inspectait cette liste (aucun relevé) |
| 9 | `OptionSpecifyType` pour un champ `Object` | Sans objet (le champ est refusé) : déprécié | Aucun usage relevé |
| 10 | Données déjà stockées (champs de sous-classe, contenu de champs `Object`) | Restent en base ; plus projetées pour un objet embarqué ; à nettoyer | Bases qui ont stocké des sous-classes |

Ce qui ne change pas : les valeurs Java rendues par une lecture (hors #4) ; les écritures d'une
entité sans sous-classe ; la mise à jour complète des objets embarqués à champs protégés (notation
pointée, listes non réécrites) ; les filtres ; le journal ; le masquage TRACE.

## Logs

- WARN (une fois par classe déclarante, champ, classe réelle), logger
  `org.atriasoft.archidata.dataAccess.model.codec.MongoCodecFactory` :
  `Field com.app.Holder.base declares com.app.Base but holds a com.app.Derived: only the fields of
  Base are stored, the fields Derived adds are dropped ([extra, secret])` (noms seulement, jamais de
  valeur).
- DEBUG, logger `org.atriasoft.archidata.dataAccess.model.ReadFieldSelector` (ou classe de
  projection) : champ projeté entier faute de pouvoir le développer (budget dépassé).
- Pas de nouveau log au refus : l'exception porte le message.
- TRACE inchangé (`filter = ...`, ` - receive data from DB: ...`) : le document reçu ne contient plus
  les sous-champs protégés.

## Limites

- Données écrites avant : les champs de sous-classe et le contenu d'anciens champs `Object` restent
  en base. Ils ne transitent plus pour un objet embarqué (projection par feuilles) mais transitent
  encore sous une `Map` ou une liste d'objets sans champ protégé (jetés en Java), et un filtre qui
  compare le sous-document entier peut encore les atteindre tant qu'ils ne sont pas nettoyés.
  Nettoyage proposé dans MIGRATION.md (`updateBsonDocuments` + `$unset`, et un pipeline pour
  repérer les clés inconnues).
- `Map<String, X>` dont les valeurs ont des champs protégés : transfert entier, filtre Java.
- Type récursif / très profond : coupé à 8 niveaux, entier en dessous.
- Éléments `null` d'une liste d'objets à champs protégés : plus rendus (changement #4).
- Types concrets non gérés par le codec (`BigDecimal`, tableaux, `LocalDateTime`...) : ni refusés ni
  gérés, inchangé.

## Validation sur une application (checklist)

Préparer une copie de la base de l'application (dump / restore) pour comparer l'ancienne et la
nouvelle version sur les mêmes données.

1. **Démarrage** : lancer l'application (ou sa suite de tests back) avec le SNAPSHOT. Chercher dans
   la sortie `DB model creation failed for` / `Unsupported field type on`. Pour forcer l'analyse de
   toutes les entités sans dépendre des appels, un test d'une ligne par entité :
   `DbClassModel.of(MonEntite.class)` (lève l'`IntrospectionException` si refus).
2. **WARN** : rechercher `but holds a` dans les logs après les écritures (aucune attendue).
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
9. **Données anciennes** : repérer les clés inconnues des sous-documents (pipeline de MIGRATION.md),
   décider du nettoyage.

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
   `DbClassModel.of`, writer par type déclaré + WARN, `FieldPathCursor` simplifié,
   `OptionSpecifyType` déprécié, tests (refus, formes exemptées, sous-classe, `Document`), tests
   existants adaptés, doc (`data_model.md`, MIGRATION, AGENTS.md).
3. `[FEAT] (dataAccess) reads project the embedded objects down to their leaf fields, protected
   sub-fields left out` : projection, tests (projection envoyée, `ReadAllColumn` / `FilterValue`,
   listes, maps, collisions, récursion, TRACE), doc (`database_access.md`, MIGRATION).
4. `[DOC] (security) ...` : SECURITY_AUDIT.md, point CS-01 mis à jour.
