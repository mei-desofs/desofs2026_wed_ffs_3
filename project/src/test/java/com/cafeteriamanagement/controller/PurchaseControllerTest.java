package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.PurchaseDTO;
import com.cafeteriamanagement.service.PurchaseService;
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

    
public class PurchaseControllerTest {
    private org.mockito.MockedStatic<org.springframework.security.core.context.SecurityContextHolder> mockedStatic;
    @Mock
    private org.springframework.security.core.Authentication authentication;

    @Mock
    private org.springframework.security.core.context.SecurityContext securityContext;
    @Mock
    private PurchaseService purchaseService;

    @InjectMocks
    private PurchaseController purchaseController;

    private PurchaseDTO purchase1;
    private PurchaseDTO purchase2;
    private LocalDate futureDate1;
    private LocalDate futureDate2;
    private String futureDate1String;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (mockedStatic != null) {
            mockedStatic.close();
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        mockedStatic = org.mockito.Mockito.mockStatic(org.springframework.security.core.context.SecurityContextHolder.class);
        mockedStatic.when(org.springframework.security.core.context.SecurityContextHolder::getContext)
            .thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
    org.springframework.security.core.GrantedAuthority grantedAuthority = mock(org.springframework.security.core.GrantedAuthority.class);
    when(grantedAuthority.getAuthority()).thenReturn("ROLE_ADMIN");
    java.util.Collection authorities = java.util.Collections.singleton(grantedAuthority);
    when(authentication.getAuthorities()).thenReturn(authorities);
    futureDate1 = LocalDate.now().plusDays(2);
    futureDate2 = futureDate1.plusDays(1);
    futureDate1String = futureDate1.toString();

    purchase1 = new PurchaseDTO();
    purchase1.setId("id1");
    purchase1.setDate(futureDate1);

    purchase2 = new PurchaseDTO();
    purchase2.setId("id2");
    purchase2.setDate(futureDate2);
    }

    @Test
    void testGetAllPurchases() {
        when(purchaseService.getAllPurchases()).thenReturn(Arrays.asList(purchase1, purchase2));
        ResponseEntity<List<PurchaseDTO>> response = purchaseController.getAllPurchases();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        List<PurchaseDTO> result = response.getBody();
        assertEquals(2, result.size());
    }

    @Test
    void testGetPurchaseById() {
        when(purchaseService.getPurchaseById("id1")).thenReturn(Optional.of(purchase1));
        ResponseEntity<PurchaseDTO> response = purchaseController.getPurchaseById("id1");
        assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(futureDate1, response.getBody().getDate());
    }

    @Test
    void testGetPurchaseById_NotFound() {
        when(purchaseService.getPurchaseById("missing")).thenReturn(Optional.empty());
        ResponseEntity<PurchaseDTO> response = purchaseController.getPurchaseById("missing");
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testGetPurchasesByClient() {
        when(purchaseService.getPurchasesByClientId("client1")).thenReturn(Arrays.asList(purchase1));
        ResponseEntity<List<PurchaseDTO>> response = purchaseController.getPurchasesByClient("client1");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        List<PurchaseDTO> result = response.getBody();
        assertEquals(1, result.size());
    }

    @Test
    void testGetPurchasesByDate() {
    when(purchaseService.getPurchasesByDate(futureDate1)).thenReturn(Arrays.asList(purchase1));
    ResponseEntity<List<PurchaseDTO>> response = purchaseController.getPurchasesByDate(futureDate1String);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        List<PurchaseDTO> result = response.getBody();
        assertEquals(1, result.size());
    }

    @Test
    void testCreatePurchase() {
        when(purchaseService.createPurchase(any(PurchaseDTO.class))).thenReturn(purchase1);
        ResponseEntity<PurchaseDTO> response = purchaseController.createPurchase(purchase1);
        assertEquals(201, response.getStatusCodeValue());
    assertEquals(futureDate1, response.getBody().getDate());
    }

    @Test
    void testUpdatePurchase() {
        when(purchaseService.getPurchaseById("id1")).thenReturn(Optional.of(purchase1));
        when(purchaseService.updatePurchase(eq("id1"), any(PurchaseDTO.class))).thenReturn(Optional.of(purchase2));
        ResponseEntity<PurchaseDTO> response = purchaseController.updatePurchase("id1", purchase2);
        assertTrue(response.getStatusCode().is2xxSuccessful());
    assertEquals(futureDate2, response.getBody().getDate());
    }

    @Test
    void testUpdatePurchase_NotFound() {
        when(purchaseService.updatePurchase(eq("missing"), any(PurchaseDTO.class))).thenReturn(Optional.empty());
        ResponseEntity<PurchaseDTO> response = purchaseController.updatePurchase("missing", purchase2);
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testDeletePurchase() {
        when(purchaseService.getPurchaseById("id1")).thenReturn(Optional.of(purchase1));
        when(purchaseService.deletePurchase("id1")).thenReturn(true);
        ResponseEntity<Void> response = purchaseController.deletePurchase("id1");
        assertEquals(204, response.getStatusCodeValue());
    }

    @Test
    void testDeletePurchase_NotFound() {
        when(purchaseService.deletePurchase("missing")).thenReturn(false);
        ResponseEntity<Void> response = purchaseController.deletePurchase("missing");
        assertEquals(404, response.getStatusCodeValue());
    }
}
