# notification_system_design.md

> **Campus Notifications Microservice — System Design**
> **Roll No:** ap23110010253 | **Name:** Likhith Chowdary Vasireddy
> **Track:** Backend | **Stack:** Java Spring Boot

---

## Stage 1

### REST API Design — Campus Notification Platform

#### Core Actions Identified

| Action | Description |
|---|---|
| Fetch all notifications | Load all notifications for logged-in student |
| Fetch by type | Filter by Placement / Event / Result |
| Fetch single notification | Get details of a specific notification by ID |
| Mark as read | Student acknowledges a notification |
| Mark all as read | Bulk acknowledge |
| Create notification | Admin/system publishes a new notification |
| Delete notification | Admin removes a notification |
| Priority Inbox | Top-N most important unread notifications |

---

#### REST API Endpoints

##### 1. Get All Notifications
```
GET /api/notifications
```
**Headers:**
```json
{
  "Authorization": "Bearer <jwt-token>",
  "Content-Type": "application/json"
}
```
**Response (200 OK):**
```json
{
  "status": "success",
  "count": 10,
  "notifications": [
    {
      "id": "d146095a-0d86-4a34-9e69-3900a14576bc",
      "type": "Result",
      "message": "mid-sem",
      "timestamp": "2026-04-22T17:51:30",
      "isRead": false
    }
  ]
}
```

---

##### 2. Get Notification by ID
```
GET /api/notifications/{id}
```
**Response (200 OK):**
```json
{
  "status": "success",
  "notification": {
    "id": "d146095a-0d86-4a34-9e69-3900a14576bc",
    "type": "Result",
    "message": "mid-sem",
    "timestamp": "2026-04-22T17:51:30",
    "isRead": false
  }
}
```
**Response (404):**
```json
{ "status": "error", "message": "Notification not found" }
```

---

##### 3. Get Notifications by Type
```
GET /api/notifications/type/{type}
```
`{type}` values: `Placement`, `Event`, `Result`

**Response (200 OK):**
```json
{
  "status": "success",
  "type": "Placement",
  "count": 3,
  "notifications": [...]
}
```

---

##### 4. Create Notification (Admin)
```
POST /api/notifications
```
**Request Body:**
```json
{
  "type": "Placement",
  "message": "TCS is hiring — Apply by May 10th"
}
```
**Response (201 Created):**
```json
{
  "status": "success",
  "notification": {
    "id": "newly-generated-uuid",
    "type": "Placement",
    "message": "TCS is hiring — Apply by May 10th",
    "timestamp": "2026-05-02T11:00:00",
    "isRead": false
  }
}
```

---

##### 5. Mark Notification as Read
```
PATCH /api/notifications/{id}/read
```
**Response (200 OK):**
```json
{ "status": "success", "message": "Notification marked as read" }
```

---

##### 6. Mark All Notifications as Read
```
PATCH /api/notifications/read-all
```
**Response (200 OK):**
```json
{ "status": "success", "message": "All notifications marked as read" }
```

---

##### 7. Delete Notification (Admin)
```
DELETE /api/notifications/{id}
```
**Response (200 OK):**
```json
{ "status": "success", "message": "Notification deleted" }
```

---

##### 8. Priority Inbox (Stage 6)
```
GET /api/notifications/priority?topN=10
```
**Response (200 OK):**
```json
{
  "status": "success",
  "topN": 10,
  "priorityInbox": [
    {
      "id": "...",
      "type": "Placement",
      "message": "CSX Corporation hiring",
      "timestamp": "2026-04-22T17:51:18",
      "isRead": false,
      "priority": 2.934
    }
  ]
}
```

---

#### Real-Time Notification Mechanism

**Chosen Approach: WebSocket + STOMP (Spring WebSocket)**

```
Student Browser ──WebSocket──► Spring WebSocket Broker
                                        │
                              /topic/notifications
                                        │
                        All subscribed students receive push
```

- Students connect on login: `ws://campus.edu/ws/notifications`
- Server subscribes each student to `/topic/notifications/{studentId}`
- On "Notify All": server broadcasts to `/topic/notifications/broadcast`
- **Why WebSocket over SSE?** Bidirectional; student can also ACK reads over the same connection
- **Fallback:** Long-polling for clients that block WebSocket (firewalls)

