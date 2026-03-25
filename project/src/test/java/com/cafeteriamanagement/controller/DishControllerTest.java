package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.DishDTO;
import com.cafeteriamanagement.service.DishService;
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

public class DishControllerTest {
    @Mock
    private DishService dishService;

    @InjectMocks
    private DishController dishController;

    private DishDTO dish1;
    private DishDTO dish2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dish1 = new DishDTO();
        dish1.setId("id1");
        dish1.setName("Pasta");
        dish2 = new DishDTO();
        dish2.setId("id2");
        dish2.setName("Salad");
    }

    @Test
    void testGetAllDishes() {
        when(dishService.getAllDishes()).thenReturn(Arrays.asList(dish1, dish2));
        ResponseEntity<List<DishDTO>> response = dishController.getAllDishes();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        List<DishDTO> result = response.getBody();
        assertEquals(2, result.size());
    }

    @Test
    void testGetDishById() {
        when(dishService.getDishById("id1")).thenReturn(Optional.of(dish1));
        ResponseEntity<DishDTO> response = dishController.getDishById("id1");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("Pasta", response.getBody().getName());
    }

    @Test
    void testGetDishById_NotFound() {
        when(dishService.getDishById("missing")).thenReturn(Optional.empty());
        ResponseEntity<DishDTO> response = dishController.getDishById("missing");
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testCreateDish() {
        when(dishService.createDish(any(DishDTO.class))).thenReturn(dish1);
        ResponseEntity<DishDTO> response = dishController.createDish(dish1);
        assertEquals(201, response.getStatusCodeValue());
        assertEquals("Pasta", response.getBody().getName());
    }

    @Test
    void testUpdateDish() {
        when(dishService.updateDish(eq("id1"), any(DishDTO.class))).thenReturn(Optional.of(dish2));
        ResponseEntity<DishDTO> response = dishController.updateDish("id1", dish2);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("Salad", response.getBody().getName());
    }

    @Test
    void testUpdateDish_NotFound() {
        when(dishService.updateDish(eq("missing"), any(DishDTO.class))).thenReturn(Optional.empty());
        ResponseEntity<DishDTO> response = dishController.updateDish("missing", dish2);
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testDeleteDish() {
        when(dishService.deleteDish("id1")).thenReturn(true);
        ResponseEntity<Void> response = dishController.deleteDish("id1");
        assertEquals(204, response.getStatusCodeValue());
    }

    @Test
    void testDeleteDish_NotFound() {
        when(dishService.deleteDish("missing")).thenReturn(false);
        ResponseEntity<Void> response = dishController.deleteDish("missing");
        assertEquals(404, response.getStatusCodeValue());
    }
}
