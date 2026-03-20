package com.tickettide.ticket.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    EXPIRED,
    REFUNDED;

    public boolean isTerminal() {
        return this != PENDING_PAYMENT && this != CONFIRMED;
    }
}
