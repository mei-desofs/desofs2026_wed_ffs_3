package com.cafeteriamanagement.model.entity;

import com.cafeteriamanagement.model.enums.Allergen;
import com.cafeteriamanagement.model.enums.IngredientType;
import com.cafeteriamanagement.model.valueobject.Name;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DishTest {

    private final Ingredient meat = new Ingredient(new Name("Beef"), IngredientType.MEAT, Allergen.NONE);
    private final Ingredient veg = new Ingredient(new Name("Carrot"), IngredientType.VEGETABLES, Allergen.NONE);

    @Test
    void constructor_setsFields() {
        Dish dish = new Dish(new Name("Stew"), List.of(meat, veg), new BigDecimal("8.00"));
        assertEquals("Stew", dish.getName().getValue());
        assertEquals(new BigDecimal("8.00"), dish.getPrice());
        assertEquals(2, dish.getIngredients().size());
        assertNotNull(dish.getExternalId());
    }

    @Test
    void isVegetarian_trueWhenNoMeatOrFish() {
        Dish dish = new Dish(new Name("Salad"), List.of(veg), new BigDecimal("5.00"));
        assertTrue(dish.isVegetarian());
    }

    @Test
    void isVegetarian_falseWhenContainsMeat() {
        Dish dish = new Dish(new Name("Stew"), List.of(meat, veg), new BigDecimal("8.00"));
        assertFalse(dish.isVegetarian());
    }

    @Test
    void getIngredients_returnsDefensiveCopy() {
        Dish dish = new Dish(new Name("Salad"), List.of(veg), new BigDecimal("5.00"));
        dish.getIngredients().clear();
        assertEquals(1, dish.getIngredients().size(), "mutating the returned list must not affect the dish");
    }

    @Test
    void updateDetails_changesFields() {
        Dish dish = new Dish(new Name("Salad"), List.of(veg), new BigDecimal("5.00"));
        dish.updateDetails(new Name("Veggie Bowl"), List.of(veg), new BigDecimal("6.50"));
        assertEquals("Veggie Bowl", dish.getName().getValue());
        assertEquals(new BigDecimal("6.50"), dish.getPrice());
    }

    @Test
    void equalsAndToString() {
        Dish dish = new Dish(new Name("Salad"), List.of(veg), new BigDecimal("5.00"));
        assertEquals(dish, dish);
        assertNotEquals(dish, null);
        assertNotEquals(dish, "x");
        assertTrue(dish.toString().contains("Salad"));
    }
}
