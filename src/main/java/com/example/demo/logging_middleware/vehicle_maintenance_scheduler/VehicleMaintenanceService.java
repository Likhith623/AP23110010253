package com.example.demo.logging_middleware.vehicle_maintenance_scheduler;

import com.example.demo.logging_middleware.AffordmedLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * VehicleMaintenanceService
 *
 * Fetches depot capacities and vehicle tasks from the Affordmed evaluation APIs,
 * then solves a 0/1 Knapsack problem per depot to maximise total operational
 * impact within the available mechanic-hour budget.
 *
 * Algorithm: Dynamic Programming — 0/1 Knapsack
 *   Time  complexity : O(depots × N × W)
 *   Space complexity : O(N × W)
 *
 * Credentials:
 *   clientID     : 4598edfb-2914-428e-84a7-e879ab34c8ad
 *   clientSecret : TuTcAddeMczaYRNa
 *   Bearer Token : eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 */
@Service
public class VehicleMaintenanceService {

    private static final String CLIENT_ID     = "4598edfb-2914-428e-84a7-e879ab34c8ad";
    private static final String CLIENT_SECRET = "TuTcAddeMczaYRNa";
    private static final String BEARER_TOKEN  =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJNYXBDbGFpbXMiOnsiYXVkIjoiaHR0cDovLzIwLjI0NC41Ni4xNDQvZXZhbHVhdGlvbi1zZXJ2aWNlIiwiZW1haWwiOiJsaWtoaXRoY2hvd2RhcnlfdmFzaXJlZGR5QHNybWFwLmVkdS5pbiIsImV4cCI6MTc3NzcwMjcxMywiaWF0IjoxNzc3NzAxODEzLCJpc3MiOiJBZmZvcmQgTWVkaWNhbCBUZWNobm9sb2dpZXMgUHJpdmF0ZSBMaW1pdGVkIiwianRpIjoiMzdlN2Y2OTEtNzliYy00NWY5LWJiYWQtNGY1M2YyZTk5OTUzIiwibG9jYWxlIjoiZW4tSU4iLCJuYW1lIjoibGlraGl0aCBjaG93ZGFyeSB2YXNpcmVkZHkiLCJzdWIiOiI0NTk4ZWRmYi0yOTE0LTQyOGUtODRhNy1lODc5YWIzNGM4YWQifSwiZW1haWwiOiJsaWtoaXRoY2hvd2RhcnlfdmFzaXJlZGR5QHNybWFwLmVkdS5pbiIsIm5hbWUiOiJsaWtoaXRoIGNob3dkYXJ5IHZhc2lyZWRkeSIsInJvbGxObyI6ImFwMjMxMTAwMTAyNTMiLCJhY2Nlc3NDb2RlIjoiUWticHhIIiwiY2xpZW50SUQiOiI0NTk4ZWRmYi0yOTE0LTQyOGUtODRhNy1lODc5YWIzNGM4YWQiLCJjbGllbnRTZWNyZXQiOiJUdVRjQWRkZU1jemFZUk5hIn0." +
            "qHnMvv0QllZCmVjko8A-10zOh-pvkuUfPF4DgA5vb0U";

    private static final String DEPOTS_API   =
            "http://20.207.122.201/evaluation-service/depots";
    private static final String VEHICLES_API =
            "http://20.207.122.201/evaluation-service/vehicles";

    private static final HttpClient    HTTP_CLIENT    = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private static final ObjectMapper  OBJECT_MAPPER  = new ObjectMapper();

    // ── Entry Point ──────────────────────────────────────────────────────────

    /**
     * Fetches depots + vehicles, runs 0/1 Knapsack per depot, prints results.
     */
    public void runScheduler() {
        AffordmedLogger.Log("backend", "info", "service",
                "VehicleMaintenanceScheduler — starting scheduler run for rollNo=ap23110010253");

        List<Depot>   depots   = fetchDepots();
        List<Vehicle> vehicles = fetchVehicles();

        if (depots.isEmpty() || vehicles.isEmpty()) {
            AffordmedLogger.Log("backend", "error", "service",
                    "VehicleMaintenanceScheduler — empty data received; aborting");
            return;
        }

        AffordmedLogger.Log("backend", "info", "service",
                String.format("Fetched %d depots and %d vehicles — beginning optimisation",
                        depots.size(), vehicles.size()));

        for (Depot depot : depots) {
            ScheduleResult result = solveKnapsack(depot, vehicles);
            printDepotResult(depot, result);
        }

        AffordmedLogger.Log("backend", "info", "service",
                "VehicleMaintenanceScheduler — all depots processed successfully");
    }

    // ── 0/1 Knapsack Solver ──────────────────────────────────────────────────

