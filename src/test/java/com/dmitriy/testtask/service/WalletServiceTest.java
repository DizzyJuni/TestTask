package com.dmitriy.testtask.service;

import com.dmitriy.testtask.dto.BalanceResponse;
import com.dmitriy.testtask.dto.OperationRequest;
import com.dmitriy.testtask.entity.Wallet;
import com.dmitriy.testtask.enums.OperationTypes;
import com.dmitriy.testtask.exceptions.InsufficientFundsException;
import com.dmitriy.testtask.exceptions.WalletNotFoundException;
import com.dmitriy.testtask.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setAmount(BigDecimal.valueOf(1000));
    }

    @Test
    void testDepositSuccess() {
        when(walletRepository.findWalletById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        OperationRequest request = new OperationRequest(walletId, OperationTypes.DEPOSIT,
                BigDecimal.valueOf(500));

        walletService.processOperation(request);
        assertThat(wallet.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void testWithdrawSuccess() {
        when(walletRepository.findWalletById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        OperationRequest request = new OperationRequest(walletId, OperationTypes.WITHDRAW,
                BigDecimal.valueOf(300));

        walletService.processOperation(request);

        assertThat(wallet.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(700));
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void testWithdrawFalse() {
        when(walletRepository.findWalletById(walletId)).thenReturn(Optional.of(wallet));

        OperationRequest request = new OperationRequest(walletId, OperationTypes.WITHDRAW,
                BigDecimal.valueOf(2000));

        assertThatThrownBy(() -> walletService.processOperation(request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void testNotFoundWallet() {
        when(walletRepository.findWalletById(any(UUID.class))).thenReturn(Optional.empty());

        OperationRequest request = new OperationRequest(walletId, OperationTypes.DEPOSIT,
                BigDecimal.valueOf(1000));

        assertThatThrownBy(() -> walletService.processOperation(request))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("Wallet not found");

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void testGetBalanceSuccess() {
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        BalanceResponse response = walletService.getBalance(walletId);
        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void testGetBalanceFalse() {
        when(walletRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getBalance(walletId))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }
}
