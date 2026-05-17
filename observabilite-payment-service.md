# Observabilité du microservice `payment-service`
## Description de l'implémentation & Présentation OpenTelemetry

---

## 1. Vue d'ensemble de l'architecture d'observabilité

Le microservice `payment-service` est une application Spring Boot (Java 17) qui expose une API REST de gestion des paiements. L'implémentation de l'observabilité repose sur deux axes complémentaires : la production de **logs JSON structurés sur stdout** et l'**instrumentation automatique OpenTelemetry** via un agent Java injecté au démarrage.

L'ensemble forme une chaîne d'observabilité cohérente où chaque log émis pendant une requête HTTP porte les mêmes identifiants de trace que les spans envoyés au backend de télémétrie (Jaeger en développement, Application Insights en production).

---

## 2. Composants mis en place

### 2.1 Logs structurés JSON — `logback-spring.xml`

La configuration Logback utilise `logstash-logback-encoder` pour produire exclusivement du JSON sur stdout. Chaque ligne de log est un objet JSON autonome contenant les champs suivants :

- `timestamp` : horodatage ISO 8601 UTC
- `level` : niveau de log (DEBUG, INFO, WARN, ERROR)
- `service` : nom du service, lu depuis `spring.application.name` via `<springProperty>`
- `env` : profil Spring actif (`spring.profiles.active`), correspondant à l'environnement de déploiement
- `traceId` : identifiant de trace injecté par l'agent OTel dans le MDC sous la clé `trace_id`, renommé en `traceId` via un provider `<pattern>`
- `spanId` : identifiant de span, même mécanique que `traceId`
- `message` : message applicatif
- `correlationId` : identifiant de corrélation inter-services, propagé via le header HTTP `X-Correlation-Id`
- `userId` : identifiant de l'utilisateur concerné par l'opération
- `exception.type`, `exception.message`, `exception.stackTrace` : présents uniquement en cas d'exception

Le niveau de log est configurable par la variable d'environnement `LOG_LEVEL`, avec `INFO` comme valeur par défaut en production. Les loggers de frameworks internes (Netty, Reactor, HikariCP) sont limités à `WARN` pour réduire le bruit.

### 2.2 Instrumentation OpenTelemetry — agent Java

L'agent `opentelemetry-javaagent.jar` est injecté via la variable d'environnement `JAVA_TOOL_OPTIONS` dans le Dockerfile :

```
ENV JAVA_TOOL_OPTIONS="-javaagent:/app/agent.jar"
```

Cet agent opère de façon transparente sans modification du code applicatif. Il intercepte automatiquement les requêtes HTTP entrantes et sortantes, les appels JDBC, et les transactions Spring pour créer des spans. Il injecte également les identifiants `trace_id` et `span_id` dans le MDC Logback à chaque entrée dans un span, ce qui permet leur inclusion dans les logs JSON.

Les traces sont exportées vers Jaeger via le protocole OTLP/gRPC sur le port 4317. La propagation du contexte de trace entre services s'effectue via le header W3C `traceparent` (`OTEL_PROPAGATORS: tracecontext,baggage`).

### 2.3 Filtre HTTP — `MdcRequestFilter`

Un filtre Servlet Spring (`@Order(HIGHEST_PRECEDENCE)`) intercepte toutes les requêtes HTTP avant qu'elles atteignent les controllers. Son rôle est de poser dans le MDC les champs métier qui ne sont pas gérés par l'agent OTel :

