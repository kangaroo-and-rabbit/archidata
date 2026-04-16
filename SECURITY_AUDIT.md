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

- [x] #4 - Mot de passe DB par défaut supprimé, crash si non configuré
- [x] #14 - Noms de classes Java supprimés des erreurs client (message conservé, OID pour corrélation logs)
- [x] #19 - Défaut API changé de 0.0.0.0:80 à localhost:80

### À faire (prochaine itération)
- [ ] #6 - CORS wildcard * - CORSFilter.java:33-38
- [ ] #8 - JWT dans les query parameters - AuthenticationFilter.java:145-155
- [ ] #12 - Pas de rate limiting sur l'authentification
- [ ] #15 - SSO public key potentiellement en HTTP - JWTWrapper.java:108-143
- [ ] #16 - Pas de révocation JWT (pas de jti) - JWTWrapper.java
- [ ] #18 - Swagger UI exposé en prod - openApiResource.java

### Dépendances
- [x] webp-imageio:0.2.2 - SUPPRIMÉ (libwebp native de 2021, vulnérable CVE-2023-4863). Lecture WebP couverte par TwelveMonkeys. Écriture WebP (thumbnails) à réintégrer quand une alternative safe existe.
