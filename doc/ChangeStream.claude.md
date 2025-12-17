# MongoDB Change Stream - Référence Technique Claude

## Architecture Actuelle (IMPORTANT)

**Mode Global Fixe** : Le système utilise un mode FullDocument **GLOBAL** défini au démarrage qui s'applique à TOUTES les collections. Ce mode NE PEUT PAS être changé après le démarrage.

## Structure des Fichiers

### Fichiers Principaux
```
src/main/org/atriasoft/archidata/dataStreamEvent/
├── ChangeNotificationManager.java  # Singleton gestionnaire principal
├── ChangeStreamWorker.java        # Worker par collection (thread)
├── ChangeEvent.java               # Événement normalisé
├── ListenerRegistrationBuilder.java # Builder pour enregistrer listeners
├── CollectionWatchBuilder.java    # Builder pour watch avec pipeline
├── ChangeNotificationListener.java # Interface listener
└── WorkerStatus.java              # Enum statut worker
```

### Tests
```
src/test/test/atriasoft/archidata/dataStreamEvent/
├── ChangeNotificationIntegrationTest.java  # Tests principaux
└── TestHelper.java                         # Utilitaires de test
```

## API Essentielle

### Démarrage (4 variantes)
```java
// 1. Avec base MongoDB et mode
manager.start(MongoDatabase database, FullDocument mode);

// 2. Avec DbConfig et mode
manager.start(DbConfig dbConfig, FullDocument mode);

// 3. Avec DbConfig (mode DEFAULT)
manager.start(DbConfig dbConfig);

// 4. Sans paramètres (depuis environnement)
manager.start();
```

### Enregistrement Listeners
```java
// Listener simple
manager.createListenerBuilder(event -> {...}, "collection")
    .withMode(FullDocument.UPDATE_LOOKUP)  // IGNORÉ - utilise mode global
    .register();

// Listener avec filtrage
manager.createListenerBuilder(event -> {...}, "collection")
    .filter(e -> e.getFullDocument().getInteger("value") > 100)
    .filterField("status", "active")
    .filterOperation(OperationType.INSERT, OperationType.UPDATE)
    .register();

// Listener global (toutes collections)
manager.registerListener(event -> {...});
```

## Modèle de Données

### ChangeEvent
```java
public class ChangeEvent {
    // Identification
    Object getOid()                    // MongoDB _id (BsonObjectId ou ObjectId)
    String getCollectionName()         // Nom de la collection

    // Type d'opération
    OperationType getOperationType()   // INSERT, UPDATE, DELETE, etc.
    boolean isInsert()
    boolean isUpdate()
    boolean isDelete()

    // Données
    Document getFullDocument()         // Document complet (si disponible)
    boolean hasFullDocument()
    UpdateDescription getUpdateDescription()  // Pour UPDATE
    Set<String> getUpdatedFields()    // Champs modifiés
    Set<String> getRemovedFields()    // Champs supprimés

    // Métadonnées
    Instant getTimestamp()
    BsonTimestamp getClusterTime()
}
```

## Points Critiques d'Implémentation

### 1. Mode Global Architecture
```java
// ✅ CORRECT - Mode défini au démarrage
manager.start(database, FullDocument.UPDATE_LOOKUP);

// ⚠️ TROMPEUR - withMode() est ignoré, utilise mode global
builder.withMode(FullDocument.DEFAULT).register();  // Utilise UPDATE_LOOKUP quand même
```

### 2. Thread Safety
- `ChangeNotificationManager` : Thread-safe (ReentrantReadWriteLock)
- Un `ChangeStreamWorker` par collection (ExecutorService)
- Listeners appelés de manière asynchrone
- Collections ConcurrentHashMap pour les listeners

### 3. Resume Tokens
- Automatiquement gérés dans `resumeTokens` Map
- Persistés entre stop/start de workers
- Évitent la relecture d'événements

### 4. Nettoyage Code Récent
**Code Supprimé** (ne plus utiliser) :
- `collectionModes` Map → supprimé
- `max()` method → supprimée
- `updateEffectiveModes()` → supprimée
- `updateConfiguration()` dans Worker → supprimée
- `requestedMode` dans ListenerRegistration → supprimé

