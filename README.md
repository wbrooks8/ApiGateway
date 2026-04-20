# Order Platform

A microservices-based order management platform built with Spring Boot and Spring Cloud Gateway. The system handles the full order lifecycle — from placement through inventory reservation to customer notification — across three independently deployable services behind a single API gateway.

## Architecture

```
Client → API Gateway (8080)
              ├── /orders/**        → Order Service (8081)
              ├── /inventory/**     → Inventory Service (8082)
              └── /notifications/** → Notification Service (8083)
```

**Order Service** stores orders in PostgreSQL and coordinates with the other services when an order is created: it reserves stock in the Inventory Service and dispatches a confirmation via the Notification Service.

**Inventory Service** tracks stock levels with a `quantity` / `reservedQuantity` model. It exposes reserve and release endpoints so that inventory is held for confirmed orders and can be returned if an order is cancelled.

**Notification Service** accepts notification requests and logs them in memory. It demonstrates the notification contract without requiring an external email or messaging provider.

**Gateway** is a Spring Cloud Gateway instance that routes all inbound traffic to the correct downstream service.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.x |
| Gateway | Spring Cloud Gateway |
| Persistence | Spring Data JPA + PostgreSQL |
| Migrations | Flyway |
| Containerisation | Docker / Docker Compose |
| Build | Gradle (multi-module) |

## Services

### Order Service — port 8081

| Method | Path | Description |
|---|---|---|
| `POST` | `/orders` | Place a new order (reserves inventory, sends notification) |
| `GET` | `/orders` | List all orders |
| `GET` | `/orders/{id}` | Get a single order |
| `GET` | `/orders/customer/{customerId}` | Get orders for a customer |
| `PUT` | `/orders/{id}/status` | Update order status |
| `DELETE` | `/orders/{id}` | Delete an order |

**Create order request:**
```json
{
  "customerId": "customer-123",
  "items": [
    { "productId": "prod-abc", "quantity": 2, "price": 29.99 }
  ]
}
```

### Inventory Service — port 8082

| Method | Path | Description |
|---|---|---|
| `POST` | `/inventory` | Add a new inventory item |
| `GET` | `/inventory` | List all inventory |
| `GET` | `/inventory/{productId}` | Get item by product ID |
| `PUT` | `/inventory/{productId}` | Update item name or quantity |
| `PUT` | `/inventory/{productId}/reserve` | Reserve stock |
| `PUT` | `/inventory/{productId}/release` | Release reserved stock |
| `DELETE` | `/inventory/{productId}` | Remove an inventory item |

**Add inventory request:**
```json
{
  "productId": "prod-abc",
  "name": "Widget Pro",
  "quantity": 100
}
```

### Notification Service — port 8083

| Method | Path | Description |
|---|---|---|
| `POST` | `/notifications` | Send a notification |
| `GET` | `/notifications` | List all notifications |
| `GET` | `/notifications/{id}` | Get a notification by ID |

## Order Flow

When `POST /orders` is called:

1. The Order Service attempts to reserve stock for each line item by calling `PUT /inventory/{productId}/reserve` on the Inventory Service.
2. If any reservation fails (insufficient stock or product not found), all previously reserved items are released and a `409 Conflict` is returned to the caller.
3. If all reservations succeed, the order is persisted with status `CONFIRMED`.
4. The Order Service fires a best-effort confirmation to the Notification Service. If this call fails the order is still confirmed; the failure is logged as a warning.

## Running with Docker Compose

### Prerequisites

- Docker Desktop

### Start the stack

```bash
docker compose up --build
```

All services start in dependency order. The databases run health checks before the application services boot.

### Stop

```bash
docker compose down
```

To also remove the persisted volumes:

```bash
docker compose down -v
```

## Running Locally

### Prerequisites

- Java 21
- PostgreSQL running on port 5432 with a second instance on 5433
- Gradle wrapper (`./gradlew`)

Create the two databases:

```sql
CREATE DATABASE orders_db;
CREATE USER order_user WITH PASSWORD 'order_password';
GRANT ALL PRIVILEGES ON DATABASE orders_db TO order_user;

CREATE DATABASE inventory_db;
CREATE USER inventory_user WITH PASSWORD 'inventory_password';
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO inventory_user;
```

Start each service:

```bash
./gradlew :order-service:bootRun
./gradlew :inventory-service:bootRun
./gradlew :notification-service:bootRun
./gradlew :gateway:bootRun
```

## Health Checks

Each service exposes a health endpoint via Spring Boot Actuator:

| Service | URL |
|---|---|
| Order Service | `http://localhost:8081/actuator/health` |
| Inventory Service | `http://localhost:8082/actuator/health` |
| Notification Service | `http://localhost:8083/actuator/health` |

## Example Walkthrough

```bash
# 1. Add inventory for a product
curl -X POST http://localhost:8080/inventory \
  -H "Content-Type: application/json" \
  -d '{"productId":"prod-001","name":"Widget Pro","quantity":50}'

# 2. Place an order
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-42",
    "items": [{"productId":"prod-001","quantity":3,"price":19.99}]
  }'

# 3. Check inventory levels (reservedQuantity is now 3)
curl http://localhost:8080/inventory/prod-001

# 4. View the confirmation notification
curl http://localhost:8080/notifications
```

## Project Structure

```
order-platform/
├── gateway/                  # Spring Cloud Gateway — routes traffic to downstream services
├── order-service/            # Order management + inter-service orchestration
├── inventory-service/        # Stock tracking with reserve/release
├── notification-service/     # Notification dispatch (in-memory store)
├── docker-compose.yml
└── settings.gradle.kts
```
