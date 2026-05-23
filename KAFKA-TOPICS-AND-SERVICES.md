# Kafka Topics & Services Integration Map

This document outlines all Kafka topics used in the ICTU-Ex backend system, which services publish to them, and which services consume from them.

---

## System Overview

The ICTU-Ex backend uses **event-driven messaging** via Apache Kafka to enable asynchronous communication between services. All services share a single PostgreSQL database and use Redis for caching/session management.

---

## Topics Summary

| Topic | Type | Publisher(s) | Consumer(s) | Purpose |
|-------|------|--------------|------------|---------|
| `user.registered` | User Events | — | `sync-group` | Track new user registrations in sync ledger |
| `user.verified` | User Events | `auth` | `notification-service` | Trigger welcome email when user verifies email |
| `user.verification-code.generated` | User Events | `auth` | `notification-service` | Send verification code email to new users |
| `product.posted` | Listing Events | `listing` | `sync-group` | Track new product listings in sync ledger |
| `message.sent` | Messaging Events | `messaging` | `notification-group`, `sync-group` | Trigger notification emails & track message sync |
| `image.upload.requested` | Image Events | `listing` | `listing-image-upload-group` | Process & upload images to Cloudinary |

---

## Detailed Topic Breakdown

### 1. **user.registered**
- **Topic Name:** `user.registered`
- **Event Class:** `UserRegisteredEvent` (defined in `shared` module)
- **Publishers:** None currently (reserved for future use; currently only `USER_VERIFIED` is published)
- **Consumers:** `sync-group` (`SyncEventConsumer`)
- **Purpose:** Log user registration events for audit/sync tracking
- **Payload Example:**
  ```json
  {
    "userId": "uuid-here",
    "email": "user@ictuniversity.edu.cm",
    "displayName": "User Name"
  }
  ```

---

### 2. **user.verified**
- **Topic Name:** `user.verified`
- **Event Class:** `UserVerifiedEvent`
- **Publisher:** `auth` service (`AuthServiceImpl.verifyCode()`)
- **Consumers:** 
  - `notification-service` (`WelcomeEmailConsumer`) → sends welcome email
  - `sync-group` (`SyncEventConsumer`) → logs to sync ledger
- **Purpose:** Trigger welcome email and track verification events
- **Trigger:** User successfully verifies their email with verification code
- **Payload:**
  ```json
  {
    "userId": "uuid-here",
    "email": "user@ictuniversity.edu.cm",
    "displayName": "User Name"
  }
  ```

---

### 3. **user.verification-code.generated**
- **Topic Name:** `user.verification-code.generated`
- **Event Class:** `VerificationCodeGeneratedEvent`
- **Publisher:** `auth` service (`AuthServiceImpl.register()`, `AuthServiceImpl.resendVerificationCode()`)
- **Consumers:** `notification-service` (`VerificationCodeEmailConsumer`) → sends verification code email
- **Purpose:** Send verification code to user email during registration or resend
- **Triggers:**
  - User registers a new account
  - User requests verification code resend
- **Payload:**
  ```json
  {
    "userId": "uuid-here",
    "email": "user@ictuniversity.edu.cm",
    "displayName": "User Name",
    "code": "123456"
  }
  ```

---

### 4. **product.posted**
- **Topic Name:** `product.posted`
- **Event Class:** `ProductPostedEvent`
- **Publisher:** `listing` service (`ListingServiceImpl.createListing()`)
- **Consumers:** `sync-group` (`SyncEventConsumer`) → logs to sync ledger
- **Purpose:** Track product/listing creation events in sync system
- **Trigger:** Seller creates a new product listing
- **Payload:**
  ```json
  {
    "listingId": "uuid-here",
    "sellerId": "uuid-here",
    "title": "Listing Title",
    "category": "ELECTRONICS"
  }
  ```

---

### 5. **message.sent**
- **Topic Name:** `message.sent`
- **Event Class:** `MessageSentEvent`
- **Publisher:** `messaging` service (`MessagingServiceImpl.sendMessage()`)
- **Consumers:**
  - `notification-group` (`EmailMessageConsumer`) → sends message notification email to receiver
  - `sync-group` (`SyncEventConsumer`) → logs to sync ledger for both sender and receiver
- **Purpose:** Trigger notification emails and track messaging events
- **Trigger:** User sends a message in a conversation
- **Payload:**
  ```json
  {
    "messageId": "uuid-here",
    "conversationId": "uuid-here",
    "senderId": "uuid-here",
    "senderName": "Sender Name",
    "receiverId": "uuid-here",
    "receiverName": "Receiver Name",
    "receiverEmail": "receiver@ictuniversity.edu.cm",
    "content": "Message content here"
  }
  ```

---

### 6. **image.upload.requested**
- **Topic Name:** `image.upload.requested`
- **Event Class:** `ImageUploadEvent`
- **Publisher:** `listing` service (`ListingServiceImpl.createListing()`)
- **Consumers:** `listing-image-upload-group` (`ImageUploadConsumer`) → uploads image to Cloudinary
- **Purpose:** Asynchronously process image uploads to cloud storage
- **Trigger:** Seller creates a listing with images
- **Payload:**
  ```json
  {
    "listingId": "uuid-here",
    "originalImageUrl": "https://...",
    "imageIndex": 0
  }
  ```

---

