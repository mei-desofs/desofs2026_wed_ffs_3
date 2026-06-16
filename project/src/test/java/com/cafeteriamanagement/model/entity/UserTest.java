package com.cafeteriamanagement.model.entity;

import com.cafeteriamanagement.model.enums.UserType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void clientKeepsBalance_employeeBalanceIsNull() {
        User client = new User("c", "p", UserType.CLIENT, new BigDecimal("10.00"));
        User employee = new User("e", "p", UserType.EMPLOYEE, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), client.getBalance());
        assertNull(employee.getBalance());
        assertNotNull(client.getExternalId());
    }

    @Test
    void deductBalance_reducesBalance() {
        User client = new User("c", "p", UserType.CLIENT, new BigDecimal("10.00"));
        client.deductBalance(new BigDecimal("4.00"));
        assertEquals(new BigDecimal("6.00"), client.getBalance());
    }

    @Test
    void deductBalance_insufficient_throws() {
        User client = new User("c", "p", UserType.CLIENT, new BigDecimal("1.00"));
        assertThrows(IllegalStateException.class, () -> client.deductBalance(new BigDecimal("5.00")));
    }

    @Test
    void deductBalance_employee_throws() {
        User employee = new User("e", "p", UserType.EMPLOYEE, null);
        assertThrows(IllegalStateException.class, () -> employee.deductBalance(BigDecimal.ONE));
    }

    @Test
    void addBalance_increasesBalance() {
        User client = new User("c", "p", UserType.CLIENT, new BigDecimal("10.00"));
        client.addBalance(new BigDecimal("5.00"));
        assertEquals(new BigDecimal("15.00"), client.getBalance());
    }

    @Test
    void addBalance_employee_throws() {
        User employee = new User("e", "p", UserType.EMPLOYEE, null);
        assertThrows(IllegalStateException.class, () -> employee.addBalance(BigDecimal.ONE));
    }

    @Test
    void hasEnoughBalance_variants() {
        User client = new User("c", "p", UserType.CLIENT, new BigDecimal("10.00"));
        assertTrue(client.hasEnoughBalance(new BigDecimal("10.00")));
        assertFalse(client.hasEnoughBalance(new BigDecimal("10.01")));
        User employee = new User("e", "p", UserType.EMPLOYEE, null);
        assertFalse(employee.hasEnoughBalance(BigDecimal.ZERO));
    }

    @Test
    void updateDetails_changesFields_employeeBalanceCleared() {
        User user = new User("c", "p", UserType.CLIENT, new BigDecimal("10.00"));
        user.updateDetails("c2", "p2", UserType.EMPLOYEE, new BigDecimal("99.00"));
        assertEquals("c2", user.getUsername());
        assertEquals("p2", user.getPassword());
        assertEquals(UserType.EMPLOYEE, user.getType());
        assertNull(user.getBalance());
    }

    @Test
    void equals_basedOnIdAndToString() {
        User user = new User("c", "p", UserType.CLIENT, BigDecimal.TEN);
        assertEquals(user, user);
        assertNotEquals(user, null);
        assertNotEquals(user, "x");
        assertTrue(user.toString().contains("c"));
    }
}
