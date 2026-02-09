package com.trading.ng.domain;

public enum OrderStatus {
    NEW,
    PARTIALLY_FILLED,
    FILLED,
    CANCELED,
    PENDING_CANCEL,
    REJECTED,
    EXPIRED
}
