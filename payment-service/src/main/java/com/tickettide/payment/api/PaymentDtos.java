package com.tickettide.payment.api;

import com.tickettide.payment.domain.Payment;
import com.tickettide.payment.domain.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    /** cardToken is a processor token (never a raw card number); see SimulatedPaymentProcessor. */
    public record PaymentRequest(@NotNull UUID orderId, @NotBlank @Size(max = 100) String cardToken) {
    }

    public record RefundRequest(@Size(max = 200) String reason) {
    }

    public record PaymentResponse(UUID id, UUID orderId, long amountCents, String currency, PaymentStatus status,
                                  String failureReason, Instant createdAt, Instant updatedAt) {
        public static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmountCents(), p.getCurrency(), p.getStatus(),
                    p.getFailureReason(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }
}
