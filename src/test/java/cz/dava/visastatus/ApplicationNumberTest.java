package cz.dava.visastatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationNumberTest {

    @Test
    void parsesFullReferenceNumber() {
        ApplicationNumber n = ApplicationNumber.parse("OAM-21880/DP-2020");
        assertEquals(new ApplicationNumber("21880", "DP", "2020"), n);
        assertEquals("OAM-21880/DP-2020", n.toString());
    }

    @Test
    void acceptsLowerCaseAndSpaces() {
        assertEquals(new ApplicationNumber("21880", "DP", "2020"), ApplicationNumber.parse("  oam-21880/dp-2020 "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "21880", "OAM-21880", "OAM-21880/DP", "OAM-abc/DP-2020", "OAM-21880/D-2020"})
    void rejectsInvalidNumbers(String text) {
        assertThrows(IllegalArgumentException.class, () -> ApplicationNumber.parse(text));
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> ApplicationNumber.parse(null));
    }
}
