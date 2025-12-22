**Plan d'Assurance Qualité (PAQ) — Projet Adhésion**

**1. Contexte du projet**

- **Résumé**: Projet `adhesion-app` est une application complète de gestion d'adhésions composée de 3 modules principaux: backend Java (Spring/Maven), frontend administratif Angular (`adhesion-admin-angular`) et client mobile Flutter (`adhesion_app_client_flutter`). Le projet inclut un conteneur PostgreSQL pour données.
- **Environnement**: Dépôt mono-repo sur la machine locale et CI (prévu). Artefacts: `pom.xml`, `Dockerfile`, `docker-compose.yml`, code Java sous `src/main/java`, app Angular sous `adhesion-admin-angular` et app Flutter sous `adhesion_app_client_flutter`.
- **Contraintes**: SLA de disponibilité pour le backend lors des tests d'intégration, compatibilité mobile Android/iOS pour le client Flutter.

**2. Objectif du projet**

- **But principal**: Fournir une application de gestion d'adhésions stable, testée et maintenable, avec une couverture de tests croissante et des processus qualité définis.
- **Objectifs QA**:
  - Assurer une couverture minimale de tests unitaires de 70% pour le backend et 60% pour le frontend.
  - Mettre en place pipeline CI automatisé exécutant tests unitaires, tests d'API et checks de lint.
  - Détecter et corriger les régressions fonctionnelles et non-fonctionnelles avant livraison.

**3. Planning (proposé — dates inventées, format JJ/MM/AAAA)**

- **Phase 0 — Préparation**: 02/01/2026 - 08/01/2026
  - Installation CI, initialisation du dépôt de tests, configuration Sonar (si applicable).
- **Phase 1 — Analyse & cahier des charges**: 09/01/2026 - 22/01/2026
  - Rédaction du Cahier des Charges fonctionnel et technique.
- **Phase 2 — Mise en place qualité & tests unitaires**: 23/01/2026 - 12/02/2026
  - Ajout tests unitaires backend (JUnit/Mockito), frontend (Jest/Karma), Flutter (flutter_test).
- **Phase 3 — Tests d'intégration & API**: 13/02/2026 - 26/02/2026
  - Tests d'API (RestAssured/postman), intégration DB, jeux de données.
- **Phase 4 — Tests UI automatisés & non-fonctionnels**: 27/02/2026 - 19/03/2026
  - Tests Selenium pour Angular, Flutter driver / integration_test pour mobile.
  - Tests performance basiques (JMeter/artillery) et tests de charge.
- **Phase 5 — Recette & stabilisation**: 20/03/2026 - 02/04/2026
  - Correction des anomalies, tests de régression, préparation release.
- **Livraison finale**: 09/04/2026

Livrables intermédiaires: cahier des charges (22/01/2026), rapport de couverture (12/02/2026), plan de tests (13/02/2026), rapport performance (19/03/2026), PAQ final (02/04/2026).

**4. Revue et cérémonies (organisation des réunions)**

- **Quick‑off meeting**: réunion de lancement — 09/01/2026. Objectif: aligner parties prenantes, périmètre et responsabilités.
- **Réunions hebdomadaires d'avancement (Stand‑up étendu)**: chaque lundi 10:00, revue progrès, blocages et priorités QA.
- **Revue de milestone (Release Review)**: à la fin de chaque phase (dates ci‑dessus) — présenter résultats tests, risques ouverts, plan d'action.
- **Comité QA**: toutes les deux semaines — participants: PO, Tech Lead, QA Engineer, Responsable Produit.
- **Revue de code (Pull Request Review)**: exigence: au moins 1 relecteur technique + 1 approbation QA pour les fonctionnalités critiques.
- **Rétrospective**: après la livraison de la release — analyser ce qui a marché / améliorations pour le prochain cycle.

**5. Documentation**

- **Documents utilisés comme input**:
  - Cahier des Charges fonctionnel et technique (à produire pendant Phase 1).
  - Spécifications d'API (OpenAPI / Postman collection).
  - Architecture technique (diagrammes Docker/DB/services).
  - Historique des incidents et anomalies (bugtracker requis — JIRA/GitHub Issues).
  - Exigences non‑fonctionnelles (SLA, performances, compatibilité mobile).
- **Processus de création**:
  - Le `Cahier des Charges` est le document central: rédigé par le PO et l'architecte, versionné dans le repo sous `docs/` et publié en PDF pour approbation.
  - Les spécifications d'API sont maintenues sous forme OpenAPI (yaml/json) et exportées en Postman collections pour tests.
  - Tous les documents sont soumis à revue par le comité QA avant approbation finale.

5-1. Règles de gestion et de structuration des documents