**WebSocket Notification Payload:**
```json
{
  "event": "NEW_NOTIFICATION",
  "data": {
    "id": "uuid",
    "type": "Placement",
    "message": "Infosys is hiring",
    "timestamp": "2026-05-02T11:00:00"
  }
}
```

---

## Stage 2

### Database Design

#### Recommended DB: PostgreSQL

**Reasons:**
1. **ACID guarantees** — notifications must be reliably stored; no partial writes
2. **Enum support** — `notification_type` maps directly to `Placement`, `Result`, `Event`
3. **JSON support** — metadata column for extensible fields without schema changes
4. **Indexing** — composite indexes on `(studentID, isRead, createdAt)` critical for performance
5. **Scalability** — table partitioning by date range handles millions of rows efficiently

---

#### DB Schema (PostgreSQL)

```sql
-- Notification type enum
CREATE TYPE notification_type AS ENUM ('Placement', 'Event', 'Result');

-- Students table (pre-existing)
CREATE TABLE students (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255)        NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    rollNo      VARCHAR(50)  UNIQUE NOT NULL,
    createdAt   TIMESTAMP DEFAULT NOW()
);

-- Notifications master table
CREATE TABLE notifications (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notificationType notification_type NOT NULL,
    message          TEXT             NOT NULL,
    createdAt        TIMESTAMP DEFAULT NOW(),
    createdBy        VARCHAR(100)
);

-- Per-student notification state (read/unread)
CREATE TABLE student_notifications (
    id             SERIAL PRIMARY KEY,
    studentID      INT  NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    notificationID UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    isRead         BOOLEAN   DEFAULT FALSE,
    readAt         TIMESTAMP,
    deliveredAt    TIMESTAMP DEFAULT NOW(),
    UNIQUE(studentID, notificationID)
);
```

---

#### Scalability Problems

| Problem | Root Cause | Solution |
|---|---|---|
| Slow unread query | Full table scan on 5M rows | Composite index on `(studentID, isRead, createdAt DESC)` |
| `student_notifications` explosion | Fan-out 50K × 5K = 250M rows | Partition by `deliveredAt` monthly |
| Cold reads on every page load | No caching | Redis cache for unread count + top-10 |
| Bulk INSERT for 50K students | Synchronous loop | Kafka/RabbitMQ + async workers |

---

#### SQL Queries

**Fetch all unread for a student:**
```sql
SELECT n.id, n.notificationType, n.message, n.createdAt
FROM notifications n
JOIN student_notifications sn ON sn.notificationID = n.id
WHERE sn.studentID = 42 AND sn.isRead = FALSE
ORDER BY n.createdAt DESC;
```

**Mark as read:**
```sql
UPDATE student_notifications
SET isRead = TRUE, readAt = NOW()
WHERE studentID = 42 AND notificationID = 'uuid';
```

**Fan-out to all students:**
```sql
INSERT INTO student_notifications (studentID, notificationID)
SELECT s.id, 'notification-uuid' FROM students s;
```

---

## Stage 3

### Query Analysis

#### Original Query (from question)
```sql
SELECT * FROM notifications
WHERE studentID = 1042 AND isRead = false
ORDER BY createdAt DESC;
```

**Is this accurate? No.**
`studentID` and `isRead` don't exist on the `notifications` table — they belong to `student_notifications`. This query would either error or scan the wrong table.

---

**Why is this slow?** (Even if corrected)
1. No composite index on `(studentID, isRead)` → PostgreSQL performs sequential scan over 5M rows
2. `SELECT *` fetches all columns including large TEXT fields unnecessarily
3. `ORDER BY createdAt DESC` requires an additional sort pass with no covering index

---

**Corrected & Optimised Query:**
```sql
SELECT n.id, n.notificationType, n.message, n.createdAt
FROM notifications n
JOIN student_notifications sn ON sn.notificationID = n.id
WHERE sn.studentID = 1042
  AND sn.isRead = FALSE
ORDER BY n.createdAt DESC;
```

**Recommended Index:**
```sql
CREATE INDEX idx_sn_student_unread_date
ON student_notifications (studentID, isRead, deliveredAt DESC);
```

**Cost:** O(N) scan → O(log N + K) B-tree lookup. Dramatically faster at 5M rows.

