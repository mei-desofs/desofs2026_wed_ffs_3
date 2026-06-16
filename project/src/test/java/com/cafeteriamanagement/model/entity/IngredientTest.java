package com.cafeteriamanagement.model.entity;

import com.cafeteriamanagement.model.enums.Allergen;
import com.cafeteriamanagement.model.enums.IngredientType;
import com.cafeteriamanagement.model.valueobject.Name;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngredientTest {

    @Test
    void constructor_setsFields() {
        Ingredient i = new Ingredient(new Name("Salmon"), IngredientType.FISH, Allergen.FISH);
        assertEquals("Salmon", i.getName().getValue());
        assertEquals(IngredientType.FISH, i.getType());
        assertEquals(Allergen.FISH, i.getAllergen());
        assertNotNull(i.getExternalId());
    }

    @Test
    void updateDetails_changesFields() {
        Ingredient i = new Ingredient(new Name("Salmon"), IngredientType.FISH, Allergen.FISH);
        i.updateDetails(new Name("Tofu"), IngredientType.LEGUMES, Allergen.SOYBEANS);
        assertEquals("Tofu", i.getName().getValue());
        assertEquals(IngredientType.LEGUMES, i.getType());
        assertEquals(Allergen.SOYBEANS, i.getAllergen());
    }

    @Test
    void equalsAndToString() {
        Ingredient i = new Ingredient(new Name("Salmon"), IngredientType.FISH, Allergen.FISH);
        assertEquals(i, i);
        assertNotEquals(i, null);
        assertNotEquals(i, "x");
        assertTrue(i.toString().contains("Salmon"));
        assertNotEquals(0, i.hashCode() + 1); // hashCode is callable
    }
}
