
package com.cafeteriamanagement.service;

import com.cafeteriamanagement.model.entity.Dish;
import com.cafeteriamanagement.model.entity.Ingredient;
import com.cafeteriamanagement.model.enums.IngredientType;
import com.cafeteriamanagement.model.enums.Allergen;
import com.cafeteriamanagement.model.valueobject.Name;
import com.cafeteriamanagement.repository.DishRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

    
public class DishServiceTest {
    @Mock
    private DishRepository dishRepository;

    @Mock
    private IngredientService ingredientService;

    @InjectMocks
    private DishService dishService;

    private Dish dish1;
    private Dish dish2;
    private Ingredient ingredient1;
    private Ingredient ingredient2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ingredient1 = new Ingredient(new Name("Tomato"), IngredientType.VEGETABLES, Allergen.NONE);
        ingredient2 = new Ingredient(new Name("Chicken"), IngredientType.MEAT, Allergen.NONE);
        dish1 = new Dish(new Name("Spaghetti"), Arrays.asList(ingredient1, ingredient2), new BigDecimal("8.50"));
        dish2 = new Dish(new Name("Salad"), Collections.singletonList(ingredient1), new BigDecimal("5.00"));
    }

    
    @Test
    void testGetAllDishes() {
        when(dishRepository.findAll()).thenReturn(Arrays.asList(dish1, dish2));
        List<Dish> result = dishRepository.findAll();
        assertEquals(2, result.size());
        assertEquals("Spaghetti", result.get(0).getName().getValue());
        assertEquals("Salad", result.get(1).getName().getValue());
    }

    
    @Test
    void testFindByExternalId_DishExists() {
        when(dishRepository.findByExternalId("id123")).thenReturn(Optional.of(dish1));
        Dish result = dishService.findByExternalId("id123");
        assertNotNull(result);
        assertEquals("Spaghetti", result.getName().getValue());
    }

    @Test
    void testFindByExternalId_DishNotFound() {
        when(dishRepository.findByExternalId("id999")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> dishService.findByExternalId("id999"));
    }

    
    @Test
    void testFindByName_DishExists() {
        when(dishRepository.findByNameValue("Spaghetti")).thenReturn(Optional.of(dish1));
        Dish result = dishService.findByName("Spaghetti");
        assertNotNull(result);
        assertEquals("Spaghetti", result.getName().getValue());
    }

    @Test
    void testFindByName_DishNotFound() {
        when(dishRepository.findByNameValue("Pizza")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> dishService.findByName("Pizza"));
    }
}
