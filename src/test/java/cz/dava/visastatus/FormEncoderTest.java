package cz.dava.visastatus;

import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormEncoderTest {

    @Test
    void encodesKeyAndValueSeparately() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("form_id", "ioff_application_status_form");
        fields.put("honeypot_time", "1700000000|sig");
        fields.put("surname", "");
        fields.put("op", "Verify me");

        assertEquals("form_id=ioff_application_status_form&honeypot_time=1700000000%7Csig&surname=&op=Verify+me",
                FormEncoder.encode(fields));
    }

    /** Documents the bug in the first version: the whole pair was encoded, so '=' turned into '%3D'. */
    @Test
    void encodingTheWholePairBreaksTheField() {
        String broken = URLEncoder.encode("form_id=ioff_application_status_form", StandardCharsets.UTF_8);
        assertEquals("form_id%3Dioff_application_status_form", broken);
        assertTrue(FormEncoder.encode(Map.of("form_id", "ioff_application_status_form")).contains("form_id="));
    }

    @Test
    void encodesCzechCharactersAsUtf8() {
        assertEquals("name=%C4%8Cech", FormEncoder.encode(Map.of("name", "Čech")));
    }
}
