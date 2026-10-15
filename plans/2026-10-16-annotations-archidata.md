# Annotations propres à archidata à la place de JPA (2026-10-16)

## Constat

archidata lit des annotations JPA (`jakarta.persistence`) et une annotation JAX-RS
détournée, mais n'en exploite qu'une petite partie, avec un sens qui n'est pas celui du
standard. Un développeur (ou une IA) croit donc avoir des comportements qui n'existent pas
(`@Column(length)`, `nullable`, `unique`, `@GeneratedValue`, relations JPA).

| Annotation lue aujourd'hui | Ce qui sert réellement |
| --- | --- |
| `jakarta.persistence.Table` | `name` : nom de la collection MongoDB |
| `jakarta.persistence.Column` | `name` : nom du champ en base ; `nullable`/`length` = code mort ; `unique` = un warning dans `IndexRegistry` |
| `jakarta.persistence.Id` | présence : clé primaire |
| `jakarta.persistence.GeneratedValue` | `strategy` lu, sans effet |
| `jakarta.persistence.ManyToOne/OneToMany/ManyToMany` | encore lus par `ClassObjectModel` (génération d'API) ; doublon des `@ManyToOneDoc`… d'archidata |
| `jakarta.ws.rs.DefaultValue` sur un champ d'entité | valeur par défaut à l'insertion (détournement d'une annotation de paramètre HTTP) |

Ce que fait l'écosystème Java pour MongoDB : Spring Data (`@Document`, `@Field`, `@Id`) et
Morphia (`@Entity`, `@Property`, `@Id`) ont leurs propres annotations ; aucun n'utilise JPA.

## Décisions (utilisateur, 2026-10-16)

- Nouvelles annotations dans `org.atriasoft.archidata.annotation` :
  - `@Collection("media")` remplace `@Table(name = "media")` ;
  - `@Field("nom")` remplace `@Column(name = "nom")` ; aucun autre attribut ;
  - `@Id` (archidata) remplace `jakarta.persistence.Id` ;
  - `@InitValue("…")` remplace `jakarta.ws.rs.DefaultValue` sur les champs d'entité :
    valeur posée en base à l'insertion quand le champ est `null`. Nom distinct choisi
    exprès : avec un `@DefaultValue` archidata, un mauvais import (`jakarta.ws.rs`)
    compilerait et serait ignoré en silence. Le `@DefaultValue` de JAX-RS reste utilisable
    normalement sur les paramètres des ressources (c'est Jersey qui le gère).
- Lecture des annotations JPA supprimée entièrement, relations comprises (`@ManyToOneDoc`,
  `@OneToManyDoc`, `@ManyToManyDoc` restent les seules annotations de relation),
  `@GeneratedValue` compris. Plus de dépendance `jakarta.persistence-api`.
- **Transition brute, sans période de compatibilité** : pas de lecture des anciennes
  annotations ni de warning. Les applications de l'entreprise sont migrées par
  l'utilisateur ; karso, karideo, karusic, karentory le seront à leur montée de version
  d'archidata.
- On garde les standards dont archidata délègue toute la sémantique : JAX-RS (Jersey),
  Bean Validation (`@NotNull`, `@Size`… via Hibernate Validator), Jackson,
  `@RolesAllowed`/`@PermitAll`/`@DenyAll`, `jakarta.annotation.Nullable`.

## Points d'attention

- Collision de noms : `@Field` avec `java.lang.reflect.Field` (9 fichiers de src/main),
  `@Collection` avec `java.util.Collection` (8 fichiers) : nom complet pour l'un des deux
  dans ces fichiers.
- Un champ déclaré `@Column(unique = true)` perd le warning d'`IndexRegistry` : l'unicité
  passe uniquement par `@Index`/`@Indexed`.
- Valeur par défaut du nom de collection sans `@Collection` : inchangée (vérifier le
  comportement actuel sans `@Table` et le garder).

## Clé primaire : `@Id` et pas `@Oid` (2026-10-16)

La clé n'est pas toujours un ObjectId : `GenericData…` utilise un `Long` (karideo 5 modèles,
karusic 4), `UUIDGenericData…` un `UUID`, et les tests ont des clés `String`/`Long`/`Integer`.
`@Id` signifie « clé primaire » sans préjuger du type (même choix que Spring Data et Morphia
pour Mongo).

Restes SQL à supprimer au passage :
- `UUIDGenericData` : `@DefaultValue("(UUID_TO_BIN(UUID(), TRUE))")` (expression MySQL) ;
- `GenericData` : `@GeneratedValue(strategy = GenerationType.IDENTITY)` ;
- `OIDGenericData` : `@BsonId` (annotation du driver Mongo) : retirer si archidata ne la lit pas.

## Étapes

1. Créer les 4 annotations (`@Collection`, `@Field`, `@Id`, `@InitValue`) (javadoc + exemple), les lire à la place des anciennes dans
   `AnnotationTools`, `DbPropertyDescriptor`, `ClassObjectModel` et le reste ; supprimer le
   code mort (`getLimitSize`, `getColumnNotNull`, `isUnique` de `@Column`, `getStrategy`,
   lecture des relations JPA).
2. Migrer les modèles d'archidata (`GenericData`, `User`, `Migration*`, journal, catcher…),
   les tests et le sample.
3. Retirer `jakarta.persistence-api` du pom ; vérifier qu'aucun import ne reste.
4. Doc : `annotations.md`, `data_model.md` et toutes les pages, `AGENTS.md`, SKILL,
   `context7.json`, `llms.txt` ; section breaking change dans `MIGRATION.md` avec la table
   de correspondance ancien → nouveau (et une commande `sed` d'aide à la migration).
5. Suite complète des tests verte, revue de code, push.
