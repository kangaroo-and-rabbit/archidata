# jdtls Quick Start Guide

## Démarrage rapide (2 minutes)

### 1. Démarrer le serveur persistant

```bash
tools/claude/jdtls/jdtls_daemon.sh start
```

Le serveur démarre en arrière-plan et indexe automatiquement le projet archidata.

### 2. Utiliser le client rapide

```bash
# Rechercher un symbole
python3 tools/claude/jdtls/jdtls_query_v2.py find ChangeNotification

# Lister les symboles d'un fichier
python3 tools/claude/jdtls/jdtls_query_v2.py symbols src/main/.../Manager.java

# Go to definition
python3 tools/claude/jdtls/jdtls_query_v2.py def fichier.java 42 15

# Find references
python3 tools/claude/jdtls/jdtls_query_v2.py refs fichier.java 42 15
```

### 3. Arrêter le serveur (fin de session)

```bash
tools/claude/jdtls/jdtls_daemon.sh stop
```

## Ajouter un nouveau projet

```bash
# Ajouter le projet
python3 tools/claude/jdtls/jdtls_server.py add kangaroo /home/heero/dev/perso/kangaroo_and_rabbit/kangaroo

# Redémarrer le daemon pour prendre en compte le nouveau projet
tools/claude/jdtls/jdtls_daemon.sh restart

# Chercher dans tous les projets
python3 tools/claude/jdtls/jdtls_query_v2.py find MyClass --all
```

## Commandes utiles

```bash
# Vérifier le statut
tools/claude/jdtls/jdtls_daemon.sh status

# Voir les logs
tools/claude/jdtls/jdtls_daemon.sh logs

# Lister les projets configurés
python3 tools/claude/jdtls/jdtls_server.py list

# Supprimer un projet
python3 tools/claude/jdtls/jdtls_server.py remove nom_projet
```

## Avantages

- **Démarrage automatique** : Le serveur démarre les projets automatiquement si nécessaire
- **Multi-projets** : Support de plusieurs projets Java (archidata, kangaroo, etc.)
- **Recherche globale** : Chercher dans tous les projets avec `--all`
- **Performance** : Requêtes rapides grâce au serveur persistant
- **Robustesse** : Fallback automatique en mode standalone si le serveur échoue

## Structure des fichiers

```
tools/claude/jdtls/
├── jdtls_config.json          # Configuration des projets
├── jdtls_server.py            # Gestionnaire de serveurs
├── jdtls_daemon.sh            # Script de gestion du daemon
├── jdtls_query_v2.py          # Client rapide (UTILISER CELUI-CI)
├── jdtls_client.py            # Client LSP bas niveau
├── jdtls_query.py             # Ancien client (standalone)
├── jdtls_helpers.sh           # Helpers bash
├── QUICKSTART.md              # Ce fichier
├── README_SERVER.md           # Documentation détaillée
└── jdtls_README.md            # Documentation ancienne
```

## Pour Claude Code

Claude peut maintenant :

1. **Chercher rapidement** dans le code sans attendre 5-10 secondes
2. **Explorer plusieurs projets** connexes (archidata, kangaroo, etc.)
3. **Naviguer efficacement** avec go-to-definition et find-references
4. **Démarrer automatiquement** le serveur si nécessaire

## Exemple de workflow complet

```bash
# 1. Démarrer le serveur au début de la journée
tools/claude/jdtls/jdtls_daemon.sh start

# 2. Ajouter des projets connexes
python3 tools/claude/jdtls/jdtls_server.py add rabbit /home/heero/dev/perso/kangaroo_and_rabbit/rabbit

# 3. Rechercher dans tous les projets
python3 tools/claude/jdtls/jdtls_query_v2.py find DatabaseManager --all

# 4. Explorer un fichier spécifique
python3 tools/claude/jdtls/jdtls_query_v2.py symbols src/main/org/atriasoft/Manager.java

# 5. Trouver où une méthode est utilisée
python3 tools/claude/jdtls/jdtls_query_v2.py refs src/main/org/atriasoft/Manager.java 100 25

# 6. Arrêter le serveur en fin de journée
tools/claude/jdtls/jdtls_daemon.sh stop
```

## Dépannage rapide

**Le serveur ne démarre pas ?**
```bash
# Vérifier les logs
tools/claude/jdtls/jdtls_daemon.sh logs

# Redémarrer
tools/claude/jdtls/jdtls_daemon.sh restart
```

**Requêtes lentes ?**

Le serveur démarre automatiquement au premier besoin (~5 secondes). Les requêtes suivantes sont rapides.

**Configuration non trouvée ?**

Vérifier que `tools/claude/jdtls/jdtls_config.json` existe et contient au moins un projet.

## Documentation complète

- `README_SERVER.md` : Documentation détaillée du serveur multi-projets
- `jdtls_README.md` : Documentation de l'ancien système standalone
