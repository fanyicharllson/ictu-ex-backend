# ICTU-Ex Architecture Diagram Description v2
**Project:** ICTU-Ex — Smart Student Marketplace  
**Architecture Style:** Event-Driven Modular Monolith  
**For:** draw.io / Lucidchart / Excalidraw

---

## DIAGRAM OVERVIEW — 5 ZONES

```
┌─────────────────────────────────────────────────────┐
│  ZONE 1: CLIENT LAYER                               │
└─────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────┐
│  ZONE 2: CI/CD LAYER (right side)                   │
└─────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────┐
│  ZONE 3: DIGITALOCEAN CLOUD (main zone)             │
│  ┌──────────────────────────────────────────────┐   │
│  │  Kubernetes Cluster                          │   │
│  │  ┌────────────┐  ┌───────────────────────┐  │   │
│  │  │ API Gateway│  │  Spring Boot App Pods  │  │   │
│  │  └────────────┘  └───────────────────────┘  │   │
│  │  ┌────────────┐  ┌───────────────────────┐  │   │
│  │  │   Kafka    │  │   Redis Cache         │  │   │
│  │  └────────────┘  └───────────────────────┘  │   │
│  │  ┌────────────┐  ┌───────────────────────┐  │   │
│  │  │ Prometheus │  │   Grafana             │  │   │
│  │  └────────────┘  └───────────────────────┘  │   │
│  │  ┌────────────────────────────────────────┐  │   │
│  │  │ MongoDB (in-cluster — activity logs)   │  │   │
│  └──────────────────────────────────────────┘  │   │
│  ┌──────────────────────────────────────────┐   │   │
│  │  Ansible (IaC)                           │   │   │
│  └──────────────────────────────────────────┘   │   │
└─────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────┐
│  ZONE 4: EXTERNAL SERVICES                          │
└─────────────────────────────────────────────────────┘
```

---

## ZONE 1: CLIENT LAYER (Top)

Draw two boxes side by side:

**Box 1 — Android App:**
```
┌──────────────────────────────┐
│  📱 Android App              │
│  Kotlin + Jetpack Compose    │
│  Firebase (offline-first)    │
│  Sensor integration          │
│  Offline sync                │
└──────────────────────────────┘
```
Color: Blue gradient | Icon: Android robot

**Box 2 — Developer Workstation:**
```
┌──────────────────────────────┐
│  💻 Developer Workstation    │
│  IntelliJ IDEA Ultimate      │
│  Kotlin + Spring Boot 3.4.4  │
│  Docker Desktop (local dev)  │
└──────────────────────────────┘
```
Color: Gray | Icon: Laptop

**Arrow:** Developer Workstation → GitHub (right side)
Label: `git push origin main`

**Arrow:** Android App → API Gateway (center zone)
Label: `HTTPS REST /api/**`

---

## ZONE 2: CI/CD LAYER (Right side vertical strip)

Draw vertically on the right side:

**Box 1:**
```
┌──────────────────────────┐
│  GitHub Repository       │
│  fanyicharllson/         │
│  ictu-ex-backend         │
│  main branch             │
└──────────────────────────┘
```
Color: Dark gray | Icon: GitHub Octocat

**Arrow down:** Label: `webhook → POST /github-webhook/`

**Box 2:**
```
┌──────────────────────────┐
│  Jenkins                 │
│  Docker container        │
│  Port 8080               │
│  ┌─────────────────────┐ │
│  │ Pipeline Stages:    │ │
│  │ 1. Checkout         │ │
│  │ 2. Build (Gradle)   │ │
│  │ 3. Test (JUnit)     │ │
│  │ 4. Coverage (Kover) │ │
│  │    min 90% gate     │ │
│  │ 5. Docker build     │ │
│  │ 6. Deploy k8s       │ │
│  └─────────────────────┘ │
└──────────────────────────┘
```
Color: Amber/Orange | Icon: Jenkins logo

**Arrow right:** Label: `docker push :BUILD_NUMBER`

**Box 3:**
```
┌──────────────────────────┐
│  Docker Hub              │
│  fanyicharllson/         │
│  ictu-ex-backend:latest  │
│  Image registry          │
└──────────────────────────┘
```
Color: Teal | Icon: Docker whale

**Arrow left (into K8s):** Label: `kubectl set image / pull`

---

## ZONE 3: DIGITALOCEAN CLOUD (Center — Largest Zone)

Draw a large rounded rectangle:
```
Label: DigitalOcean Frankfurt Region
       Ubuntu 24.04 LTS
       167.172.99.14 | 4GB RAM / 2vCPU
```
Border: Blue dashed | Background: Very light blue

### Sub-zone A: Ansible IaC (Top left of DO zone)

