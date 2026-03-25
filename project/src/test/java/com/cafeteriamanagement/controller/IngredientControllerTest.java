package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.IngredientDTO;
import com.cafeteriamanagement.service.IngredientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IngredientControllerTest {
    @Mock
    private IngredientService ingredientService;

    @InjectMocks
    private IngredientController ingredientController;

    private IngredientDTO ingredient1;
    private IngredientDTO ingredient2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ingredient1 = new IngredientDTO();
        ingredient1.setId("id1");
        ingredient1.setName("Tomato");
        ingredient2 = new IngredientDTO();
        ingredient2.setId("id2");
        ingredient2.setName("Cheese");
    }

    @Test
    void testGetAllIngredients() {
        when(ingredientService.getAllIngredients()).thenReturn(Arrays.asList(ingredient1, ingredient2));
        ResponseEntity<List<IngredientDTO>> response = ingredientController.getAllIngredients();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        List<IngredientDTO> result = response.getBody();
        assertEquals(2, result.size());
    }

    @Test
    void testGetIngredientById() {
        when(ingredientService.getIngredientById("id1")).thenReturn(Optional.of(ingredient1));
        ResponseEntity<IngredientDTO> response = ingredientController.getIngredientById("id1");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("Tomato", response.getBody().getName());
    }

    @Test
    void testGetIngredientById_NotFound() {
        when(ingredientService.getIngredientById("missing")).thenReturn(Optional.empty());
        ResponseEntity<IngredientDTO> response = ingredientController.getIngredientById("missing");
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testCreateIngredient() {
        when(ingredientService.createIngredient(any(IngredientDTO.class))).thenReturn(ingredient1);
        ResponseEntity<IngredientDTO> response = ingredientController.createIngredient(ingredient1);
        assertEquals(201, response.getStatusCodeValue());
        assertEquals("Tomato", response.getBody().getName());
    }

    @Test
    void testUpdateIngredient() {
        when(ingredientService.updateIngredient(eq("id1"), any(IngredientDTO.class))).thenReturn(Optional.of(ingredient2));
        ResponseEntity<IngredientDTO> response = ingredientController.updateIngredient("id1", ingredient2);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("Cheese", response.getBody().getName());
    }

    @Test
    void testUpdateIngredient_NotFound() {
        when(ingredientService.updateIngredient(eq("missing"), any(IngredientDTO.class))).thenReturn(Optional.empty());
        ResponseEntity<IngredientDTO> response = ingredientController.updateIngredient("missing", ingredient2);
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testDeleteIngredient() {
        when(ingredientService.deleteIngredient("id1")).thenReturn(true);
        ResponseEntity<Void> response = ingredientController.deleteIngredient("id1");
        assertEquals(204, response.getStatusCodeValue());
    }

    @Test
    void testDeleteIngredient_NotFound() {
        when(ingredientService.deleteIngredient("missing")).thenReturn(false);
        ResponseEntity<Void> response = ingredientController.deleteIngredient("missing");
        assertEquals(404, response.getStatusCodeValue());
    }
}
