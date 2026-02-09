# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**trading-ng** — cryptocurrency trading application for Binance EU.

- **Language:** Java 25
- **Framework:** Spring Boot 4.0.2 (Spring Framework 7)
- **Build Tool:** Maven
- **Database:** MySQL
- **Binance SDK:** `io.github.binance:binance-spot:8.0.0` (official auto-generated connector)
- **Virtual Threads:** enabled globally via `spring.threads.virtual.enabled=true`

## Build & Run Commands

All commands run from the `app/` directory:

```bash
mvn clean install              # Build + tests
mvn test                       # Run all tests
mvn test -Dtest=ClassName      # Run single test class
mvn test -Dtest=Class#method   # Run single test method
mvn spring-boot:run            # Run the app (port 8080)
mvn clean package -DskipTests  # Package JAR
```

## Environment Variables

| Variable | Description |
|---|---|
| `BINANCE_API_KEY` | Binance API key |
| `BINANCE_PRIVATE_KEY_PATH` | Path to RSA/Ed25519 private key file |
| `BINANCE_PRIVATE_KEY_PASS` | Private key passphrase (if encrypted) |
| `BINANCE_USE_TESTNET` | `true` to use testnet (default: `false`) |
| `BINANCE_BASE_URL` | Override REST base URL (default: `https://api.binance.com`) |
| `BINANCE_STREAM_URL` | Override WebSocket URL (default: `wss://stream.binance.com:9443`) |
| `DB_PASSWORD` | MySQL password (default: empty) |

## Architecture

```
com.trading.ng
├── config/                        ← Spring @Configuration
│   ├── BinanceProperties          ← @ConfigurationProperties record (binance.*)
│   └── BinanceConfig              ← SpotRestApi + SpotWebSocketStreams beans
├── domain/                        ← JPA entities + enums
│   ├── TradingOrder               ← Order entity (trading_orders table)
│   ├── Trade                      ← Fill/trade entity (trades table)
│   └── OrderSide / OrderType / OrderStatus
├── dto/                           ← Java records for API request/response
│   ├── PlaceOrderRequest          ← Validated order placement DTO
│   ├── OrderResponse / PriceUpdate / AccountBalance
├── repository/                    ← Spring Data JPA interfaces
│   ├── TradingOrderRepository
│   └── TradeRepository
├── service/                       ← Business logic layer
│   ├── MarketDataStreamService    ← WebSocket streams → in-memory price cache
│   ├── OrderService               ← Place/cancel/query orders via Binance REST
│   └── PortfolioService           ← Account balances via Binance REST
└── controller/                    ← REST API (all under /api)
    ├── MarketDataController       ← GET /api/market/prices[/{symbol}]
    ├── OrderController            ← CRUD /api/orders
    └── PortfolioController        ← GET /api/portfolio/balances[/{asset}]
```

### Key Design Decisions

- **WebSocket-first market data:** `MarketDataStreamService` subscribes to Binance mini-ticker and trade streams on startup. Prices are cached in `ConcurrentHashMap` and served via REST. Virtual threads poll `StreamBlockingQueueWrapper` queues from the Binance SDK.
- **Binance SDK (binance-spot 8.x):** Uses auto-generated `SpotRestApi` for REST and `SpotWebSocketStreams` for market data. Configuration via `ClientConfiguration` / `SignatureConfiguration` / `WebSocketClientConfiguration`.
- **EUR pairs:** Default symbols are `BTCEUR`, `ETHEUR`, `BNBEUR` (configurable in `binance.symbols`).
- **Entities named to avoid SQL reserved words:** `TradingOrder` instead of `Order`.

## REST API Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/market/prices` | All cached prices |
| GET | `/api/market/prices/{symbol}` | Price for symbol |
| POST | `/api/orders` | Place new order |
| DELETE | `/api/orders/{symbol}/{orderId}` | Cancel order |
| GET | `/api/orders/{symbol}/{orderId}` | Get order status |
| GET | `/api/orders/open/{symbol}` | List open orders |
| GET | `/api/orders/history/{symbol}` | Order history |
| GET | `/api/portfolio/balances` | All non-zero balances |
| GET | `/api/portfolio/balances/{asset}` | Single asset balance |

## Conventions

- Base package: `com.trading.ng`
- DTOs are Java `record` types with Jakarta Validation annotations
- Configuration properties use `@ConfigurationProperties` records
- Tests use JUnit 5 + H2 in-memory DB (`src/test/resources/application.yml`)
- Controllers are thin — all business logic in services