## Service Communication Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                      ICTU-Ex Backend System                     │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────┐        ┌──────────────┐      ┌─────────────┐  │
│  │    AUTH     │───────▶│   KAFKA      │◀─────│  LISTING    │  │
│  │  Service    │        │  Message Bus │      │  Service    │  │
│  └─────────────┘        └──────────────┘      └─────────────┘  │
│       │                       ▲                      │           │
│       │                       │                      │           │
│       └───┬──────────────────┬┴──────────────────────┤           │
│           │                  │                       │           │
│      user.verified ┬    image.upload   message.sent │           │
│  verification-code│    .requested      product.posted│           │
│           ▼       ▼                                  ▼           │
│      ┌──────────────────────────────────────────────────┐       │
│      │         NOTIFICATION Service                     │       │
│      │ (sends emails via @KafkaListener consumers)     │       │
│      └──────────────────────────────────────────────────┘       │
│                                                                  │
│      ┌──────────────────────────────────────────────────┐       │
│      │          SYNC Service                            │       │
│      │ (logs all events to sync ledger for replication)│       │
│      └──────────────────────────────────────────────────┘       │
│                                                                  │
│      ┌──────────────────────────────────────────────────┐       │
│      │    MESSAGING Service                             │       │
│      │ (handles conversations & messages)               │       │
│      └──────────────────────────────────────────────────┘       │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  PostgreSQL Database (Shared)                           │   │
│  │  ├─ Users (auth module)                                │   │
│  │  ├─ Listings (listing module)                          │   │
│  │  ├─ Messages & Conversations (messaging module)        │   │
│  │  └─ Sync Records (sync module)                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Redis (Caching & Session Store)                       │   │
│  │  ├─ Listing cache (TTL-based)                          │   │
│  │  └─ Token blacklist (JWT logout)                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
└────────────────────────────────────────────────────────────────┘
```

---

## Consumer Groups

Consumer groups enable horizontal scaling and ensure message ordering per partition:

| Group Name | Service | Topics | Instances |
|------------|---------|--------|-----------|
| `sync-group` | `sync` | `user.registered`, `user.verified`, `product.posted`, `message.sent` | Typically 1 |
| `notification-service` | `notification` | `user.verified`, `user.verification-code.generated` | Typically 1 |
| `notification-group` | `notification` | `message.sent` | Typically 1 |
| `listing-image-upload-group` | `listing` | `image.upload.requested` | Typically 1 |

---

## Event Flow Examples

### User Registration & Verification Flow
```
1. User calls POST /auth/register
   ↓
2. Auth service saves user to DB
   ↓
3. Auth publishes VERIFICATION_CODE_GENERATED event
   ↓
4. Notification service consumes → sends verification code email
   ↓
5. User clicks link with code, calls PATCH /auth/verify
   ↓
6. Auth service saves verified status
   ↓
7. Auth publishes USER_VERIFIED event
   ↓
8. Notification service consumes → sends welcome email
9. Sync service consumes → logs to sync ledger
```

### Product Listing Creation Flow
```
1. Seller calls POST /listings with images
   ↓
2. Listing service saves to DB
   ↓
3. Listing publishes PRODUCT_POSTED event
   ↓
4. Sync service consumes → logs to sync ledger
   ↓
5. For each image:
   Listing publishes IMAGE_UPLOAD_REQUESTED event
   ↓
   Listing service (ImageUploadConsumer) consumes → uploads to Cloudinary
```

### Message Sending Flow
```
1. User calls POST /messaging/{conversationId}/send
   ↓
2. Messaging service saves message to DB
   ↓
3. Messaging publishes MESSAGE_SENT event
   ↓
4. Notification service consumes → sends email to receiver
5. Sync service consumes → logs events for both users
```

---

## Architecture Classification

**This system is NOT a traditional microservices architecture** because:
- ❌ Services do **NOT** have independent databases (shared PostgreSQL)
- ❌ There is **NO** data isolation per service
- ❌ Database schema is centralized, not service-owned

**This system IS:**
- ✅ **Service-Oriented Architecture (SOA)** – multiple logical service components
- ✅ **Event-Driven** – services communicate asynchronously via Kafka
- ✅ **Client-Server** – REST APIs for synchronous client requests
- ✅ **Modular Monolith** – separate Gradle modules deployed as a single application or loosely coupled services sharing one DB

---

## Configuration Files

- **Kafka Config:** `shared/src/main/kotlin/com/fanyiadrien/shared/kafka/`
  - `KafkaTopics.kt` – Topic constants
  - `EventPublisher.kt` – Publishing utility
  - `KafkaTemplateConfig.kt` – Producer/Consumer bean setup
  
- **Deployment:**
  - `docker-compose.yml` – Local Kafka, PostgreSQL, Redis
  - `k8s/kafka.yaml` – Kubernetes Kafka deployment

---

## Best Practices & Notes

1. **Idempotent Consumers:** Event consumers should be idempotent (safe to replay events)
2. **Error Handling:** Failed event processing is logged; consider Dead Letter Queue (DLQ) for production
3. **Ordering:** Within a Kafka partition, message ordering is guaranteed
4. **Scalability:** Consumer groups can be scaled by adding more instances; Kafka will rebalance partitions
5. **Monitoring:** Use Kafka Consumer Lag tools to monitor lag between published and consumed events
6. **Single DB:** All services query/update the same PostgreSQL database; consider eventual consistency patterns when scaling further

---

## Future Considerations

- Implement Dead Letter Queue (DLQ) for failed event processing
- Add event versioning/schema registry for schema evolution
- Implement Outbox pattern for transactional event publishing
- Consider splitting database per service for true microservices architecture
- Add event replay/event sourcing capabilities if audit requirements grow


