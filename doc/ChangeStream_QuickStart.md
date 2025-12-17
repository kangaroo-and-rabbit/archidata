# MongoDB Change Stream - Guide de Démarrage Rapide

## Vue d'ensemble

Le système de notifications Change Stream d'ArchiData permet de recevoir en temps réel les modifications effectuées dans MongoDB. Il utilise les Change Streams natifs de MongoDB pour capturer les événements INSERT, UPDATE et DELETE.

## Démarrage en 3 étapes

### 1. Démarrer le gestionnaire

```java
import org.atriasoft.archidata.dataStreamEvent.ChangeNotificationManager;
import com.mongodb.client.model.changestream.FullDocument;

// Obtenir l'instance singleton
ChangeNotificationManager manager = ChangeNotificationManager.getInstance();

// Démarrer avec la base de données MongoDB
MongoDatabase database = // votre instance MongoDB
manager.start(database, FullDocument.UPDATE_LOOKUP);
```

**Modes disponibles :**
- `FullDocument.DEFAULT` : Document complet pour INSERT uniquement
- `FullDocument.UPDATE_LOOKUP` : Document complet pour INSERT et UPDATE (recommandé)
- `FullDocument.WHEN_AVAILABLE` : Document complet si disponible

### 2. Enregistrer un listener

```java
// Écouter une collection spécifique
manager.createListenerBuilder(event -> {
    System.out.println("Modification détectée : " + event.getOperationType());

    if (event.isInsert()) {
        System.out.println("Nouveau document : " + event.getFullDocument());
    } else if (event.isUpdate()) {
        System.out.println("Champs modifiés : " + event.getUpdatedFields());
        System.out.println("Document mis à jour : " + event.getFullDocument());
    } else if (event.isDelete()) {
        System.out.println("Document supprimé : " + event.getOid());
    }
}, "users")  // nom de la collection
.register();
```

### 3. Arrêter le gestionnaire

```java
// Arrêt propre
manager.stop();
```

## Exemple Complet

```java
public class ChangeStreamExample {
    public static void main(String[] args) {
        // Configuration
        ChangeNotificationManager manager = ChangeNotificationManager.getInstance();
        MongoDatabase database = getMongoDatabase(); // votre méthode

        try {
            // Démarrer avec UPDATE_LOOKUP pour avoir les documents complets
            manager.start(database, FullDocument.UPDATE_LOOKUP);

            // Enregistrer un listener pour la collection "products"
            manager.createListenerBuilder(event -> {
                switch (event.getOperationType().getValue()) {
                    case "insert":
                        handleNewProduct(event.getFullDocument());
                        break;
                    case "update":
                        handleProductUpdate(event.getOid(), event.getFullDocument());
                        break;
                    case "delete":
                        handleProductDeletion(event.getOid());
                        break;
                }
            }, "products").register();

            // Votre application continue...
            Thread.sleep(60000); // Exemple : écouter pendant 1 minute

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Toujours arrêter proprement
            manager.stop();
        }
    }

    private static void handleNewProduct(Document product) {
        System.out.println("Nouveau produit ajouté : " + product.getString("name"));
    }

    private static void handleProductUpdate(Object oid, Document product) {
        System.out.println("Produit mis à jour : " + product.getString("name"));
    }

    private static void handleProductDeletion(Object oid) {
        System.out.println("Produit supprimé : " + oid);
    }
}
```

## Points Importants

### Architecture Globale
- Le mode (DEFAULT, UPDATE_LOOKUP, etc.) est défini **globalement** au démarrage
- Tous les listeners utilisent le même mode
- Le mode ne peut pas être changé après le démarrage

### Gestion des Listeners
- Les listeners peuvent être ajoutés/supprimés à tout moment
- Chaque collection peut avoir plusieurs listeners
- Les listeners reçoivent tous les événements de leur collection

### Performance
- Un worker thread par collection surveillée
- Les événements sont traités de manière asynchrone
- Resume tokens automatiques pour éviter la perte d'événements

## Cas d'Usage Courants

### 1. Synchronisation de Cache

```java
manager.createListenerBuilder(event -> {
    if (event.isUpdate() || event.isDelete()) {
        cache.invalidate(event.getOid());
    }
    if (event.isInsert() || event.isUpdate()) {
        cache.put(event.getOid(), event.getFullDocument());
    }
}, "cached_collection").register();
```

### 2. Audit et Logging

```java
manager.createListenerBuilder(event -> {
    auditLog.log(
        "Collection: " + event.getCollectionName() +
        ", Operation: " + event.getOperationType() +
        ", Timestamp: " + event.getTimestamp()
    );
}, "sensitive_data").register();
```

### 3. Notifications Temps Réel

```java
manager.createListenerBuilder(event -> {
    if (event.isInsert() && "order".equals(event.getCollectionName())) {
        notificationService.sendNewOrderAlert(event.getFullDocument());
    }
}, "orders").register();
```

## Prochaines Étapes

Pour des fonctionnalités avancées, consultez le [Guide Avancé](ChangeStream_Advanced.md) qui couvre :
- Filtrage côté serveur avec pipelines d'agrégation
- Filtrage côté client avec prédicats
- Listeners globaux multi-collections
- Gestion fine des erreurs et reconnexions
- Métriques et monitoring