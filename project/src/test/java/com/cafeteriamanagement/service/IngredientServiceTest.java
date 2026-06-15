package com.cafeteriamanagement.service;

import com.cafeteriamanagement.dto.IngredientDTO;
import com.cafeteriamanagement.model.entity.Ingredient;
import com.cafeteriamanagement.model.enums.Allergen;
import com.cafeteriamanagement.model.enums.IngredientType;
import com.cafeteriamanagement.model.valueobject.Name;
import com.cafeteriamanagement.repository.IngredientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private ModelMapper modelMapper;

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
    void getAllIngredients_returnsAll() {
        when(ingredientRepository.findAll()).thenReturn(Arrays.asList(ingredient1, ingredient2));
        List<IngredientDTO> result = ingredientService.getAllIngredients();
        assertEquals(2, result.size());
        assertEquals("Tomato", result.get(0).getName());
    }

    @Test
    void getIngredientById_present() {
        when(ingredientRepository.findByExternalId("id1")).thenReturn(Optional.of(ingredient1));
        Optional<IngredientDTO> result = ingredientService.getIngredientById("id1");
        assertTrue(result.isPresent());
        assertEquals(IngredientType.VEGETABLES, result.get().getType());
    }

    @Test
    void getIngredientById_notFound() {
        when(ingredientRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        assertFalse(ingredientService.getIngredientById("missing").isPresent());
    }

    @Test
    void createIngredient_success() {
        IngredientDTO dto = new IngredientDTO(null, "Onion", IngredientType.VEGETABLES, Allergen.NONE);
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(inv -> inv.getArgument(0));
        IngredientDTO result = ingredientService.createIngredient(dto);
        assertEquals("Onion", result.getName());
        verify(ingredientRepository).save(any(Ingredient.class));
    }

    @Test
    void updateIngredient_success() {
        when(ingredientRepository.findByExternalId("id1")).thenReturn(Optional.of(ingredient1));
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(inv -> inv.getArgument(0));
        IngredientDTO dto = new IngredientDTO(null, "Garlic", IngredientType.VEGETABLES, Allergen.NONE);
        Optional<IngredientDTO> result = ingredientService.updateIngredient("id1", dto);
        assertTrue(result.isPresent());
        assertEquals("Garlic", result.get().getName());
    }

    @Test
    void updateIngredient_notFound() {
        when(ingredientRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        IngredientDTO dto = new IngredientDTO(null, "Garlic", IngredientType.VEGETABLES, Allergen.NONE);
        assertFalse(ingredientService.updateIngredient("missing", dto).isPresent());
    }

    @Test
    void deleteIngredient_success() {
        when(ingredientRepository.findByExternalId("id1")).thenReturn(Optional.of(ingredient1));
        assertTrue(ingredientService.deleteIngredient("id1"));
        verify(ingredientRepository).delete(ingredient1);
    }

    @Test
    void deleteIngredient_notFound() {
        when(ingredientRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        assertFalse(ingredientService.deleteIngredient("missing"));
    }

    @Test
    void findByExternalId_exists() {
        when(ingredientRepository.findByExternalId("id1")).thenReturn(Optional.of(ingredient1));
        assertEquals("Tomato", ingredientService.findByExternalId("id1").getName().getValue());
    }

    @Test
    void findByExternalId_notFound_throws() {
        when(ingredientRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> ingredientService.findByExternalId("missing"));
    }

    @Test
    void findByName_exists() {
        when(ingredientRepository.findByNameValue("Tomato")).thenReturn(Optional.of(ingredient1));
        assertEquals(IngredientType.VEGETABLES, ingredientService.findByName("Tomato").getType());
    }

    @Test
    void findByName_notFound_throws() {
        when(ingredientRepository.findByNameValue("Potato")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> ingredientService.findByName("Potato"));
    }
}
