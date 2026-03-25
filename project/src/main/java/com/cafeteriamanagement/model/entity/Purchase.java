package com.cafeteriamanagement.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @NotNull(message = "Client is required")
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    @NotNull(message = "Dish is required")
    private Dish dish;

    @Column(nullable = false)
    @NotNull(message = "Date is required")
    private LocalDate date;

    protected Purchase() {}

    public Purchase(User client, Dish dish, LocalDate date) {
        validatePurchase(client, dish, date);
        this.externalId = UUID.randomUUID().toString();
        this.client = client;
        this.dish = dish;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public User getClient() {
        return client;
    }

    public Dish getDish() {
        return dish;
    }

    public LocalDate getDate() {
        return date;
    }

    public void updateDetails(User client, Dish dish, LocalDate date) {
        validatePurchase(client, dish, date);
        this.client = client;
        this.dish = dish;
        this.date = date;
    }

    private void validatePurchase(User client, Dish dish, LocalDate date) {
        if (date == null || !date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Purchase date must be in the future");
        }
        if (client == null || !client.hasEnoughBalance(dish.getPrice())) {
            throw new IllegalArgumentException("Client does not have enough balance");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Purchase purchase = (Purchase) o;
        return Objects.equals(id, purchase.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Purchase{" +
                "externalId='" + externalId + '\'' +
                ", client=" + client.getUsername() +
                ", dish=" + dish.getName() +
                ", date=" + date +
                '}';
    }
}