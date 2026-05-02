# Vehicle Maintenance Scheduler

**Roll No:** ap23110010253 | **Name:** Likhith Chowdary Vasireddy

## Overview

0/1 Knapsack DP algorithm to schedule daily vehicle maintenance,
maximising total operational impact within each depot's mechanic-hour budget.

## Algorithm

- **Type:** Dynamic Programming — 0/1 Knapsack
- **Time complexity:** O(N × W) per depot
- **Space complexity:** O(N × W)

## API Endpoints Used

| Endpoint | Purpose |
|---|---|
| `GET http://20.207.122.201/evaluation-service/depots` | Fetch depot budgets |
| `GET http://20.207.122.201/evaluation-service/vehicles` | Fetch vehicle tasks |

## REST Trigger

```
GET /api/vehicle-maintenance/schedule
```

## Source

Java source: `src/main/java/com/example/demo/logging_middleware/vehicle_maintenance_scheduler/`
