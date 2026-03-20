package com.tickettide.ticket.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * An order captures a snapshot of the event and price at reservation time, so later
 * edits by the organizer never change what an attendee was charged.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_title", nullable = false)
    private String eventTitle;

    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;

    @Column(name = "ticket_type_name", nullable = false)
    private String ticketTypeName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    @Column(name = "total_cents", nullable = false)
    private long totalCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Order() {
    }

    public Order(UUID userId, String userEmail, UUID eventId, String eventTitle, UUID ticketTypeId,
                 String ticketTypeName, int quantity, long unitPriceCents, String currency, Duration hold) {
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.userEmail = userEmail;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.ticketTypeId = ticketTypeId;
        this.ticketTypeName = ticketTypeName;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.totalCents = Math.multiplyExact(unitPriceCents, quantity);
        this.currency = currency;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.expiresAt = now.plus(hold);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean isHoldExpired(Instant now) {
        return status == OrderStatus.PENDING_PAYMENT && !now.isBefore(expiresAt);
    }

    public void confirm(UUID paymentId) {
        requireStatus(OrderStatus.PENDING_PAYMENT, "confirm");
        this.paymentId = paymentId;
        transition(OrderStatus.CONFIRMED);
    }

    public void cancel() {
        requireStatus(OrderStatus.PENDING_PAYMENT, "cancel");
        transition(OrderStatus.CANCELLED);
    }

    public void expire() {
        requireStatus(OrderStatus.PENDING_PAYMENT, "expire");
        transition(OrderStatus.EXPIRED);
    }

    public void refund() {
        requireStatus(OrderStatus.CONFIRMED, "refund");
        transition(OrderStatus.REFUNDED);
    }

    private void requireStatus(OrderStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("Cannot %s an order that is %s".formatted(action, status));
        }
    }

    private void transition(OrderStatus next) {
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
    public UUID getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public UUID getTicketTypeId() { return ticketTypeId; }
    public String getTicketTypeName() { return ticketTypeName; }
    public int getQuantity() { return quantity; }
    public long getUnitPriceCents() { return unitPriceCents; }
    public long getTotalCents() { return totalCents; }
    public String getCurrency() { return currency; }
    public OrderStatus getStatus() { return status; }
    public UUID getPaymentId() { return paymentId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
