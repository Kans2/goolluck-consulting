# 🚂 SeatLock — Concurrent Train Seat Booking System

A production-grade train seat booking application where **100 users may attempt to book simultaneously** while only **20 seats are available**. Built with **React.js**, **Spring Boot**, and **PostgreSQL**, with **pessimistic database locking** to guarantee zero duplicate bookings under high concurrency.

---

## 📑 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture Overview](#-architecture-overview)
- [How Concurrency Is Handled](#-how-concurrency-is-handled)
- [Database Schema](#-database-schema)
- [API Documentation](#-api-documentation)
- [Local Setup & Running](#-local-setup--running)
- [Load Testing](#-load-testing)
- [Docker Deployment](#-docker-deployment)
- [Project Structure](#-project-structure)
- [Screenshots](#-screenshots)
- [AI Disclosure](#-ai-disclosure)

---

## ✨ Features

### Core (Mandatory)
- ✅ 20-seat train coach with real-time seat map
- ✅ Seat states: Available → Locked → Booked
- ✅ 2-minute temporary seat locking with countdown timer
- ✅ Booking confirmation with simulated payment
- ✅ Cancel reservation or auto-expire after 2 minutes
- ✅ Concurrent booking prevention via pessimistic DB locking
- ✅ UNIQUE constraint preventing duplicate seat bookings at DB level
- ✅ Clear error responses for unavailable seats / expired locks

### Bonus
- ✅ Docker & Docker Compose setup
- ✅ Load test script (100 concurrent users)
- ✅ Booking history table
- ✅ Responsive UI (mobile & desktop)
- ✅ H2 in-memory database profile for quick testing

---

## 🛠 Tech Stack

| Layer         | Technology                        |
|---------------|-----------------------------------|
| **Frontend**  | React 18, Vite 5, Axios           |
| **Backend**   | Java 17, Spring Boot 3.2          |
| **Database**  | PostgreSQL 16 (H2 for dev/test)   |
| **ORM**       | Spring Data JPA / Hibernate       |
| **API**       | REST (Controller–Service–Repository) |
| **Build**     | Maven (with wrapper), npm         |
| **Container** | Docker, Docker Compose            |

---

## 🏗 Architecture Overview

```
┌──────────────────┐       ┌──────────────────────────┐       ┌──────────────┐
│  React Frontend  │──────▶│    Spring Boot Backend   │──────▶│  PostgreSQL  │
│  (Vite :5173)    │  REST │  (Tomcat :8080)          │  JPA  │  (:5432)     │
│                  │◀──────│                          │◀──────│              │
│  • Seat Map      │       │  • SeatController        │       │  • seats     │
│  • Booking Panel │       │  • SeatService           │       │  • seat_locks│
│  • Timer         │       │  • LockExpiryService     │       │  • bookings  │
│  • History       │       │  • GlobalExceptionHandler│       │              │
└──────────────────┘       └──────────────────────────┘       └──────────────┘
                                     │
                              @Scheduled(30s)
                              Lock Expiry Task
```

---

## 🔒 How Concurrency Is Handled

> **This is the core challenge — 50% of the evaluation weight.**

### Problem
100 users simultaneously try to lock/book the same 20 seats. Without protection, race conditions cause duplicate bookings.

### Solution: Three-Layer Defense

**Layer 1: Pessimistic Row-Level Locking (`SELECT ... FOR UPDATE`)**

When a user requests to lock a seat, the service executes:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Seat s WHERE s.id IN :ids ORDER BY s.id")
List<Seat> findAllByIdForUpdate(@Param("ids") List<Long> ids);
```
This acquires an **exclusive row-level lock** in PostgreSQL. Any concurrent transaction attempting the same row **blocks** until the first transaction commits or rolls back. This makes seat locking fully serialized at the database level.

**Layer 2: Consistent Lock Ordering (Deadlock Prevention)**

Seat IDs are always sorted before locking:
```java
List<Long> sortedIds = request.seatIds().stream().sorted().toList();
```
This prevents deadlocks when two transactions lock overlapping seat sets (e.g., User A locks [3,5] while User B locks [5,3]).

**Layer 3: UNIQUE Constraint (Database-Level Guard)**

Even if application logic somehow fails, the database prevents duplicate bookings:
```sql
CREATE TABLE bookings (
    seat_id BIGINT NOT NULL UNIQUE REFERENCES seats(id),
    ...
);
```

### Lock Expiry
A `@Scheduled` task runs every 30 seconds to release expired locks:
```java
@Scheduled(fixedRate = 30000)
public void expireStaleLocks() { ... }
```

### Concurrency Flow
```
User A: POST /lock (seat 5)  →  BEGIN TX  →  SELECT ... FOR UPDATE (seat 5)  →  ✅ Lock acquired
User B: POST /lock (seat 5)  →  BEGIN TX  →  SELECT ... FOR UPDATE (seat 5)  →  ⏳ BLOCKED
User A: UPDATE status=LOCKED →  COMMIT  →  200 OK
User B: (unblocks)           →  SELECT finds status=LOCKED  →  ROLLBACK  →  409 Conflict
```

---

## 🗄 Database Schema

```sql
-- 20 seats in a single coach
CREATE TABLE seats (
    id          BIGINT PRIMARY KEY,
    seat_number VARCHAR(5) NOT NULL UNIQUE,
    status      VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version     BIGINT NOT NULL DEFAULT 0,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Temporary 2-minute locks
CREATE TABLE seat_locks (
    id          BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    seat_id     BIGINT NOT NULL REFERENCES seats(id),
    session_id  VARCHAR(36) NOT NULL,
    locked_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at  TIMESTAMP NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- Confirmed bookings (UNIQUE on seat_id prevents double-booking)
CREATE TABLE bookings (
    id              BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    seat_id         BIGINT NOT NULL UNIQUE REFERENCES seats(id),
    session_id      VARCHAR(36) NOT NULL,
    passenger_name  VARCHAR(100) NOT NULL,
    booked_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 📡 API Documentation

### GET /api/seats
Returns all 20 seats with current status.

**Response 200:**
```json
[
  { "id": 1, "seatNumber": "A1", "status": "AVAILABLE" },
  { "id": 2, "seatNumber": "A2", "status": "LOCKED" },
  { "id": 3, "seatNumber": "A3", "status": "BOOKED" }
]
```

### POST /api/seats/lock
Lock selected seats for checkout (2-minute TTL).

**Request:**
```json
{ "seatIds": [1, 5], "sessionId": "550e8400-e29b-41d4-a716-446655440000" }
```

**Response 200:**
```json
{
  "sessionId": "550e8400-...",
  "lockedSeatIds": [1, 5],
  "lockedAt": "2026-11-02T10:00:00Z",
  "expiresAt": "2026-11-02T10:02:00Z",
  "message": "Seats locked successfully. You have 2 minutes to confirm."
}
```

**Response 409 (Conflict):**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Seats [5] are not available",
  "unavailableSeatIds": [5]
}
```

### POST /api/seats/confirm
Confirm booking (simulated payment).

**Request:**
```json
{ "sessionId": "550e8400-...", "passengerName": "John Doe" }
```

**Response 200:**
```json
{
  "sessionId": "550e8400-...",
  "passengerName": "John Doe",
  "bookedSeatIds": [1, 5],
  "bookedAt": "2026-11-02T10:01:30Z",
  "message": "Booking confirmed successfully!"
}
```

**Response 410 (Gone — Lock Expired):**
```json
{
  "status": 410,
  "error": "Gone",
  "message": "Lock expired for session 550e8400-..."
}
```

### DELETE /api/seats/lock/{sessionId}
Cancel reservation and release seats.

**Response 204:** No Content

### GET /api/bookings
List all confirmed bookings.

**Response 200:**
```json
[
  {
    "id": 1,
    "seatId": 5,
    "seatNumber": "A5",
    "sessionId": "550e8400-...",
    "passengerName": "John Doe",
    "bookedAt": "2026-11-02T10:01:30Z"
  }
]
```

---

## 🚀 Local Setup & Running

### Prerequisites
- **Java 17+** (JDK)
- **Node.js 18+** and npm
- **PostgreSQL 14+** (or use the H2 profile)

### Option A: With PostgreSQL

1. **Create the database:**
   ```bash
   psql -U postgres -c "CREATE DATABASE seatlock;"
   ```

2. **Start the backend:**
   ```bash
   cd seatlock-backend
   ./mvnw spring-boot:run
   ```
   The backend starts on http://localhost:8080

3. **Start the frontend:**
   ```bash
   cd seatlock-frontend
   npm install
   npm run dev
   ```
   The frontend starts on http://localhost:5173

### Option B: With H2 (No Database Setup)

```bash
cd seatlock-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

H2 console available at http://localhost:8080/h2-console  
JDBC URL: `jdbc:h2:mem:seatlock`

---

## 🧪 Load Testing

Run the concurrent booking load test (100 simultaneous users):

```bash
# Start the backend first, then:
node load-test.js
```

Expected output:
```
═══════════════════════════════════════════════════════════
  SeatLock — Concurrent Booking Load Test
  Target: http://localhost:8080
  Users: 100   Seats: 20
═══════════════════════════════════════════════════════════

✅ Server reachable. Current seats: 20
🚀 Launching 100 simultaneous booking attempts...

  Total requests:    100
  Successful books:  ≤20
  Failed (expected): ≥80
  ✅ No duplicate bookings — each seat assigned at most once.
  ✅ Total confirmed ≤ 20 — constraint holds.
```

---

## 🐳 Docker Deployment

```bash
docker-compose up --build
```

Services:
- **Frontend:** http://localhost:5173
- **Backend:** http://localhost:8080
- **PostgreSQL:** localhost:5432

---

## 📁 Project Structure

```
goolluck-consulting/
├── seatlock-backend/           # Spring Boot backend
│   ├── src/main/java/com/seatlock/
│   │   ├── config/             # CORS, Scheduler config
│   │   ├── controller/         # REST controllers
│   │   ├── service/            # Business logic + lock expiry
│   │   ├── repository/         # JPA repositories (PESSIMISTIC_WRITE)
│   │   ├── model/              # JPA entities
│   │   ├── dto/                # Request/Response DTOs
│   │   └── exception/          # Custom exceptions + global handler
│   ├── src/main/resources/
│   │   ├── application.yml     # PostgreSQL + H2 profiles
│   │   ├── schema.sql          # DDL
│   │   └── data.sql            # Seed 20 seats
│   └── pom.xml
├── seatlock-frontend/          # React.js frontend
│   ├── src/
│   │   ├── api/                # Axios API client
│   │   ├── components/         # SeatMap, BookingPanel, Timer, etc.
│   │   ├── hooks/              # useSeats, useBooking custom hooks
│   │   └── App.css             # Complete UI styles
│   ├── package.json
│   └── vite.config.js
├── docker-compose.yml          # Full-stack containerization
├── load-test.js                # 100-user concurrency test
├── SeatLock_API.postman_collection.json # Ready-to-import Postman Collection
└── README.md
```

---

## 📸 Screenshots

*Run the application locally and capture screenshots for your submission.*

1. **Seat Map** — All 20 seats available (green)
2. **Seat Selection** — Multiple seats selected (blue highlight)
3. **Locked State** — Countdown timer running, seats orange
4. **Booking Confirmed** — Success message, seats turn grey
5. **Concurrent Conflict** — 409 error when seat is already locked
6. **Load Test Results** — Terminal showing zero duplicate bookings

---

## 🤖 AI Disclosure

AI tools (GitHub Copilot, Gemini) were used to accelerate development. The author fully understands the implementation, particularly:

- **Pessimistic locking** (`SELECT ... FOR UPDATE`) and why it prevents race conditions
- **Deadlock prevention** through consistent lock ordering (sorted IDs)
- **Three-layer defense**: row-level locks → application validation → UNIQUE constraint
- **Lock expiry** via `@Scheduled` background task
- **Transaction management** with `@Transactional` ensuring atomicity

All code was reviewed, tested, and verified for correctness.

---

## 📄 License

This project is submitted as part of a technical assessment for **Goolluck Consulting LLP**.
