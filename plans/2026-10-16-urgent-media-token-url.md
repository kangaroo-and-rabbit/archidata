# URGENT — prochaine version : plus de JWT dans l'URL des médias (W-09)

Décision utilisateur (2026-10-16) : à faire en priorité dans la prochaine version, pas
dans le lot de sécurité en cours.

## Problème

Pour les balises `<img>` / `<video>` / `<audio>`, les fronts passent le **JWT complet en
query string** (`?Authorization=…`, endpoints `@PermitTokenInURI`) :
- le jeton finit dans l'historique du navigateur, les logs des proxys et du serveur, les
  en-têtes `Referer` ;
- les réponses média sont servies avec un cache `public, immutable` d'un an : un cache
  partagé peut conserver la réponse liée au jeton ;
- un jeton volé de cette façon donne tous les droits de l'utilisateur jusqu'à expiration.

Voir SECURITY_AUDIT.md, constat W-09 (et W-02 pour le scénario XSS + vol de jeton).

## Piste retenue

URL de média **signées à courte durée** : l'API renvoie (ou le front demande) une URL
`…/data/{id}?exp=…&sig=…` où `sig` = HMAC(id, exp, utilisateur) avec une clé serveur ;
validité de quelques minutes ; plus aucun JWT dans l'URL. Le cache reste possible sur
l'URL signée (privé, durée ≤ validité).

## Impacts

- archidata : générateur et vérificateur de signature, filtre, `DataResource`.
- fronts karideo / karusic (et karso si concerné) : demander les URL signées au lieu de
  coller le jeton.
- En attendant : ne pas élargir l'usage de `@PermitTokenInURI`.
