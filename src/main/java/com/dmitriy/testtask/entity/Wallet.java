package com.dmitriy.testtask.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Wallet {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "balance", nullable = false)
    private BigDecimal amount;

}
