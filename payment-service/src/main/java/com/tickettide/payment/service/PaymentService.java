package com.tickettide.payment.service;

import com.tickettide.common.security.AuthenticatedUser;
import com.tickettide.common.web.ApiException;
import com.tickettide.common.web.PageResponse;
import com.tickettide.payment.api.PaymentDtos.PaymentRequest;
import com.tickettide.payment.api.PaymentDtos.PaymentResponse;
import com.tickettide.payment.client.TicketClient;
import com.tickettide.payment.client.TicketClient.OrderView;
import com.tickettide.payment.domain.Payment;
import com.tickettide.payment.domain.PaymentRepository;
import com.tickettide.payment.domain.PaymentStatus;
import com.tickettide.payment.processor.PaymentProcessor;
import com.tickettide.payment.processor.PaymentProcessor.ChargeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates a charge across the processor and ticket-service. Deliberately not
 * one big DB transaction: we never hold a connection open across network calls,
 * and every step is recorded so a retry with the same Idempotency-Key is safe.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final PaymentProcessor processor;
    private final TicketClient ticketClient;

    public PaymentService(PaymentRepository payments, PaymentProcessor processor, TicketClient ticketClient) {
        this.payments = payments;
        this.processor = processor;
        this.ticketClient = ticketClient;
    }

    public PaymentResponse pay(PaymentRequest req, String idempotencyKey, AuthenticatedUser user) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw ApiException.badRequest("Idempotency-Key header (1-100 chars) is required");
        }

        Optional<Payment> replay = payments.findByUserIdAndIdempotencyKey(user.id(), idempotencyKey);
        if (replay.isPresent()) {
            return replayed(replay.get(), req);
        }

        OrderView order = ticketClient.order(req.orderId());
        if (!order.userId().equals(user.id())) {
            throw ApiException.notFound("Order");
        }
        if (!order.awaitingPayment()) {
            throw ApiException.conflict("Order is not awaiting payment");
        }
        if (payments.existsByOrderIdAndStatusIn(order.id(), EnumSet.of(PaymentStatus.PENDING, PaymentStatus.SUCCEEDED))) {
            throw ApiException.conflict("Order already has a payment in progress or completed");
        }

        Payment payment;
        try {
            payment = payments.saveAndFlush(
                    new Payment(order.id(), user.id(), order.totalCents(), order.currency(), idempotencyKey));
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a concurrent request using the same key or for the same order.
            return payments.findByUserIdAndIdempotencyKey(user.id(), idempotencyKey)
                    .map(p -> replayed(p, req))
                    .orElseThrow(() -> ApiException.conflict("Order already has a payment in progress"));
        }

        ChargeResult result = processor.charge(payment.getAmountCents(), payment.getCurrency(), req.cardToken(),
                payment.getId().toString());
        if (!result.success()) {
            payment.failed(result.failureReason());
            payments.save(payment);
            log.info("Payment {} for order {} declined: {}", payment.getId(), order.id(), result.failureReason());
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Payment declined: " + result.failureReason());
        }

        payment.succeeded(result.reference());
        payments.save(payment);

        try {
            ticketClient.markPaid(order.id(), payment.getId());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                // The hold lapsed between our check and the capture: give the money back.
                processor.refund(result.reference(), payment.getAmountCents(), payment.getCurrency());
                payment.refunded("Order expired before payment completed");
                payments.save(payment);
                throw ApiException.conflict("Your reservation expired; the charge has been refunded");
            }
            throw e;
        }
        log.info("Payment {} captured for order {}", payment.getId(), order.id());
        return PaymentResponse.from(payment);
    }

    public PaymentResponse refund(UUID paymentId, String reason, AuthenticatedUser admin) {
        if (!admin.isAdmin()) {
            throw ApiException.forbidden("Only admins can issue refunds");
        }
        Payment payment = payments.findById(paymentId).orElseThrow(() -> ApiException.notFound("Payment"));
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw ApiException.conflict("Only succeeded payments can be refunded");
        }
        // Mark the order refunded first: if ticket-service rejects (e.g. tickets used), no money moves.
        ticketClient.markRefunded(payment.getOrderId(), payment.getId());
        processor.refund(payment.getProcessorRef(), payment.getAmountCents(), payment.getCurrency());
        payment.refunded(reason == null || reason.isBlank() ? "Refunded by admin" : reason);
        payments.save(payment);
        log.info("Payment {} refunded by {}", paymentId, admin.id());
        return PaymentResponse.from(payment);
    }

    public PageResponse<PaymentResponse> mine(AuthenticatedUser user, Pageable page) {
        return PageResponse.of(payments.findByUserIdOrderByCreatedAtDesc(user.id(), page), PaymentResponse::from);
    }

    public PaymentResponse get(UUID id, AuthenticatedUser user) {
        Payment payment = payments.findById(id).orElseThrow(() -> ApiException.notFound("Payment"));
        if (!user.isAdmin() && !payment.getUserId().equals(user.id())) {
            throw ApiException.notFound("Payment");
        }
        return PaymentResponse.from(payment);
    }

    private PaymentResponse replayed(Payment existing, PaymentRequest req) {
        if (!existing.getOrderId().equals(req.orderId())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Idempotency-Key was already used for a different order");
        }
        if (existing.getStatus() == PaymentStatus.FAILED) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Payment declined: " + existing.getFailureReason());
        }
        return PaymentResponse.from(existing);
    }
}
