package com.cafeteriamanagement.model.entity;

import com.cafeteriamanagement.model.enums.Allergen;
import com.cafeteriamanagement.model.enums.IngredientType;
import com.cafeteriamanagement.model.valueobject.Name;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuTest {

    private final LocalDate future = LocalDate.now().plusYears(1);

    private Dish dishWith(String name, IngredientType type) {
        Ingredient ing = new Ingredient(new Name(name + "ing"), type, Allergen.NONE);
        return new Dish(new Name(name), List.of(ing), new BigDecimal("5.00"));
    }

    @Test
    void constructor_success_withValidDishes() {
        Menu menu = new Menu(future,
                dishWith("Beef", IngredientType.MEAT),
                dishWith("Cod", IngredientType.FISH),
                dishWith("Salad", IngredientType.VEGETABLES));
        assertNotNull(menu.getExternalId());
        assertEquals(future, menu.getDate());
        assertEquals("Beef", menu.getMeatDish().getName().getValue());
    }

    @Test
    void constructor_allowsNullDishes() {
        Menu menu = new Menu(future, null, null, null);
        assertNull(menu.getMeatDish());
        assertNull(menu.getFishDish());
        assertNull(menu.getVegetarianDish());
    }

    @Test
    void constructor_pastDate_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Menu(LocalDate.now().minusDays(1), null, null, null));
    }

    @Test
    void constructor_meatDishWithoutMeat_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Menu(future, dishWith("Fake", IngredientType.VEGETABLES), null, null));
    }

    @Test
    void constructor_fishDishWithoutFish_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Menu(future, null, dishWith("Fake", IngredientType.VEGETABLES), null));
    }

    @Test
    void constructor_vegetarianDishWithMeat_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Menu(future, null, null, dishWith("FakeVeg", IngredientType.MEAT)));
    }

    @Test
    void updateDetails_changesFields() {
        Menu menu = new Menu(future, null, null, null);
        LocalDate newDate = future.plusDays(1);
        menu.updateDetails(newDate, dishWith("Beef", IngredientType.MEAT), null, null);
        assertEquals(newDate, menu.getDate());
        assertEquals("Beef", menu.getMeatDish().getName().getValue());
    }

    @Test
    void equalsAndToString() {
        Menu menu = new Menu(future, null, null, null);
        assertEquals(menu, menu);
        assertNotEquals(menu, null);
        assertNotEquals(menu, "x");
        assertTrue(menu.toString().contains(future.toString()));
    }
}
