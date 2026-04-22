package com.dmitriy.testtask;

import com.dmitriy.testtask.dto.OperationRequest;
import com.dmitriy.testtask.enums.OperationTypes;
import com.dmitriy.testtask.entity.Wallet;
import com.dmitriy.testtask.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class WalletConcurrencyTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WalletRepository walletRepository;

    private UUID walletId;
    private String depositUrl;

    @BeforeEach
    void setUp() {
        depositUrl = "/api/v1/wallet";

        Wallet wallet = new Wallet();
        wallet.setAmount(BigDecimal.ZERO);
        Wallet savedWallet = walletRepository.saveAndFlush(wallet);

        this.walletId = savedWallet.getId();
    }

    @Test
    @SuppressWarnings("resource")
    void testConcurrentDeposits() throws InterruptedException {
        int threadsCount = 200;
        int amountPerThread = 100;
        BigDecimal expectedTotal = BigDecimal.valueOf(threadsCount * amountPerThread);

        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch latch = new CountDownLatch(threadsCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadsCount; i++) {
            executor.submit(() -> {
                try {
                    OperationRequest request = new OperationRequest(walletId, OperationTypes.DEPOSIT, BigDecimal.valueOf(amountPerThread));
                    ResponseEntity<Void> response = restTemplate.postForEntity(depositUrl, request, Void.class);

                    if (response.getStatusCode() == HttpStatus.OK) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // Ждём все потоки
        executor.shutdown();

        // Проверяем, что все запросы успешны
        assertThat(errorCount.get()).isEqualTo(0);
        assertThat(successCount.get()).isEqualTo(threadsCount);

        // Проверяем финальный баланс
        Wallet finalWallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(finalWallet.getAmount()).isEqualByComparingTo(expectedTotal);
    }
}