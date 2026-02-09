package com.trading.ng.service;

import com.binance.connector.client.common.ApiException;
import com.binance.connector.client.common.ApiResponse;
import com.binance.connector.client.spot.rest.api.SpotRestApi;
import com.binance.connector.client.spot.rest.model.GetAccountResponse;
import com.trading.ng.dto.AccountBalance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final SpotRestApi spotRestApi;

    public PortfolioService(SpotRestApi spotRestApi) {
        this.spotRestApi = spotRestApi;
    }

    public List<AccountBalance> getBalances() {
        try {
            ApiResponse<GetAccountResponse> response = spotRestApi.getAccount(true, null);
            GetAccountResponse account = response.getData();

            return account.getBalances().stream()
                    .map(b -> new AccountBalance(
                            b.getAsset(),
                            new BigDecimal(b.getFree()),
                            new BigDecimal(b.getLocked())
                    ))
                    .filter(b -> b.free().compareTo(BigDecimal.ZERO) > 0
                            || b.locked().compareTo(BigDecimal.ZERO) > 0)
                    .toList();
        } catch (ApiException e) {
            log.error("Binance API error getting account: {}", e.getMessage());
            throw new RuntimeException("Failed to get account balances: " + e.getMessage(), e);
        }
    }

    public AccountBalance getBalance(String asset) {
        return getBalances().stream()
                .filter(b -> b.asset().equalsIgnoreCase(asset))
                .findFirst()
                .orElse(new AccountBalance(asset.toUpperCase(), BigDecimal.ZERO, BigDecimal.ZERO));
    }
}
