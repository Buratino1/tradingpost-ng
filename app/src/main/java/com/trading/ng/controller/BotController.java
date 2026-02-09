package com.trading.ng.controller;

import com.trading.ng.dto.BotConfigRequest;
import com.trading.ng.dto.BotStatusResponse;
import com.trading.ng.service.TradingBotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bot")
public class BotController {

    private final TradingBotService botService;

    public BotController(TradingBotService botService) {
        this.botService = botService;
    }

    @PostMapping("/start")
    public ResponseEntity<BotStatusResponse> start() {
        botService.start();
        return ResponseEntity.ok(botService.getStatus());
    }

    @PostMapping("/stop")
    public ResponseEntity<BotStatusResponse> stop() {
        botService.stop();
        return ResponseEntity.ok(botService.getStatus());
    }

    @GetMapping("/status")
    public BotStatusResponse status() {
        return botService.getStatus();
    }

    @PutMapping("/config")
    public ResponseEntity<BotStatusResponse> updateConfig(
            @Valid @RequestBody BotConfigRequest request) {
        botService.updateConfig(request);
        return ResponseEntity.ok(botService.getStatus());
    }
}
