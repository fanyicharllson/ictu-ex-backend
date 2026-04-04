# ICTU-Ex Backend

> **Smart Student Marketplace** — Software Architecture Project  
> ICT University Cameroon | Kotlin + Spring Boot 3.4.4 | Modular Monolith + Event-Driven

---

## Project Overview

ICTU-Ex is a peer-to-peer student marketplace enabling students at ICT University Cameroon to buy and sell items within the campus community. The backend is a Kotlin + Spring Boot 3.4.4 modular monolith using Spring Modulith to enforce clean module boundaries, with an event-driven architecture powered by Apache Kafka.

This backend is one part of a two-course project:

- **Android App** (Android Development course) — Kotlin + Jetpack Compose + Firebase for auth and real-time features
- **Backend API** (Software Architecture course) — Spring Boot REST API serving marketplace logic, deployed on DigitalOcean with Kubernetes

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 21) |
| Framework | Spring Boot 3.4.4 |
| Architecture | Modular Monolith + Spring Modulith + Event-Driven (Kafka) |
| Database | PostgreSQL + Flyway migrations |
| Containerization | Docker + Jib by Google |
| Orchestration | k3s (Kubernetes) on DigitalOcean |
| CI/CD | Jenkins (Docker container) |
| Monitoring | Prometheus + Grafana |
| IaC | Ansible (3 playbooks) |
| Android | Kotlin + Jetpack Compose + Firebase (separate course) |

---

## Module Structure

Spring Modulith enforces that modules only communicate through their public APIs — direct access to `internal` packages fails at compile time.

| Module | Package | Responsibility |
|---|---|---|
| `ictu-ex-app` | `com.fanyiadrien.ictuexbackend` | Main entry point, Spring Boot bootstrap, app config |
| `shared` | `com.fanyiadrien.shared` | Shared domain events, common models, utilities |
| `auth` | `com.fanyiadrien.auth` | Student registration, JWT tokens, Spring Security |
| `listing` | `com.fanyiadrien.listing` | Create/search/manage marketplace item listings |
| `messaging` | `com.fanyiadrien.messaging` | Buyer-seller real-time chat |
| `notification` | `com.fanyiadrien.notification` | Kafka consumer, push alert dispatch |
| `sync` | `com.fanyiadrien.sync` | Offline sync support for Android clients |

### Module Boundary Rules

- Public API classes live directly in the module package (e.g., `com.fanyiadrien.auth.AuthUser`)
- Internal implementation lives in the `internal/` sub-package — never accessible cross-module
- Modules communicate via Spring Application Events (Kafka) — not direct method calls

---

## Project Structure

```
ictu-ex-backend/
├── ictu-ex-app/                        # Main Spring Boot entry point
│   └── src/main/
│       ├── kotlin/com/fanyiadrien/ictuexbackend/
│       │   └── IctuExBackendApplication.kt
│       └── resources/
│           └── application.properties
├── shared/                             # Shared events & models
├── auth/
│   └── src/main/
│       ├── kotlin/com/fanyiadrien/auth/
│       │   ├── AuthUser.kt             # Public API
│       │   ├── AuthController.kt       # REST endpoints
│       │   └── internal/              # Hidden from other modules
│       │       ├── AuthService.kt
│       │       ├── SecurityConfig.kt
│       │       └── persistence/
│       │           ├── UserEntity.kt
│       │           └── UserRepository.kt
│       └── resources/db/migration/
│           └── V1__create_users_table.sql
├── listing/
│   └── src/main/kotlin/com/fanyiadrien/listing/
│       └── internal/
├── messaging/
├── notification/
├── sync/
├── k8s/                                # Kubernetes manifests
├── docker-compose.yml                  # Local dev: Postgres + Kafka
├── Dockerfile
├── Jenkinsfile
├── build.gradle.kts                    # Parent build config
└── settings.gradle.kts
```

---

## Core Data Models

Models reused and extended from the Android Firebase app.

### User — `auth` module

```sql
id                UUID         PRIMARY KEY DEFAULT gen_random_uuid()
email             VARCHAR(255) NOT NULL UNIQUE
display_name      VARCHAR(255) NOT NULL
student_id        VARCHAR(100) NOT NULL UNIQUE
user_type         VARCHAR(20)  CHECK (user_type IN ('SELLER', 'BUYER'))
password_hash     VARCHAR(255) NOT NULL
profile_image_url VARCHAR(500)
created_at        TIMESTAMP    DEFAULT now()
updated_at        TIMESTAMP    DEFAULT now()
```

### Listing — `listing` module

```sql
id           UUID           PRIMARY KEY DEFAULT gen_random_uuid()
seller_id    UUID           REFERENCES users(id)
title        VARCHAR(255)
description  TEXT
price        DECIMAL(10,2)
category     VARCHAR(100)
available    BOOLEAN        DEFAULT true
created_at   TIMESTAMP      DEFAULT now()
```

---

## Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `spring-boot-starter-web` | 3.4.4 | REST API layer |
| `spring-boot-starter-data-jpa` | 3.4.4 | Database access via Hibernate |
| `spring-modulith` | 1.3.x | Module boundary enforcement |
| `spring-kafka` | 3.x | Event-driven messaging |
| `spring-boot-starter-security` | 3.4.4 | Auth + JWT |
| `flyway-core` | 10.x | Database schema migrations |
| `postgresql` | 42.x | DB driver |
| `spring-boot-starter-actuator` | 3.4.4 | Health + metrics endpoints |
| `micrometer-registry-prometheus` | latest | Prometheus metrics export |
| `kotlinx-coroutines` | 1.8.x | Async Kotlin |
| `spring-boot-starter-test` | 3.4.4 | Unit + integration tests |

