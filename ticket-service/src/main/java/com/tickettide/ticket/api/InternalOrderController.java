package com.tickettide.ticket.api;

import com.tickettide.ticket.api.OrderDtos.InternalOrderView;
import com.tickettide.ticket.api.OrderDtos.PaymentEvent;
import com.tickettide.ticket.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/orders")
public class InternalOrderController {

    private final OrderService orders;

    public InternalOrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/{id}")
    public InternalOrderView get(@PathVariable UUID id) {
        return orders.internalView(id);
    }

    @PostMapping("/{id}/paid")
    public InternalOrderView paid(@PathVariable UUID id, @Valid @RequestBody PaymentEvent event) {
        return orders.markPaid(id, event.paymentId());
    }

    @PostMapping("/{id}/refunded")
    public InternalOrderView refunded(@PathVariable UUID id, @Valid @RequestBody PaymentEvent event) {
        return orders.markRefunded(id, event.paymentId());
    }
}
