package com.dmitriy.testtask;

import com.dmitriy.testtask.dto.BalanceResponse;
import com.dmitriy.testtask.dto.ErrorResponse;
import com.dmitriy.testtask.dto.OperationRequest;
import com.dmitriy.testtask.entity.Wallet;
import com.dmitriy.testtask.enums.OperationTypes;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class WalletControllerIntegrationTest {

    private UUID id;

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testdb")
            .withPassword("testdb");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WalletRepository repository;


    @BeforeEach
    void setUp() {
        Wallet wallet = new Wallet();
        wallet.setAmount(BigDecimal.valueOf(1000));
        Wallet saved = repository.save(wallet);
        id = saved.getId();
    }

    @Test
    void testPostDepositSuccess() {
        OperationRequest request = new OperationRequest(id, OperationTypes.DEPOSIT,
                BigDecimal.valueOf(500));

        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/wallet", request,
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        BalanceResponse balance = restTemplate.getForObject("/api/v1/wallets/" + id, BalanceResponse.class);
        assertThat(balance.balance()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }

    @Test
    void testPostWithdrawSuccess() {
        OperationRequest request = new OperationRequest(id, OperationTypes.WITHDRAW,
                BigDecimal.valueOf(300));

        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/wallet", request,
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        BalanceResponse balance = restTemplate.getForObject("/api/v1/wallets/" + id, BalanceResponse.class);
        assertThat(balance.balance()).isEqualByComparingTo(BigDecimal.valueOf(700));
    }

    @Test
    void testGetBalanceSuccess() {
        ResponseEntity<BalanceResponse> response = restTemplate.getForEntity("/api/v1/wallets/" + id,
                BalanceResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertNotNull(response.getBody());
        BalanceResponse balance = restTemplate.getForObject("/api/v1/wallets/" + id, BalanceResponse.class);
        assertThat(balance.balance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void testGetBalanceFalse() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity("/api/v1/wallets/" + UUID.randomUUID(),
                ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertNotNull(response.getBody());
        assertThat(response.getBody().message()).contains("Wallet not found");
    }

    @Test
    void testPostWithdrawFalse() {
        OperationRequest request = new OperationRequest(id, OperationTypes.WITHDRAW,
                BigDecimal.valueOf(2000));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity("/api/v1/wallet", request,
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertNotNull(response.getBody());
        assertThat(response.getBody().message()).contains("Insufficient funds");

        BalanceResponse balance = restTemplate.getForObject("/api/v1/wallets/" + id, BalanceResponse.class);
        assertThat(balance.balance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void testWalletNotFound() {
        OperationRequest request = new OperationRequest(UUID.randomUUID(), OperationTypes.DEPOSIT,
                BigDecimal.valueOf(1000));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity("/api/v1/wallet", request,
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertNotNull(response.getBody());
        assertThat(response.getBody().message()).contains("Wallet not found");

        BalanceResponse balance = restTemplate.getForObject("/api/v1/wallets/" + id, BalanceResponse.class);
        assertThat(balance.balance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void testInvalidJson()  {
        String invalidJson = "{\"id\": \"123\", \"operationType\" \"DEPOSIT\", \"amount\": }";
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity("/api/v1/wallet", invalidJson,
                ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testInvalidUUID() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity("/api/v1/wallets/not_uuid",
                ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testAmountNotPositive() {
        OperationRequest request = new OperationRequest(id, OperationTypes.DEPOSIT,
                BigDecimal.valueOf(-1000));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity("/api/v1/wallet", request,
                ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertNotNull(response.getBody());
        assertThat(response.getBody().message()).contains("Invalid request data");
    }
}
