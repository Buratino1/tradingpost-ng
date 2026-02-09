package com.trading.ng.controller;

import com.trading.ng.dto.PriceUpdate;
import com.trading.ng.service.MarketDataStreamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/market")
public class MarketDataController {

    private final MarketDataStreamService marketDataService;

    public MarketDataController(MarketDataStreamService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/prices")
    public Collection<PriceUpdate> getAllPrices() {
        return marketDataService.getAllLatestPrices();
    }

    @GetMapping("/prices/{symbol}")
    public ResponseEntity<PriceUpdate> getPrice(@PathVariable String symbol) {
        PriceUpdate price = marketDataService.getLatestPrice(symbol);
        if (price == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(price);
    }
}
