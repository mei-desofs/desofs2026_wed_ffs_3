package com.cafeteriamanagement.service;

import com.cafeteriamanagement.dto.MenuDTO;
import com.cafeteriamanagement.model.entity.Dish;
import com.cafeteriamanagement.model.entity.Ingredient;
import com.cafeteriamanagement.model.entity.Menu;
import com.cafeteriamanagement.model.enums.IngredientType;
import com.cafeteriamanagement.model.valueobject.Name;
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

class MenuServiceTest {

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

        Name mockName = mock(Name.class);
        when(mockName.getValue()).thenReturn("Test Dish");
        when(meatDish.getName()).thenReturn(mockName);
        when(fishDish.getName()).thenReturn(mockName);
        when(vegetarianDish.getName()).thenReturn(mockName);

        Ingredient meatIngredient = mock(Ingredient.class);
        when(meatIngredient.getType()).thenReturn(IngredientType.MEAT);
        when(meatDish.getIngredients()).thenReturn(Collections.singletonList(meatIngredient));

        Ingredient fishIngredient = mock(Ingredient.class);
        when(fishIngredient.getType()).thenReturn(IngredientType.FISH);
        when(fishDish.getIngredients()).thenReturn(Collections.singletonList(fishIngredient));

        when(vegetarianDish.getIngredients()).thenReturn(Collections.emptyList());

