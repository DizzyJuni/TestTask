package com.dmitriy.testtask.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record WalletRequest(
        @NotNull @PositiveOrZero BigDecimal amount) {
}
