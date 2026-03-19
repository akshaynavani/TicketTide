package com.tickettide.event.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

    @Id
    private UUID id;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private String city;

    private String category;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(name = "image_key")
    private String imageKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("priceCents ASC")
    private List<TicketType> ticketTypes = new ArrayList<>();

    protected Event() {
    }

    public Event(UUID organizerId, String title, String description, String venue, String city,
                 String category, Instant startsAt, Instant endsAt) {
        this.id = UUID.randomUUID();
        this.organizerId = organizerId;
        this.status = EventStatus.DRAFT;
        this.createdAt = Instant.now();
        update(title, description, venue, city, category, startsAt, endsAt);
    }

    public void update(String title, String description, String venue, String city,
                       String category, Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Event must end after it starts");
        }
        this.title = title;
        this.description = description;
        this.venue = venue;
        this.city = city;
        this.category = category;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        touch();
    }

    public void publish() {
        if (status != EventStatus.DRAFT) {
            throw new IllegalStateException("Only draft events can be published");
        }
        if (ticketTypes.isEmpty()) {
            throw new IllegalStateException("Add at least one ticket type before publishing");
        }
        if (startsAt.isBefore(Instant.now())) {
            throw new IllegalStateException("Cannot publish an event that has already started");
        }
        status = EventStatus.PUBLISHED;
        touch();
    }

    public void cancel() {
        if (status == EventStatus.CANCELLED) {
            throw new IllegalStateException("Event is already cancelled");
        }
        status = EventStatus.CANCELLED;
        touch();
    }

    public TicketType addTicketType(String name, long priceCents, String currency, int capacity) {
        if (status == EventStatus.CANCELLED) {
            throw new IllegalStateException("Cannot add ticket types to a cancelled event");
        }
        TicketType type = new TicketType(this, name, priceCents, currency, capacity);
        ticketTypes.add(type);
        touch();
        return type;
    }

    public boolean isOnSale() {
        return status == EventStatus.PUBLISHED && Instant.now().isBefore(startsAt);
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizerId() { return organizerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getVenue() { return venue; }
    public String getCity() { return city; }
    public String getCategory() { return category; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public EventStatus getStatus() { return status; }
    public String getImageKey() { return imageKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<TicketType> getTicketTypes() { return ticketTypes; }
}