        menu1 = new Menu(LocalDate.now().plusYears(2), meatDish, fishDish, vegetarianDish);
        menu2 = new Menu(LocalDate.now().plusYears(2).plusDays(1), meatDish, fishDish, vegetarianDish);
    }

    // ---- read ----

    @Test
    void getAllMenus_returnsAll() {
        when(menuRepository.findAll()).thenReturn(Arrays.asList(menu1, menu2));
        List<MenuDTO> result = menuService.getAllMenus();
        assertEquals(2, result.size());
        assertEquals(menu1.getDate(), result.get(0).getDate());
    }

    @Test
    void getMenuById_present() {
        when(menuRepository.findByExternalId("id1")).thenReturn(Optional.of(menu1));
        assertTrue(menuService.getMenuById("id1").isPresent());
    }

    @Test
    void getMenuById_notFound() {
        when(menuRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        assertFalse(menuService.getMenuById("missing").isPresent());
    }

    @Test
    void getMenuByDate_present() {
        when(menuRepository.findByDate(menu1.getDate())).thenReturn(Optional.of(menu1));
        assertTrue(menuService.getMenuByDate(menu1.getDate()).isPresent());
    }

    @Test
    void getMenuByDate_notFound() {
        LocalDate missing = LocalDate.now().plusYears(3);
        when(menuRepository.findByDate(missing)).thenReturn(Optional.empty());
        assertFalse(menuService.getMenuByDate(missing).isPresent());
    }

    @Test
    void findByDate_present() {
        when(menuRepository.findByDate(menu1.getDate())).thenReturn(Optional.of(menu1));
        assertEquals(menu1.getDate(), menuService.findByDate(menu1.getDate()).getDate());
    }

    @Test
    void findByDate_notFound_throws() {
        LocalDate missing = LocalDate.now().plusYears(3);
        when(menuRepository.findByDate(missing)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> menuService.findByDate(missing));
    }

    // ---- create ----

    @Test
    void createMenu_success() {
        LocalDate date = LocalDate.now().plusYears(2);
        MenuDTO dto = new MenuDTO(null, date, "Meat", null, null);
        when(menuRepository.existsByDate(date)).thenReturn(false);
        when(dishService.findByName("Meat")).thenReturn(meatDish);
        when(menuRepository.save(any(Menu.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuDTO result = menuService.createMenu(dto);
        assertEquals(date, result.getDate());
        assertEquals("Test Dish", result.getMeatDishName());
        verify(menuRepository).save(any(Menu.class));
    }

    @Test
    void createMenu_dateAlreadyExists_throws() {
        LocalDate date = LocalDate.now().plusYears(2);
        when(menuRepository.existsByDate(date)).thenReturn(true);
        MenuDTO dto = new MenuDTO(null, date, "Meat", null, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> menuService.createMenu(dto));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(menuRepository, never()).save(any());
    }

    @Test
    void createMenu_singleMissingDish_throws() {
        LocalDate date = LocalDate.now().plusYears(2);
        when(menuRepository.existsByDate(date)).thenReturn(false);
        when(dishService.findByName("Ghost")).thenThrow(new IllegalArgumentException("not found"));
        MenuDTO dto = new MenuDTO(null, date, "Ghost", null, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> menuService.createMenu(dto));
        assertTrue(ex.getMessage().contains("Dish not found: Ghost"));
    }

    @Test
    void createMenu_multipleMissingDishes_throws() {
        LocalDate date = LocalDate.now().plusYears(2);
        when(menuRepository.existsByDate(date)).thenReturn(false);
        when(dishService.findByName("Ghost")).thenThrow(new IllegalArgumentException("not found"));
        when(dishService.findByName("Phantom")).thenThrow(new IllegalArgumentException("not found"));
        MenuDTO dto = new MenuDTO(null, date, "Ghost", "Phantom", null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> menuService.createMenu(dto));
        assertTrue(ex.getMessage().contains("Dishes not found"));
    }

    // ---- update ----

    @Test
    void updateMenu_success() {
        when(menuRepository.findByExternalId("id1")).thenReturn(Optional.of(menu1));
        when(dishService.findByName("Meat")).thenReturn(meatDish);
        when(menuRepository.save(any(Menu.class))).thenAnswer(inv -> inv.getArgument(0));
        // same date -> no existsByDate conflict check
        MenuDTO dto = new MenuDTO(null, menu1.getDate(), "Meat", null, null);
        Optional<MenuDTO> result = menuService.updateMenu("id1", dto);
        assertTrue(result.isPresent());
        assertEquals("Test Dish", result.get().getMeatDishName());
    }

    @Test
    void updateMenu_notFound() {
        when(menuRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        MenuDTO dto = new MenuDTO(null, LocalDate.now().plusYears(2), "Meat", null, null);
        assertFalse(menuService.updateMenu("missing", dto).isPresent());
    }

    @Test
    void updateMenu_dateConflict_throws() {
        when(menuRepository.findByExternalId("id1")).thenReturn(Optional.of(menu1));
        LocalDate newDate = LocalDate.now().plusYears(5);
        when(menuRepository.existsByDate(newDate)).thenReturn(true);
        MenuDTO dto = new MenuDTO(null, newDate, "Meat", null, null);
        assertThrows(IllegalArgumentException.class, () -> menuService.updateMenu("id1", dto));
    }

    @Test
    void updateMenu_missingDish_throws() {
        when(menuRepository.findByExternalId("id1")).thenReturn(Optional.of(menu1));
        when(dishService.findByName("Ghost")).thenThrow(new IllegalArgumentException("not found"));
        MenuDTO dto = new MenuDTO(null, menu1.getDate(), "Ghost", null, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> menuService.updateMenu("id1", dto));
        assertTrue(ex.getMessage().contains("Dish not found: Ghost"));
    }

    // ---- delete ----

    @Test
    void deleteMenu_success() {
        when(menuRepository.findByExternalId("id1")).thenReturn(Optional.of(menu1));
        assertTrue(menuService.deleteMenu("id1"));
        verify(menuRepository).delete(menu1);
    }

    @Test
    void deleteMenu_notFound() {
        when(menuRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        assertFalse(menuService.deleteMenu("missing"));
    }

    // ---- isFutureDate ----

    @Test
    void isFutureDate_trueForFuture() {
        assertTrue(menuService.isFutureDate(LocalDate.now().plusDays(1)));
    }

    @Test
    void isFutureDate_falseForPastOrToday() {
        assertFalse(menuService.isFutureDate(LocalDate.now()));
        assertFalse(menuService.isFutureDate(LocalDate.now().minusDays(1)));
    }

    @Test
    void isFutureDate_falseForNull() {
        assertFalse(menuService.isFutureDate(null));
    }
}
