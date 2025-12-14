# MongoDB Change Stream - Guide Avancé

## Table des Matières

1. [Méthodes de Démarrage Avancées](#méthodes-de-démarrage-avancées)
2. [Filtrage des Événements](#filtrage-des-événements)
3. [Listeners Globaux](#listeners-globaux)
4. [Gestion des Collections](#gestion-des-collections)
5. [Métriques et Monitoring](#métriques-et-monitoring)
6. [Gestion des Erreurs](#gestion-des-erreurs)
7. [Optimisations et Bonnes Pratiques](#optimisations-et-bonnes-pratiques)

## Méthodes de Démarrage Avancées

### Démarrage avec Configuration par Défaut

```java
// Utilise la configuration depuis les variables d'environnement
ChangeNotificationManager manager = ChangeNotificationManager.getInstance();
manager.start(); // Utilise DbConfig depuis l'environnement
```

### Démarrage avec DbConfig

```java
import org.atriasoft.archidata.db.DbConfig;

// Configuration personnalisée
DbConfig dbConfig = new DbConfig(
    "mongodb.example.com",  // hostname
    (short) 27017,           // port
    "username",              // login
    "password",              // password
    "myDatabase",            // database name
    true,                    // keep connected
    List.of(MyEntity.class)  // classes
);

// Démarrer avec configuration personnalisée et mode
manager.start(dbConfig, FullDocument.UPDATE_LOOKUP);
```

### Démarrage avec MongoDatabase Existant

```java
MongoClient mongoClient = // votre client MongoDB existant
MongoDatabase database = mongoClient.getDatabase("myDb");

// Utiliser une connexion existante
manager.start(database, FullDocument.UPDATE_LOOKUP);
```

## Filtrage des Événements

### Filtrage Côté Client avec Prédicats

```java
// Filtrer par valeur de champ
manager.createListenerBuilder(this::handleEvent, "users")
    .filter(event -> {
        // Ne garder que les utilisateurs actifs
        Document doc = event.getFullDocument();
        return doc != null && doc.getBoolean("active", false);
    })
    .register();

// Filtrer par type d'opération
manager.createListenerBuilder(this::handleEvent, "products")
    .filterOperation(OperationType.INSERT, OperationType.DELETE)
    .register();
```

### Filtrage par Champs Spécifiques

```java
// Ne recevoir que les événements où role = "admin"
manager.createListenerBuilder(this::handleEvent, "users")
    .filterField("role", "admin")
    .register();

// Filtrage complexe sur plusieurs critères
manager.createListenerBuilder(this::handleEvent, "orders")
    .filter(event -> {
        if (!event.isUpdate()) return true;

        // Pour les updates, ne garder que si le statut change
        return event.getUpdatedFields().contains("status");
    })
    .register();
```

### Filtrage Côté Serveur avec Pipelines

```java
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

// Pipeline d'agrégation MongoDB
List<Bson> pipeline = Arrays.asList(
    Aggregates.match(
        Filters.and(
            Filters.in("operationType", "insert", "update"),
            Filters.eq("fullDocument.priority", "high")
        )
    )
);

// Créer un watcher avec pipeline
CollectionWatchBuilder builder = manager.createWatchBuilder("critical_tasks");
builder.withPipeline(pipeline)
       .withMode(FullDocument.UPDATE_LOOKUP)
       .watch();
```

## Listeners Globaux

### Écouter Toutes les Collections

```java
// Listener global - reçoit les événements de TOUTES les collections
manager.registerListener(event -> {
    logger.info("Change in {}: {} on document {}",
        event.getCollectionName(),
        event.getOperationType(),
        event.getOid()
    );
});
```

### Combiner Listeners Globaux et Spécifiques

```java
// Listener global pour le logging
manager.registerListener(event -> {
    auditLog.record(event);
});

// Listeners spécifiques pour la logique métier
manager.createListenerBuilder(this::handleUserChange, "users").register();
manager.createListenerBuilder(this::handleOrderChange, "orders").register();
```

## Gestion des Collections

### Surveillance Explicite de Collections

```java
// Commencer à surveiller des collections sans listener
manager.watchCollection("logs", "metrics", "events");

// Plus tard, ajouter des listeners
manager.createListenerBuilder(this::processLog, "logs").register();
```

### Arrêter la Surveillance

```java
// Arrêter de surveiller une collection spécifique
manager.unwatchCollection("temporary_collection");

// Nettoyer tous les listeners mais garder les workers actifs
manager.clearAllListeners();
```

### Désinscription de Listeners

```java
// Méthode 1 : Avec retour d'enregistrement
ListenerRegistrationBuilder.Registration registration =
    manager.createListenerBuilder(this::handleEvent, "users")
           .register();

// Plus tard...
registration.unregister();

// Méthode 2 : Désinscription directe
ChangeNotificationListener listener = this::handleEvent;
manager.createListenerBuilder(listener, "users").register();

// Plus tard...
manager.unregisterListener(listener, "users");
```

## Métriques et Monitoring

### Obtenir les Statistiques

```java
// Vérifier l'état du manager
boolean isRunning = manager.isRunning();

// Collections surveillées
Set<String> watchedCollections = manager.getWatchedCollections();

// Nombre total d'événements traités
long totalEvents = manager.getTotalEventsProcessed();

// Mode effectif pour chaque collection
Map<String, FullDocument> modes = manager.getCollectionModes();
```

### Monitoring des Workers

```java
// Vérifier si une collection est surveillée
boolean isWatching = manager.isWatching("users");

// Logger les métriques périodiquement
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(() -> {
    logger.info("Change Stream Stats: {} events processed, {} collections watched",
        manager.getTotalEventsProcessed(),
        manager.getWatchedCollections().size()
    );
}, 0, 60, TimeUnit.SECONDS);
```

## Gestion des Erreurs

### Reconnexion Automatique

Le système gère automatiquement :
- Les déconnexions réseau temporaires
- La reprise après interruption avec resume tokens
- La recréation des workers en cas d'erreur

### Gestion d'Erreurs dans les Listeners

```java
manager.createListenerBuilder(event -> {
    try {
        processEvent(event);
    } catch (BusinessException e) {
        logger.error("Failed to process event: {}", event, e);
        // L'événement est perdu pour ce listener mais pas pour les autres
        errorHandler.handleFailedEvent(event, e);
    }
}, "critical_collection").register();
```

### Pattern Circuit Breaker

```java
public class ResilientListener implements ChangeNotificationListener {
    private final CircuitBreaker circuitBreaker = new CircuitBreaker();

    @Override
    public void onEvent(ChangeEvent event) {
        circuitBreaker.executeSupplier(() -> {
            // Traitement qui peut échouer
            externalService.process(event);
            return null;
        });
    }
}

manager.createListenerBuilder(new ResilientListener(), "users").register();
```

## Optimisations et Bonnes Pratiques

### 1. Choisir le Bon Mode FullDocument

```java
// Pour synchronisation de cache - besoin du document complet
manager.start(database, FullDocument.UPDATE_LOOKUP);

// Pour audit simple - pas besoin du document complet
manager.start(database, FullDocument.DEFAULT);
```

### 2. Utiliser le Filtrage Serveur pour Réduire le Trafic

```java
// Mauvais : filtrage côté client sur gros volume
manager.createListenerBuilder(event -> {
    if (event.getFullDocument().getString("country").equals("FR")) {
        process(event);
    }
}, "global_users").register();

// Bon : filtrage côté serveur
CollectionWatchBuilder builder = manager.createWatchBuilder("global_users");
builder.withPipeline(Arrays.asList(
    Aggregates.match(Filters.eq("fullDocument.country", "FR"))
)).watch();
```

### 3. Éviter les Listeners Trop Lourds

```java
// Mauvais : traitement synchrone lourd
manager.createListenerBuilder(event -> {
    // Opération longue bloquante
    heavyDatabaseOperation(event);
    sendEmailNotification(event);
}, "orders").register();

// Bon : déléguer à un thread pool
ExecutorService executor = Executors.newFixedThreadPool(10);
manager.createListenerBuilder(event -> {
    executor.submit(() -> {
        heavyDatabaseOperation(event);
        sendEmailNotification(event);
    });
}, "orders").register();
```

### 4. Gérer la Mémoire avec de Gros Volumes

```java
// Limiter le nombre d'événements en mémoire
BlockingQueue<ChangeEvent> eventQueue = new LinkedBlockingQueue<>(1000);

manager.createListenerBuilder(event -> {
    try {
        // Ajouter avec timeout pour éviter le blocage
        if (!eventQueue.offer(event, 5, TimeUnit.SECONDS)) {
            logger.warn("Event queue full, dropping event");
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}, "high_volume_collection").register();

// Consommateur séparé
new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        try {
            ChangeEvent event = eventQueue.take();
            processEventAsync(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
}).start();
```

### 5. Grouper les Listeners par Logique Métier

```java
public class OrderService {
    public void initializeChangeStreams(ChangeNotificationManager manager) {
        // Tous les listeners liés aux commandes dans une classe
        manager.createListenerBuilder(this::handleOrderChange, "orders")
               .register();

        manager.createListenerBuilder(this::handlePaymentChange, "payments")
               .filter(e -> e.getFullDocument() != null &&
                           e.getFullDocument().containsKey("orderId"))
               .register();

        manager.createListenerBuilder(this::handleShipmentChange, "shipments")
               .filterField("status", "shipped")
               .register();
    }

    private void handleOrderChange(ChangeEvent event) {
        // Logique métier pour les commandes
    }

    private void handlePaymentChange(ChangeEvent event) {
        // Logique métier pour les paiements
    }

    private void handleShipmentChange(ChangeEvent event) {
        // Logique métier pour les expéditions
    }
}
```

## Architecture Interne

### Composants Principaux

1. **ChangeNotificationManager** : Gestionnaire singleton principal
2. **ChangeStreamWorker** : Un worker par collection surveillée
3. **ListenerRegistration** : Enregistrement d'un listener avec ses filtres
4. **ChangeEvent** : Événement normalisé contenant les données de modification

### Flux de Données

```
MongoDB Change Stream
        ↓
ChangeStreamWorker (par collection)
        ↓
ChangeNotificationManager (distribution)
        ↓
Filtrage côté client
        ↓
Listeners enregistrés
```

### Thread Safety

- Toutes les opérations publiques sont thread-safe
- Les listeners sont appelés de manière asynchrone
- Les collections de listeners utilisent ConcurrentHashMap
- Protection par ReentrantReadWriteLock pour les opérations critiques

## Dépannage

### Problème : Événements Manqués

**Solution** : Vérifier que le mode FullDocument correspond aux besoins
```java
// Pour ne pas manquer les détails des updates
manager.start(database, FullDocument.UPDATE_LOOKUP);
```

### Problème : Performance Dégradée

**Solution** : Utiliser le filtrage serveur et optimiser les listeners
```java
// Filtrer au maximum côté serveur
// Traiter de manière asynchrone côté client
```

### Problème : Mémoire Excessive

**Solution** : Limiter les buffers et traiter en streaming
```java
// Utiliser des queues bornées
// Traiter et libérer rapidement les événements
```

## Exemples Complets

### Système de Cache Distribué

```java
public class DistributedCacheManager {
    private final Cache<Object, Document> cache;
    private final ChangeNotificationManager changeManager;

    public void initialize(MongoDatabase database) {
        changeManager = ChangeNotificationManager.getInstance();
        changeManager.start(database, FullDocument.UPDATE_LOOKUP);

        // Synchroniser le cache pour plusieurs collections
        String[] collections = {"users", "products", "categories"};

        for (String collection : collections) {
            changeManager.createListenerBuilder(event -> {
                Object oid = event.getOid();

                switch (event.getOperationType().getValue()) {
                    case "insert":
                    case "update":
                        cache.put(oid, event.getFullDocument());
                        break;
                    case "delete":
                        cache.invalidate(oid);
                        break;
                }
            }, collection).register();
        }
    }

    public void shutdown() {
        changeManager.stop();
    }
}
```

### Pipeline ETL Temps Réel

```java
public class RealTimeETL {
    private final ChangeNotificationManager manager;
    private final DataWarehouse warehouse;

    public void startETL(MongoDatabase source) {
        manager = ChangeNotificationManager.getInstance();
        manager.start(source, FullDocument.UPDATE_LOOKUP);

        // Transformer et charger en temps réel
        manager.createListenerBuilder(event -> {
            if (event.isInsert() || event.isUpdate()) {
                Document doc = event.getFullDocument();

                // Transformation
                DataRecord record = transform(doc);

                // Chargement asynchrone
                CompletableFuture.runAsync(() -> {
                    warehouse.load(record);
                });
            }
        }, "transactions")
        .filter(e -> e.getFullDocument() != null &&
                     e.getFullDocument().getDouble("amount") > 1000)
        .register();
    }

    private DataRecord transform(Document doc) {
        // Logique de transformation ETL
        return new DataRecord(doc);
    }
}
```