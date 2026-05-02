package com.example.demo.logging_middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * AffordmedLogger — Reusable Logging Middleware
 *
 * Provides a static Log(stack, level, pkg, message) method that ships
 * every log entry to the Affordmed evaluation Log API (POST).
 *
 * Credentials:
 *   email        : likhithchowdary_vasireddy@srmap.edu.in
 *   name         : likhith chowdary vasireddy
 *   rollNo       : ap23110010253
 *   accessCode   : QkbpxH
 *   clientID     : 4598edfb-2914-428e-84a7-e879ab34c8ad
 *   clientSecret : TuTcAddeMczaYRNa
 *
 * Usage:
 *   AffordmedLogger.Log("backend", "info", "handler", "Request received");
 */
@Component
public class AffordmedLogger {

    // ── Affordmed API ────────────────────────────────────────────────────────
    private static final String LOG_API_URL =
            "http://20.207.122.201/evaluation-service/logs";

    // ── Your Credentials ─────────────────────────────────────────────────────
    private static final String CLIENT_ID     = "4598edfb-2914-428e-84a7-e879ab34c8ad";
    private static final String CLIENT_SECRET = "TuTcAddeMczaYRNa";
    private static final String BEARER_TOKEN  =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJNYXBDbGFpbXMiOnsiYXVkIjoiaHR0cDovLzIwLjI0NC41Ni4xNDQvZXZhbHVhdGlvbi1zZXJ2aWNlIiwiZW1haWwiOiJsaWtoaXRoY2hvd2RhcnlfdmFzaXJlZGR5QHNybWFwLmVkdS5pbiIsImV4cCI6MTc3NzcwNjEyNSwiaWF0IjoxNzc3NzA1MjI1LCJpc3MiOiJBZmZvcmQgTWVkaWNhbCBUZWNobm9sb2dpZXMgUHJpdmF0ZSBMaW1pdGVkIiwianRpIjoiZTYwNDEyNGEtOTY2Yi00ZTQ1LWI5YzAtZThkYmRlZTMzMTU1IiwibG9jYWxlIjoiZW4tSU4iLCJuYW1lIjoibGlraGl0aCBjaG93ZGFyeSB2YXNpcmVkZHkiLCJzdWIiOiI0NTk4ZWRmYi0yOTE0LTQyOGUtODRhNy1lODc5YWIzNGM4YWQifSwiZW1haWwiOiJsaWtoaXRoY2hvd2RhcnlfdmFzaXJlZGR5QHNybWFwLmVkdS5pbiIsIm5hbWUiOiJsaWtoaXRoIGNob3dkYXJ5IHZhc2lyZWRkeSIsInJvbGxObyI6ImFwMjMxMTAwMTAyNTMiLCJhY2Nlc3NDb2RlIjoiUWticHhIIiwiY2xpZW50SUQiOiI0NTk4ZWRmYi0yOTE0LTQyOGUtODRhNy1lODc5YWIzNGM4YWQiLCJjbGllbnRTZWNyZXQiOiJUdVRjQWRkZU1jemFZUk5hIn0." +
            "QPWuQHUlTRYcbAFLbT4KjqOYVhm8T-nlDGl2UmK77-g";

    // ── Allowed Values (lower-case only) ─────────────────────────────────────
    // Stack   : "backend" | "frontend"
    // Level   : "debug" | "info" | "warn" | "error" | "fatal"
    // Package : backend-only  → "cache","controller","cron_job","db","domain",
    //                           "handler","repository","route","service"
    //           both          → "auth","config","middleware","utils"

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ── Core Log Function ─────────────────────────────────────────────────────

    /**
     * Ships a structured log entry to the Affordmed evaluation server.
     *
     * @param stack   "backend" or "frontend"
     * @param level   "debug", "info", "warn", "error", "fatal"
     * @param pkg     Package name (must match allowed list for the stack)
     * @param message Human-readable description of the event
     * @return The logID returned by the server, or null on failure
     */
    public static String Log(String stack, String level, String pkg, String message) {
        try {
            // Affordmed API rejects messages longer than 48 characters
            if (message != null && message.length() > 48) {
                message = message.substring(0, 45) + "...";
            }

            Map<String, String> body = new HashMap<>();
            body.put("stack",   stack);
            body.put("level",   level);
            body.put("package", pkg);
            body.put("message", message);

            String jsonBody = OBJECT_MAPPER.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LOG_API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type",  "application/json")
                    .header("Authorization", "Bearer " + BEARER_TOKEN)
                    .header("clientID",      CLIENT_ID)
                    .header("clientSecret",  CLIENT_SECRET)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response =
                    HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resp =
                        OBJECT_MAPPER.readValue(response.body(), Map.class);
                String logID = (String) resp.get("logID");
                System.out.printf("[AffordmedLogger] %s | %-5s | %-12s | %s  → logID=%s%n",
                        stack, level.toUpperCase(), pkg, message, logID);
                return logID;
            } else {
                System.err.printf("[AffordmedLogger] HTTP %d — %s%n",
                        response.statusCode(), response.body());
            }

        } catch (Exception ex) {
            System.err.printf("[AffordmedLogger] Failed to send log — %s%n", ex.getMessage());
        }
        return null;
    }

    private AffordmedLogger() {}
}
