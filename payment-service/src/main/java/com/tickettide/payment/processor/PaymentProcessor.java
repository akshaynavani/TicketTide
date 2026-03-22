package com.tickettide.payment.processor;

/**
 * Abstraction over a card processor (Stripe, Adyen, ...). The idempotency key is
 * forwarded so a retried charge is never captured twice at the processor either.
 */
public interface PaymentProcessor {

    record ChargeResult(boolean success, String reference, String failureReason) {
        public static ChargeResult ok(String reference) {
            return new ChargeResult(true, reference, null);
        }

        public static ChargeResult declined(String reason) {
            return new ChargeResult(false, null, reason);
        }
    }

    ChargeResult charge(long amountCents, String currency, String cardToken, String idempotencyKey);

    void refund(String reference, long amountCents, String currency);
}