```
┌─────────────────────────────────────┐
│  🔧 Ansible — Infrastructure as Code│
│  Runs from developer laptop via SSH  │
│                                     │
│  playbook1-install.yml              │
│  → Java 21, Docker, Git, curl       │
│                                     │
│  playbook2-services.yml             │
│  → Start Docker, install k8s        │
│                                     │
│  playbook3-healthcheck.yml          │
│  → Verify all services running      │
│                                     │
│  playbook4-full-recovery.yml        │
│  → Full VPS restore in 10 minutes   │
└─────────────────────────────────────┘
```
Color: Yellow/amber dashed border | Icon: Ansible red A

**Arrow:** Ansible → VPS Server
Label: `SSH provisioning`

---

### Sub-zone B: Kubernetes Cluster (Main area of DO zone)

Draw a large blue dashed border box:
```
Label: Kubernetes Cluster
       namespace: ictu-ex
       k8s — Lightweight Kubernetes
```

Inside arrange pods in this layout:

#### ROW 1 — Entry Point

**Pod 1:**
```
┌────────────────────────────────┐
│  Nginx                         │
│  Reverse Proxy                 │
│  Port 30080 (NodePort)         │
│  Routes all external traffic   │
│  SSL termination               │
└────────────────────────────────┘
```
Color: Teal | Icon: Nginx logo

**Arrow down to Row 2**

---

#### ROW 2 — API Gateway Layer

**Pod 2:**
```
┌────────────────────────────────────────┐
│  API Gateway Filter Chain              │
│  (Spring Servlet Filter Layer)         │
│                                        │
│  ┌─────────────────────────────────┐  │
│  │ RateLimiterFilter               │  │
│  │ 60 requests/min per IP          │  │
│  │ Returns 429 Too Many Requests   │  │
│  └─────────────────────────────────┘  │
│              ↓                         │
│  ┌─────────────────────────────────┐  │
│  │ JwtGatewayFilter                │  │
│  │ Validates Bearer token          │  │
│  │ Checks Redis blacklist          │  │
│  │ Public: /api/auth/**            │  │
│  │ Protected: everything else      │  │
│  └─────────────────────────────────┘  │
│              ↓                         │
│  ┌─────────────────────────────────┐  │
│  │ RequestLoggingFilter            │  │
│  │ Logs all incoming requests      │  │
│  └─────────────────────────────────┘  │
└────────────────────────────────────────┘
```
Color: Purple border | Icon: Shield/filter icon

---

#### ROW 3 — Application Layer

**Pod 3 (replicas: 2 — draw two overlapping boxes):**
```
┌──────────────────────────────────────────────────┐
│  Spring Boot Application Pods (x2 replicas)      │
│  Kotlin + Spring Boot 3.4.4                      │
│  Spring Modulith — enforced module boundaries    │
│  Port 8080                                       │
│                                                  │
│  Modules:                                        │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │  auth    │ │ listing  │ │   messaging      │ │
│  │ register │ │  CRUD    │ │   buyer-seller   │ │
│  │ login    │ │ search   │ │   chat           │ │
│  │ logout   │ │ Kafka ↗  │ │   Kafka ↗        │ │
│  └──────────┘ └──────────┘ └──────────────────┘ │
│  ┌──────────┐ ┌──────────┐                       │
│  │notification│ │  shared  │                     │
│  │ consumer │ │  events  │                       │
│  │ email    │ │  kafka   │                       │
│  │          │ │  redis   │                       │
│  └──────────┘ └──────────┘                       │
│                                                  │
│  readinessProbe: /actuator/health                │
│  livenessProbe:  /actuator/health                │
└──────────────────────────────────────────────────┘
```
Color: Purple | Icon: Spring leaf logo

---

#### ROW 4 — Event Bus & Cache

**Pod 4 — Kafka:**
```
┌────────────────────────────────────┐
│  Apache Kafka 3.7                  │
│  KRaft Mode (no Zookeeper)         │
│  Port 9092                         │
│                                    │
│  Topics:                           │
│  • user.registered                 │
│  • product.posted                  │
│  • message.sent                    │
│                                    │
│  consumers:                        │
│  notification-service group        │
└────────────────────────────────────┘
```
Color: Red/coral | Icon: Kafka logo

**Pod 5 — Redis:**
```
┌────────────────────────────────────┐
│  Redis 7.2                         │
│  Port 6379                         │
│  Max memory: 128MB                 │
│  Policy: allkeys-lru               │
│                                    │
│  Use cases:                        │
│  • JWT token blacklisting          │
│  • Listing search cache (5min TTL) │
│  • Rate limiting counters          │
└────────────────────────────────────┘
```
Color: Orange-red | Icon: Redis logo

