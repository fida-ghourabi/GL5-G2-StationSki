# Boréal — poste de station

Application Angular en français dans `frontend/`, indépendante du backend Spring Boot situé dans `backend/`. Le nom Boréal est une identité d’interface configurable, pas une donnée provenant du serveur.

## Lancer le frontend

Prérequis vérifiés dans l’environnement de développement : Node **24.11.1**, npm **11.6.2**. Le projet utilise **Angular 21** (core **21.2.25**, CLI local **21.2.26** dans le verrou installé), TypeScript **5.9.3** et RxJS. **Le CLI global Angular 19 n’est pas utilisé.**

Depuis la racine du dépôt :

```powershell
cd frontend
npm ci
npm start
```

Ouvrir **http://localhost:4200**. Le backend doit déjà fonctionner sur **http://localhost:8087/stationSki**. Les erreurs de connexion apparaissent explicitement dans les pages ; aucune donnée d’exemple ne remplace une réponse manquante.

Les commandes npm utilisent le CLI local. `npm ci` télécharge les dépendances du fichier `package-lock.json` ; il n’est nécessaire qu’au premier lancement ou après changement du verrou.

Pour démarrer le backend dans un autre terminal, depuis la racine :

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Le profil backend `local` synchronise le schéma via Hibernate `update` : choisir votre base de développement. Le frontend ne démarre pas le backend et n’exécute aucune migration.

## Construire

```powershell
npm run build
```

Sortie statique : `frontend/dist/boreal/browser/`.

En développement, `proxy.conf.json` transmet `/stationSki/**` à `http://localhost:8087`, sans réécriture. Aucune modification CORS du backend n’est nécessaire. Après modification du proxy, redémarrer `npm start`.

En production, configurer le serveur web pour :

1. servir les fichiers de `dist/boreal/browser` ;
2. transmettre `/stationSki/` au backend en conservant ce préfixe ;
3. rediriger les autres routes Angular non physiques vers `index.html`.

Le proxy de développement n’est pas inclus dans les fichiers de production.

## Configuration et architecture

`src/app/core/config/station.config.ts` contient le nom de station, le préfixe API et l’unité monétaire. Le backend n’indique pas de devise : **`u.m.`** est affiché par défaut pour éviter d’inventer une monnaie. Adapter cette valeur à votre station.

```text
src/app/
  core/
    api/              client HTTP, lookups et orchestration des ressources
    config/           identité et préfixe de l’API
    error-handling/   messages HTTP et métier
    layout/           navigation responsive et notifications
  shared/
    components/       tables, états, formulaires et confirmations accessibles
    models/           contrats typés et définitions des ressources
    utilities/        libellés français, dates locales, montants et champs
  features/
    dashboard/ skiers/ pistes/ courses/ instructors/
    registrations/ planning/ subscriptions/ payments/
```

Les pages sont chargées à la demande par le routeur. Les formulaires CRUD utilisent un composant partagé et des définitions par domaine ; planning et tableau de bord ont leurs propres présentations métier. Les services par domaine représentent les contrats réels ; l’orchestrateur partagé gère les commandes CRUD communes.

Après création, le dossier retourné par le serveur est ouvert directement pour retrouver immédiatement l’élément, même hors des filtres de la liste. Après création/modification d’un créneau, le planning se positionne à sa date.

Pas de bibliothèque UI, d’icônes ou de calendrier supplémentaire. Les icônes et illustrations sont en SVG local et les polices utilisent celles du système. Aucun compte utilisateur, rôle, service bancaire, donnée de carte ni stockage local de données métier.

## Pages et comportement

- **Tableau de bord** : effectifs réels, inscriptions datées de la période, départs sous 7 jours et raccourcis d’accueil. Les erreurs sont indépendantes par bloc.
- **Skieurs** : recherche paginée nom/ville, création/modification/suppression, dossier, abonnement et piste, historiques d’inscriptions/paiements.
- **Pistes** : filtre de difficulté, CRUD et skieurs affectés ; aucune ouverture de piste simulée.
- **Cours** : filtres niveau/type/support, CRUD, participants distincts, moniteur, inscriptions et créneaux des 90 prochains jours.
- **Moniteurs** : recherche et CRUD, cours associés, retrait, disponibilités et accès au planning filtré.
- **Inscriptions** : période, cours, semaine ou historique complet d’un skieur ; création sur un créneau ouvert et annulation confirmée.
- **Planning** : vues jour/semaine/liste, filtres, création/modification/annulation des créneaux, inscription directe, ajout et consultation des disponibilités.
- **Abonnements** : combinaison des trois listes par type, filtre de dates/types/statut, CRUD et échéances. Les dates de fin restent calculées par le backend.
- **Paiements** : recherche par skieur ou abonnement, enregistrement en attente, confirmation/échec, reçu JSON téléchargeable et remboursement partiel/total confirmé.
- **404** et menu mobile avec focus clavier, navigation active, états de chargement/erreur/vide, confirmations et notifications de réussite.

Les dates de filtre « au » sont inclusives dans l’interface : l’orchestrateur convertit la borne en début du lendemain pour les endpoints semi-ouverts. Les abonnements conservent la recherche `BETWEEN` inclusive. Les dates/heures du planning restent locales, sans conversion UTC par `toISOString()`.

## Périmètre nettoyé

Les rapports et le matériel/location sont retirés du projet : navigation, routes, pages, services API, modèles et références depuis les dossiers/tableau de bord. Les anciens liens `/reports`, `/equipment` et `/rentals` aboutissent à la page 404 ; aucun appel HTTP n’est fait à leurs anciennes APIs. Les paiements, remboursements et reçus restent conservés. Le tableau de bord ne reconstitue pas un module de rapports financiers à partir d’autres données.

## Limites liées aux APIs

Voir [l’inventaire des routes et points d’intégration](docs/api-integration.md).

- Pas de `GET /payments` global : l’écran demande un skieur ou un abonnement. Un lien depuis une fiche préremplit le filtre.
- Pas de `GET /subscription/all` global : l’écran réunit les listes `ANNUAL`, `MONTHLY`, `SEMESTRIEL`.
- Les recherches globales d’inscriptions sont datées. L’historique par skieur conserve les anciennes lignes sans créneau ; dans ce mode, les dates de filtre sont explicitement ignorées. Un lien d’inscription conserve `skierId` dans l’URL car il n’existe pas de route `GET /registration/{id}`.
- Les créneaux proposés dans le formulaire d’inscription couvrent les 90 prochains jours. Le planning permet de sélectionner une autre période et d’inscrire directement depuis un créneau.
- Ni modification/suppression des disponibilités, ni suppression des paiements/remboursements, ni affectation inverse piste→skieur, ni suppression skieur→piste ne sont exposées. Les opérations correspondantes ne sont pas inventées.
- Aucun PDF fiscal ni séquence de facture n’est généré. Le reçu téléchargé est le JSON fourni par le serveur.
- Les capacités/âges/exclusivités/transitions et protections d’historique restent vérifiés côté serveur. Les messages `400`, `404`, `409` apparaissent dans le formulaire sans perdre la saisie.

## Vérifications effectuées

- Installation npm et versions Angular 21 vérifiées.
- Compilation de production Angular/TypeScript/templates stricts réussie.
- Aucun test automatisé ni test backend exécuté.
- Aucune création/suppression de données serveur effectuée. Le rendu visuel en navigateur et les parcours HTTP de bout en bout restent à vérifier avec le backend en fonctionnement.
