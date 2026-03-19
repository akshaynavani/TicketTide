package com.tickettide.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "ticket_types")
public class TicketType {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int available;

    @Version
    private long version;

    protected TicketType() {
    }

    TicketType(Event event, String name, long priceCents, String currency, int capacity) {
        if (priceCents < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.id = UUID.randomUUID();
        this.event = event;
        this.name = name;
        this.priceCents = priceCents;
        this.currency = currency.toUpperCase(Locale.ROOT);
        this.capacity = capacity;
        this.available = capacity;
    }

    public int sold() {
        return capacity - available;
    }

    public UUID getId() { return id; }
    public Event getEvent() { return event; }
    public String getName() { return name; }
    public long getPriceCents() { return priceCents; }
    public String getCurrency() { return currency; }
    public int getCapacity() { return capacity; }
    public int getAvailable() { return available; }
}
