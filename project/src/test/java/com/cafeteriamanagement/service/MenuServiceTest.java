
package com.cafeteriamanagement.service;
import com.cafeteriamanagement.model.entity.Ingredient;

import com.cafeteriamanagement.dto.MenuDTO;
import com.cafeteriamanagement.model.entity.Dish;
import com.cafeteriamanagement.model.entity.Menu;
import com.cafeteriamanagement.repository.MenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MenuServiceTest {
    @Mock
    private MenuRepository menuRepository;

    @Mock
    private DishService dishService;

    @InjectMocks
    private MenuService menuService;

    private Menu menu1;
    private Menu menu2;
    private Dish meatDish;
    private Dish fishDish;
    private Dish vegetarianDish;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        meatDish = mock(Dish.class);
        fishDish = mock(Dish.class);
        vegetarianDish = mock(Dish.class);
        
        com.cafeteriamanagement.model.valueobject.Name mockName = mock(com.cafeteriamanagement.model.valueobject.Name.class);
        when(mockName.getValue()).thenReturn("Test Dish");
        when(meatDish.getName()).thenReturn(mockName);
        when(fishDish.getName()).thenReturn(mockName);
        when(vegetarianDish.getName()).thenReturn(mockName);
        
        Ingredient meatIngredient = mock(Ingredient.class);
        when(meatIngredient.getType()).thenReturn(com.cafeteriamanagement.model.enums.IngredientType.MEAT);
        when(meatDish.getIngredients()).thenReturn(java.util.Collections.singletonList(meatIngredient));
        
        Ingredient fishIngredient = mock(Ingredient.class);
        when(fishIngredient.getType()).thenReturn(com.cafeteriamanagement.model.enums.IngredientType.FISH);
    when(fishDish.getIngredients()).thenReturn(java.util.Collections.singletonList(fishIngredient));
    
    when(vegetarianDish.getIngredients()).thenReturn(java.util.Collections.emptyList());
    
    LocalDate futureDate1 = LocalDate.now().plusYears(2);
    LocalDate futureDate2 = LocalDate.now().plusYears(2).plusDays(1);
    menu1 = new Menu(futureDate1, meatDish, fishDish, vegetarianDish);
    
    menu2 = new Menu(futureDate2, meatDish, fishDish, vegetarianDish);
    }

    @Test
    void testGetAllMenus() {
        when(menuRepository.findAll()).thenReturn(Arrays.asList(menu1, menu2));
        List<MenuDTO> result = menuService.getAllMenus();
        assertEquals(2, result.size());
        assertEquals(menu1.getDate(), result.get(0).getDate());
        assertEquals(menu2.getDate(), result.get(1).getDate());
    }

    
    @Test
    void testGetMenuByDate_MenuExists() {
        when(menuRepository.findByDate(menu1.getDate())).thenReturn(Optional.of(menu1));
        Optional<MenuDTO> result = menuService.getMenuByDate(menu1.getDate());
        assertTrue(result.isPresent());
        assertEquals(menu1.getDate(), result.get().getDate());
    }

    @Test
    void testGetMenuByDate_MenuNotFound() {
    LocalDate missingDate = LocalDate.now().plusYears(3);
    
    when(menuRepository.findByDate(missingDate)).thenReturn(Optional.empty());
    Optional<MenuDTO> result = menuService.getMenuByDate(missingDate);
    assertFalse(result.isPresent());
    }

    
    @Test
    void testFindByDate_MenuExists() {
        when(menuRepository.findByDate(menu1.getDate())).thenReturn(Optional.of(menu1));
        Menu result = menuService.findByDate(menu1.getDate());
        assertNotNull(result);
        assertEquals(menu1.getDate(), result.getDate());
    }

    @Test
    void testFindByDate_MenuNotFound() {
    LocalDate missingDate = LocalDate.now().plusYears(3);
    
    when(menuRepository.findByDate(missingDate)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> menuService.findByDate(missingDate));
    }
}
