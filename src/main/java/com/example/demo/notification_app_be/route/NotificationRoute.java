package com.example.demo.notification_app_be.route;

import com.example.demo.logging_middleware.AffordmedLogger;
import com.example.demo.notification_app_be.handler.NotificationHandler;
import com.example.demo.notification_app_be.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * NotificationRoute
 *
 * Thin routing layer — maps HTTP endpoints to NotificationHandler.
 * Zero business logic here. Handler does validation; service does logic.
 *
 * Routes:
 *   GET    /api/notifications                    → list all
 *   GET    /api/notifications/priority?topN=10   → priority inbox (Stage 6)
 *   GET    /api/notifications/type/{type}        → filter by type
 *   GET    /api/notifications/{id}               → get by ID
 *   POST   /api/notifications                    → create
 *   PATCH  /api/notifications/{id}/read          → mark as read
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationRoute {

    private final NotificationHandler handler;

    public NotificationRoute(NotificationService service) {
        this.handler = new NotificationHandler(service);
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        AffordmedLogger.Log("backend", "info", "route",
                "GET /api/notifications — request received; rollNo=ap23110010253");
        return handler.handleGetAll();
    }

    // NOTE: /priority declared before /{id} to avoid Spring path conflict
    @GetMapping("/priority")
    public ResponseEntity<?> getPriority(
            @RequestParam(defaultValue = "10") int topN) {
        AffordmedLogger.Log("backend", "info", "route",
                "GET /api/notifications/priority — topN=" + topN);
        return handler.handleGetPriority(topN);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<?> getByType(@PathVariable String type) {
        AffordmedLogger.Log("backend", "info", "route",
                "GET /api/notifications/type/" + type);
        return handler.handleGetByType(type);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        AffordmedLogger.Log("backend", "info", "route",
                "GET /api/notifications/" + id);
        return handler.handleGetById(id);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        AffordmedLogger.Log("backend", "info", "route",
                "POST /api/notifications — type=" + body.get("type"));
        return handler.handleCreate(body);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable String id) {
        AffordmedLogger.Log("backend", "info", "route",
                "PATCH /api/notifications/" + id + "/read");
        return handler.handleMarkRead(id);
    }
}
