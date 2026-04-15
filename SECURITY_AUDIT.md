# Audit de Sécurité - archidata (2026-04-15)

## Statut des vulnérabilités

### Corrigées
- [x] #2 - `Math.random()` pour génération de tokens (CRITIQUE)
- [x] #3 - Path Traversal dans FrontGeneric.java (HIGH)
- [x] #5 - Mot de passe DB dans DbConfig.toString() (HIGH)
- [x] #7 - Validation audience JWT jamais faite (HIGH)
- [x] #10 - SSRF sans protection dans uploadDataFromUri (MEDIUM)
- [x] #11 - SSRF bypass dans ProxyResource (MEDIUM)
- [x] #13 - Upload fichiers sans validation type (MEDIUM)
- [x] #17 - Tokens JWT complets dans les logs (LOW)

### Corrigées (cette itération)
- [x] #1 - Mode test bypass JWT (CRITIQUE) - voir MIGRATION.md pour le breaking change
- [x] #9 - Champs ConfigBaseVariable privés + setters avec lock/unlock sécurisé - voir MIGRATION.md

### À faire (prochaine itération)
- [ ] #4 - Mot de passe DB par défaut hardcodé - ConfigBaseVariable.java:157
- [ ] #6 - CORS wildcard * - CORSFilter.java:33-38
- [ ] #8 - JWT dans les query parameters - AuthenticationFilter.java:145-155
- [ ] #12 - Pas de rate limiting sur l'authentification
- [ ] #14 - Messages d'erreur avec détails internes - ExceptionCatcher.java
- [ ] #15 - SSO public key potentiellement en HTTP - JWTWrapper.java:108-143
- [ ] #16 - Pas de révocation JWT (pas de jti) - JWTWrapper.java
- [ ] #18 - Swagger UI exposé en prod - openApiResource.java
- [ ] #19 - API par défaut en HTTP sur 0.0.0.0:80 - ConfigBaseVariable.java:186

### Dépendances à vérifier
- [ ] webp-imageio:0.2.2 - vérifier version libwebp embarquée (CVE-2023-4863)