---

**Should we index every column?**

**No — harmful advice.** Reasons:
- Every index adds overhead on every INSERT / UPDATE / DELETE
- Low-cardinality columns (e.g. `isRead` boolean) alone are poor index candidates
- The query planner may ignore redundant indexes
- Too many indexes confuse cost estimation
- Composite indexes on high-cardinality `WHERE` columns are optimal

---

**Students with Placement notification in last 7 days:**
```sql
SELECT DISTINCT s.id, s.name, s.email
FROM students s
JOIN student_notifications sn ON sn.studentID = s.id
JOIN notifications n ON n.id = sn.notificationID
WHERE n.notificationType = 'Placement'
  AND n.createdAt >= NOW() - INTERVAL '7 days';
```

---

## Stage 4

### Caching Strategy

**Problem:** DB overwhelmed by notifications fetched on every page load for every student.

---

#### Strategy 1: Redis Cache (Read-Through)

Cache the unread notification list per student in Redis with TTL.

```
Request → Redis HIT → return cached list
        → Redis MISS → query DB → store in Redis → return
```

**Cache key:** `notifications:student:{studentID}:unread` | **TTL:** 60s

| Pro | Con |
|---|---|
| Eliminates repeated DB hits | Cache invalidation complexity |
| Sub-millisecond latency | Stale data for up to TTL seconds |
| Horizontally scalable | Additional infra (Redis cluster) |

---

#### Strategy 2: Unread Count Cache

Cache only the **count** (integer) in Redis. Fetch the full list only when the panel opens.

| Pro | Con |
|---|---|
| Very low memory use | Full list still requires DB on open |
| Near-zero page load latency | Only solves page-load count badge |

---

#### Strategy 3: Pagination + Lazy Loading

```
GET /api/notifications?page=0&size=20
```

Only fetch the first 20; load more on scroll.

| Pro | Con |
|---|---|
| Massively reduced payload | UI must handle infinite scroll |
| DB queries hit only LIMIT rows | COUNT query still needed for total |

---

#### Strategy 4: Event-Driven Invalidation

On new notification creation:
1. Insert to DB
2. Publish `notification.created` to Kafka/Redis Pub-Sub
3. All WebSocket-connected students receive push
4. Redis cache for affected students is invalidated

**Recommended Combined Strategy:**
1. Redis for unread count (TTL=30s)
2. Pagination (size=20)
3. WebSocket push for real-time delivery
4. Event-driven cache invalidation

---

## Stage 5

### Redesigning "Notify All"

#### Problems with Original Pseudocode
```python
function notify_all(student_ids: array, message: string):
    for student_id in student_ids:
        send_email(student_id, message)   # synchronous
        save_to_db(student_id, message)   # synchronous
        push_to_app(student_id, message)  # synchronous
```

**Issues:**
1. Synchronous loop — 50,000 iterations blocks the thread for minutes
2. No error handling — one failure stops the entire loop
3. Atomicity mismatch — email and DB not linked; partial success undetected
4. No retry — 200 failed emails silently dropped
5. No idempotency — if job crashes and restarts, duplicates are sent
6. Rate limiting — 50K sequential API calls will be throttled

---

#### What to Do When 200 Emails Fail Mid-Way

1. All failed records go to a Dead Letter Queue (DLQ)
2. Retry job reprocesses only the 200 failures (exponential backoff)
3. Use idempotency keys — do NOT re-run the full `notify_all`
4. Alert ops team via monitoring system

---

#### Should DB save and email send happen atomically?

**No.** Database write should happen first (source of truth). Email is a side-effect handled asynchronously. If email fails, the notification is still persisted — retry only the email step.

---

#### Redesigned Pseudocode