- Il lit le header `X-Correlation-Id` pour assurer la traçabilité inter-services. Si le header est absent (requête externe), il génère un UUID aléatoire.
- Il lit le header `X-User-Id` comme valeur de fallback pour `userId` (utile sur les routes GET qui n'ont pas de body).
- Il garantit le nettoyage du MDC dans un bloc `finally`, évitant toute fuite entre requêtes sur le pool de threads Tomcat.

### 2.4 Controller — `PaymentController`

Le controller enrichit le MDC après la désérialisation du body de la requête. Cette séparation est nécessaire car le body n'est pas accessible dans le filtre sans consommer l'`InputStream` de façon irréversible.

Sur le `POST /api/payments`, `MDC.put("userId", request.getUserId())` écrase le fallback posé par le filtre avec la valeur réelle issue du body. Ce mécanisme garantit que tous les logs émis par le service layer et le repository layer portent le `userId` correct.

### 2.5 Configuration Docker Compose

Le service est configuré avec les variables d'environnement suivantes pour l'observabilité :

```yaml
LOG_LEVEL: DEBUG
OTEL_SERVICE_NAME: payment-service
OTEL_RESOURCE_ATTRIBUTES: "service.name=payment-service,service.version=1.0.0"
OTEL_PROPAGATORS: tracecontext,baggage
OTEL_TRACES_EXPORTER: otlp
OTEL_EXPORTER_OTLP_PROTOCOL: grpc
OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4317
```

---

## 3. Comportement observé en production

### Cycle de vie d'un `POST /api/payments`

```
MdcRequestFilter      →  MDC : { correlationId, userId: "anonymous" }
OTel Agent            →  MDC : { trace_id, span_id }  (span HTTP ouvert)
PaymentController     →  MDC : { userId: "user-xyz" } (écrase le fallback)
PaymentService        →  logs avec tous les champs MDC
PaymentRepository     →  nouveaux spans JDBC ouverts (spanId change)
Réponse HTTP          →  span HTTP fermé
MdcRequestFilter      →  MDC.clear()
```

### Champs MDC selon le type de requête

| Route | correlationId | userId | traceId | spanId |
|---|---|---|---|---|
| POST /api/payments | ✅ header ou UUID | ✅ body | ✅ OTel | ✅ OTel |
| GET /api/payments | ✅ header ou UUID | "anonymous" | ✅ OTel | ✅ OTel |
| Threads HikariCP | — | — | vide | vide |

Les logs des threads de maintenance HikariCP (pool stats) ont des `traceId` vides car ils s'exécutent en dehors de tout contexte de requête HTTP — comportement normal et attendu.

---

## 4. Présentation : OpenTelemetry, Logs et Tracing

### 4.1 Qu'est-ce qu'OpenTelemetry ?

OpenTelemetry (OTel) est un standard open source né de la fusion de deux projets pionniers, OpenCensus (Google) et OpenTracing (CNCF), en 2019. Il est aujourd'hui le standard de facto pour l'instrumentation des applications cloud-native, maintenu par la Cloud Native Computing Foundation (CNCF).

Son objectif est de fournir un ensemble unifié de SDKs, d'APIs et d'outils pour collecter trois types de signaux de télémétrie : les **traces**, les **métriques** et les **logs**. L'idée fondamentale est de séparer l'instrumentation (le code qui produit les données) du backend (le système qui les stocke et les analyse). Une application instrumentée avec OTel peut envoyer ses données vers Jaeger, Zipkin, Datadog, Azure Application Insights ou tout autre outil compatible, sans modifier une ligne de code applicatif.

### 4.2 Les trois piliers de l'observabilité

**Les traces distribuées** constituent le signal le plus différenciant d'OpenTelemetry. Une trace représente le chemin complet d'une requête à travers un système distribué, de son entrée jusqu'à sa réponse. Elle est composée d'une série de **spans**, chacun représentant une opération unitaire (appel HTTP entrant, requête SQL, appel à un service externe).

Chaque span porte un `traceId` (identifiant unique de la transaction bout-en-bout) et un `spanId` (identifiant de l'opération courante). Lorsqu'un service appelle un autre service, il transmet le `traceId` dans les headers HTTP via le standard W3C Trace Context (`traceparent`). Le service appelé crée un nouveau span enfant qui hérite du même `traceId`, permettant de reconstruire l'arbre complet de la transaction dans un outil de visualisation.

**Les métriques** sont des mesures numériques agrégées dans le temps : compteurs de requêtes, latences (histogrammes), taux d'erreur, utilisation mémoire. Elles permettent l'alerting et la visualisation de tendances sur des dashboards.

**Les logs** sont des événements discrets horodatés. Dans le contexte OTel, leur valeur est décuplée quand ils sont **corrélés aux traces** : un log portant le même `traceId` qu'un span peut être retrouvé instantanément lors du diagnostic d'une anomalie.

### 4.3 Le modèle de données d'une trace

```
traceId: 443daf5421f19754838a9aafda6cd4e1  (identique sur toute la transaction)
│
├── Span: POST /api/payments           spanId: f4fe2b34a92497c5   (span racine)
│   ├── Span: processPayment()         spanId: 2d0d0e28931d9d5b   (enfant)
│   │   ├── Span: SELECT payments      spanId: 03b59fa8f4d84c6a   (JDBC)
│   │   └── Span: INSERT payments      spanId: c3f1e4a8d656be0a   (JDBC)
│   └── [logs corrélés avec traceId identique]
```

Ce modèle hiérarchique permet de visualiser dans Jaeger ou Application Insights la timeline exacte d'une requête, d'identifier le span le plus lent, et de naviguer depuis un log d'erreur directement vers la trace correspondante.

### 4.4 L'instrumentation automatique Java

L'agent OTel Java (`opentelemetry-javaagent.jar`) fonctionne par **bytecode instrumentation** au démarrage de la JVM. Il utilise l'API Java `java.lang.instrument` pour modifier les classes chargées en mémoire avant leur exécution. Cette technique, appelée "auto-instrumentation", permet d'instrumenter automatiquement des dizaines de frameworks (Spring MVC, Spring Data JPA, JDBC, gRPC, Kafka, etc.) sans modifier le code source.

Concrètement, pour Spring Boot, l'agent intercepte :
- chaque requête HTTP entrante pour ouvrir un span racine
- chaque appel `RestTemplate` ou `WebClient` sortant pour propager le contexte et créer un span enfant
- chaque exécution de requête JDBC pour créer un span avec la requête SQL
- chaque méthode annotée `@Transactional` pour créer des spans de transaction

L'agent injecte également les identifiants de trace dans le MDC SLF4J/Logback (`trace_id`, `span_id`), ce qui permet leur inclusion automatique dans les logs sans aucune modification du code de logging.

### 4.5 La propagation du contexte W3C Trace Context

Le standard W3C Trace Context définit deux headers HTTP pour la propagation du contexte de trace entre services :

- `traceparent` : porte le `traceId`, le `spanId` du span parent, et des flags (ex: `00-443daf5421f19754838a9aafda6cd4e1-f4fe2b34a92497c5-01`)
- `tracestate` : porte des métadonnées vendor-spécifiques optionnelles

Quand `order-service` appelle `payment-service`, il envoie ce header. L'agent OTel sur `payment-service` le lit, extrait le `traceId` et crée un nouveau span enfant avec ce même `traceId`. C'est ce mécanisme qui permet à une transaction `catalog → order → payment` d'apparaître comme une seule transaction dans Application Insights avec les trois spans visibles dans la même timeline.

### 4.6 La corrélation logs/traces : la valeur ajoutée clé

Sans corrélation, le diagnostic d'un incident en production ressemble à une enquête avec deux sources d'information indépendantes : d'un côté les traces qui montrent où la latence se passe, de l'autre les logs qui montrent ce qui s'est passé, mais sans lien explicite entre les deux.

Avec la corrélation OTel + MDC, chaque log porte le `traceId` de la requête qui l'a généré. Le workflow de diagnostic devient :

1. Une alerte se déclenche sur une latence anormale (`p99 > 2s`) dans Application Insights.
2. On navigue vers la trace correspondante dans l'interface, on identifie le span lent (ex: `SELECT payments` à 1.8s).
3. On filtre les logs avec le même `traceId` pour voir le contexte complet : `userId`, `correlationId`, paramètres de la requête, messages d'erreur éventuels.
4. En 30 secondes, on sait qui était l'utilisateur, quelle était sa requête, et dans quelle opération le temps a été perdu.

C'est précisément ce que l'architecture mise en place dans ce microservice rend possible.

### 4.7 Roadmap : migration vers Azure Application Insights

L'implémentation actuelle exporte les traces vers Jaeger (backend local pour le développement). La migration vers Azure Application Insights pour les environnements de recette et production nécessite deux changements :

Remplacer l'agent OTel générique par l'agent Azure Monitor (`applicationinsights-agent.jar`) qui intègre nativement l'exporteur Azure Monitor, la capture des logs Logback, et le remontage automatique des exceptions dans la table `exceptions` d'Application Insights.

Configurer la variable d'environnement `APPLICATIONINSIGHTS_CONNECTION_STRING` avec la chaîne de connexion de la ressource `appi-formation-ecom-payment-{env}`. L'agent se charge du reste : traces dans la table `dependencies`/`requests`, logs dans `traces`, exceptions dans `exceptions`, toutes corrélées par le même `operationId` (équivalent du `traceId` dans Application Insights).

---

*Document généré le 03/05/2026 — payment-service v1.0.0*
