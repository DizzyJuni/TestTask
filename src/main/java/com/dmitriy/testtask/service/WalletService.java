package com.dmitriy.testtask.service;

import com.dmitriy.testtask.dto.BalanceResponse;
import com.dmitriy.testtask.dto.OperationRequest;
import com.dmitriy.testtask.dto.WalletRequest;
import com.dmitriy.testtask.entity.Wallet;
import com.dmitriy.testtask.enums.OperationTypes;
import com.dmitriy.testtask.exceptions.InsufficientFundsException;
import com.dmitriy.testtask.exceptions.WalletNotFoundException;
import com.dmitriy.testtask.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void processOperation(OperationRequest operationRequest) {
        UUID walletId = operationRequest.id();
        OperationTypes type = operationRequest.operationTypes();
        BigDecimal amount = operationRequest.amount();

        log.info("Processing {} of {} for wallet {}", type, amount, walletId);

        Wallet wallet = walletRepository.findWalletById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found " + walletId));

        BigDecimal currentBalance = wallet.getAmount();
        BigDecimal newBalance;

        if (type == OperationTypes.DEPOSIT) {
            newBalance = currentBalance.add(amount);
        } else {
            if (currentBalance.compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient funds. Current: " + currentBalance + ", Requested: " + amount
                );
            }
            newBalance = currentBalance.subtract(amount);
        }

        wallet.setAmount(newBalance);
        walletRepository.save(wallet);

        log.info("Operation completed. New balance for wallet {}: {}", walletId, newBalance);
    }

    public void createWallet(WalletRequest request) {
        Wallet wallet = new Wallet();

        wallet.setAmount(request.amount() != null
                ? request.amount()
                : BigDecimal.ZERO);
        Wallet saved = walletRepository.save(wallet);
        log.info("Created wallet with id: {}, balance: {}", saved.getId(), saved.getAmount());
    }

    public BalanceResponse getBalance(UUID id) {
        log.info("Try get balance wallet by id {}", id);

        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        log.info("Get balance succussed.");
        return new BalanceResponse(wallet.getAmount());
    }
}
