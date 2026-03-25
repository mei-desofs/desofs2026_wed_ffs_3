package com.cafeteriamanagement.service;


import com.cafeteriamanagement.model.entity.Ingredient;
import com.cafeteriamanagement.model.enums.IngredientType;
import com.cafeteriamanagement.model.enums.Allergen;
import com.cafeteriamanagement.model.valueobject.Name;
import com.cafeteriamanagement.repository.IngredientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IngredientServiceTest {
    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private IngredientService ingredientService;

    private Ingredient ingredient1;
    private Ingredient ingredient2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ingredient1 = new Ingredient(new Name("Tomato"), IngredientType.VEGETABLES, Allergen.NONE);
        ingredient2 = new Ingredient(new Name("Chicken"), IngredientType.MEAT, Allergen.NONE);
    }

    @Test
    void testFindAllIngredients() {
        when(ingredientRepository.findAll()).thenReturn(Arrays.asList(ingredient1, ingredient2));
        List<Ingredient> result = ingredientRepository.findAll();
        assertEquals(2, result.size());
        assertEquals("Tomato", result.get(0).getName().getValue());
        assertEquals("Chicken", result.get(1).getName().getValue());
    }

    @Test
    void testFindByName_IngredientExists() {
        when(ingredientRepository.findByNameValue("Tomato")).thenReturn(Optional.of(ingredient1));
        Optional<Ingredient> result = ingredientRepository.findByNameValue("Tomato");
        assertTrue(result.isPresent());
        assertEquals(IngredientType.VEGETABLES, result.get().getType());
    }

    @Test
    void testFindByName_IngredientNotFound() {
        when(ingredientRepository.findByNameValue("Potato")).thenReturn(Optional.empty());
        Optional<Ingredient> result = ingredientRepository.findByNameValue("Potato");
        assertFalse(result.isPresent());
    }
}
