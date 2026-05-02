package com.example.demo.notification_app_be.handler;

import com.example.demo.logging_middleware.AffordmedLogger;
import com.example.demo.notification_app_be.model.Notification;
import com.example.demo.notification_app_be.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * NotificationHandler
 *
 * Handles all notification request processing.
 * Intentionally decoupled from Spring MVC routing so the route layer
 * stays thin and this layer can be unit-tested independently.
 *
 * Validates inputs → delegates to service → builds HTTP responses.
 */
public class NotificationHandler {

    private final NotificationService service;

    public NotificationHandler(NotificationService service) {
        this.service = service;
    }

    // ── GET all ──────────────────────────────────────────────────────────────

    public ResponseEntity<?> handleGetAll() {
        AffordmedLogger.Log("backend", "info", "handler",
                "NotificationHandler.handleGetAll — processing");
        List<Notification> list = service.getAllNotifications();
        AffordmedLogger.Log("backend", "debug", "handler",
                "handleGetAll — returning " + list.size() + " notifications");
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "count", list.size(),
                "notifications", list));
    }

    // ── GET by ID ─────────────────────────────────────────────────────────────

    public ResponseEntity<?> handleGetById(String id) {
        AffordmedLogger.Log("backend", "info", "handler",
                "NotificationHandler.handleGetById — id=" + id);
        if (id == null || id.isBlank()) {
            AffordmedLogger.Log("backend", "warn", "handler",
                    "handleGetById — blank id received");
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "ID must not be blank"));
        }
        Optional<Notification> result = service.getNotificationById(id);
        if (result.isEmpty()) {
            AffordmedLogger.Log("backend", "warn", "handler",
                    "handleGetById — not found: id=" + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Notification not found: " + id));
        }
        return ResponseEntity.ok(Map.of("status", "success", "notification", result.get()));
    }

    // ── GET by type ───────────────────────────────────────────────────────────

    public ResponseEntity<?> handleGetByType(String type) {
        AffordmedLogger.Log("backend", "info", "handler",
                "NotificationHandler.handleGetByType — type=" + type);
        if (!List.of("Placement", "Event", "Result").contains(type)) {
            AffordmedLogger.Log("backend", "warn", "handler",
                    "handleGetByType — invalid type: " + type);
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error",
                                 "message", "type must be one of: Placement, Event, Result"));
        }
        List<Notification> typed = service.getByType(type);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "type", type,
                "count", typed.size(),
                "notifications", typed));
    }

    // ── POST create ───────────────────────────────────────────────────────────

    /**
     * Creates a notification.
     * Body: { "type": "Placement|Event|Result", "message": "..." }
     */
    public ResponseEntity<?> handleCreate(Map<String, String> body) {
        AffordmedLogger.Log("backend", "info", "handler",
                "NotificationHandler.handleCreate — type=" + body.get("type"));
        String type    = body.get("type");
        String message = body.get("message");

        if (type == null || type.isBlank()) {
            AffordmedLogger.Log("backend", "warn", "handler",
                    "handleCreate — missing field: type");
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Field 'type' is required"));
        }
        if (message == null || message.isBlank()) {
            AffordmedLogger.Log("backend", "warn", "handler",
                    "handleCreate — missing field: message");
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Field 'message' is required"));
        }
        if (!List.of("Placement", "Event", "Result").contains(type)) {
            AffordmedLogger.Log("backend", "error", "handler",
                    "handleCreate — invalid type: " + type);
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error",
                                 "message", "type must be one of: Placement, Event, Result"));
        }

        Notification created = service.createNotification(type, message);
        AffordmedLogger.Log("backend", "info", "handler",
                "handleCreate — created id=" + created.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("status", "success", "notification", created));
    }

    // ── PATCH mark read ───────────────────────────────────────────────────────

    public ResponseEntity<?> handleMarkRead(String id) {
        AffordmedLogger.Log("backend", "info", "handler",
                "NotificationHandler.handleMarkRead — id=" + id);
        boolean updated = service.markAsRead(id);
        if (!updated) {
            AffordmedLogger.Log("backend", "warn", "handler",
                    "handleMarkRead — not found in local store: id=" + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error",
                                 "message", "Notification not found in local store: " + id));
        }
        AffordmedLogger.Log("backend", "info", "handler",
                "handleMarkRead — success id=" + id);
        return ResponseEntity.ok(Map.of("status", "success",
                "message", "Notification marked as read"));
    }

    // ── GET priority inbox ────────────────────────────────────────────────────

    public ResponseEntity<?> handleGetPriority(int topN) {
        AffordmedLogger.Log("backend", "info", "handler",
                "NotificationHandler.handleGetPriority — topN=" + topN);
        if (topN <= 0) {
            AffordmedLogger.Log("backend", "warn", "handler",
                    "handleGetPriority — invalid topN=" + topN);
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "topN must be positive"));
        }
        List<Notification> top = service.getTopPriorityNotifications(topN);
        AffordmedLogger.Log("backend", "debug", "handler",
                "handleGetPriority — returning " + top.size() + " notifications");
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "topN", topN,
                "priorityInbox", top));
    }
}
