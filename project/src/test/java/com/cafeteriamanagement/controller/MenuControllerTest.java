package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.MenuDTO;
import com.cafeteriamanagement.service.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MenuControllerTest {
    @Mock
    private MenuService menuService;

    @InjectMocks
    private MenuController menuController;

    private MenuDTO menu1;
    private MenuDTO menu2;
    private LocalDate futureDate1;
    private LocalDate futureDate2;
    private String futureDate1String;
    private String futureDate2String;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    futureDate1 = LocalDate.now().plusDays(2);
    futureDate2 = futureDate1.plusDays(1);
    futureDate1String = futureDate1.toString();
    futureDate2String = futureDate2.toString();

    menu1 = new MenuDTO();
    menu1.setId("id1");
    menu1.setDate(futureDate1);

    menu2 = new MenuDTO();
    menu2.setId("id2");
    menu2.setDate(futureDate2);
    }

    @Test
    void testGetAllMenus() {
        when(menuService.getAllMenus()).thenReturn(Arrays.asList(menu1, menu2));
        ResponseEntity<List<MenuDTO>> response = menuController.getAllMenus();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        List<MenuDTO> result = response.getBody();
        assertEquals(2, result.size());
    }

    @Test
    void testGetMenuById() {
        when(menuService.getMenuById("id1")).thenReturn(Optional.of(menu1));
        ResponseEntity<MenuDTO> response = menuController.getMenuById("id1");
        assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(futureDate1, response.getBody().getDate());
    }

    @Test
    void testGetMenuById_NotFound() {
        when(menuService.getMenuById("missing")).thenReturn(Optional.empty());
        ResponseEntity<MenuDTO> response = menuController.getMenuById("missing");
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testGetMenuByDate() {
    when(menuService.getMenuByDate(futureDate1)).thenReturn(Optional.of(menu1));
    ResponseEntity<MenuDTO> response = menuController.getMenuByDate(futureDate1String);
        assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals("id1", response.getBody().getId());
    }

    @Test
    void testGetMenuByDate_NotFound() {
    LocalDate missingDate = futureDate2.plusDays(1);
    when(menuService.getMenuByDate(missingDate)).thenReturn(Optional.empty());
    ResponseEntity<MenuDTO> response = menuController.getMenuByDate(missingDate.toString());
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testCreateMenu() {
        when(menuService.createMenu(any(MenuDTO.class))).thenReturn(menu1);
        ResponseEntity<MenuDTO> response = menuController.createMenu(menu1);
        assertEquals(201, response.getStatusCodeValue());
    assertEquals(futureDate1, response.getBody().getDate());
    }

    @Test
    void testUpdateMenu() {
        when(menuService.updateMenu(eq("id1"), any(MenuDTO.class))).thenReturn(Optional.of(menu2));
        ResponseEntity<MenuDTO> response = menuController.updateMenu("id1", menu2);
        assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(futureDate2, response.getBody().getDate());
    }

    @Test
    void testUpdateMenu_NotFound() {
        when(menuService.updateMenu(eq("missing"), any(MenuDTO.class))).thenReturn(Optional.empty());
        ResponseEntity<MenuDTO> response = menuController.updateMenu("missing", menu2);
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testDeleteMenu() {
        when(menuService.deleteMenu("id1")).thenReturn(true);
        ResponseEntity<Void> response = menuController.deleteMenu("id1");
        assertEquals(204, response.getStatusCodeValue());
    }

    @Test
    void testDeleteMenu_NotFound() {
        when(menuService.deleteMenu("missing")).thenReturn(false);
        ResponseEntity<Void> response = menuController.deleteMenu("missing");
        assertEquals(404, response.getStatusCodeValue());
    }
}
