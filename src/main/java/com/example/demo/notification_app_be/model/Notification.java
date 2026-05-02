package com.example.demo.notification_app_be.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification domain model.
 *
 * Represents a campus notification (Placement / Event / Result).
 *
 * Fields:
 *   id        — UUID
 *   type      — "Placement" | "Event" | "Result"
 *   message   — Notification body
 *   timestamp — When published
 *   isRead    — Has the student read it?
 *   priority  — Computed composite score (Stage 6)
 */
public class Notification {

    private String        id;
    private String        type;
    private String        message;
    private LocalDateTime timestamp;
    private boolean       isRead;
    private double        priority;

    public Notification() {
        this.id     = UUID.randomUUID().toString();
        this.isRead = false;
    }

    public Notification(String id, String type, String message, LocalDateTime timestamp) {
        this.id        = id;
        this.type      = type;
        this.message   = message;
        this.timestamp = timestamp;
        this.isRead    = false;
    }

    public String        getId()                      { return id;               }
    public void          setId(String id)             { this.id = id;            }
    public String        getType()                    { return type;             }
    public void          setType(String type)         { this.type = type;        }
    public String        getMessage()                 { return message;          }
    public void          setMessage(String m)         { this.message = m;        }
    public LocalDateTime getTimestamp()               { return timestamp;        }
    public void          setTimestamp(LocalDateTime t){ this.timestamp = t;      }
    public boolean       isRead()                     { return isRead;           }
    public void          setRead(boolean read)        { this.isRead = read;      }
    public double        getPriority()                { return priority;         }
    public void          setPriority(double p)        { this.priority = p;       }

    @Override
    public String toString() {
        return String.format("Notification{id='%s', type='%s', message='%s', " +
                "timestamp=%s, isRead=%b, priority=%.3f}",
                id, type, message, timestamp, isRead, priority);
    }
}
