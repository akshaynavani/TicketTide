package com.tickettide.ticket.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    protected Ticket() {
    }

    public Ticket(Order order, String code) {
        this.id = UUID.randomUUID();
        this.orderId = order.getId();
        this.userId = order.getUserId();
        this.eventId = order.getEventId();
        this.ticketTypeId = order.getTicketTypeId();
        this.code = code;
        this.status = TicketStatus.VALID;
        this.issuedAt = Instant.now();
    }

    public void checkIn() {
        switch (status) {
            case CHECKED_IN -> throw new IllegalStateException("Ticket was already checked in at " + checkedInAt);
            case VOID -> throw new IllegalStateException("Ticket has been voided");
            case VALID -> {
                status = TicketStatus.CHECKED_IN;
                checkedInAt = Instant.now();
            }
        }
    }

    public void voidTicket() {
        status = TicketStatus.VOID;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getUserId() { return userId; }
    public UUID getEventId() { return eventId; }
    public UUID getTicketTypeId() { return ticketTypeId; }
    public String getCode() { return code; }
    public TicketStatus getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getCheckedInAt() { return checkedInAt; }
}
