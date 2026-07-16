# jdtls Serveur Persistant Multi-Projets

## Vue d'ensemble

Système de serveur jdtls persistant avec support multi-projets pour accélérer les requêtes de navigation de code.

## Nouveaux fichiers

- `jdtls_config.json` : Configuration des projets et paramètres serveur
- `jdtls_server.py` : Gestionnaire de serveurs jdtls multi-projets
- `jdtls_daemon.sh` : Script de gestion du daemon (start/stop/status)
- `jdtls_query_v2.py` : Client amélioré (utilise serveur ou fallback standalone)

## Configuration Multi-Projets

### Fichier de configuration : `jdtls_config.json`

```json
{
  "projects": [
    {
      "name": "archidata",
      "path": "/home/heero/dev/perso/kangaroo_and_rabbit/archidata",
      "auto_start": true
    }
  ],
  "server": {
    "java_home": "/usr/lib/jvm/java-25-openjdk",
    "jdtls_path": "/usr/share/java/jdtls",
    "workspace_base": "/tmp/jdtls-workspaces",
    "log_level": "ERROR"
  }
}
```

### Ajouter un projet

```bash
# Ajout manuel
python3 tools/claude/jdtls/jdtls_server.py add <nom> <chemin>

# Exemple
python3 tools/claude/jdtls/jdtls_server.py add kangaroo /home/heero/dev/perso/kangaroo_and_rabbit/kangaroo
```

### Lister les projets

```bash
python3 tools/claude/jdtls/jdtls_server.py list
```

### Supprimer un projet

```bash
python3 tools/claude/jdtls/jdtls_server.py remove <nom>
```

## Gestion du Daemon

### Démarrer le serveur en arrière-plan

```bash
tools/claude/jdtls/jdtls_daemon.sh start
```

Le serveur démarre automatiquement tous les projets marqués `auto_start: true`.

### Vérifier le statut

```bash
tools/claude/jdtls/jdtls_daemon.sh status
```

**Sortie :**
```
jdtls daemon is RUNNING (PID: 12345)
Logs: /tmp/jdtls_daemon.log

Configured projects:
  archidata: RUNNING auto-start
    Path: /home/heero/dev/perso/kangaroo_and_rabbit/archidata
  kangaroo: RUNNING auto-start
    Path: /home/heero/dev/perso/kangaroo_and_rabbit/kangaroo
```

### Arrêter le serveur

```bash
tools/claude/jdtls/jdtls_daemon.sh stop
```

### Redémarrer le serveur

```bash
tools/claude/jdtls/jdtls_daemon.sh restart
```

### Voir les logs

```bash
tools/claude/jdtls/jdtls_daemon.sh logs
```

## Utilisation du Client v2

### Recherche simple (projet courant)

```bash
python3 tools/claude/jdtls/jdtls_query_v2.py find ChangeNotification
```

### Recherche multi-projets

```bash
python3 tools/claude/jdtls/jdtls_query_v2.py find Manager --all
```

**Sortie avec indicateur de projet :**
```
Found 5 symbol(s) matching 'Manager':

  [archidata] ChangeNotificationManager (Class) in org.atriasoft.archidata
    /home/heero/.../ChangeNotificationManager.java:30:14

  [kangaroo] SessionManager (Class) in com.example.kangaroo
    /home/heero/.../SessionManager.java:15:14
```

### Autres commandes (inchangées)

```bash
# Lister symboles d'un fichier
python3 tools/claude/jdtls/jdtls_query_v2.py symbols src/main/.../Manager.java

# Go to definition
python3 tools/claude/jdtls/jdtls_query_v2.py def src/main/.../Manager.java 42 15

# Find references
python3 tools/claude/jdtls/jdtls_query_v2.py refs src/main/.../Manager.java 42 15
```

## Mode de fonctionnement

### Avec serveur persistant (RAPIDE)

- **Première requête** : Démarre le serveur (~5 secondes)
- **Requêtes suivantes** : Instantanées (<1 seconde)
- Le serveur reste en mémoire entre les requêtes

### Fallback standalone (LENT mais fiable)

Si le serveur ne peut pas démarrer, le client v2 bascule automatiquement en mode standalone (comme l'ancien comportement).

## Workflow recommandé

### 1. Démarrage de session

```bash
# Démarrer le daemon au début de ta session de travail
tools/claude/jdtls/jdtls_daemon.sh start
```

### 2. Ajouter des projets au besoin

```bash
# Ajouter un projet connexe
python3 tools/claude/jdtls/jdtls_server.py add mon_projet /chemin/vers/projet
```

### 3. Utiliser le client v2

```bash
# Recherche rapide (utilise le serveur)
python3 tools/claude/jdtls/jdtls_query_v2.py find MyClass

# Recherche multi-projets
python3 tools/claude/jdtls/jdtls_query_v2.py find MyClass --all
```

### 4. Arrêt de session

```bash
# Arrêter le daemon
tools/claude/jdtls/jdtls_daemon.sh stop
```

## Avantages

### Performance

- **Serveur persistant** : Pas de temps de démarrage JVM à chaque requête
- **Index en cache** : L'index reste en mémoire
- **Requêtes instantanées** : Réponse en < 1 seconde après démarrage

### Multi-projets

- **Recherche globale** : Chercher un symbole dans tous les projets
- **Gestion centralisée** : Un seul serveur pour plusieurs projets
- **Configuration simple** : Fichier JSON unique

### Robustesse

- **Auto-démarrage** : Le serveur démarre automatiquement si nécessaire
- **Fallback** : Bascule en mode standalone si le serveur échoue
- **Détection auto** : Détecte automatiquement le projet d'un fichier

## Détection automatique de projet

Le système détecte automatiquement quel projet utiliser en fonction du fichier :

```bash
# Fichier dans archidata
python3 tools/claude/jdtls/jdtls_query_v2.py symbols \
  /home/heero/dev/perso/kangaroo_and_rabbit/archidata/src/main/Manager.java
# → Utilise le serveur jdtls d'archidata

# Fichier dans kangaroo
python3 tools/claude/jdtls/jdtls_query_v2.py symbols \
  /home/heero/dev/perso/kangaroo_and_rabbit/kangaroo/src/main/App.java
# → Utilise le serveur jdtls de kangaroo
```

## Dépannage

### Le serveur ne démarre pas

```bash
# Vérifier les logs
tools/claude/jdtls/jdtls_daemon.sh logs

# Vérifier la configuration
python3 tools/claude/jdtls/jdtls_server.py list
```

### Performances lentes

```bash
# Redémarrer le serveur
tools/claude/jdtls/jdtls_daemon.sh restart
```

### Ajouter plus de mémoire

Éditer `jdtls_client.py` et modifier `-Xmx1G` à `-Xmx2G` (ou plus).

## Intégration avec Claude

Claude peut maintenant :

1. Démarrer le serveur automatiquement si nécessaire
2. Chercher rapidement dans le code
3. Explorer plusieurs projets connexes
4. Bénéficier de réponses instantanées

## Fichiers anciens (compatibilité)

- `jdtls_client.py` : Client LSP bas niveau (toujours utilisé)
- `jdtls_query.py` : Ancien client (mode standalone uniquement)
- `jdtls_helpers.sh` : Helpers bash (optionnel)

**Recommandation** : Utiliser `jdtls_query_v2.py` pour bénéficier du serveur persistant.
