# Plan de Refactoring Archidata - Bean Architecture

## Vision Globale

Refactoring en 3 couches :
1. **Bean** (générique, extractible) - Introspection de classes Java
2. **DbModel** (intermédiaire archidata) - Métadonnées DB pré-calculées
3. **DBAccessMongo** (intégration) - Utilise DbModel au lieu de la réflexion directe

## État Actuel

### Terminé (non commité)
- [x] **Bean Layer (Tier 1)** - Complet et testé
  - `ClassModel` - Introspection centralisée avec cache thread-safe
  - `PropertyDescriptor` - Descripteur immutable unifiant field/getter/setter
  - `ConstructorDescriptor` - Métadonnées constructeur (record)
  - `TypeInfo` - Résolution des types génériques (record)
  - `LambdaAccessorFactory` - Accesseurs lambda haute performance (LambdaMetafactory → MethodHandle → reflection)
  - `PropertyGetter/PropertySetter` - Interfaces fonctionnelles
  - `UnifiedAnnotationLookup` - Fusion annotations field/getter/setter/interfaces
  - `IntrospectionException` - Exception dédiée
  - `TestClassModel` - 22 tests couvrant tous les cas

- [x] **DbModel Layer (Tier 2)** - Complet structurellement
  - `DbClassModel` - Cache DB-aware avec pré-catégorisation des champs
  - `DbPropertyDescriptor` - Métadonnées DB (column name, action, nullable, addOn, async flags...)
  - `DbFieldAction` - Enum des rôles (PRIMARY_KEY, CREATION_TIMESTAMP, ADDON, etc.)

- [x] **Intégration DBAccessMongo (Tier 3)** - Complet
  - DBAccessMongo migré (~95%) vers DbClassModel/DbPropertyDescriptor
  - Interface `DataAccessAddOn` migrée : `Field` → `DbPropertyDescriptor`
  - 3 AddOns migrés : ManyToMany, ManyToOne, OneToMany
  - 5 commonTools migrés : FieldTools, ListInDbTools, ManyToManyTools, ManyToOneTools, OneToManyTools
  - Compilation OK

### Ancien système (staged pour suppression - status AD)
  - `BeanModel`, `BeanModelObject`, `BeanMember`, `ConstructorModel`
  - `BeanException`, `BeanGetter`, `BeanSetter`
  - `BeanIOField`, `BeanIOMethodGetter`, `BeanIOMethodSetter`
  - `ReflectClass`, `ReflectTools`

---

## Plan de Travail

### Phase 1 : Stabiliser ce qui existe (PRIORITAIRE)
> Objectif : Que tout compile et que les tests passent

1. **Vérifier la compilation du projet**
   - Builder le projet, corriger les erreurs de compilation
   - S'assurer que les imports sont corrects entre les 3 couches

2. **Valider les tests bean**
   - Exécuter `TestClassModel` et corriger si nécessaire
   - S'assurer que les 22 tests passent

3. **Valider l'intégration DBAccessMongo**
   - Exécuter les tests existants d'archidata
   - Corriger les régressions

4. **Nettoyage de l'ancien système bean**
   - Supprimer les fichiers AD (ancien bean system)
   - Vérifier qu'aucune référence ne pointe vers l'ancien système
   - Nettoyer les imports

### Phase 2 : Compléter la couche intermédiaire DbModel
> Objectif : Toute la logique archidata pré-calculée dans DbModel

5. **Revoir DbClassModel pour couvrir tous les cas**
   - Vérifier que tous les add-ons sont correctement détectés
   - S'assurer que `generateSelectFields()` est complet
   - Valider la résolution des noms de table/collection

6. **Optimiser les add-ons dans DbPropertyDescriptor**
   - Pré-calculer les flags async (insert/update)
   - Pré-calculer `needsPreviousData` par champ
   - Stocker la référence add-on directement

7. **Refactorer les add-ons (ManyToMany, ManyToOne, OneToMany)**
   - Migrer l'interface `DataAccessAddOn` pour utiliser `PropertyDescriptor` au lieu de `Field`
   - Simplifier : on n'a que 3 add-ons, l'interface peut être plus directe
   - Pré-calculer les requêtes MongoDB pour les updates de relations
   - Générer les requêtes Mongo directement au lieu de passer par l'abstraction archidata complète
   - Gérer `updateAt` directement dans les requêtes d'add-on

