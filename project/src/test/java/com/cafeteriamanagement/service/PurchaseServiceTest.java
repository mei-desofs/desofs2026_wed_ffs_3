package com.cafeteriamanagement.service;

import com.cafeteriamanagement.dto.PurchaseDTO;
import com.cafeteriamanagement.model.entity.Dish;
import com.cafeteriamanagement.model.entity.Menu;
import com.cafeteriamanagement.model.entity.Purchase;
import com.cafeteriamanagement.model.entity.User;
import com.cafeteriamanagement.model.valueobject.Name;
import com.cafeteriamanagement.repository.PurchaseRepository;
import com.cafeteriamanagement.security.SecurityAuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private UserService userService;
    @Mock
    private DishService dishService;
    @Mock
    private MenuService menuService;
    @Mock
    private SecurityAuditLogger securityAuditLogger;

    @InjectMocks
    private PurchaseService purchaseService;

    private Purchase purchase1;
    private Purchase purchase2;
    private final LocalDate futureDate = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        purchase1 = buildPurchaseMock("id1", "client1", "Dish 1");
        purchase2 = buildPurchaseMock("id2", "client2", "Dish 2");
    }

    private Purchase buildPurchaseMock(String externalId, String username, String dishName) {
        User user = mock(User.class);
        when(user.getUsername()).thenReturn(username);
        Name name = mock(Name.class);
        when(name.getValue()).thenReturn(dishName);
        Dish dish = mock(Dish.class);
        when(dish.getName()).thenReturn(name);
        Purchase purchase = mock(Purchase.class);
        when(purchase.getClient()).thenReturn(user);
        when(purchase.getDish()).thenReturn(dish);
        when(purchase.getDate()).thenReturn(futureDate);
        when(purchase.getExternalId()).thenReturn(externalId);
        return purchase;
    }

    // ---- read operations ----

    @Test
    void getAllPurchases_returnsAll() {
        when(purchaseRepository.findAll()).thenReturn(Arrays.asList(purchase1, purchase2));
        List<PurchaseDTO> result = purchaseService.getAllPurchases();
        assertEquals(2, result.size());
        assertEquals("client1", result.get(0).getClientUsername());
        assertEquals("Dish 1", result.get(0).getDishName());
    }

    @Test
    void getPurchaseById_present() {
        when(purchaseRepository.findByExternalId("id1")).thenReturn(Optional.of(purchase1));
        Optional<PurchaseDTO> result = purchaseService.getPurchaseById("id1");
        assertTrue(result.isPresent());
        assertEquals("id1", result.get().getId());
    }

    @Test
    void getPurchaseById_notFound() {
        when(purchaseRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        assertFalse(purchaseService.getPurchaseById("missing").isPresent());
    }

    @Test
    void getPurchasesByClientId_returnsClientPurchases() {
        User client = mock(User.class);
        when(userService.findByExternalId("ext-1")).thenReturn(client);
        when(purchaseRepository.findByClient(client)).thenReturn(List.of(purchase1));
        List<PurchaseDTO> result = purchaseService.getPurchasesByClientId("ext-1");
        assertEquals(1, result.size());
    }

    @Test
    void getPurchasesByClientUsername_returnsClientPurchases() {
        User client = mock(User.class);
        when(userService.findByUsername("client1")).thenReturn(client);
        when(purchaseRepository.findByClient(client)).thenReturn(List.of(purchase1));
        assertEquals(1, purchaseService.getPurchasesByClientUsername("client1").size());
    }

    @Test
    void getPurchasesByDate_returnsMatching() {
        when(purchaseRepository.findByDate(futureDate)).thenReturn(Arrays.asList(purchase1, purchase2));
        assertEquals(2, purchaseService.getPurchasesByDate(futureDate).size());
    }

    @Test
    void getUserExternalIdByUsername_returnsExternalId() {
        User user = mock(User.class);
        when(user.getExternalId()).thenReturn("ext-42");
        when(userService.findByUsername("client1")).thenReturn(user);
        assertEquals("ext-42", purchaseService.getUserExternalIdByUsername("client1"));
    }

    // ---- createPurchase ----

    @Test
    void createPurchase_success_dishIsMeatDish() {
        PurchaseDTO dto = new PurchaseDTO(null, "client1", "Dish 1", futureDate);
        User client = clientWithBalance();
        Dish dish = dishWith(1L, "Dish 1");
        when(userService.findByUsername("client1")).thenReturn(client);
        when(dishService.findByName("Dish 1")).thenReturn(dish);
        Menu menu = mock(Menu.class);
        when(menu.getMeatDish()).thenReturn(dish);
        when(menuService.findByDate(futureDate)).thenReturn(menu);
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseDTO result = purchaseService.createPurchase(dto);

        assertEquals("client1", result.getClientUsername());
        assertEquals("Dish 1", result.getDishName());
        verify(client).deductBalance(dish.getPrice());
        verify(purchaseRepository).save(any(Purchase.class));
    }

    @Test
    void createPurchase_success_dishIsFishDish() {
        PurchaseDTO dto = new PurchaseDTO(null, "client1", "Dish 1", futureDate);
        User client = clientWithBalance();
        Dish dish = dishWith(2L, "Dish 1");
        when(userService.findByUsername("client1")).thenReturn(client);
        when(dishService.findByName("Dish 1")).thenReturn(dish);
        Menu menu = mock(Menu.class);
        when(menu.getMeatDish()).thenReturn(null);
        when(menu.getFishDish()).thenReturn(dish);
        when(menuService.findByDate(futureDate)).thenReturn(menu);
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        assertNotNull(purchaseService.createPurchase(dto));
    }

    @Test
    void createPurchase_success_dishIsVegetarianDish() {
        PurchaseDTO dto = new PurchaseDTO(null, "client1", "Dish 1", futureDate);
        User client = clientWithBalance();
        Dish dish = dishWith(3L, "Dish 1");
        when(userService.findByUsername("client1")).thenReturn(client);
        when(dishService.findByName("Dish 1")).thenReturn(dish);
        Menu menu = mock(Menu.class);
        when(menu.getMeatDish()).thenReturn(null);
        when(menu.getFishDish()).thenReturn(null);
        when(menu.getVegetarianDish()).thenReturn(dish);
        when(menuService.findByDate(futureDate)).thenReturn(menu);
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        assertNotNull(purchaseService.createPurchase(dto));
    }

    @Test
    void createPurchase_dishNotInMenu_throws() {
        PurchaseDTO dto = new PurchaseDTO(null, "client1", "Dish 1", futureDate);
        User client = clientWithBalance();
        Dish dish = dishWith(9L, "Dish 1");
        when(userService.findByUsername("client1")).thenReturn(client);
        when(dishService.findByName("Dish 1")).thenReturn(dish);
        Menu menu = mock(Menu.class);
        when(menu.getMeatDish()).thenReturn(null);
        when(menu.getFishDish()).thenReturn(null);
        when(menu.getVegetarianDish()).thenReturn(null);
        when(menuService.findByDate(futureDate)).thenReturn(menu);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> purchaseService.createPurchase(dto));
        assertTrue(ex.getMessage().contains("not available"));
    }

    @Test
    void createPurchase_noMenuForDate_throwsFriendlyMessage() {
        PurchaseDTO dto = new PurchaseDTO(null, "client1", "Dish 1", futureDate);
        User client = clientWithBalance();
        Dish dish = dishWith(1L, "Dish 1");
        when(userService.findByUsername("client1")).thenReturn(client);
        when(dishService.findByName("Dish 1")).thenReturn(dish);
        when(menuService.findByDate(futureDate))
                .thenThrow(new IllegalArgumentException("Menu not found for date " + futureDate));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> purchaseService.createPurchase(dto));
        assertTrue(ex.getMessage().contains("No menu available"));
    }

    /**
     * TC39 / FR17b / NFR09 (ACID): a CLIENT without enough balance must not be able to
     * place a purchase. The balance guard rejects the operation before anything is persisted,
     * so no Purchase row is saved and no audit event is emitted — the service is left in a
     * consistent state (the {@code @Transactional} boundary additionally guarantees that any
     * partial change is rolled back).
     */
    @Test
    void createPurchase_insufficientBalance_isRejectedAndNothingPersisted() {
        PurchaseDTO dto = new PurchaseDTO(null, "client1", "Dish 1", futureDate);
        User client = mock(User.class);
        when(client.getUsername()).thenReturn("client1");
        when(client.hasEnoughBalance(any())).thenReturn(true);
        Dish dish = dishWith(1L, "Dish 1");
        when(userService.findByUsername("client1")).thenReturn(client);
        when(dishService.findByName("Dish 1")).thenReturn(dish);
        Menu menu = mock(Menu.class);
        when(menu.getMeatDish()).thenReturn(dish);
        when(menuService.findByDate(futureDate)).thenReturn(menu);
        doThrow(new IllegalStateException("Insufficient balance")).when(client).deductBalance(any());

        assertThrows(IllegalStateException.class, () -> purchaseService.createPurchase(dto));

        verify(purchaseRepository, never()).save(any(Purchase.class));
        verify(securityAuditLogger, never())
                .logPurchaseOperation(anyString(), anyString(), anyString(), anyString());
    }

    // ---- updatePurchase ----

    @Test
    void updatePurchase_success() {
        PurchaseDTO dto = new PurchaseDTO(null, "client1", "Dish 1", futureDate);
        // existing is a mock, so updateDetails() is a no-op: its getters keep returning these.
        Purchase existing = mock(Purchase.class);
        User oldClient = clientWithBalance();
        when(oldClient.getUsername()).thenReturn("client1");
        Dish oldDish = dishWith(1L, "Old");
        when(existing.getClient()).thenReturn(oldClient);
        when(existing.getDish()).thenReturn(oldDish);
        when(existing.getExternalId()).thenReturn("id1");
        when(existing.getDate()).thenReturn(futureDate);
        when(purchaseRepository.findByExternalId("id1")).thenReturn(Optional.of(existing));

        User newClient = clientWithBalance();
        Dish newDish = dishWith(1L, "Dish 1");
        when(userService.findByUsername("client1")).thenReturn(newClient);
        when(dishService.findByName("Dish 1")).thenReturn(newDish);
        Menu menu = mock(Menu.class);
        when(menu.getMeatDish()).thenReturn(newDish);
        when(menuService.findByDate(futureDate)).thenReturn(menu);
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<PurchaseDTO> result = purchaseService.updatePurchase("id1", dto);
        assertTrue(result.isPresent());
        verify(oldClient).addBalance(oldDish.getPrice());   // refund of the old dish
        verify(newClient).deductBalance(newDish.getPrice()); // charge for the new dish
    }

    @Test
    void updatePurchase_notFound() {
        when(purchaseRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        Optional<PurchaseDTO> result = purchaseService.updatePurchase("missing",
                new PurchaseDTO(null, "client1", "Dish 1", futureDate));
        assertFalse(result.isPresent());
    }

    // ---- deletePurchase ----

    @Test
    void deletePurchase_success_refundsBalance() {
        Purchase existing = mock(Purchase.class);
        User client = clientWithBalance();
        Dish dish = dishWith(1L, "Dish 1");
        when(existing.getClient()).thenReturn(client);
        when(existing.getDish()).thenReturn(dish);
        when(purchaseRepository.findByExternalId("id1")).thenReturn(Optional.of(existing));

        assertTrue(purchaseService.deletePurchase("id1"));
        verify(client).addBalance(dish.getPrice());
        verify(purchaseRepository).delete(existing);
    }

    @Test
    void deletePurchase_notFound() {
        when(purchaseRepository.findByExternalId("missing")).thenReturn(Optional.empty());
        assertFalse(purchaseService.deletePurchase("missing"));
    }

    // ---- helpers ----

    private User clientWithBalance() {
        User client = mock(User.class);
        when(client.hasEnoughBalance(any())).thenReturn(true);
        when(client.getUsername()).thenReturn("client1");
        return client;
    }

    private Dish dishWith(Long id, String name) {
        Dish dish = mock(Dish.class);
        when(dish.getId()).thenReturn(id);
        when(dish.getPrice()).thenReturn(BigDecimal.valueOf(5));
        Name n = mock(Name.class);
        when(n.getValue()).thenReturn(name);
        when(dish.getName()).thenReturn(n);
        return dish;
    }
}
