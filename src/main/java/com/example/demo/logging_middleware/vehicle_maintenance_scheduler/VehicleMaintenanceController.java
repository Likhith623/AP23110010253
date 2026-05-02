package com.example.demo.logging_middleware.vehicle_maintenance_scheduler;

import com.example.demo.logging_middleware.AffordmedLogger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * VehicleMaintenanceController
 *
 * Exposes the vehicle maintenance scheduler via REST.
 *
 * GET /api/vehicle-maintenance/schedule
 *   → Triggers the 0/1 Knapsack optimisation for all depots.
 *     Results are printed to server logs.
 */
@RestController
@RequestMapping("/api/vehicle-maintenance")
public class VehicleMaintenanceController {

    private final VehicleMaintenanceService schedulerService;

    public VehicleMaintenanceController(VehicleMaintenanceService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @GetMapping("/schedule")
    public ResponseEntity<Map<String, String>> runSchedule() {
        AffordmedLogger.Log("backend", "info", "handler",
                "GET /api/vehicle-maintenance/schedule — received; rollNo=ap23110010253");
        try {
            schedulerService.runScheduler();
            AffordmedLogger.Log("backend", "info", "handler",
                    "GET /api/vehicle-maintenance/schedule — completed successfully");
            return ResponseEntity.ok(Map.of(
                    "status",  "success",
                    "message", "Schedule computed for all depots. See server logs for results."
            ));
        } catch (Exception ex) {
            AffordmedLogger.Log("backend", "error", "handler",
                    "Schedule failed: " + ex.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", ex.getMessage()));
        }
    }
}
