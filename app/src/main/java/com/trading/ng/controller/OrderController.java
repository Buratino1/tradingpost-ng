package com.trading.ng.controller;

import com.trading.ng.dto.OrderResponse;
import com.trading.ng.dto.PlaceOrderRequest;
import com.trading.ng.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(request);
    }

    @DeleteMapping("/{symbol}/{orderId}")
    public OrderResponse cancelOrder(
            @PathVariable String symbol,
            @PathVariable Long orderId) {
        return orderService.cancelOrder(symbol, orderId);
    }

    @GetMapping("/{symbol}/{orderId}")
    public OrderResponse getOrder(
            @PathVariable String symbol,
            @PathVariable Long orderId) {
        return orderService.getOrder(symbol, orderId);
    }

    @GetMapping("/open/{symbol}")
    public List<OrderResponse> getOpenOrders(@PathVariable String symbol) {
        return orderService.getOpenOrders(symbol);
    }

    @GetMapping("/history/{symbol}")
    public List<OrderResponse> getOrderHistory(@PathVariable String symbol) {
        return orderService.getOrderHistory(symbol);
    }
}
