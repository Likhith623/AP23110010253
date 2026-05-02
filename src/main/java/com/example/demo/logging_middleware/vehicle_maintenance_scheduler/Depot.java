package com.example.demo.logging_middleware.vehicle_maintenance_scheduler;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Depot model — represents a single depot from the Depots API.
 *
 * Fields:
 *   id            — Depot identifier
 *   mechanicHours — Daily mechanic-hour budget (knapsack capacity)
 */
public class Depot {

    @JsonProperty("ID")
    private int id;

    @JsonProperty("MechanicHours")
    private int mechanicHours;

    public Depot() {}

    public Depot(int id, int mechanicHours) {
        this.id           = id;
        this.mechanicHours = mechanicHours;
    }

    public int  getId()                  { return id;           }
    public void setId(int id)            { this.id = id;        }

    public int  getMechanicHours()       { return mechanicHours; }
    public void setMechanicHours(int mh) { this.mechanicHours = mh; }

    @Override
    public String toString() {
        return String.format("Depot{id=%d, mechanicHours=%d}", id, mechanicHours);
    }
}
