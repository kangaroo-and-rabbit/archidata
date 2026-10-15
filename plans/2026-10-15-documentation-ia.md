# Documentation archidata utilisable par les humains et les IA (2026-10-15)

## Constat (audit du 2026-10-15)

- Packaging déjà correct : jars `-sources` et `-javadoc` publiés sur Maven Central
  (0.50.2), miroir GitHub public à jour, licence MPL 2.0.
- Javadoc : 97 % des classes publiques et ~92 % des méthodes publiques documentées.
  Manques : `AuthenticationFilter`, `MySecurityContext`, `migration/model/Migration*`,
  `CheckForeignKey`, `DataAccessConnectionContext`, `GenericTiming` ; `package-info`
  absents pour `crypto`, `pagination`, `dataAccess/statistic` ; `overview.html` incomplet.
- Doc cassée : `doc/TUTORIAL_01_first_install.md`, `doc/migration.md` et
  `sample/archidata/basic` utilisent `GlobalConfiguration` et `DataFactoryTsApi`, qui
  n'existent plus. Le sample n'a pas de pom et n'expose aucune ressource REST.
- Packages sans doc : backup, cron, catcher/exception, bean, converter, tools,
  dataAccess/statistic, générateurs Dot/Drawio.
- Trois fichiers ChangeStream en doublon, en français, avec des classes fictives ;
  `ChangeStream.claude.md` cite des méthodes absentes du code.
- Rien pour les IA : ni `AGENTS.md`, ni `CLAUDE.md`, ni `llms.txt`.

## Principes

- La doc est en anglais (comme le reste de `doc/`).
- Tout exemple de code doit compiler : le sample est un vrai projet Maven construit
  contre la version courante ; la doc renvoie vers lui plutôt que de recopier du code.
- Pour les IA, le levier fiable est la doc disponible à l'exécution (`llms.txt`,
  `llms-full.txt` embarqué dans le jar, `AGENTS.md`, skill), pas l'entraînement des
  modèles, qu'on ne contrôle pas.

## Chantiers

1. Sample compilable + tutoriel + `migration.md` réparés (sample = référence).
2. Javadoc : classes manquantes, `package-info` manquants, `overview.html` complet.
3. ChangeStream : un seul fichier anglais vérifié contre `dataStreamEvent`.
4. Nouvelles pages : backup, cron, erreurs (catcher/exception → codes HTTP),
   tools, converter/bean, statistic, générateurs Dot/Drawio.
5. Couche IA : `AGENTS.md` (+ `CLAUDE.md` qui y renvoie), `llms.txt`, script de
   génération de `llms-full.txt`, embarqué dans le jar sous `META-INF/archidata/`.
6. Référence unique des annotations (après la fin du travail de l'autre session sur
   `doc/data_model.md`).
7. Skill Claude Code `archidata` (recettes, pièges) utilisable par karideo, karusic
   et karso.
8. À décider avec l'utilisateur : site GitHub Pages, soumission à Context7, nettoyage
   de la racine (`res.txt`, `test_*.tar.gz`, `exampleDot.dot`, `pom.xml.versionsBackup`,
   `typescript_model/`, `record_*.ts`).

## Contraintes

- Une autre session modifie `doc/data_model.md`, `doc/typescript_api_generation.md`,
  `annotation/apiGenerator/*`, `externalRestApi/typescript/TsClassElement.java` : ne
  pas y toucher.
- Builds Maven sérialisés par `flock` (un seul `target/`).

## Décisions utilisateur (2026-10-15)

- Site : GitHub Pages généré depuis doc/ (configuration mkdocs.yml) avec la javadoc.
- Fichiers en vrac à la racine : ne pas y toucher.
- `context7.json` préparé ; la soumission sur context7.com est faite par l'utilisateur.

## Bugs du code découverts pendant la rédaction (non corrigés, à traiter à part)

1. `ConfigBaseVariable.getDBAbleToCreate()` utilise `Boolean.getBoolean(valeur)` (lit une
   propriété système) : renvoie false même si la variable vaut "true".
2. `ConfigBaseVariable.getDBPort()` utilise `Short.parseShort` : port > 32767 refusé.
3. `InputExceptionCatcher` écrit toujours status=400 dans le JSON, même si le code HTTP est autre.
4. `JacksonExceptionCatcher` répond 500 pour un JSON mal formé (devrait être 400).
5. `RESTApiRequest.fetchString()`/`fetchByte()` ne vérifient pas le statut ; la map des
   headers n'est jamais remplie.
6. `CronTask` : `*/n` = valeur % n (jour du mois `*/2` → 2,4,6…), jour du mois ET jour de
   semaine combinés en ET (Unix : OU), dimanche = 7 seulement, `removeTask` ignore les
   tâches ponctuelles.
7. `BackupEngine` : un delta nommé `<date>_full_delta` n'est plus parsable.
8. `OffsetDateTime` non géré par `MongoCodecFactory`.
9. Javadoc d'`UuidUtils` : annonce des UUID ordonnés, or ils ne sont pas triables.
10. `tools.PublicKey` inutilisé (doublon de `JWTWrapper.PublicKey`).
11. `QueryStatistics` crée un `CronScheduler` à threads non daemon.
12. `MigrationStep` met à jour `"log"` alors que le champ s'appelle `logs` (à vérifier).
13. `MigrationEngine` : un dernier nom de migration inconnu n'applique rien, sans erreur ;
    `revertSingle` ne fait rien.
14. Le pom déclare `logback-classic` deux fois (la seconde en scope test) : pas de backend
    SLF4J transitif pour les applications.
15. `Common.ADMIN_TOKEN` (src/test) : rôles `Boolean` et sujet `Long`, refusés par
    `AuthenticationFilter`.
16. `ChangeNotificationManager` : le mode FullDocument par listener est ignoré ; javadoc de
    `WorkerStatus` annonce un backoff exponentiel alors que le délai est fixe.
