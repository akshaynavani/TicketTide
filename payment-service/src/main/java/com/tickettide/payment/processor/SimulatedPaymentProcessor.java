package com.tickettide.payment.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * Deterministic stand-in for a real processor, modelled on Stripe's test tokens:
 * <ul>
 *   <li>{@code tok_visa}, {@code tok_mastercard} or any other {@code tok_*} - approved</li>
 *   <li>{@code tok_declined} - card declined</li>
 *   <li>{@code tok_insufficient_funds} - insufficient funds</li>
 * </ul>
 * Charges are de-duplicated by idempotency key like a real processor would.
 */
@Component
public class SimulatedPaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentProcessor.class);

    private static final Map<String, String> DECLINES = Map.of(
            "tok_declined", "card_declined",
            "tok_insufficient_funds", "insufficient_funds",
            "tok_expired", "expired_card");

    private final Map<String, ChargeResult> charges = new ConcurrentHashMap<>();

    @Override
    public ChargeResult charge(long amountCents, String currency, String cardToken, String idempotencyKey) {
        return charges.computeIfAbsent(idempotencyKey, key -> {
            if (cardToken == null || !cardToken.startsWith("tok_")) {
                return ChargeResult.declined("invalid_card_token");
            }
            String decline = DECLINES.get(cardToken);
            if (decline != null) {
                log.info("Simulated decline ({}) for {} {}", decline, amountCents, currency);
                return ChargeResult.declined(decline);
            }
            return ChargeResult.ok("ch_sim_" + shortHash(key));
        });
    }

    @Override
    public void refund(String reference, long amountCents, String currency) {
        log.info("Simulated refund of {} {} against {}", amountCents, currency, reference);
    }

    private static String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