---

## Infrastructure

### DigitalOcean Droplet

- **IP:** `167.172.99.14`
- **OS:** Ubuntu 24.04
- **Specs:** 2 vCPU, 1.9 GB RAM + 2 GB swap
- **Orchestration:** k3s (lightweight Kubernetes)

### Services Running on VPS (k3s pods)

- PostgreSQL — primary database
- Apache Kafka — event bus
- Prometheus — metrics scraping
- Grafana — monitoring dashboards
- Jenkins — CI/CD (Docker container on port 8080)

### Ansible Playbooks

| Playbook | Purpose |
|---|---|
| `playbook1-install.yml` | Installs Java 21, Docker, k3s, Git |
| `playbook2-services.yml` | Starts Kafka, Postgres, Prometheus, Grafana |
| `playbook3-healthcheck.yml` | Verifies all services are healthy |

---

## Local Development Setup

### Prerequisites

- JDK 21
- Docker + Docker Compose
- IntelliJ IDEA (recommended)

### 1. Clone the Repository

```bash
git clone https://github.com/fanyicharllson/ictu-ex-backend.git
cd ictu-ex-backend
```

### 2. Start Local Services

```bash
docker-compose up -d
```

Spins up PostgreSQL and Kafka locally.

### 3. Run the Application

```bash
./gradlew :ictu-ex-app:bootRun
```

### 4. Verify Flyway Migration

On first start you should see in logs:

```
Flyway: Migrating schema "public" to version 1 - create users table
Spring Data: Found 1 JPA repository interfaces.
Started IctuExBackendApplication
```

### 5. Verify Table on VPS (after Jenkins deploy)

```bash
kubectl exec -it <postgres-pod> -- psql -U postgres -d ictuex -c "\dt"
```

### 6. Test Auth Endpoint

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@ictu.cm",
    "displayName": "John Doe",
    "studentId": "ICT2024001",
    "userType": "BUYER",
    "password": "securepass"
  }'
```

---

## CI/CD Pipeline

Jenkins pipeline (Jenkinsfile) runs on every push to `main`:

1. **Checkout** — clone repo
2. **Build** — `./gradlew build`
3. **Test** — `./gradlew test` (includes Spring Modulith boundary tests)
4. **Docker Build** — Jib or Dockerfile
5. **Deploy** — `kubectl apply` to k3s cluster on DigitalOcean

---

## API Endpoints

### Auth — `/api/auth`

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | No | Register a new student (SELLER or BUYER) |
| `POST` | `/api/auth/login` | No | Authenticate and receive JWT *(coming soon)* |
| `GET` | `/api/auth/me` | Yes | Get current authenticated user *(coming soon)* |

### Listing — `/api/listings` *(coming soon)*

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `GET` | `/api/listings` | No | Browse all available listings |
| `POST` | `/api/listings` | Yes (SELLER) | Create a new listing |
| `GET` | `/api/listings/{id}` | No | Get listing details |
| `PUT` | `/api/listings/{id}` | Yes (owner) | Update a listing |
| `DELETE` | `/api/listings/{id}` | Yes (owner) | Remove a listing |

---

## Build Status

| Task | Status |
|---|---|
| DigitalOcean droplet provisioned (2vCPU, 1.9GB + 2GB swap) | ✅ Done |
| Swap space configured | ✅ Done |
| Ansible installed + 3 playbooks | ✅ Done |
| Docker + k3s + Java 21 installed on server | ✅ Done |
| Jenkins running as Docker container (port 8080) | ✅ Done |
| k3s cluster running | ✅ Done |
| Postgres + Kafka + Prometheus + Grafana pods | ✅ Done |
| Spring Boot modular monolith created | ✅ Done |
| All submodules configured | ✅ Done |
| ModularityTests passing | ✅ Done |
| Jenkinsfile + Dockerfile + k8s YAMLs committed | ✅ Done |
| Auth module — UserEntity + UserRepository + Flyway migration | ✅ Done |
| Fix gradlew permission in Jenkinsfile → Jenkins build | ⬜ Todo |
| Jenkins deploys app → verify `users` table on VPS | ⬜ Todo |
| JWT implementation (auth module) | ⬜ Todo |
| Listing module CRUD | ⬜ Todo |
| Kafka event publishing | ⬜ Todo |
| Messaging module | ⬜ Todo |
| Notification module (Kafka consumer) | ⬜ Todo |
| Sync module (offline Android) | ⬜ Todo |

---

## Team

- **Team Lead / Architect:** Fanyi Charllson Adrienne
- **Institution:** ICT University Cameroon
- **Course:** Software Architecture
- **GitHub:** [fanyicharllson/ictu-ex-backend](https://github.com/fanyicharllson/ictu-ex-backend)
- **Group ID:** `com.fanyiadrien`
- **Base Package:** `com.fanyiadrien.ictuexbackend`

---

*ICTU-Ex Backend — ICT University Cameroon — Software Architecture Course — 2026*