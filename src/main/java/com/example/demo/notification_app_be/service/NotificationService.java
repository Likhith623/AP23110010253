package com.example.demo.notification_app_be.service;

import com.example.demo.logging_middleware.AffordmedLogger;
import com.example.demo.notification_app_be.model.Notification;
import com.example.demo.notification_app_be.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * NotificationService
 *
 * Business logic for the campus notification platform.
 *
 * Stage 6 — Priority Inbox:
 *   Priority = typeWeight × e^(−λ × ageInHours)
 *   typeWeight : Placement=3, Result=2, Event=1
 *   λ = 0.05   : gradual recency decay
 *
 * This ensures newer, higher-type notifications surface first while
 * older placements can still outrank brand-new events.
 */
@Service
public class NotificationService {

    private static final double WEIGHT_PLACEMENT = 3.0;
    private static final double WEIGHT_RESULT    = 2.0;
    private static final double WEIGHT_EVENT     = 1.0;
    private static final double DECAY_LAMBDA     = 0.05;

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public List<Notification> getAllNotifications() {
        AffordmedLogger.Log("backend", "info", "service",
                "NotificationService.getAllNotifications — retrieving all");
        List<Notification> all = repository.findAll();
        AffordmedLogger.Log("backend", "debug", "service",
                "getAllNotifications — count=" + all.size());
        return all;
    }

    public Optional<Notification> getNotificationById(String id) {
        AffordmedLogger.Log("backend", "info", "service",
                "NotificationService.getNotificationById — id=" + id);
        Optional<Notification> result = repository.findById(id);
        if (result.isEmpty())
            AffordmedLogger.Log("backend", "warn", "service",
                    "getNotificationById — not found: id=" + id);
        return result;
    }

    public List<Notification> getByType(String type) {
        AffordmedLogger.Log("backend", "info", "service",
                "NotificationService.getByType — type=" + type);
        List<Notification> typed = repository.findByType(type);
        AffordmedLogger.Log("backend", "debug", "service",
                "getByType — found " + typed.size() + " of type=" + type);
        return typed;
    }

    public Notification createNotification(String type, String message) {
        AffordmedLogger.Log("backend", "info", "service",
                "NotificationService.createNotification — type=" + type
                + " message=" + message);
        Notification n = new Notification(
                UUID.randomUUID().toString(), type, message, LocalDateTime.now());
        n.setPriority(computePriority(n));
        Notification saved = repository.save(n);
        AffordmedLogger.Log("backend", "info", "service",
                "createNotification — saved id=" + saved.getId()
                + " priority=" + saved.getPriority());
        return saved;
    }

    public boolean markAsRead(String id) {
        AffordmedLogger.Log("backend", "info", "service",
                "NotificationService.markAsRead — id=" + id);
        boolean updated = repository.markAsRead(id);
        if (!updated)
            AffordmedLogger.Log("backend", "warn", "service",
                    "markAsRead — not in local store: id=" + id);
        return updated;
    }

    // ── Stage 6: Priority Inbox ───────────────────────────────────────────────

    /**
     * Returns top-N priority unread notifications.
     *
     * Priority formula: typeWeight × e^(−0.05 × ageInHours)
     *
     * @param topN number of notifications to return (e.g. 10, 15, 20)
     */
    public List<Notification> getTopPriorityNotifications(int topN) {
        AffordmedLogger.Log("backend", "info", "service",
                "NotificationService.getTopPriorityNotifications — topN=" + topN);

        List<Notification> all = repository.findAll();
        all.forEach(n -> n.setPriority(computePriority(n)));

        List<Notification> top = all.stream()
                .filter(n -> !n.isRead())
                .sorted(Comparator.comparingDouble(Notification::getPriority).reversed())
                .limit(topN)
                .toList();

        AffordmedLogger.Log("backend", "info", "service",
                "getTopPriorityNotifications — returning " + top.size() + " of "
                + all.stream().filter(n -> !n.isRead()).count() + " unread");

        // Print priority inbox to console (for screenshots)
        System.out.printf("%n╔═══════════════════ PRIORITY INBOX — Top %d ═══════════════════╗%n", topN);
        for (int i = 0; i < top.size(); i++) {
            Notification n = top.get(i);
            System.out.printf("║  #%-2d  [%-9s]  Score=%-7.4f  %s%n",
                    i + 1, n.getType(), n.getPriority(), n.getMessage());
        }
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");

        return top;
    }

    // ── Priority Computation ─────────────────────────────────────────────────

    /**
     * Composite priority score.
     *
     * priority = typeWeight × e^(−λ × ageInHours)
     *
     * Examples (λ=0.05):
     *   New Placement (0 h old) : 3 × e^0     = 3.000
     *   24h Placement           : 3 × e^−1.2  ≈ 0.903
     *   New Event (0 h old)     : 1 × e^0     = 1.000
     */
    private double computePriority(Notification n) {
        double typeWeight = switch (n.getType()) {
            case "Placement" -> WEIGHT_PLACEMENT;
            case "Result"    -> WEIGHT_RESULT;
            case "Event"     -> WEIGHT_EVENT;
            default          -> 1.0;
        };

        double ageHours = n.getTimestamp() != null
                ? java.time.Duration.between(n.getTimestamp(), LocalDateTime.now())
                                    .toMinutes() / 60.0
                : 0.0;

        return typeWeight * Math.exp(-DECAY_LAMBDA * ageHours);
    }
}
