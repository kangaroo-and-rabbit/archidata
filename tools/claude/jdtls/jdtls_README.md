# Interface jdtls pour Claude Code

## Description

Interface en ligne de commande pour communiquer avec jdtls (Eclipse JDT Language Server) permettant d'interroger l'index du code Java de manière simple et rapide.

**⚠️ OBSOLÈTE** : Ce document décrit l'ancien système standalone. Pour le nouveau système avec serveur persistant et support multi-projets, voir :
- **`QUICKSTART.md`** - Guide de démarrage rapide (recommandé)
- **`README_SERVER.md`** - Documentation complète du serveur

## Fichiers

**Version moderne (recommandée) :**
- `tools/claude/jdtls/jdtls_query_v2.py` : Client moderne avec serveur persistant
- `tools/claude/jdtls/jdtls_daemon.sh` : Gestion du daemon (start/stop/status)
- `tools/claude/jdtls/jdtls_server.py` : Gestionnaire multi-projets
- `tools/claude/jdtls/jdtls_config.json` : Configuration des projets

**Version standalone (ancienne) :**
- `tools/claude/jdtls/jdtls_client.py` : Client LSP bas niveau (JSON-RPC)
- `tools/claude/jdtls/jdtls_query.py` : Interface standalone (lente mais fiable)
- `tools/claude/jdtls/jdtls_helpers.sh` : Fonctions bash helper (optionnel)

## Migration vers le nouveau système

Au lieu d'utiliser `jdtls_query.py`, utilise maintenant :

```bash
# 1. Démarrer le serveur persistant
tools/claude/jdtls/jdtls_daemon.sh start

# 2. Utiliser le client v2 (plus rapide)
python3 tools/claude/jdtls/jdtls_query_v2.py find <nom>
```

Voir `QUICKSTART.md` pour plus de détails.

## Installation

Déjà configuré pour le projet archidata ! Les scripts utilisent des chemins relatifs et peuvent être appelés depuis n'importe où.

## Utilisation

### 1. Rechercher un symbole (classe, interface, méthode)

```bash
python3 tools/claude/jdtls/jdtls_query.py find <nom>
```

**Exemple :**
```bash
python3 tools/claude/jdtls/jdtls_query.py find ChangeNotificationManager
```

**Sortie :**
```
Found 2 symbol(s) matching 'ChangeNotificationManager':

  ChangeNotificationManager (Class) in org.atriasoft.archidata.dataStreamEvent
    /home/heero/.../ChangeNotificationManager.java:30:14

  ChangeNotificationManagerTest (Class) in test.atriasoft.archidata.dataStreamEvent
    /home/heero/.../ChangeNotificationManagerTest.java:24:14
```

### 2. Lister les symboles d'un fichier

```bash
python3 tools/claude/jdtls/jdtls_query.py symbols <fichier>
```

**Exemple :**
```bash
python3 tools/claude/jdtls/jdtls_query.py symbols src/main/org/atriasoft/archidata/dataStreamEvent/ChangeNotificationManager.java
```

**Sortie :**
```
Symbols in src/main/.../ChangeNotificationManager.java:

  LOGGER (Constant) at line 31
  INSTANCE (Constant) at line 32
  executorService (Field) at line 35
  start(MongoDatabase) (Method) at line 77
  stop() (Method) at line 101
  ...
```

### 3. Trouver la définition d'un symbole

```bash
python3 tools/claude/jdtls/jdtls_query.py def <fichier> <ligne> <colonne>
```

**Exemple :**
```bash
python3 tools/claude/jdtls/jdtls_query.py def src/test/.../ChangeNotificationIntegrationTest.java 42 10
```

**Sortie :**
```
Definition: /home/heero/.../ChangeNotificationManager.java:66:40
```

### 4. Trouver toutes les références à un symbole

```bash
python3 tools/claude/jdtls/jdtls_query.py refs <fichier> <ligne> <colonne>
```

**Exemple :**
```bash
python3 tools/claude/jdtls/jdtls_query.py refs src/main/.../ChangeNotificationManager.java 66 40
```

**Sortie :**
```
Found 15 reference(s):

  /home/heero/.../ChangeNotificationIntegrationTest.java:50:34
  /home/heero/.../ChangeNotificationManagerTest.java:35:34
  ...
```

## Avantages

### Par rapport à ctags :
- **Index dynamique** : Se met à jour automatiquement à chaque lancement
- **Compréhension sémantique** : Comprend vraiment le code Java (types, héritage, etc.)
- **Précision** : Trouve exactement les bonnes définitions/références

### Par rapport à grep :
- **Recherche par symbole** : Trouve `ChangeNotificationManager` sans avoir à connaître le chemin
- **Navigation code** : "Go to definition" et "Find references" fonctionnent comme dans un IDE
- **Filtrage intelligent** : Ignore les commentaires, strings, etc.

## Performance (mode standalone)

- **Chaque requête** : 5-10 secondes (démarre une nouvelle instance jdtls)
- **Requêtes** : Instantanées une fois le serveur démarré

**⚠️ Pour de meilleures performances, utilise le serveur persistant** (voir `QUICKSTART.md`) :
- **Première requête** : 5-10 secondes
- **Requêtes suivantes** : < 1 seconde

## Notes techniques

- Utilise le protocole LSP (Language Server Protocol)
- Communique avec jdtls via JSON-RPC sur stdin/stdout
- Index stocké dans `/tmp/jdtls-workspace-archidata/`
- Nécessite Java 21+ (utilise Java 25)

## Limitations (mode standalone)

- Chaque requête démarre une nouvelle session jdtls
- Temps de démarrage incompressible de 5-10 secondes par requête
- Consommation mémoire JVM (~1GB par instance)
- Pas de support multi-projets

## Améliorations implémentées

✅ **Serveur persistant** : Implémenté dans `jdtls_server.py` et `jdtls_daemon.sh`
✅ **Support multi-projets** : Configuré via `jdtls_config.json`
✅ **Client amélioré** : `jdtls_query_v2.py` utilise le serveur ou fallback standalone

Voir `README_SERVER.md` pour la documentation complète.

## Exemples d'usage pour Claude

**Mode standalone (ancien - lent) :**
```bash
# Trouver toutes les classes de test
python3 tools/claude/jdtls/jdtls_query.py find Test

# Explorer un fichier avant de le modifier
python3 tools/claude/jdtls/jdtls_query.py symbols src/main/.../MonFichier.java

# Comprendre où une méthode est utilisée
python3 tools/claude/jdtls/jdtls_query.py refs src/main/.../Manager.java 100 20
```

**⚠️ Version recommandée** (voir `QUICKSTART.md`) :
```bash
# Démarrer le serveur une fois
tools/claude/jdtls/jdtls_daemon.sh start

# Utiliser le client v2 (beaucoup plus rapide)
python3 tools/claude/jdtls/jdtls_query_v2.py find Test
python3 tools/claude/jdtls/jdtls_query_v2.py symbols src/main/.../MonFichier.java
python3 tools/claude/jdtls/jdtls_query_v2.py refs src/main/.../Manager.java 100 20

# Recherche multi-projets
python3 tools/claude/jdtls/jdtls_query_v2.py find MyClass --all
```
