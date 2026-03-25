package com.cafeteriamanagement.service;

import com.cafeteriamanagement.dto.PurchaseDTO;
import com.cafeteriamanagement.model.entity.Purchase;
import com.cafeteriamanagement.repository.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PurchaseServiceTest {
    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private PurchaseService purchaseService;

    private Purchase purchase1;
    private Purchase purchase2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        purchase1 = mock(Purchase.class);
        purchase2 = mock(Purchase.class);
        com.cafeteriamanagement.model.entity.User user1 = mock(com.cafeteriamanagement.model.entity.User.class);
        com.cafeteriamanagement.model.entity.User user2 = mock(com.cafeteriamanagement.model.entity.User.class);
        when(user1.getUsername()).thenReturn("client1");
        when(user2.getUsername()).thenReturn("client2");
        com.cafeteriamanagement.model.entity.Dish dish1 = mock(com.cafeteriamanagement.model.entity.Dish.class);
        com.cafeteriamanagement.model.entity.Dish dish2 = mock(com.cafeteriamanagement.model.entity.Dish.class);
        com.cafeteriamanagement.model.valueobject.Name name1 = mock(com.cafeteriamanagement.model.valueobject.Name.class);
        com.cafeteriamanagement.model.valueobject.Name name2 = mock(com.cafeteriamanagement.model.valueobject.Name.class);
        when(name1.getValue()).thenReturn("Dish 1");
        when(name2.getValue()).thenReturn("Dish 2");
        when(dish1.getName()).thenReturn(name1);
        when(dish2.getName()).thenReturn(name2);
        when(purchase1.getClient()).thenReturn(user1);
        when(purchase2.getClient()).thenReturn(user2);
        when(purchase1.getDish()).thenReturn(dish1);
        when(purchase2.getDish()).thenReturn(dish2);
        when(purchase1.getDate()).thenReturn(LocalDate.now().plusDays(1));
        when(purchase2.getDate()).thenReturn(LocalDate.now().plusDays(2));
        when(purchase1.getExternalId()).thenReturn("id1");
        when(purchase2.getExternalId()).thenReturn("id2");
    }

    @Test
    void testGetAllPurchases() {
        when(purchaseRepository.findAll()).thenReturn(Arrays.asList(purchase1, purchase2));
        List<PurchaseDTO> result = purchaseService.getAllPurchases();
        assertEquals(2, result.size());
    }

    @Test
    void testGetPurchaseById() {
        when(purchaseRepository.findByExternalId("id1")).thenReturn(Optional.of(purchase1));
        Optional<PurchaseDTO> result = purchaseService.getPurchaseById("id1");
        assertTrue(result.isPresent());
    }

    @Test
    void testGetPurchaseById_NotFound() {
        when(purchaseRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        Optional<PurchaseDTO> result = purchaseService.getPurchaseById("missing");
        assertFalse(result.isPresent());
    }
}
