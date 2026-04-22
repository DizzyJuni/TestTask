package com.dmitriy.testtask.dto;

import com.dmitriy.testtask.enums.OperationTypes;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record OperationRequest(
        @NotNull UUID id,
        @NotNull OperationTypes operationTypes,
        @NotNull @Positive BigDecimal amount
) {
}