---

#### ROW 5 — Monitoring

**Pod 6 — Prometheus:**
```
┌────────────────────────────────────┐
│  Prometheus                        │
│  Port 30090 (NodePort)             │
│  Scrape interval: 15s              │
│  Target: /actuator/prometheus      │
│                                    │
│  Metrics:                          │
│  • JVM heap/non-heap               │
│  • HTTP request count              │
│  • CPU usage                       │
│  • Kafka consumer lag              │
└────────────────────────────────────┘
```
Color: Orange | Icon: Prometheus fire logo

**Pod 7 — Grafana:**
```
┌────────────────────────────────────┐
│  Grafana                           │
│  Port 30030 (NodePort)             │
│  Dashboard: ICTU-Ex Monitoring     │
│                                    │
│  Panels:                           │
│  • App Uptime                      │
│  • JVM Heap Memory                 │
│  • CPU Usage                       │
│  • Load Average                    │
│  • Open File Descriptors           │
└────────────────────────────────────┘
```
Color: Orange | Icon: Grafana logo

---

#### ROW 6 — In-Cluster Database (Secondary)

**Pod 8 — MongoDB:**
```
┌────────────────────────────────────┐
│  MongoDB                           │
│  In-cluster (Kubernetes)           │
│  Port 27017                        │
│                                    │
│  Stores:                           │
│  • Activity logs                   │
│  • Audit trails                    │
│  • Search history                  │
│  • Analytics events                │
└────────────────────────────────────┘
```
Color: Green | Icon: MongoDB leaf logo

---

## ZONE 4: EXTERNAL SERVICES (Outside DO box)

Draw these boxes outside and below/right of the main DO zone:

**External DB (Primary):**
```
┌────────────────────────────────────────┐
│  🗄️ DigitalOcean Managed PostgreSQL    │
│  PostgreSQL 17                         │
│  Frankfurt Region (same DC)            │
│  1GB RAM / 1vCPU                       │
│  Private network connection            │
│  SSL required (port 25060)             │
│                                        │
│  High Availability:                    │
│  • Primary instance                    │
│  • Standby replica (auto-failover)     │
│  • Daily automated backups             │
│  • PgBouncer connection pooling        │
│                                        │
│  Database: ictuex                      │
│  Tables: users, listings,              │
│          messages, notifications       │
└────────────────────────────────────────┘
```
Color: Blue | Icon: Database cylinder

**Arrow from Spring Boot → Managed PostgreSQL:**
Label: `JDBC SSL port 25060 / private network`

**Arrow from MongoDB pod → label:**
Label: `In-cluster: activity & audit logs`

**Email Service:**
```
┌────────────────────────────────┐
│  Resend Email API              │
│  Transactional emails          │
│  Welcome email on register     │
│  Notification emails           │
└────────────────────────────────┘
```
Color: Gray | Icon: Email envelope

**Arrow from notification module → Resend:**
Label: `HTTP API / welcome email`

**Image Hosting:**
```
┌────────────────────────────────┐
│  Cloudinary                    │
│  Image hosting & CDN           │
│  Listing photos                │
│  Student profile pictures      │
│  Optimized delivery            │
└────────────────────────────────┘
```
Color: Blue | Icon: Cloud/image icon

**Arrow from listing module → Cloudinary:**
Label: `HTTP multipart upload`

**Firebase:**
```
┌────────────────────────────────┐
│  Firebase                      │
│  Android offline-first sync    │
│  Push notifications (FCM)      │
│  Real-time updates             │
└────────────────────────────────┘
```
Color: Orange/yellow | Icon: Firebase logo

**Arrow from Android App → Firebase:**
Label: `SDK / offline sync`

---

## ALL ARROWS SUMMARY

```
1.  Android App ──────────────────→ Nginx
    "HTTPS REST /api/**"

2.  Developer Laptop ─────────────→ GitHub
    "git push origin main"

3.  GitHub ───────────────────────→ Jenkins
    "webhook POST /github-webhook/"

4.  Jenkins ──────────────────────→ Docker Hub
    "docker push :BUILD_NUMBER"

5.  Docker Hub ───────────────────→ K8s Cluster
    "kubectl pull & deploy"

6.  Nginx ────────────────────────→ API Gateway Filter
    "forward all requests"

7.  API Gateway ──────────────────→ Spring Boot pods
    "validated & rate-limited requests"

8.  Spring Boot ──────────────────→ Kafka
    "publish domain events"

9.  Kafka ────────────────────────→ Spring Boot consumers
    "consume events async"

10. Spring Boot ──────────────────→ Redis
    "cache read/write & blacklist"

11. Spring Boot ──────────────────→ DO Managed PostgreSQL
    "JDBC SSL port 25060"

12. Spring Boot ──────────────────→ MongoDB
    "activity & audit logs"

13. Spring Boot ──────────────────→ Cloudinary
    "image upload HTTP"

14. Notification module ──────────→ Resend API
    "welcome & alert emails"

15. Android App ──────────────────→ Firebase
    "offline sync & push"

16. Prometheus ───────────────────→ Spring Boot
    "scrape /actuator/prometheus every 15s"

17. Grafana ──────────────────────→ Prometheus
    "PromQL metric queries"

18. Ansible ──────────────────────→ VPS
    "SSH provisioning & config"

19. Jenkins ──────────────────────→ K8s via kubectl
    "kubectl apply -f k8s/"
```