## Patterns d'Utilisation

### Pattern Standard
```java
// Démarrage
ChangeNotificationManager manager = ChangeNotificationManager.getInstance();
manager.start(database, FullDocument.UPDATE_LOOKUP);

// Listener avec filtrage
manager.createListenerBuilder(event -> {
    if (event.isInsert() || event.isUpdate()) {
        Document doc = event.getFullDocument();
        processDocument(doc);
    }
}, "users")
.filter(e -> e.getFullDocument() != null)
.register();

// Arrêt
manager.stop();
```

### Pattern Tests
```java
@BeforeAll
static void setup() {
    manager.start(database, FullDocument.UPDATE_LOOKUP);
}

@AfterEach
void cleanup() {
    manager.clearAllListeners();  // Garde workers actifs
}

@Test
void test() {
    List<ChangeEvent> events = new ArrayList<>();
    manager.createListenerBuilder(events::add, "test_collection")
        .withMode(FullDocument.UPDATE_LOOKUP)  // Doit matcher mode global
        .register();

    TestHelper.waitForStreamInitialization(manager, "test_collection", 2000);
    // ... operations ...
    TestHelper.waitForEvents(events, expectedCount, timeout);
}
```

## Méthodes Utiles pour Debug

```java
// État
boolean isRunning = manager.isRunning();
boolean isWatching = manager.isWatching("collection");
Set<String> watched = manager.getWatchedCollections();
long totalEvents = manager.getTotalEventsProcessed();

// Mode effectif (toujours global maintenant)
FullDocument mode = manager.computeEffectiveMode("any_collection");

// Nettoyage
manager.clearAllListeners();        // Supprime listeners, garde workers
manager.unwatchCollection("coll");  // Arrête worker pour collection
manager.stop();                      // Arrêt complet
```

## Erreurs Communes

### 1. Mode Mismatch
```java
// ❌ ERREUR : Essayer de changer le mode
manager.start(database, FullDocument.DEFAULT);
// Plus tard...
builder.withMode(FullDocument.UPDATE_LOOKUP);  // Ignoré!
```

### 2. Oublier l'initialisation
```java
// ❌ ERREUR : Pas d'attente
manager.createListenerBuilder(...).register();
dao.insert(entity);  // Event peut être manqué

// ✅ CORRECT : Attendre l'initialisation
manager.createListenerBuilder(...).register();
TestHelper.waitForStreamInitialization(manager, "collection", 2000);
dao.insert(entity);
```

### 3. Comparaison OID
```java
// ❌ ERREUR : Comparer OID MongoDB avec ID entité
event.getOid() == entity.id  // Types différents!

// ✅ CORRECT : OID est l'_id MongoDB
BsonObjectId oid = (BsonObjectId) event.getOid();
// oid.getValue() est l'ObjectId MongoDB, pas entity.id
```

## Flux Données Simplifié

```
MongoDB Oplog
    ↓
ChangeStreamWorker (1 par collection)
    ↓ (BsonDocument → ChangeEvent)
ChangeNotificationManager
    ↓ (distribution)
ListenerRegistration.acceptEvent() (filtrage client)
    ↓
Listener.onEvent(ChangeEvent)
```

## Configuration Environnement

Variables utilisées par `start()` sans paramètres :
- `DB_HOST` : Hostname MongoDB
- `DB_PORT` : Port MongoDB
- `DB_LOGIN` : Username
- `DB_PASSWORD` : Password
- `DB_NAME` : Nom base de données
- `DB_KEEP_CONNECTED` : Maintenir connexion

## Résumé Exécutif

1. **Mode GLOBAL** fixé au démarrage, non modifiable
2. **1 Worker = 1 Thread** par collection surveillée
3. **Resume Tokens** automatiques entre redémarrages
4. **Thread-safe** avec locks et ConcurrentHashMap
5. **Filtrage client** uniquement (pipeline serveur via CollectionWatchBuilder)
6. **Architecture simplifiée** après refactoring (plus de modes per-collection)