### Phase 3 : Accesseurs lambda typés pour performance maximale ✅ TERMINÉ
> Objectif : Éliminer le boxing/unboxing et le cast à chaque accès

8. **Créer des accesseurs typés via lambda** ✅
   - `TypedPropertyGetter<T, V>` / `TypedPropertySetter<T, V>` - Interfaces fonctionnelles typées
   - `LambdaAccessorFactory.createTypedGetter/Setter` - Factory via LambdaMetafactory avec fallback MethodHandle
   - Supportent Method et Field (getter/setter ou accès direct)
   - Compatibilité totale avec l'API existante (getValue/setValue non cassée)

9. **Intégrer les accesseurs typés dans PropertyDescriptor** ✅
   - `PropertyDescriptor.createTypedGetter(beanType, valueType)` - Crée un getter typé à la demande
   - `PropertyDescriptor.createTypedSetter(beanType, valueType)` - Crée un setter typé à la demande
   - `PropertyDescriptor.getRawGetter/getRawSetter()` - Accès direct aux lambdas non-typées
   - Callers doivent cacher les accesseurs typés (pas de stockage interne)

### Phase 4 : Simplification MongoDB directe pour les add-ons ✅ TERMINÉ
> Objectif : Requêtes MongoDB directes au lieu de passer par l'abstraction archidata

10. **MongoLinkManager créé** ✅ (`dataAccess/mongo/MongoLinkManager.java`)
    - `addToList()` - Atomic `$addToSet` pour ajouter à un array
    - `removeFromList()` - Atomic `$pull` pour retirer d'un array
    - `setField()` - Atomic `$set`/`$unset` pour un champ scalaire
    - `setFieldAndGetPrevious()` - Atomic `findOneAndUpdate` pour swap + get previous
    - `setFieldToNullWhere()` - Atomic `updateMany` pour reset par filtre
    - `addAllToList()` / `removeAllFromList()` - Batch avec `$each` / `$pullAll`
    - Toutes les opérations incluent automatiquement la mise à jour `updateAt`

11. **3 AddOns migrés vers MongoLinkManager** ✅
    - `AddOnManyToManyDoc` → `MongoLinkManager.addToList/removeFromList` (avant: ManyToManyTools read-modify-write)
    - `AddOnManyToOneDoc` → `MongoLinkManager.addToList/removeFromList` (avant: ListInDbTools read-modify-write)
    - `AddOnOneToManyDoc` → `MongoLinkManager.setField/setFieldAndGetPrevious/removeFromList` (avant: OneToManyTools read-modify-write)
    - `fillFromDoc()` migrés vers `DbClassModel` (au lieu de `AnnotationTools.getPrimaryKeyField/getIdField`)

12. **commonTools simplifiés** ✅
    - `ListInDbTools` → wrapper léger autour de `MongoLinkManager` (résolution String→column via `DbClassModel`)
    - `ManyToManyTools` → réécrit avec `MongoLinkManager.addToList/removeFromList` atomiques
    - `OneToManyTools` / `ManyToOneTools` → plus utilisés (obsolètes, gardés pour compatibilité)
    - `FieldTools` → encore utilisé par `updateRemoteLinks`

### Phase 4b : Codecs MongoDB pré-compilés (Writer + Reader) ✅ TERMINÉ
> Objectif : Éliminer les ~20 if/else de vérification de type dans setValueToDb/createObjectFromDocument

13. **Codec architecture créée** ✅ (`dataAccess/model/codec/`)
    - `MongoTypeWriter` / `MongoTypeReader` — Interfaces fonctionnelles pour conversion type-level
    - `MapKeyConverter` — Conversion clés de Map (String/Integer/Long/Short/ObjectId/Enum)
    - `MongoFieldCodec` — Codec champ-level combinant getter/setter lambda + writer/reader + dbFieldName
    - `MongoCodecFactory` — Factory statique résolvant le bon codec selon TypeInfo (une seule fois)

