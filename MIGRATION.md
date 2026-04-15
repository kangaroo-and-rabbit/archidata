# Migration Guide

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
| `ConfigBaseVariable.apiAdress` | `ConfigBaseVariable.getlocalAddress()` |
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
