package com.tickettide.ticket.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates 12-character codes from an unambiguous alphabet (no 0/O, 1/I/L),
 * giving ~60 bits of entropy so codes cannot be guessed at the door.
 */
@Component
public class TicketCodeGenerator {

    private static final char[] ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final int LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        char[] out = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            out[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(out);
    }
}