14. **Types supportés** ✅
    - Primitifs et boxed (long/int/float/double/boolean/short) → identité
    - String, UUID, ObjectId, Date, Character → identité
    - Instant → Date.from(), LocalDate → format String, LocalTime → nanoOfDay
    - Enum → toString (writer) / HashMap O(1) lookup (reader, au lieu du scan linéaire)
    - List<E>, Set<E> → lambda récursive via codec de l'élément E
    - Map<K,V> → MapKeyConverter pré-compilé + codec de V
    - Sub-objets (POJO/record) → récursion via DbClassModel

15. **Intégration dans DbPropertyDescriptor** ✅
    - Champ `MongoFieldCodec codec` construit en 3ème passe dans DbClassModel
    - Getter `getCodec()` — accès direct au codec pré-compilé

16. **Migration DBAccessMongo** ✅
    - `convertInDocument()` : regular fields et special fields → `desc.getCodec().writeToDoc()`
    - `insertPrimaryKey()` : case NORMAL/JSON → `codec.writeToDoc()`
    - `update()` : regular fields → `codec.writeToDoc()`
    - `createObjectFromDocument()` : lecture champs → `codec.readFromDoc()` avec support OptionSpecifyType

17. **Migration AddOns** ✅
    - `AddOnManyToManyDoc.insertData()` → `desc.getCodec().writeToDoc()`
    - `AddOnManyToOneDoc.insertData()` → `desc.getCodec().writeToDoc()`
    - `AddOnOneToManyDoc.insertData()` → `desc.getCodec().writeToDoc()`

### Phase 5 : Tests
> Objectif : Couverture complète et tests de performance

13. **Tests du bean (ClassModel)**
    - Compléter si nécessaire avec des cas edge
    - Tester les performances d'introspection (temps de premier accès vs cache hit)

14. **Tests de la couche DbModel**
    - Créer des tests pour `DbClassModel` et `DbPropertyDescriptor`
    - Vérifier la pré-catégorisation des champs
    - Tester avec différents modèles (simple, avec add-ons, avec JSON, etc.)

15. **Tests de performance CRUD**
    - Benchmarker insert/update/read/delete avant/après refactoring
    - Comparer les performances des accesseurs lambda vs reflection
    - Mesurer le gain des requêtes MongoDB directes pour les add-ons
    - Utiliser JMH ou un framework similaire pour les micro-benchmarks

16. **Tests d'intégration**
    - Valider les scénarios ManyToMany complets (add, remove, cascade)
    - Valider ManyToOne/OneToMany
    - Tester soft-delete avec cascade
    - Tester les edge cases (null values, empty lists, etc.)

### Phase 6 : Nettoyage final

17. **Supprimer le code legacy**
    - Retirer les méthodes d'`AnnotationTools` rendues obsolètes
    - Supprimer les imports inutilisés
    - Nettoyer les TODO/FIXME

18. **Documentation**
    - Documenter l'architecture en 3 couches
    - Documenter comment ajouter un nouvel add-on
    - Documenter le processus d'extraction future du bean package

---

## Inspirations Jackson

Éléments de Jackson à considérer :
- **`BeanSerializer` / `BeanDeserializer`** : Sérialisation/désérialisation via accesseurs pré-compilés
- **`BeanPropertyWriter`** : Écrit une propriété avec son sérialiseur dédié (typed)
- **`PropertyAccessor`** : Hiérarchie d'accesseurs (field, method, constructor param)
- **`AnnotationIntrospector`** : Fusion d'annotations multi-sources (déjà fait via `UnifiedAnnotationLookup`)
- **`TypeFactory` / `JavaType`** : Résolution de types génériques (déjà fait via `TypeInfo`)
- **`LambdaPropertyAccessor`** (Jackson 2.18+) : Accesseurs lambda pour perf maximale

## Notes Techniques

- Seul MongoDB est ciblé (plus de SQL), ce qui simplifie grandement
- Les 3 add-ons sont : `AddOnManyToManyDoc`, `AddOnManyToOneDoc`, `AddOnOneToManyDoc`
- Le bean package doit rester indépendant (pas de dépendance vers archidata)
- Thread-safety via `ConcurrentHashMap.computeIfAbsent()` aux deux niveaux de cache
