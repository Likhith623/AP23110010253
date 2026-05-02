package com.example.demo.logging_middleware.vehicle_maintenance_scheduler;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Vehicle model — represents a single maintenance task from the Vehicles API.
 *
 * Fields:
 *   taskID   — Unique UUID for this maintenance task
 *   duration — Estimated service time in mechanic-hours (knapsack weight)
 *   impact   — Operational importance score (knapsack value)
 */
public class Vehicle {

    @JsonProperty("TaskID")
    private String taskID;

    @JsonProperty("Duration")
    private int duration;

    @JsonProperty("Impact")
    private int impact;

    public Vehicle() {}

    public Vehicle(String taskID, int duration, int impact) {
        this.taskID   = taskID;
        this.duration = duration;
        this.impact   = impact;
    }

    public String getTaskID()          { return taskID;   }
    public void   setTaskID(String t)  { this.taskID = t; }

    public int    getDuration()        { return duration;   }
    public void   setDuration(int d)   { this.duration = d; }

    public int    getImpact()          { return impact;     }
    public void   setImpact(int i)     { this.impact = i;   }

    @Override
    public String toString() {
        return String.format("Vehicle{taskID='%s', duration=%d, impact=%d}",
                taskID, duration, impact);
    }
}
