package com.trading.ng.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trades", indexes = {
        @Index(name = "idx_trade_symbol", columnList = "symbol"),
        @Index(name = "idx_trade_order", columnList = "binanceOrderId")
})
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long binanceTradeId;

    @Column(nullable = false)
    private String symbol;

    private Long binanceOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(precision = 20, scale = 8)
    private BigDecimal commission;

    private String commissionAsset;

    private boolean maker;

    @Column(nullable = false)
    private Instant tradeTime;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }

    public Long getBinanceTradeId() { return binanceTradeId; }
    public void setBinanceTradeId(Long binanceTradeId) { this.binanceTradeId = binanceTradeId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Long getBinanceOrderId() { return binanceOrderId; }
    public void setBinanceOrderId(Long binanceOrderId) { this.binanceOrderId = binanceOrderId; }

    public OrderSide getSide() { return side; }
    public void setSide(OrderSide side) { this.side = side; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getCommission() { return commission; }
    public void setCommission(BigDecimal commission) { this.commission = commission; }

    public String getCommissionAsset() { return commissionAsset; }
    public void setCommissionAsset(String commissionAsset) { this.commissionAsset = commissionAsset; }

    public boolean isMaker() { return maker; }
    public void setMaker(boolean maker) { this.maker = maker; }

    public Instant getTradeTime() { return tradeTime; }
    public void setTradeTime(Instant tradeTime) { this.tradeTime = tradeTime; }

    public Instant getCreatedAt() { return createdAt; }
}
