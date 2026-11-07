package cz.dava.visastatus;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormParserTest {

    private static final URI PAGE_URL = URI.create("https://frs.gov.cz/en/ioff/application-status");
    private static final ApplicationNumber NUMBER = ApplicationNumber.parse("OAM-21880/TP-2019");

    private final FormParser parser = new FormParser();

    @Test
    void takesFreshTokensFromThePage() throws Exception {
        Map<String, String> fields = parser.parse(resource("form-page.html"), PAGE_URL, NUMBER).fields();

        assertEquals("form-FRESH_TOKEN_123", fields.get("form_build_id"));
        assertEquals("ioff_application_status_form", fields.get("form_id"));
        assertEquals("1700000000|signature-abc", fields.get("honeypot_time"));
        assertEquals("", fields.get("surname"), "honeypot field must stay empty");
        assertEquals("Verify", fields.get("op"));
    }

    @Test
    void fillsInTheApplicationNumber() throws Exception {
        Map<String, String> fields = parser.parse(resource("form-page.html"), PAGE_URL, NUMBER).fields();

        assertEquals("21880", fields.get("ioff_application_number"));
        assertEquals("TP", fields.get("ioff_application_code"));
        assertEquals("2019", fields.get("ioff_application_year"));
    }

    @Test
    void keepsFieldsInPageOrder() throws Exception {
        List<String> names = List.copyOf(parser.parse(resource("form-page.html"), PAGE_URL, NUMBER).fields().keySet());
        assertEquals(List.of("ioff_application_number", "ioff_application_code", "ioff_application_year",
                "op", "form_build_id", "form_id", "honeypot_time", "surname"), names);
    }

    @Test
    void resolvesRelativeFormAction() throws Exception {
        assertEquals(PAGE_URL, parser.parse(resource("form-page.html"), PAGE_URL, NUMBER).action());
    }

    @Test
    void failsClearlyWhenThePageHasNoForm() {
        StatusCheckException e = assertThrows(StatusCheckException.class,
                () -> parser.parse("<html><body>Moved</body></html>", PAGE_URL, NUMBER));
        assertFalse(e.getMessage().isBlank());
    }

    @Test
    void failsWhenTheTokenIsMissing() {
        String html = "<form><input name='ioff_application_number'></form>";
        assertThrows(StatusCheckException.class, () -> parser.parse(html, PAGE_URL, NUMBER));
    }

    static String resource(String name) throws IOException {
        try (InputStream in = FormParserTest.class.getResourceAsStream("/" + name)) {
            if (in == null) {
                throw new IOException("Missing test resource " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