    private ScheduleResult solveKnapsack(Depot depot, List<Vehicle> vehicles) {
        int capacity = depot.getMechanicHours();
        int n        = vehicles.size();

        AffordmedLogger.Log("backend", "debug", "service",
                String.format("Depot %d — knapsack: capacity=%d h, items=%d",
                        depot.getId(), capacity, n));

        // dp[i][w] = maximum impact using first i items with weight limit w
        int[][] dp = new int[n + 1][capacity + 1];

        for (int i = 1; i <= n; i++) {
            int w   = vehicles.get(i - 1).getDuration();
            int val = vehicles.get(i - 1).getImpact();
            for (int c = 0; c <= capacity; c++) {
                dp[i][c] = dp[i - 1][c];
                if (w <= c && dp[i - 1][c - w] + val > dp[i][c]) {
                    dp[i][c] = dp[i - 1][c - w] + val;
                }
            }
        }

        // Backtrack to identify chosen tasks
        List<Vehicle> selected   = new ArrayList<>();
        int           remaining  = capacity;
        for (int i = n; i >= 1; i--) {
            if (dp[i][remaining] != dp[i - 1][remaining]) {
                Vehicle v = vehicles.get(i - 1);
                selected.add(v);
                remaining -= v.getDuration();
            }
        }

        int totalImpact   = dp[n][capacity];
        int totalDuration = selected.stream().mapToInt(Vehicle::getDuration).sum();

        AffordmedLogger.Log("backend", "info", "service",
                String.format("Depot %d — optimal: %d tasks, duration=%d h, impact=%d",
                        depot.getId(), selected.size(), totalDuration, totalImpact));

        return new ScheduleResult(selected, totalImpact, totalDuration);
    }

    // ── API Fetch Helpers ────────────────────────────────────────────────────

    private List<Depot> fetchDepots() {
        AffordmedLogger.Log("backend", "info", "service",
                "Fetching depots from Affordmed API — " + DEPOTS_API);
        try {
            HttpRequest req = buildGetRequest(DEPOTS_API);
            HttpResponse<String> resp =
                    HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                AffordmedLogger.Log("backend", "error", "service",
                        "Depots API returned HTTP " + resp.statusCode());
                return List.of();
            }

            JsonNode  root   = OBJECT_MAPPER.readTree(resp.body());
            JsonNode  arr    = root.get("depots");
            List<Depot> list = new ArrayList<>();
            if (arr != null && arr.isArray()) {
                for (JsonNode n : arr)
                    list.add(OBJECT_MAPPER.treeToValue(n, Depot.class));
            }
            AffordmedLogger.Log("backend", "debug", "service",
                    "Parsed " + list.size() + " depots");
            return list;

        } catch (Exception ex) {
            AffordmedLogger.Log("backend", "error", "service",
                    "Exception fetching depots: " + ex.getMessage());
            return List.of();
        }
    }

    private List<Vehicle> fetchVehicles() {
        AffordmedLogger.Log("backend", "info", "service",
                "Fetching vehicles from Affordmed API — " + VEHICLES_API);
        try {
            HttpRequest req = buildGetRequest(VEHICLES_API);
            HttpResponse<String> resp =
                    HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                AffordmedLogger.Log("backend", "error", "service",
                        "Vehicles API returned HTTP " + resp.statusCode());
                return List.of();
            }

            JsonNode     root = OBJECT_MAPPER.readTree(resp.body());
            JsonNode     arr  = root.get("vehicles");
            List<Vehicle> list = new ArrayList<>();
            if (arr != null && arr.isArray()) {
                for (JsonNode n : arr)
                    list.add(OBJECT_MAPPER.treeToValue(n, Vehicle.class));
            }
            AffordmedLogger.Log("backend", "debug", "service",
                    "Parsed " + list.size() + " vehicles");
            return list;

        } catch (Exception ex) {
            AffordmedLogger.Log("backend", "error", "service",
                    "Exception fetching vehicles: " + ex.getMessage());
            return List.of();
        }
    }

    private HttpRequest buildGetRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .header("clientID",      CLIENT_ID)
                .header("clientSecret",  CLIENT_SECRET)
                .GET()
                .build();
    }

    // ── Result Printer ───────────────────────────────────────────────────────

    private void printDepotResult(Depot depot, ScheduleResult r) {
        System.out.printf(
            "%n╔══════════════════════════════════════════════════════════╗%n");
        System.out.printf(
            "║  Depot #%d │ Budget: %d hrs │ Max Impact: %d%n",
            depot.getId(), depot.getMechanicHours(), r.totalImpact());
        System.out.printf(
            "║  Hours Used: %d / %d │ Tasks Selected: %d%n",
            r.totalDuration(), depot.getMechanicHours(), r.selectedVehicles().size());
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        for (Vehicle v : r.selectedVehicles()) {
            System.out.printf("║  %-38s  Duration=%-3d  Impact=%d%n",
                    v.getTaskID().substring(0, Math.min(38, v.getTaskID().length())),
                    v.getDuration(), v.getImpact());
        }
        System.out.println(
            "╚══════════════════════════════════════════════════════════╝");
    }

    // ── Inner Record ─────────────────────────────────────────────────────────
    public record ScheduleResult(
            List<Vehicle> selectedVehicles,
            int totalImpact,
            int totalDuration) {}
}
