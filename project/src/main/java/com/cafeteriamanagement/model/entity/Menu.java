package com.cafeteriamanagement.model.entity;

import com.cafeteriamanagement.model.enums.IngredientType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "menus")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId;

    @Column(nullable = false, unique = true)
    @NotNull(message = "Date is required")
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meat_dish_id")
    private Dish meatDish;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fish_dish_id")
    private Dish fishDish;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vegetarian_dish_id")
    private Dish vegetarianDish;

    protected Menu() {}

    public Menu(LocalDate date, Dish meatDish, Dish fishDish, Dish vegetarianDish) {
        this.externalId = UUID.randomUUID().toString();
        this.date = date;
        this.meatDish = meatDish;
        this.fishDish = fishDish;
        this.vegetarianDish = vegetarianDish;
        
        validateFutureDate();
        
        validateMeatDish();
        validateFishDish();
        validateVegetarianDish();
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public LocalDate getDate() {
        return date;
    }

    public Dish getMeatDish() {
        return meatDish;
    }

    public Dish getFishDish() {
        return fishDish;
    }

    public Dish getVegetarianDish() {
        return vegetarianDish;
    }

    public void updateDetails(LocalDate date, Dish meatDish, Dish fishDish, Dish vegetarianDish) {
        this.date = date;
        this.meatDish = meatDish;
        this.fishDish = fishDish;
        this.vegetarianDish = vegetarianDish;
        
        validateFutureDate();
        
        validateMeatDish();
        validateFishDish();
        validateVegetarianDish();
    }

    private void validateFutureDate() {
        if (date != null && !date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Menu date must be in the future");
        }
    }
    
    private void validateMeatDish() {
        if (meatDish != null) {
            boolean hasMeatOrEggs = meatDish.getIngredients().stream()
                    .anyMatch(ingredient -> 
                        ingredient.getType() == IngredientType.MEAT || 
                        ingredient.getType() == IngredientType.EGGS
                    );
            if (!hasMeatOrEggs) {
                throw new IllegalArgumentException("Meat dish must contain at least one MEAT or EGGS ingredient");
            }
        }
    }
    
    private void validateFishDish() {
        if (fishDish != null) {
            boolean hasFish = fishDish.getIngredients().stream()
                    .anyMatch(ingredient -> ingredient.getType() == IngredientType.FISH);
            if (!hasFish) {
                throw new IllegalArgumentException("Fish dish must contain at least one FISH ingredient");
            }
        }
    }

    private void validateVegetarianDish() {
        if (vegetarianDish != null) {
            boolean hasMeatOrFish = vegetarianDish.getIngredients().stream()
                    .anyMatch(ingredient -> 
                        ingredient.getType() == IngredientType.MEAT || 
                        ingredient.getType() == IngredientType.FISH
                    );
            if (hasMeatOrFish) {
                throw new IllegalArgumentException("Vegetarian dish cannot contain MEAT or FISH ingredients");
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Menu menu = (Menu) o;
        return Objects.equals(id, menu.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Menu{" +
                "externalId='" + externalId + '\'' +
                ", date=" + date +
                '}';
    }
}