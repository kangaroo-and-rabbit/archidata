# Migration Guide

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
