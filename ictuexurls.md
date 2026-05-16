# ICTU-Ex — Service URLs & Application Flow
**Project:** ICTU-Ex Smart Student Marketplace  
**Institution:** ICT University Cameroon  
**Team:** Fanyi Charllson & Adrien Tello

---

## 🌐 Service URLs

| Service | Domain (Primary) | IP Fallback | Status |
|---|---|---|---|
| **API** | https://api.ictuex.teamnest.me | http://167.172.99.14:30080 | ✅ Live |
| **Swagger UI** | https://api.ictuex.teamnest.me/swagger-ui.html | http://167.172.99.14:30080/swagger-ui.html | ✅ Live |
| **Jenkins** | https://jenkins.ictuex.teamnest.me | http://167.172.99.14:8080 | ✅ Live |
| **Grafana** | https://grafana.ictuex.teamnest.me | http://167.172.99.14:30030 | ✅ Live |
| **Prometheus** | https://prometheus.ictuex.teamnest.me | http://167.172.99.14:30090 | ✅ Live |
| **GitHub Repo** | https://github.com/fanyicharllson/ictu-ex-backend | — | ✅ Public |

### Credentials
| Service | Username | Password |
|---|---|---|
| Grafana | admin | (set on first login) |
| Jenkins | admin | (set during setup) |
| PostgreSQL | doadmin | (DigitalOcean managed) |

---

## 🔌 API Endpoints

### Authentication — `/api/auth`
| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| POST | `/api/auth/register` | ❌ | Register ICTU student |
| POST | `/api/auth/login` | ❌ | Login with ICTU email |
| POST | `/api/auth/logout` | ✅ Bearer | Logout — blacklists JWT in Redis |
| GET | `/api/auth/validate` | ✅ Bearer | Validate JWT token |

### Listings — `/api/listings`
| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| POST | `/api/listings` | ✅ Bearer | Create new listing |
| GET | `/api/listings` | ❌ | Browse all active listings |
| GET | `/api/listings/{id}` | ❌ | Get single listing |
| PUT | `/api/listings/{id}` | ✅ Bearer | Update listing (seller only) |
| DELETE | `/api/listings/{id}` | ✅ Bearer | Delete listing (seller only) |
| GET | `/api/listings/search` | ❌ | Search by title or category |

### Messaging — `/api/messages`
| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| POST | `/api/messages` | ✅ Bearer | Send message to seller |
| GET | `/api/messages/{userId}` | ✅ Bearer | Get conversation |

### Health & Monitoring
| Method | Endpoint | Description |
|---|---|---|
| GET | `/actuator/health` | App health status |
| GET | `/actuator/prometheus` | Prometheus metrics |

---

## 🔄 Application Flow

### 1. Student Registration Flow
```
Android App
    │
    │  POST /api/auth/register
    │  { email, password, displayName, studentId }
    ▼
Nginx (reverse proxy)
    │
    ▼
RateLimiterFilter → max 60 req/min per IP
    │
    ▼
JwtGatewayFilter → /api/auth/register is PUBLIC, skip JWT check
    │
    ▼
AuthController.register()
    │
    ├─→ Validate ICTU email (@ictuniversity.edu.cm only)
    ├─→ Check duplicate email in PostgreSQL
    ├─→ Check duplicate studentId in PostgreSQL
    ├─→ Hash password (BCrypt)
    ├─→ Save UserEntity to PostgreSQL
    ├─→ Generate JWT token
    │
    ├─→ Publish to Kafka ──────────────────────────────────────┐
    │   Topic: user.registered                                  │
    │   { userId, email, displayName, studentId }               │
    │                                              Notification │
    │                                              Consumer     │
    │                                              picks up     │
    │                                              event        │
    │                                                  │        │
    │                                                  ▼        │
    │                                            Resend API     │
    │                                            Welcome email  │
    │                                            sent to student│
    ▼
    Return { token, user } to Android App
```

---

### 2. Student Login Flow
```
Android App
    │
    │  POST /api/auth/login
    │  { email, password }
    ▼
Nginx → RateLimiter → JwtGatewayFilter (PUBLIC path)
    │
    ▼
AuthController.login()
    │
    ├─→ Find user by email in PostgreSQL
    ├─→ Compare password with BCrypt hash
    ├─→ Generate new JWT token (24hr expiry)
    ▼
Return { token, user } to Android App

Android stores token → uses in all future requests
as: Authorization: Bearer <token>
```

---

### 3. Protected Request Flow (e.g. Create Listing)
```
Android App
    │
    │  POST /api/listings
    │  Authorization: Bearer eyJhbGci...
    │  { title, description, price, category }
    ▼
Nginx
    │
    ▼
RateLimiterFilter
    ├─→ Check request count for this IP
    ├─→ Under 60/min? Continue
    └─→ Over 60/min? Return 429 Too Many Requests
    │
    ▼
JwtGatewayFilter
    ├─→ Extract token from Authorization header
    ├─→ Validate JWT signature and expiry
    ├─→ Check Redis blacklist (is token logged out?)
    ├─→ Token invalid or blacklisted? Return 401
    └─→ Token valid? Continue
    │
    ▼
ListingController.create()
    │
    ├─→ Extract sellerId from JWT
    ├─→ Save ListingEntity to PostgreSQL
    ├─→ Upload images to Cloudinary
    │
    ├─→ Publish to Kafka ──────────────────────────────────────┐
    │   Topic: product.posted                                   │
    │   { listingId, sellerId, title, category }                │
    │                                              Notification │
    │                                              Consumer     │
    │                                              notifies     │
    │                                              interested   │
    │                                              buyers       │
    ▼
Return { listing } to Android App
```

