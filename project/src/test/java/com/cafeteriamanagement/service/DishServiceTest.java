package com.cafeteriamanagement.service;

import com.cafeteriamanagement.dto.DishDTO;
import com.cafeteriamanagement.model.entity.Dish;
import com.cafeteriamanagement.model.entity.Ingredient;
import com.cafeteriamanagement.model.enums.Allergen;
import com.cafeteriamanagement.model.enums.IngredientType;
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

class DishServiceTest {

    @Mock
    private DishRepository dishRepository;
    @Mock
    private IngredientService ingredientService;

    @InjectMocks
    private DishService dishService;

    private Dish dish1;
    private Dish dish2;
    private Ingredient tomato;
    private Ingredient chicken;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tomato = new Ingredient(new Name("Tomato"), IngredientType.VEGETABLES, Allergen.NONE);
        chicken = new Ingredient(new Name("Chicken"), IngredientType.MEAT, Allergen.NONE);
        dish1 = new Dish(new Name("Spaghetti"), Arrays.asList(tomato, chicken), new BigDecimal("8.50"));
        dish2 = new Dish(new Name("Salad"), Collections.singletonList(tomato), new BigDecimal("5.00"));
    }

    @Test
    void getAllDishes_returnsAll() {
        when(dishRepository.findAll()).thenReturn(Arrays.asList(dish1, dish2));
        List<DishDTO> result = dishService.getAllDishes();
        assertEquals(2, result.size());
        assertEquals("Spaghetti", result.get(0).getName());
        assertEquals(List.of("Tomato", "Chicken"), result.get(0).getIngredientNames());
    }

    @Test
    void getDishById_present() {
        when(dishRepository.findByExternalId("id1")).thenReturn(Optional.of(dish1));
        assertTrue(dishService.getDishById("id1").isPresent());
    }

    @Test
    void getDishById_notFound() {
        when(dishRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        assertFalse(dishService.getDishById("missing").isPresent());
    }

    @Test
    void createDish_success() {
        DishDTO dto = new DishDTO(null, "Pasta", List.of("Tomato", "Chicken"), new BigDecimal("9.00"));
        when(ingredientService.findByName("Tomato")).thenReturn(tomato);
        when(ingredientService.findByName("Chicken")).thenReturn(chicken);
        when(dishRepository.save(any(Dish.class))).thenAnswer(inv -> inv.getArgument(0));

        DishDTO result = dishService.createDish(dto);
        assertEquals("Pasta", result.getName());
        verify(dishRepository).save(any(Dish.class));
    }

    @Test
    void createDish_singleMissingIngredient_throws() {
        DishDTO dto = new DishDTO(null, "Pasta", List.of("Tomato", "Ghost"), new BigDecimal("9.00"));
        when(ingredientService.findByName("Tomato")).thenReturn(tomato);
        when(ingredientService.findByName("Ghost"))
                .thenThrow(new IllegalArgumentException("Ingredient not found with name: Ghost"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dishService.createDish(dto));
        assertTrue(ex.getMessage().contains("Ingredient not found: Ghost"));
        verify(dishRepository, never()).save(any());
    }

    @Test
    void createDish_multipleMissingIngredients_throws() {
        DishDTO dto = new DishDTO(null, "Pasta", List.of("Ghost", "Phantom"), new BigDecimal("9.00"));
        when(ingredientService.findByName("Ghost"))
                .thenThrow(new IllegalArgumentException("not found"));
        when(ingredientService.findByName("Phantom"))
                .thenThrow(new IllegalArgumentException("not found"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dishService.createDish(dto));
        assertTrue(ex.getMessage().contains("Ingredients not found"));
        assertTrue(ex.getMessage().contains("Ghost"));
        assertTrue(ex.getMessage().contains("Phantom"));
    }

    @Test
    void updateDish_success() {
        when(dishRepository.findByExternalId("id1")).thenReturn(Optional.of(dish1));
        when(ingredientService.findByName("Tomato")).thenReturn(tomato);
        when(dishRepository.save(any(Dish.class))).thenAnswer(inv -> inv.getArgument(0));

        DishDTO dto = new DishDTO(null, "Tomato Soup", List.of("Tomato"), new BigDecimal("6.00"));
        Optional<DishDTO> result = dishService.updateDish("id1", dto);
        assertTrue(result.isPresent());
        assertEquals("Tomato Soup", result.get().getName());
    }

    @Test
    void updateDish_notFound() {
        when(dishRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        DishDTO dto = new DishDTO(null, "Tomato Soup", List.of("Tomato"), new BigDecimal("6.00"));
        assertFalse(dishService.updateDish("missing", dto).isPresent());
    }

    @Test
    void deleteDish_success() {
        when(dishRepository.findByExternalId("id1")).thenReturn(Optional.of(dish1));
        assertTrue(dishService.deleteDish("id1"));
        verify(dishRepository).delete(dish1);
    }

    @Test
    void deleteDish_notFound() {
        when(dishRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        assertFalse(dishService.deleteDish("missing"));
    }

    @Test
    void findByExternalId_exists() {
        when(dishRepository.findByExternalId("id1")).thenReturn(Optional.of(dish1));
        assertEquals("Spaghetti", dishService.findByExternalId("id1").getName().getValue());
    }

    @Test
    void findByExternalId_notFound_throws() {
        when(dishRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> dishService.findByExternalId("missing"));
    }

    @Test
    void findByName_exists() {
        when(dishRepository.findByNameValue("Spaghetti")).thenReturn(Optional.of(dish1));
        assertEquals("Spaghetti", dishService.findByName("Spaghetti").getName().getValue());
    }

    @Test
    void findByName_notFound_throws() {
        when(dishRepository.findByNameValue("Pizza")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> dishService.findByName("Pizza"));
    }

    @Test
    void getDishesByAllergen_returnsFiltered() {
        Ingredient egg = new Ingredient(new Name("Egg"), IngredientType.EGGS, Allergen.EGGS);
        Dish eggDish = new Dish(new Name("Omelette"), List.of(egg), new BigDecimal("6.00"));
        when(dishRepository.findByIngredientsAllergen(Allergen.EGGS)).thenReturn(List.of(eggDish));

        List<DishDTO> result = dishService.getDishesByAllergen("EGGS");
        assertEquals(1, result.size());
        assertEquals("Omelette", result.get(0).getName());
    }

    @Test
    void getDishesByAllergen_invalidAllergen_throws() {
        assertThrows(IllegalArgumentException.class, () -> dishService.getDishesByAllergen("UNKNOWN_ALLERGEN"));
    }

    @Test
    void createDish_withDescription_setsDescription() {
        DishDTO dto = new DishDTO(null, "Pasta", List.of("Tomato"), new BigDecimal("9.00"));
        dto.setDescription("Classic tomato pasta");
        when(ingredientService.findByName("Tomato")).thenReturn(tomato);
        when(dishRepository.save(any(Dish.class))).thenAnswer(inv -> inv.getArgument(0));

        DishDTO result = dishService.createDish(dto);
        assertEquals("Classic tomato pasta", result.getDescription());
    }
}
