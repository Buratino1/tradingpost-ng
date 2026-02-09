package com.trading.ng.controller;

import com.trading.ng.dto.AccountBalance;
import com.trading.ng.service.PortfolioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/balances")
    public List<AccountBalance> getBalances() {
        return portfolioService.getBalances();
    }

    @GetMapping("/balances/{asset}")
    public AccountBalance getBalance(@PathVariable String asset) {
        return portfolioService.getBalance(asset);
    }
}
