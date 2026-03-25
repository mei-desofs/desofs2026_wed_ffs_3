package com.cafeteriamanagement.model.entity;

import com.cafeteriamanagement.model.enums.IngredientType;
import com.cafeteriamanagement.model.valueobject.Name;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "dishes")
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId;

    @Embedded
    @Valid
    @NotNull(message = "Name is required")
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "name", unique = true))
    })
    private Name name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "dish_ingredients",
        joinColumns = @JoinColumn(name = "dish_id"),
        inverseJoinColumns = @JoinColumn(name = "ingredient_id")
    )
    private List<Ingredient> ingredients = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    protected Dish() {}

    public Dish(Name name, List<Ingredient> ingredients, BigDecimal price) {
        this.externalId = UUID.randomUUID().toString();
        this.name = name;
        this.ingredients = new ArrayList<>(ingredients);
        this.price = price;
        validateVegetarianDish();
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public Name getName() {
        return name;
    }

    public List<Ingredient> getIngredients() {
        return new ArrayList<>(ingredients);
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isVegetarian() {
        return ingredients.stream()
                .noneMatch(ingredient -> 
                    ingredient.getType() == IngredientType.MEAT || 
                    ingredient.getType() == IngredientType.FISH);
    }

    public void updateDetails(Name name, List<Ingredient> ingredients, BigDecimal price) {
        this.name = name;
        this.ingredients = new ArrayList<>(ingredients);
        this.price = price;
        validateVegetarianDish();
    }

    private void validateVegetarianDish() {
        if (isVegetarian()) {
            for (Ingredient ingredient : ingredients) {
                if (ingredient.getType() == IngredientType.MEAT || ingredient.getType() == IngredientType.FISH) {
                    throw new IllegalArgumentException("Vegetarian dishes cannot contain meat or fish ingredients.");
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dish dish = (Dish) o;
        return Objects.equals(id, dish.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Dish{" +
                "externalId='" + externalId + '\'' +
                ", name=" + name +
                ", price=" + price +
                ", ingredientCount=" + ingredients.size() +
                '}';
    }
}