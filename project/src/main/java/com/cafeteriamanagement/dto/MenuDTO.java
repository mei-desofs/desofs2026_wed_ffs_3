package com.cafeteriamanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Daily menu configuration")
public class MenuDTO {

    @Schema(description = "External identifier for the menu", example = "550e8400-e29b-41d4-a716-446655440020", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @NotNull(message = "Date is required")
    @Schema(description = "Date the menu applies to", example = "2024-05-21")
    private LocalDate date;

    @Schema(description = "Name of the meat-based dish for the day", example = "Grilled Chicken")
    private String meatDishName;

    @Schema(description = "Name of the fish-based dish for the day", example = "Baked Salmon")
    private String fishDishName;

    @Schema(description = "Name of the vegetarian dish for the day", example = "Veggie Pasta")
    private String vegetarianDishName;

    public MenuDTO() {}

    public MenuDTO(String id, LocalDate date, String meatDishName, String fishDishName, String vegetarianDishName) {
        this.id = id;
        this.date = date;
        this.meatDishName = meatDishName;
        this.fishDishName = fishDishName;
        this.vegetarianDishName = vegetarianDishName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getMeatDishName() {
        return meatDishName;
    }

    public void setMeatDishName(String meatDishName) {
        this.meatDishName = meatDishName;
    }

    public String getFishDishName() {
        return fishDishName;
    }

    public void setFishDishName(String fishDishName) {
        this.fishDishName = fishDishName;
    }

    public String getVegetarianDishName() {
        return vegetarianDishName;
    }

    public void setVegetarianDishName(String vegetarianDishName) {
        this.vegetarianDishName = vegetarianDishName;
    }
}