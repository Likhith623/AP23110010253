# Notification App — Backend

**Roll No:** ap23110010253 | **Name:** Likhith Chowdary Vasireddy

## Structure

```
notification_app_be/
├── handler/     — Request processing (validation + response building)
├── repository/  — Data access (Affordmed Notifications API + in-memory store)
├── route/       — Spring MVC REST endpoint mapping
└── service/     — Business logic (CRUD + Stage 6 Priority Inbox)
```

## REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/notifications` | All notifications |
| GET | `/api/notifications/{id}` | Single notification |
| GET | `/api/notifications/type/{type}` | Filter by Placement/Event/Result |
| GET | `/api/notifications/priority?topN=10` | Priority inbox (Stage 6) |
| POST | `/api/notifications` | Create notification |
| PATCH | `/api/notifications/{id}/read` | Mark as read |

## Credentials

```json
{
  "email": "likhithchowdary_vasireddy@srmap.edu.in",
  "rollNo": "ap23110010253",
  "clientID": "4598edfb-2914-428e-84a7-e879ab34c8ad"
}
```

## Source

Java source: `src/main/java/com/example/demo/notification_app_be/`
