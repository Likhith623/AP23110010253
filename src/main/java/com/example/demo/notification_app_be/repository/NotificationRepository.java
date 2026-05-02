package com.example.demo.notification_app_be.repository;

import com.example.demo.logging_middleware.AffordmedLogger;
import com.example.demo.notification_app_be.model.Notification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * NotificationRepository
 *
 * Data-access layer for the campus notification platform.
 *
 * Fetches live notifications from the Affordmed Notification API.
 * An in-memory list holds any locally created notifications.
 *
 * Credentials:
 *   clientID     : 4598edfb-2914-428e-84a7-e879ab34c8ad
 *   clientSecret : TuTcAddeMczaYRNa
 *   Bearer Token : (JWT — see AffordmedLogger)
 *
 * API: GET http://20.207.122.201/evaluation-service/notifications
 */
@Repository
public class NotificationRepository {

    private static final String CLIENT_ID     = "4598edfb-2914-428e-84a7-e879ab34c8ad";
    private static final String CLIENT_SECRET = "TuTcAddeMczaYRNa";
    private static final String BEARER_TOKEN  =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJNYXBDbGFpbXMiOnsiYXVkIjoiaHR0cDovLzIwLjI0NC41Ni4xNDQvZXZhbHVhdGlvbi1zZXJ2aWNlIiwiZW1haWwiOiJsaWtoaXRoY2hvd2RhcnlfdmFzaXJlZGR5QHNybWFwLmVkdS5pbiIsImV4cCI6MTc3NzcwNjEyNSwiaWF0IjoxNzc3NzA1MjI1LCJpc3MiOiJBZmZvcmQgTWVkaWNhbCBUZWNobm9sb2dpZXMgUHJpdmF0ZSBMaW1pdGVkIiwianRpIjoiZTYwNDEyNGEtOTY2Yi00ZTQ1LWI5YzAtZThkYmRlZTMzMTU1IiwibG9jYWxlIjoiZW4tSU4iLCJuYW1lIjoibGlraGl0aCBjaG93ZGFyeSB2YXNpcmVkZHkiLCJzdWIiOiI0NTk4ZWRmYi0yOTE0LTQyOGUtODRhNy1lODc5YWIzNGM4YWQifSwiZW1haWwiOiJsaWtoaXRoY2hvd2RhcnlfdmFzaXJlZGR5QHNybWFwLmVkdS5pbiIsIm5hbWUiOiJsaWtoaXRoIGNob3dkYXJ5IHZhc2lyZWRkeSIsInJvbGxObyI6ImFwMjMxMTAwMTAyNTMiLCJhY2Nlc3NDb2RlIjoiUWticHhIIiwiY2xpZW50SUQiOiI0NTk4ZWRmYi0yOTE0LTQyOGUtODRhNy1lODc5YWIzNGM4YWQiLCJjbGllbnRTZWNyZXQiOiJUdVRjQWRkZU1jemFZUk5hIn0." +
            "QPWuQHUlTRYcbAFLbT4KjqOYVhm8T-nlDGl2UmK77-g";

    private static final String NOTIFICATIONS_API =
            "http://20.207.122.201/evaluation-service/notifications";

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private static final ObjectMapper OBJECT_MAPPER;
    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // In-memory store for locally created notifications
    private final List<Notification> localStore = new ArrayList<>();

    // ── Repository Methods ───────────────────────────────────────────────────

    /** Returns all notifications: API + local store. */
    public List<Notification> findAll() {
        AffordmedLogger.Log("backend", "info", "repository",
                "NotificationRepository.findAll — fetching from API + local store");
        List<Notification> api = fetchFromApi();
        List<Notification> all = new ArrayList<>(api);
        all.addAll(localStore);
        AffordmedLogger.Log("backend", "debug", "repository",
                "findAll — total=" + all.size() + " (" + api.size() + " API + " + localStore.size() + " local)");
        return all;
    }

    /** Finds a notification by ID. */
    public Optional<Notification> findById(String id) {
        AffordmedLogger.Log("backend", "debug", "repository",
                "NotificationRepository.findById — id=" + id);
        return findAll().stream().filter(n -> n.getId().equals(id)).findFirst();
    }

    /** Returns notifications filtered by type. */
    public List<Notification> findByType(String type) {
        AffordmedLogger.Log("backend", "info", "repository",
                "NotificationRepository.findByType — type=" + type);
        return findAll().stream()
                .filter(n -> n.getType().equalsIgnoreCase(type))
                .toList();
    }

    /** Persists a locally created notification. */
    public Notification save(Notification n) {
        AffordmedLogger.Log("backend", "info", "repository",
                "NotificationRepository.save — id=" + n.getId() + " type=" + n.getType());
        localStore.add(n);
        return n;
    }

    /** Marks a notification as read in the local store. */
    public boolean markAsRead(String id) {
        AffordmedLogger.Log("backend", "info", "repository",
                "NotificationRepository.markAsRead — id=" + id);
        Optional<Notification> match = localStore.stream()
                .filter(n -> n.getId().equals(id)).findFirst();
        match.ifPresent(n -> n.setRead(true));
        return match.isPresent();
    }

    // ── Internal API Fetch ───────────────────────────────────────────────────

    private List<Notification> fetchFromApi() {
        AffordmedLogger.Log("backend", "debug", "repository",
                "Calling Notifications API: " + NOTIFICATIONS_API);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(NOTIFICATIONS_API))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + BEARER_TOKEN)
                    .header("clientID",      CLIENT_ID)
                    .header("clientSecret",  CLIENT_SECRET)
                    .GET()
                    .build();

            HttpResponse<String> resp =
                    HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                AffordmedLogger.Log("backend", "error", "repository",
                        "Notifications API returned HTTP " + resp.statusCode()
                        + " body=" + resp.body());
                return List.of();
            }

            JsonNode root = OBJECT_MAPPER.readTree(resp.body());
            JsonNode arr  = root.get("notifications");
            List<Notification> result = new ArrayList<>();

            if (arr != null && arr.isArray()) {
                for (JsonNode node : arr) {
                    Notification n = new Notification();
                    n.setId(node.get("ID").asText());
                    n.setType(node.get("Type").asText());
                    n.setMessage(node.get("Message").asText());

                    String ts = node.get("Timestamp").asText();
                    // Normalise to 19 chars (drop fractional seconds if any)
                    String normalized = ts.length() > 19 ? ts.substring(0, 19) : ts;
                    n.setTimestamp(LocalDateTime.parse(normalized, TIMESTAMP_FORMAT));
                    result.add(n);
                }
            }

            AffordmedLogger.Log("backend", "info", "repository",
                    "Notifications API returned " + result.size() + " notifications");
            return result;

        } catch (Exception ex) {
            AffordmedLogger.Log("backend", "error", "repository",
                    "Exception in fetchFromApi: " + ex.getMessage());
            return List.of();
        }
    }
}
