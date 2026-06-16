package com.cafeteriamanagement.model.entity;

import com.cafeteriamanagement.model.enums.Allergen;
import com.cafeteriamanagement.model.enums.IngredientType;
import com.cafeteriamanagement.model.enums.PurchaseStatus;
import com.cafeteriamanagement.model.enums.UserType;
import com.cafeteriamanagement.model.valueobject.Name;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseTest {

    private final LocalDate future = LocalDate.now().plusDays(1);

    private Dish dish(BigDecimal price) {
        Ingredient veg = new Ingredient(new Name("Carrot"), IngredientType.VEGETABLES, Allergen.NONE);
        return new Dish(new Name("Salad"), List.of(veg), price);
    }

    private User client(String balance) {
        return new User("c", "p", UserType.CLIENT, new BigDecimal(balance));
    }

    @Test
    void constructor_success() {
        Purchase p = new Purchase(client("100.00"), dish(new BigDecimal("5.00")), future);
        assertNotNull(p.getExternalId());
        assertEquals(future, p.getDate());
        assertEquals("c", p.getClient().getUsername());
    }

    @Test
    void constructor_pastDate_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Purchase(client("100.00"), dish(new BigDecimal("5.00")), LocalDate.now().minusDays(1)));
    }

    @Test
    void constructor_today_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Purchase(client("100.00"), dish(new BigDecimal("5.00")), LocalDate.now()));
    }

    @Test
    void constructor_nullDate_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Purchase(client("100.00"), dish(new BigDecimal("5.00")), null));
    }

    @Test
    void constructor_insufficientBalance_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Purchase(client("1.00"), dish(new BigDecimal("5.00")), future));
    }

    @Test
    void updateDetails_changesFields() {
        Purchase p = new Purchase(client("100.00"), dish(new BigDecimal("5.00")), future);
        LocalDate newDate = future.plusDays(2);
        User newClient = client("50.00");
        p.updateDetails(newClient, dish(new BigDecimal("3.00")), newDate);
        assertEquals(newDate, p.getDate());
        assertSame(newClient, p.getClient());
    }

    @Test
    void equalsAndToString() {
        Purchase p = new Purchase(client("100.00"), dish(new BigDecimal("5.00")), future);
        assertEquals(p, p);
        assertNotEquals(p, null);
        assertNotEquals(p, "x");
        assertTrue(p.toString().contains("c"));
    }

    @Test
    void newPurchase_statusIsPending() {
        Purchase p = new Purchase(client("100.00"), dish(new BigDecimal("5.00")), future);
        assertEquals(PurchaseStatus.PENDING, p.getStatus());
    }

    @Test
    void confirm_setsStatusConfirmed() {
        Purchase p = new Purchase(client("100.00"), dish(new BigDecimal("5.00")), future);
        p.confirm();
        assertEquals(PurchaseStatus.CONFIRMED, p.getStatus());
    }

    @Test
    void cancel_setsStatusCancelled() {
        Purchase p = new Purchase(client("100.00"), dish(new BigDecimal("5.00")), future);
        p.cancel();
        assertEquals(PurchaseStatus.CANCELLED, p.getStatus());
    }

    @Test
    void confirm_afterCancel_throws() {
        Purchase p = new Purchase(client("100.00"), dish(new BigDecimal("5.00")), future);
        p.cancel();
        assertThrows(IllegalStateException.class, p::confirm);
    }

    @Test
    void cancel_afterConfirm_throws() {
        Purchase p = new Purchase(client("100.00"), dish(new BigDecimal("5.00")), future);
        p.confirm();
        assertThrows(IllegalStateException.class, p::cancel);
    }
}
