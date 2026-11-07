package cz.dava.visastatus;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds an {@code application/x-www-form-urlencoded} body, the format a browser uses for an HTML form. */
final class FormEncoder {

    private FormEncoder() {
    }

    /**
     * Encodes the key and the value separately and joins them with {@code =} and {@code &}.
     *
     * <p>Encoding the whole {@code "key=value"} string at once is a bug: {@code =} becomes
     * {@code %3D} and the server no longer sees the field at all.
     */
    static String encode(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(e -> encodePart(e.getKey()) + "=" + encodePart(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String encodePart(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
