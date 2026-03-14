package com.tickettide.common.security;

public enum Role {
    ATTENDEE,
    ORGANIZER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
