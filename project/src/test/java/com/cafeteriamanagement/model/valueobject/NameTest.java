package com.cafeteriamanagement.model.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameTest {

    @Test
    void validName_isStored() {
        assertEquals("Tomato Soup", new Name("Tomato Soup").getValue());
    }

    @Test
    void name_isTrimmed() {
        assertEquals("Soup", new Name("  Soup  ").getValue());
    }

    @Test
    void nullName_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Name(null));
    }

    @Test
    void blankName_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Name("   "));
    }

    @Test
    void nameWithDigits_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Name("Soup123"));
    }

    @Test
    void nameWithSymbols_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Name("Soup!"));
    }

    @Test
    void equalsAndHashCode_byValue() {
        assertEquals(new Name("Soup"), new Name("Soup"));
        assertEquals(new Name("Soup").hashCode(), new Name("Soup").hashCode());
        assertNotEquals(new Name("Soup"), new Name("Salad"));
        assertNotEquals(new Name("Soup"), null);
        assertNotEquals(new Name("Soup"), "Soup");
    }

    @Test
    void toString_returnsValue() {
        assertEquals("Soup", new Name("Soup").toString());
    }
}