```python
function notify_all(student_ids: array, message: string):
    # Step 1: Single master notification record
    notification_id = db.insert_notification(message, type="Placement")
    log("info", "service", f"Master notification created: {notification_id}")

    # Step 2: Bulk fan-out to student_notifications (one transaction)
    db.bulk_insert_student_notifications(student_ids, notification_id)
    log("info", "service", f"DB fan-out complete for {len(student_ids)} students")

    # Step 3: Enqueue email jobs in batches (async, retriable)
    for batch in chunk(student_ids, size=500):
        queue.publish("email.send", {
            "student_ids":     batch,
            "notification_id": notification_id,
            "message":         message,
            "idempotency_key": f"{notification_id}:{hash(batch)}"
        })
    log("info", "service", "Email jobs enqueued in batches of 500")

    # Step 4: WebSocket broadcast (non-blocking, best-effort)
    websocket_broker.broadcast("/topic/notifications/all", {
        "event":           "NEW_NOTIFICATION",
        "notification_id": notification_id,
        "message":         message
    })
    log("info", "service", "WebSocket broadcast sent to all connected students")


# Async worker (runs on N parallel consumers)
function email_consumer(job):
    try:
        for student_id in job.student_ids:
            send_email(student_id, job.message)
    except EmailException as e:
        log("error", "service", f"Email batch failed — {e}")
        dead_letter_queue.push(job)   # retry with backoff
```

#### Reliability Summary

| Concern | Old | New |
|---|---|---|
| Speed | Sync loop — minutes | Async parallel workers — seconds |
| Failures | Silent drop | DLQ + retry |
| DB + Email atomicity | Tightly coupled | Decoupled — DB first, email async |
| Duplicates | None prevented | Idempotency keys per batch |
| Crash recovery | Reruns entirely | Queue checkpoints |

---

## Stage 6

### Priority Inbox Implementation

#### Approach

Composite priority score balancing type importance and recency:

```
priority = typeWeight × e^(−λ × ageInHours)

typeWeight : Placement=3, Result=2, Event=1
λ = 0.05   : gradual recency decay
```

**Examples:**
| Notification | Age | Priority |
|---|---|---|
| New Placement | 0 h | 3 × 1.000 = **3.000** |
| New Event | 0 h | 1 × 1.000 = **1.000** |
| 24h Placement | 24 h | 3 × e⁻¹·² ≈ **0.903** |
| 24h Result | 24 h | 2 × e⁻¹·² ≈ **0.602** |

---

#### Java Implementation

```java
// In NotificationService.java
private double computePriority(Notification n) {
    double typeWeight = switch (n.getType()) {
        case "Placement" -> 3.0;
        case "Result"    -> 2.0;
        case "Event"     -> 1.0;
        default          -> 1.0;
    };
    double ageHours = Duration.between(n.getTimestamp(), LocalDateTime.now())
                               .toMinutes() / 60.0;
    return typeWeight * Math.exp(-0.05 * ageHours);
}

public List<Notification> getTopPriorityNotifications(int topN) {
    return repository.findAll().stream()
        .filter(n -> !n.isRead())
        .peek(n -> n.setPriority(computePriority(n)))
        .sorted(Comparator.comparingDouble(Notification::getPriority).reversed())
        .limit(topN)
        .toList();
}
```

**REST Endpoint:**
```
GET /api/notifications/priority?topN=10
```

---

#### Maintaining Top-10 Efficiently as New Notifications Arrive

Use a **Min-Heap of size K** — O(N log K) instead of O(N log N):

```python
import heapq

def maintain_top_k(stream_of_notifications, k=10):
    min_heap = []   # min-heap of (priority, notification)
    for notif in stream_of_notifications:
        p = compute_priority(notif)
        if len(min_heap) < k:
            heapq.heappush(min_heap, (p, notif))
        elif p > min_heap[0][0]:          # beats the current minimum
            heapq.heapreplace(min_heap, (p, notif))
    return [n for _, n in sorted(min_heap, reverse=True)]
```

- **Time:** O(N log K) — far better than O(N log N) full sort
- **Space:** O(K) — only K items kept in memory
- **On new notification:** single heap comparison O(log K) — near-instant

---

**Sample Priority Inbox Response:**
```json
{
  "status": "success",
  "topN": 10,
  "priorityInbox": [
    { "id": "b283218f...", "type": "Placement", "message": "CSX Corporation hiring",     "priority": 2.934 },
    { "id": "8a7412bd...", "type": "Placement", "message": "AMD Inc. hiring",            "priority": 2.931 },
    { "id": "d146095a...", "type": "Result",    "message": "mid-sem",                   "priority": 1.956 },
    { "id": "1cfce5ee...", "type": "Event",     "message": "tech-fest",                 "priority": 0.977 }
  ]
}
```
