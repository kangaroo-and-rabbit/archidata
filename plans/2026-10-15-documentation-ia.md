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

## Bugs du code découverts pendant la rédaction (état au 2026-10-16)

1. `ConfigBaseVariable.getDBAbleToCreate()` lisait une propriété système
   (`Boolean.getBoolean`) : corrigé (8c011df, `parseBoolean`).
2. `ConfigBaseVariable.getDBPort()` refusait les ports > 32767 : corrigé (efa68ec, `int`
   validé 1..65535 ; `DbConfig` prend un `Integer`, changement d'API).
3. `InputExceptionCatcher` écrivait toujours status=400 : corrigé (d5bf4d7).
4. `JacksonExceptionCatcher` répondait 500 pour un JSON mal formé : corrigé (38b8c31, 400).
5. `RESTApiRequest.fetchString()`/`fetchByte()` ne vérifient pas le statut : faux diagnostic
   (accès bruts voulus ; la correction 94c815f a été annulée par bf0c426). La map des headers
   jamais remplie : corrigé (94c815f, `header(key, value)`).
6. `CronTask` (`*/n`, ET au lieu de OU, dimanche = 7 seul) : corrigé (a95554b, syntaxe Vixie).
   `removeTask` ignorait les tâches ponctuelles : corrigé (le code retire aussi
   `scheduledTasks`).
7. `BackupEngine` : delta `<date>_full_delta` non parsable : corrigé (b2b65ea).
8. `OffsetDateTime` non géré par `MongoCodecFactory` : corrigé (a7c0b6a, date BSON relue en
   UTC, offset perdu). `LocalDateTime` reste non supporté.
9. Javadoc d'`UuidUtils` (UUID ordonnés) : corrigé (cb927a7, javadoc).
10. `tools.PublicKey` inutilisé : corrigé (94cd9e7, déprécié `forRemoval`, remplacé par
    `JWTWrapper.PublicKey`).
11. `QueryStatistics` : scheduler à threads non daemon : corrigé (c177d86).
12. `MigrationStep` mettait à jour `"log"` au lieu de `logs` : corrigé (afd15b0).
13. `MigrationEngine` : dernier nom inconnu sans erreur, `revertSingle` muet : corrigé (1bf004f,
    `MigrationException` et `UnsupportedOperationException`).
14. Le pom déclare `logback-classic` deux fois : faux diagnostic. Le doublon a été retiré ; il
    n'y a pas de backend transitif, et c'est voulu (scope test).
15. `Common.ADMIN_TOKEN` invalide : corrigé (9a63987).
16. `ChangeNotificationManager` : mode FullDocument par listener ignoré : non corrigé, raison :
    le comportement reste ignoré ; l'API est dépréciée, logue un warning et le délai de reprise
    fixe est documenté (ef179fd).