- **Arborescence**: `docs/` à la racine du projet. Sous-dossiers: `specs/`, `design/`, `tests/`, `reports/`.
- **Nommage**: `YYYYMMDD_type_titre_version.ext` (ex: `20260122_cahier_des_charges_v1.0.pdf`).
- **Versioning**: chaque document majeur a un champ `version` et un changelog minimal. Les documents sources (Markdown/OpenAPI) sont dans Git; les exports PDF sont produits via pipeline.
- **Accès**: contrôle via gestionnaire de droits (à configurer dans le dépôt ou serveur documentaire).
- **Modèle**: utiliser template standard pour cahier des charges (objectifs, périmètre, exigences, critères d'acceptation, contraintes, annexes).

5-2. Utilisation du Cahier des Charges

- **Rôle**: document contractuel de référence définissant exigences et critères d'acceptation. Toute modification suit procédure de change request et doit être approuvée par PO et comité QA.
- **Validation**: signature électronique ou approbation Git PR (merge only after approval).

**6. Critères et métriques de qualité**

- **Critères fonctionnels**:
  - Tous les cas critiques (liste définie dans cahier des charges) sont couverts par des tests automatisés.
  - Taux de régression attendu: 0 bloquants ouverts en sortie de recette.
- **Métriques techniques**:
  - **Couverture tests unitaires**: Backend >= 70%, Angular >= 60%, Flutter >= 60%.
  - **Code Quality (Sonar)**: Technical Debt / Reliability rating <= seuil défini (ex: major < 5).
  - **Bugs ouverts**: Nombre de bugs critiques/majeurs <= 0 / <= 3 respectivement lors des releases.
  - **Mean Time To Repair (MTTR)**: objectif <= 72 heures pour anomalies critiques.
  - **Performance**: API P95 response time < 500ms pour 95% des requêtes en test de charge léger.
  - **Sécurité**: Zero vulnérabilités CVE critiques dans dépendances (scan automatisé dépendances).
- **Suivi**: rapports hebdomadaires, dashboard Sonar + CI pipeline badges.

**7. Tests (stratégie détaillée)**n- **Tests unitaires**:

- Backend: `JUnit5` + `Mockito` — tests pour services, repositories, utilitaires.
- Angular: `Karma` + `Jasmine` ou `Jest` — tests de composants et services.
- Flutter: `flutter_test` — tests widget et logique.
- **Tests d'intégration**:
  - Backend: tests d'intégration Spring Boot (testcontainers pour PostgreSQL), RestAssured pour endpoints.
  - Scénarios end-to-end API via Postman collections / Newman dans CI.
- **Tests UI automatisés**:
  - Angular (administration): Selenium WebDriver (ou Playwright) avec scripts clés (login, CRUD adhérents, export).
  - Mobile Flutter: integration_test ou `flutter_driver` (selon version) pour scénarios critiques (création d'adhésion, navigation principale).
- **Tests fonctionnels manuels**:
  - Scénarios de recette listés dans `docs/tests/plan_de_tests.md` et exécutés par QA.
- **Tests non-fonctionnels**:
  - Performance: JMeter / k6 pour scénarios principaux, vérification P95, throughput.
  - Sécurité: scan dépendances (OWASP Dependency‑Check / Snyk) et tests basiques d'injection.
  - Compatibilité: tests sur Android (API >= 21), iOS versions cibles, navigateurs Chrome/Firefox pour admin.
- **Automatisation & CI**:
  - Pipeline CI: étapes lint → build → tests unitaires → tests d'intégration → packaging → rapports.
  - Exécution des suites longues (UI, perf) en pipeline nightly ou manuelle pré-release.

**8. Normes et conventions de codage**

- **Java (backend)**:
  - Standard: suivre Google Java Style / conventions Spring.
  - Formatage automatique via `maven-checkstyle-plugin` / `spotless` si possible.
  - Nommage: classes PascalCase, méthodes camelCase, constantes UPPER_SNAKE.
  - Exceptions: utiliser exceptions typées et messages localisables.
- **Angular (frontend admin)**:
  - Standard: Angular Style Guide (Tour of Heroes conventions).
  - Lint: `eslint` + `prettier` rules. Components réutilisables, services pour logique métier.
- **Flutter (mobile)**:
  - Style: suivre `flutter format` et `effective_dart` lint rules.
  - Architecture: séparation `presentation` / `domain` / `data` (ou `bloc` / `provider` selon choix).
- **Commits & PRs**:
  - Message de commit: `[TYPE] scope: courte description` (ex: `feat(auth): add login endpoint`).
  - Contrib: PRs petites, description complète, checklist QA (tests unitaires, review approuvée, build CI verte).
- **Sécurité du code**:
  - Pas d'informations sensibles dans le code (credentials dans variables d'environnement ou secret manager).
  - Static analysis et dépendancy scan intégrés dans pipeline.



