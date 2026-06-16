package com.cafeteriamanagement.controller;

import com.cafeteriamanagement.dto.PurchaseDTO;
import com.cafeteriamanagement.model.enums.PurchaseStatus;
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
import java.util.Map;
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

    private void authenticateAsClient(String username) {
        when(authentication.getName()).thenReturn(username);
        org.springframework.security.core.GrantedAuthority clientAuthority =
            mock(org.springframework.security.core.GrantedAuthority.class);
        when(clientAuthority.getAuthority()).thenReturn("ROLE_CLIENT");
        java.util.Collection authorities = java.util.Collections.singleton(clientAuthority);
        when(authentication.getAuthorities()).thenReturn(authorities);
    }

    @Test
    void getPurchasesByClient_asClient_ownData_ok() {
        authenticateAsClient("client1");
        when(purchaseService.getUserExternalIdByUsername("client1")).thenReturn("ext-client1");
        when(purchaseService.getPurchasesByClientId("ext-client1")).thenReturn(Arrays.asList(purchase1));

        ResponseEntity<List<PurchaseDTO>> response = purchaseController.getPurchasesByClient("ext-client1");

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getPurchasesByClient_asClient_otherData_forbidden() {
        authenticateAsClient("client1");
        when(purchaseService.getUserExternalIdByUsername("client1")).thenReturn("ext-client1");

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
            () -> purchaseController.getPurchasesByClient("ext-other"));
        verify(purchaseService, never()).getPurchasesByClientId("ext-other");
    }

    @Test
    void createPurchase_asClient_forAnotherUser_forbidden() {
        authenticateAsClient("client1");
        PurchaseDTO payload = new PurchaseDTO();
        payload.setClientUsername("otheruser");
        payload.setDate(futureDate1);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
            () -> purchaseController.createPurchase(payload));
        verify(purchaseService, never()).createPurchase(any(PurchaseDTO.class));
    }

    @Test
    void updatePurchase_asClient_otherUsersPurchase_forbidden() {
        authenticateAsClient("client1");
        PurchaseDTO existing = new PurchaseDTO();
        existing.setId("id1");
        existing.setDate(futureDate1);
        existing.setClientUsername("otheruser");
        when(purchaseService.getPurchaseById("id1")).thenReturn(Optional.of(existing));

        PurchaseDTO payload = new PurchaseDTO();
        payload.setDate(futureDate1);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
            () -> purchaseController.updatePurchase("id1", payload));
        verify(purchaseService, never()).updatePurchase(eq("id1"), any(PurchaseDTO.class));
    }

    @Test
    void deletePurchase_asClient_otherUsersPurchase_forbidden() {
        authenticateAsClient("client1");
        PurchaseDTO existing = new PurchaseDTO();
        existing.setId("id1");
        existing.setDate(futureDate1);
        existing.setClientUsername("otheruser");
        when(purchaseService.getPurchaseById("id1")).thenReturn(Optional.of(existing));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
            () -> purchaseController.deletePurchase("id1"));
        verify(purchaseService, never()).deletePurchase("id1");
    }

    @Test
    void testUpdateStatus_adminConfirm() {
        purchase1.setClientUsername("admin");
        when(purchaseService.updateStatus("id1", PurchaseStatus.CONFIRMED)).thenReturn(Optional.of(purchase1));
        ResponseEntity<PurchaseDTO> response = purchaseController.updateStatus("id1", Map.of("status", "CONFIRMED"));
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void testUpdateStatus_adminCancel() {
        purchase1.setClientUsername("admin");
        when(purchaseService.updateStatus("id1", PurchaseStatus.CANCELLED)).thenReturn(Optional.of(purchase1));
        ResponseEntity<PurchaseDTO> response = purchaseController.updateStatus("id1", Map.of("status", "CANCELLED"));
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void testUpdateStatus_notFound() {
        when(purchaseService.updateStatus("missing", PurchaseStatus.CONFIRMED)).thenReturn(Optional.empty());
        ResponseEntity<PurchaseDTO> response = purchaseController.updateStatus("missing", Map.of("status", "CONFIRMED"));
        assertEquals(404, response.getStatusCodeValue());
    }
}
