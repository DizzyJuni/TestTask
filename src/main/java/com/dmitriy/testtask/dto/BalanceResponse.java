package com.dmitriy.testtask.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        BigDecimal balance
) {
}
