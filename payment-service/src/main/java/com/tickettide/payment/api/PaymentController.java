package com.tickettide.payment.api;

import com.tickettide.common.security.CurrentUser;
import com.tickettide.common.web.PageResponse;
import com.tickettide.payment.api.PaymentDtos.PaymentRequest;
import com.tickettide.payment.api.PaymentDtos.PaymentResponse;
import com.tickettide.payment.api.PaymentDtos.RefundRequest;
import com.tickettide.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping
    public PaymentResponse pay(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                               @Valid @RequestBody PaymentRequest request) {
        return payments.pay(request, idempotencyKey, CurrentUser.get());
    }

    @GetMapping
    public PageResponse<PaymentResponse> mine(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return payments.mine(CurrentUser.get(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return payments.get(id, CurrentUser.get());
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refund(@PathVariable UUID id, @RequestBody(required = false) RefundRequest request) {
        return payments.refund(id, request == null ? null : request.reason(), CurrentUser.get());
    }
}
