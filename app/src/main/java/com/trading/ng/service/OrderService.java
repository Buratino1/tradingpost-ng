package com.trading.ng.service;

import com.binance.connector.client.common.ApiException;
import com.binance.connector.client.common.ApiResponse;
import com.binance.connector.client.spot.rest.api.SpotRestApi;
import com.binance.connector.client.spot.rest.model.NewOrderRequest;
import com.binance.connector.client.spot.rest.model.NewOrderResponse;
import com.binance.connector.client.spot.rest.model.GetOrderResponse;
import com.binance.connector.client.spot.rest.model.GetOpenOrdersResponse;
import com.binance.connector.client.spot.rest.model.DeleteOrderResponse;
import com.binance.connector.client.spot.rest.model.Side;
import com.binance.connector.client.spot.rest.model.TimeInForce;
import com.trading.ng.domain.OrderSide;
import com.trading.ng.domain.OrderStatus;
import com.trading.ng.domain.OrderType;
import com.trading.ng.domain.TradingOrder;
import com.trading.ng.dto.OrderResponse;
import com.trading.ng.dto.PlaceOrderRequest;
import com.trading.ng.repository.TradingOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final SpotRestApi spotRestApi;
    private final TradingOrderRepository orderRepo;

    public OrderService(SpotRestApi spotRestApi, TradingOrderRepository orderRepo) {
        this.spotRestApi = spotRestApi;
        this.orderRepo = orderRepo;
    }

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest req) {
        log.info("Placing order: {} {} {} qty={} price={}",
                req.symbol(), req.side(), req.type(), req.quantity(), req.price());

        try {
            NewOrderRequest binanceReq = new NewOrderRequest();
            binanceReq.setSymbol(req.symbol());
            binanceReq.setSide(toSide(req.side()));
            binanceReq.setType(toType(req.type()));
            binanceReq.setQuantity(req.quantity().doubleValue());
            if (req.price() != null) {
                binanceReq.setPrice(req.price().doubleValue());
            }
            if (req.timeInForce() != null) {
                binanceReq.setTimeInForce(TimeInForce.fromValue(req.timeInForce()));
            }
            if (req.stopPrice() != null) {
                binanceReq.setStopPrice(req.stopPrice().doubleValue());
            }

            ApiResponse<NewOrderResponse> response = spotRestApi.newOrder(binanceReq);
            NewOrderResponse data = response.getData();

            TradingOrder order = new TradingOrder();
            order.setSymbol(req.symbol());
            order.setBinanceOrderId(data.getOrderId());
            order.setClientOrderId(data.getClientOrderId());
            order.setSide(req.side());
            order.setType(req.type());
            order.setStatus(OrderStatus.valueOf(data.getStatus()));
            order.setPrice(req.price());
            order.setQuantity(req.quantity());
            order.setExecutedQty(new BigDecimal(data.getExecutedQty()));

            order = orderRepo.save(order);
            log.info("Order placed: binanceOrderId={}, status={}",
                    data.getOrderId(), data.getStatus());
            return toResponse(order);

        } catch (ApiException e) {
            log.error("Binance API error placing order: {} (code={})", e.getMessage(), e.getCode());
            throw new RuntimeException("Failed to place order: " + e.getMessage(), e);
        }
    }

    @Transactional
    public OrderResponse cancelOrder(String symbol, Long binanceOrderId) {
        log.info("Cancelling order: symbol={}, orderId={}", symbol, binanceOrderId);

        try {
            ApiResponse<DeleteOrderResponse> response =
                    spotRestApi.deleteOrder(symbol, binanceOrderId, null, null, null, null);

            orderRepo.findByBinanceOrderId(binanceOrderId).ifPresent(order -> {
                order.setStatus(OrderStatus.CANCELED);
                orderRepo.save(order);
            });

            TradingOrder order = orderRepo.findByBinanceOrderId(binanceOrderId).orElse(null);
            return order != null ? toResponse(order) : null;

        } catch (ApiException e) {
            log.error("Binance API error cancelling order: {}", e.getMessage());
            throw new RuntimeException("Failed to cancel order: " + e.getMessage(), e);
        }
    }

    public OrderResponse getOrder(String symbol, Long binanceOrderId) {
        try {
            ApiResponse<GetOrderResponse> response =
                    spotRestApi.getOrder(symbol, binanceOrderId, null, null);
            GetOrderResponse data = response.getData();

            return orderRepo.findByBinanceOrderId(binanceOrderId)
                    .map(this::toResponse)
                    .orElseGet(() -> new OrderResponse(
                            null,
                            data.getOrderId(),
                            data.getSymbol(),
                            OrderSide.valueOf(data.getSide()),
                            OrderType.valueOf(data.getType()),
                            OrderStatus.valueOf(data.getStatus()),
                            new BigDecimal(data.getPrice()),
                            new BigDecimal(data.getOrigQty()),
                            new BigDecimal(data.getExecutedQty()),
                            null, null
                    ));
        } catch (ApiException e) {
            log.error("Binance API error getting order: {}", e.getMessage());
            throw new RuntimeException("Failed to get order: " + e.getMessage(), e);
        }
    }

    public List<OrderResponse> getOpenOrders(String symbol) {
        try {
            ApiResponse<GetOpenOrdersResponse> response =
                    spotRestApi.getOpenOrders(symbol, null);
            // Sync with local DB
            return orderRepo.findBySymbolAndStatusOrderByCreatedAtDesc(symbol, OrderStatus.NEW)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        } catch (ApiException e) {
            log.error("Binance API error getting open orders: {}", e.getMessage());
            throw new RuntimeException("Failed to get open orders: " + e.getMessage(), e);
        }
    }

    public List<OrderResponse> getOrderHistory(String symbol) {
        return orderRepo.findBySymbolOrderByCreatedAtDesc(symbol)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderResponse toResponse(TradingOrder o) {
        return new OrderResponse(
                o.getId(), o.getBinanceOrderId(), o.getSymbol(),
                o.getSide(), o.getType(), o.getStatus(),
                o.getPrice(), o.getQuantity(), o.getExecutedQty(),
                o.getCreatedAt(), o.getUpdatedAt()
        );
    }

    private static Side toSide(OrderSide side) {
        return Side.fromValue(side.name());
    }

    private static com.binance.connector.client.spot.rest.model.OrderType toType(OrderType type) {
        return com.binance.connector.client.spot.rest.model.OrderType.fromValue(type.name());
    }
}
