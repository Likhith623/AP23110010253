# Backend Microservice

A Spring Boot application that includes a logging middleware, a vehicle maintenance scheduler, and a notification system.

---

## What's inside

**1. Logging Middleware**  
A reusable logger that sends every log entry to an external evaluation server in real time. Every time the app does something meaningful — receiving a request, hitting a repository, returning a response — it fires a log with the right stack, level, and package. The server responds with a `logID` confirming it was accepted.

**2. Vehicle Maintenance Scheduler**  
Uses a 0/1 Knapsack dynamic programming algorithm. It fetches depot and vehicle task data from an external API, then for each depot figures out the best combination of tasks to fit within that depot's time budget. Results are printed as formatted tables in the terminal.

**3. Notification System**  
Pulls live notifications from an external API and merges them with any locally created ones. Supports filtering by type, a priority inbox with a time-decay ranking algorithm, creating new notifications, and marking them as read.

---

## How to run

Make sure you have Java 17+ installed.

```bash
./mvnw spring-boot:run
```

Server starts on `http://localhost:8080`

---

## API endpoints

| Method | URL | What it does |
|--------|-----|--------------|
| GET | `/api/vehicle-maintenance/schedule` | Runs the knapsack scheduler for all depots |
| GET | `/api/notifications` | Gets all notifications (external + local) |
| GET | `/api/notifications/priority` | Top 10 by priority score — add `?topN=5` to change |
| GET | `/api/notifications/type/{type}` | Filter by type (e.g. Placement, Event, Result) |
| POST | `/api/notifications` | Create a new notification |
| PATCH | `/api/notifications/{id}/read` | Mark a notification as read |

For POST, send JSON like this:
```json
{
  "type": "Placement",
  "message": "Your message here"
}
```

---

## Notes

- All logs are sent to an external evaluation server on every request
- Log messages are capped at 48 characters as required by the API spec
- The priority score uses time-decay so older notifications naturally rank lower
- Once a notification is marked as read it won't show up in the priority inbox

---

## Stack

- Java 24
- Spring Boot 3.4.1
- Maven
- Java HttpClient for external API calls
