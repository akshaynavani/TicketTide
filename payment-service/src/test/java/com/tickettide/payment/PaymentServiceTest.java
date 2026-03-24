package com.tickettide.payment;

import com.tickettide.common.security.AuthenticatedUser;
import com.tickettide.common.security.Role;
import com.tickettide.common.web.ApiException;
import com.tickettide.payment.api.PaymentDtos.PaymentRequest;
import com.tickettide.payment.api.PaymentDtos.PaymentResponse;
import com.tickettide.payment.client.TicketClient;
import com.tickettide.payment.client.TicketClient.OrderView;
import com.tickettide.payment.domain.Payment;
import com.tickettide.payment.domain.PaymentRepository;
import com.tickettide.payment.domain.PaymentStatus;
import com.tickettide.payment.processor.SimulatedPaymentProcessor;
import com.tickettide.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final TicketClient ticketClient = mock(TicketClient.class);
    private final SimulatedPaymentProcessor processor = new SimulatedPaymentProcessor();
    private final AuthenticatedUser buyer = new AuthenticatedUser(UUID.randomUUID(), "b@x.io", Role.ATTENDEE);
    private final UUID orderId = UUID.randomUUID();
    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(payments, processor, ticketClient);
        when(payments.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payments.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketClient.order(orderId)).thenReturn(pendingOrder(buyer.id()));
    }

    private OrderView pendingOrder(UUID owner) {
        return new OrderView(orderId, owner, "b@x.io", "Show", "PENDING_PAYMENT", 5000, "USD",
                Instant.now().plusSeconds(300), null);
    }

    @Test
    void successfulChargeConfirmsOrder() {
        PaymentResponse res = service.pay(new PaymentRequest(orderId, "tok_visa"), "key-1", buyer);

        assertThat(res.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(res.amountCents()).isEqualTo(5000);
        verify(ticketClient).markPaid(eq(orderId), eq(res.id()));
    }

    @Test
    void declinedCardRecordsFailureAndDoesNotConfirm() {
        assertThatThrownBy(() -> service.pay(new PaymentRequest(orderId, "tok_declined"), "key-2", buyer))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).status()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        verify(ticketClient, never()).markPaid(any(), any());
    }

    @Test
    void replayWithSameKeyReturnsOriginalPaymentWithoutCharging() {
        Payment existing = new Payment(orderId, buyer.id(), 5000, "USD", "key-3");
        existing.succeeded("ch_sim_abc");
        when(payments.findByUserIdAndIdempotencyKey(buyer.id(), "key-3")).thenReturn(Optional.of(existing));

        PaymentResponse res = service.pay(new PaymentRequest(orderId, "tok_visa"), "key-3", buyer);

        assertThat(res.id()).isEqualTo(existing.getId());
        verify(ticketClient, never()).order(any());
        verify(payments, never()).saveAndFlush(any());
    }

    @Test
    void reusingKeyForDifferentOrderIsRejected() {
        Payment existing = new Payment(UUID.randomUUID(), buyer.id(), 5000, "USD", "key-4");
        when(payments.findByUserIdAndIdempotencyKey(buyer.id(), "key-4")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.pay(new PaymentRequest(orderId, "tok_visa"), "key-4", buyer))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void cannotPayForSomeoneElsesOrder() {
        when(ticketClient.order(orderId)).thenReturn(pendingOrder(UUID.randomUUID()));

        assertThatThrownBy(() -> service.pay(new PaymentRequest(orderId, "tok_visa"), "key-5", buyer))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void secondPaymentForSameOrderIsRejected() {
        when(payments.existsByOrderIdAndStatusIn(eq(orderId), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> service.pay(new PaymentRequest(orderId, "tok_visa"), "key-6", buyer))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).status()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void chargeIsRefundedWhenHoldExpiresMidPayment() {
        when(ticketClient.markPaid(eq(orderId), any())).thenThrow(
                HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY, new byte[0], null));

        assertThatThrownBy(() -> service.pay(new PaymentRequest(orderId, "tok_visa"), "key-7", buyer))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("refunded");
    }

    @Test
    void onlyAdminsCanRefund() {
        assertThatThrownBy(() -> service.refund(UUID.randomUUID(), null, buyer))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).status()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
