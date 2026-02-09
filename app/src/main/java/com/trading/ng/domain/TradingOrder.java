package com.trading.ng.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trading_orders", indexes = {
        @Index(name = "idx_order_symbol", columnList = "symbol"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_binance_id", columnList = "binanceOrderId")
})
public class TradingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(unique = true)
    private Long binanceOrderId;

    private String clientOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(precision = 20, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(precision = 20, scale = 8)
    private BigDecimal executedQty;

    @Column(precision = 20, scale = 8)
    private BigDecimal stopPrice;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Long getBinanceOrderId() { return binanceOrderId; }
    public void setBinanceOrderId(Long binanceOrderId) { this.binanceOrderId = binanceOrderId; }

    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }

    public OrderSide getSide() { return side; }
    public void setSide(OrderSide side) { this.side = side; }

    public OrderType getType() { return type; }
    public void setType(OrderType type) { this.type = type; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getExecutedQty() { return executedQty; }
    public void setExecutedQty(BigDecimal executedQty) { this.executedQty = executedQty; }

    public BigDecimal getStopPrice() { return stopPrice; }
    public void setStopPrice(BigDecimal stopPrice) { this.stopPrice = stopPrice; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