---

### 4. Logout Flow (Redis Blacklisting)
```
Android App
    │
    │  POST /api/auth/logout
    │  Authorization: Bearer eyJhbGci...
    ▼
JwtGatewayFilter → validates token first
    │
    ▼
AuthController.logout()
    │
    ├─→ Extract remaining expiry from JWT
    ├─→ Store token in Redis with TTL = remaining expiry
    │   Key: blacklist:eyJhbGci...
    │   Value: "blacklisted"
    │   TTL: e.g. 23h (if token has 23h left)
    ▼
Return { message: "Logged out successfully" }

Next request with same token:
JwtGatewayFilter → checks Redis → found in blacklist → 401 ❌
Token is dead even before natural expiry ✅
```

---

### 5. CI/CD Deployment Flow
```
Developer pushes code
    │
    │  git push origin main
    ▼
GitHub Repository
    │
    │  webhook POST /github-webhook/
    ▼
Jenkins (https://jenkins.ictuex.teamnest.me)
    │
    ├─→ Stage 1: Checkout — git pull latest code
    ├─→ Stage 2: Build — ./gradlew :ictu-ex-app:bootJar
    ├─→ Stage 3: Test — ./gradlew test (JUnit + Mockito)
    ├─→ Stage 4: Coverage — ./gradlew koverVerify (min 90%)
    │              BELOW 90%? Pipeline BLOCKED ❌
    ├─→ Stage 5: Docker build → push to Docker Hub
    │              fanyicharllson/ictu-ex-backend:BUILD_NUMBER
    └─→ Stage 6: Deploy — kubectl apply -f k8s/
                          kubectl set image deployment/ictu-ex-app
    │
    ▼
Kubernetes Rolling Update
    ├─→ Start new pod with new image
    ├─→ Check readinessProbe: GET /actuator/health
    ├─→ Health OK? Route traffic to new pod
    └─→ Terminate old pod (zero downtime)
    │
    ▼
App live at https://api.ictuex.teamnest.me
```

---

### 6. Monitoring Flow
```
Spring Boot App (every request)
    │
    │  Exposes metrics at /actuator/prometheus
    ▼
Prometheus (scrapes every 15 seconds)
    │
    ├─→ JVM heap usage
    ├─→ HTTP request count & latency
    ├─→ CPU usage
    ├─→ Kafka consumer lag
    └─→ Active DB connections
    │
    ▼
Grafana (https://grafana.ictuex.teamnest.me)
    │
    └─→ ICTU-Ex Application Monitoring Dashboard
        ├─→ App Uptime
        ├─→ JVM Heap Memory gauge
        ├─→ CPU Usage graph
        ├─→ Load Average graph
        └─→ Open File Descriptors
```

---

## 🏗️ Infrastructure Quick Reference

### Server Details
```
Provider:  DigitalOcean
Region:    Frankfurt
OS:        Ubuntu 24.04 LTS
IP:        167.172.99.14
RAM:       4GB
vCPU:      2
Disk:      80GB SSD
```

### Database
```
Provider:  DigitalOcean Managed PostgreSQL
Version:   PostgreSQL 17
Region:    Frankfurt (same DC — low latency)
Plan:      Basic (1GB RAM / 1vCPU)
HA:        Primary + Standby replica
Backups:   Daily automated
SSL:       Required (port 25060)
Database:  ictuex
```

### Kubernetes Pods
```
Namespace: ictu-ex

Pod                  Replicas  Purpose
ictu-ex-app          2         Spring Boot application
nginx                1         Reverse proxy
kafka                1         Event bus (KRaft mode)
redis                1         Cache + JWT blacklist
prometheus           1         Metrics collection
grafana              1         Monitoring dashboard
```

### Ports
```
30080  →  App API (via Nginx)
30090  →  Prometheus
30030  →  Grafana
8080   →  Jenkins
22     →  SSH
80     →  HTTP (redirects to HTTPS)
443    →  HTTPS
```

---

## 🚑 Quick Troubleshooting

```bash
# Check all pods
k3s kubectl get pods -n ictu-ex

# Check app logs
k3s kubectl logs -n ictu-ex -l app=ictu-ex-app -f

# Check specific pod logs
k3s kubectl logs -n ictu-ex <pod-name> --previous

# Restart app pods
k3s kubectl rollout restart deployment/ictu-ex-app -n ictu-ex

# Check server RAM
free -h

# Run health check
cd ~/ansible && ansible-playbook playbook3-healthcheck.yml

# Full VPS recovery (fresh server)
ansible-playbook playbook4-full-recovery.yml
```