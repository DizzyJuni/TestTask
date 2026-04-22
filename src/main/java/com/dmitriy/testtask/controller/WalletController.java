package com.dmitriy.testtask.controller;

import com.dmitriy.testtask.dto.BalanceResponse;
import com.dmitriy.testtask.dto.OperationRequest;
import com.dmitriy.testtask.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Wallet API", description = "Управление кошелками")
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/wallet")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Выполнить операцию (пополнения/снятия)")
    public void processOperation(@Valid @RequestBody OperationRequest request) {
        walletService.processOperation(request);
    }

    @GetMapping("/wallets/{id}")
    @Operation(summary = "Получить баланс кошелька")
    public ResponseEntity<BalanceResponse> getBalanceById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(walletService.getBalance(id));
    }
}
