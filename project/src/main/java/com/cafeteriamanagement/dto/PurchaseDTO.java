package com.cafeteriamanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Client purchase request/response payload")
public class PurchaseDTO {

    @Schema(description = "External identifier for the purchase", example = "550e8400-e29b-41d4-a716-446655440030", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @NotNull(message = "Client username is required")
    @Schema(description = "Username of the client making the purchase", example = "jane_doe")
    private String clientUsername;

    @NotNull(message = "Dish name is required")
    @Schema(description = "Name of the dish selected by the client", example = "Veggie Sandwich")
    private String dishName;

    @NotNull(message = "Date is required")
    @Schema(description = "Date the purchase applies to. Must be in the future", example = "2024-06-01")
    private LocalDate date;

    public PurchaseDTO() {}

    public PurchaseDTO(String id, String clientUsername, String dishName, LocalDate date) {
        this.id = id;
        this.clientUsername = clientUsername;
        this.dishName = dishName;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public void setClientUsername(String clientUsername) {
        this.clientUsername = clientUsername;
    }

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}