---

## DUAL DATABASE STRATEGY (Important for report)

Draw a separate small diagram showing:

```
PRIMARY DATABASE (External — HA):          SECONDARY DATABASE (In-cluster):
┌─────────────────────────────┐           ┌──────────────────────────────┐
│ DO Managed PostgreSQL 17    │           │ MongoDB (Kubernetes pod)      │
│                             │           │                              │
│ • Transactional data        │           │ • Activity logs              │
│ • Users table               │           │ • Audit trails               │
│ • Listings table            │           │ • Search analytics           │
│ • Messages table            │           │ • Event history              │
│                             │           │                              │
│ HA: Primary + Standby       │           │ Flexible schema for          │
│ Auto-failover               │           │ unstructured log data        │
│ Daily backups               │           │                              │
│ Private network             │           │ No SPOF concern here —       │
│                             │           │ logs are non-critical         │
└─────────────────────────────┘           └──────────────────────────────┘
         ↑                                          ↑
  Managed by DigitalOcean              Managed by Kubernetes
  Zero SPOF                           Ephemeral acceptable
```

**Label this:** `Figure: Dual Database Strategy — Separation of Concerns`

---

## KAFKA EVENT FLOW DIAGRAM (Separate)

```
┌─────────────────────────────────────────────────────────────────┐
│                    KAFKA EVENT FLOWS                             │
│                                                                 │
│  [Auth Module]                                                  │
│  POST /api/auth/register ──publishes──→ [user.registered]      │
│                                              ↓ consumed by      │
│                               [Notification Module]             │
│                                              ↓                  │
│                               [Resend API] → Welcome email      │
│                                                                 │
│  ─────────────────────────────────────────────────────────────  │
│                                                                 │
│  [Listing Module]                                               │
│  POST /api/listings ──────publishes──→ [product.posted]        │
│                                              ↓ consumed by      │
│                               [Notification Module]             │
│                                              ↓                  │
│                               Push alert to interested buyers   │
│                                                                 │
│  ─────────────────────────────────────────────────────────────  │
│                                                                 │
│  [Messaging Module]                                             │
│  POST /api/messages ──────publishes──→ [message.sent]          │
│                                              ↓ consumed by      │
│                               [Notification Module]             │
│                                              ↓                  │
│                               Push notification to recipient    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## COLOR SCHEME

```
Purple   (#6C63FF) → App layer, Spring Boot
Teal     (#0d9488) → Nginx gateway
Red      (#dc2626) → Kafka event bus
Amber    (#D97706) → Jenkins CI/CD
Green    (#16a34a) → Prometheus/Grafana monitoring
Blue     (#1d4ed8) → PostgreSQL managed DB
Green    (#15803d) → MongoDB in-cluster
Orange   (#ea580c) → Redis cache
Gray     (#374151) → GitHub, Docker Hub
Yellow   (#ca8a04) → Ansible IaC
Firebase (#F57C00) → Firebase external
```

---

## DIAGRAM TITLES FOR REPORT

```
Figure 1: ICTU-Ex Complete System Architecture
Figure 2: Kubernetes Cluster — Pod Deployment View
Figure 3: CI/CD Pipeline — Jenkins Automated Flow
Figure 4: Event-Driven Architecture — Kafka Event Flows
Figure 5: API Gateway Filter Chain
Figure 6: Dual Database Strategy
Figure 7: Spring Modulith — Module Boundary Diagram
Figure 8: Monitoring Stack — Prometheus & Grafana
```

---

## TOOLS RECOMMENDATION FOR TEAMMATE

1. **draw.io** (diagrams.net) — Main architecture diagram
    - Import AWS/GCP icon pack
    - Use swim lanes for zones
    - Export PNG at 300 DPI

2. **Excalidraw** — Module boundary diagram
    - Hand-drawn style = unique look
    - Not AI-looking at all

3. **Mermaid** — Kafka event flows
    - Code-generated diagrams
    - Clean and professional