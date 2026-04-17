# Payment Service

Microservice de gestion des paiements pour une application e-commerce.

## Stack technique

- Java 17
- Spring Boot 3.5.13
- Spring Data JPA
- PostgreSQL
- Maven
- Docker

## Démarrage rapide

### Avec Docker Compose

```bash
# Cloner et builder
docker-compose up --build
```

### Sans Docker

```bash
# Configurer PostgreSQL (port 5432)
# Database: payment_db | User: postgres | Password: postgres

# Lancer l'application
mvn spring-boot:run
```

## API Endpoints

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/payments` | Créer un paiement |
| GET | `/api/payments` | Lister tous les paiements |
| GET | `/api/payments/{id}` | Détail d'un paiement |

### Créer un paiement

```bash
curl -X POST http://localhost:8082/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "123e4567-e89b-12d3-a456-426614174000",
    "userId": "user123",
    "amount": 99.99
  }'
```

**Réponse (201 Created) :**
```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "orderId": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "user123",
  "amount": 99.99,
  "status": "SUCCESS",
  "createdAt": "2024-01-15T10:30:00"
}
```

> Le statut est généré aléatoirement : 80% de succès, 20% d'échec.

### Lister les paiements

```bash
curl http://localhost:8082/api/payments
```

### Récupérer un paiement

```bash
curl http://localhost:8082/api/payments/{id}
```

## Configuration

`application.properties` :

```properties
server.port=8082
spring.datasource.url=jdbc:postgresql://localhost:5432/payment_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

## Variables d'environnement (Docker)

| Variable | Défaut |
|----------|--------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/payment_db` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` |

## Structure du projet

```
src/main/java/com/ecommerce/paymentservice/
├── controller/PaymentController.java
├── service/PaymentService.java
├── repository/PaymentRepository.java
├── model/Payment.java
└── dto/PaymentRequest.java / PaymentResponse.java
```

