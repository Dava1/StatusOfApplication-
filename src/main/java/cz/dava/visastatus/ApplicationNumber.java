package cz.dava.visastatus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reference number of a residence permit application, e.g. {@code OAM-21880/DP-2020}.
 *
 * @param number sequence number, e.g. {@code "21880"}
 * @param type   two-letter application type, e.g. {@code "DP"} (long-term residence)
 * @param year   year the application was submitted, e.g. {@code "2020"}
 */
public record ApplicationNumber(String number, String type, String year) {

    private static final Pattern FORMAT =
            Pattern.compile("^OAM-(\\d{1,7})(?:-\\d+)?/([A-Z]{2})-(\\d{4})$");

    /**
     * Parses a number like {@code OAM-21880/DP-2020}. Lower case and surrounding spaces are accepted.
     *
     * @throws IllegalArgumentException if the text does not look like a reference number
     */
    public static ApplicationNumber parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Application number is missing");
        }
        Matcher m = FORMAT.matcher(text.trim().toUpperCase());
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Expected format OAM-<number>/<type>-<year>, for example OAM-21880/DP-2020, but got: " + text);
        }
        return new ApplicationNumber(m.group(1), m.group(2), m.group(3));
    }

    @Override
    public String toString() {
        return "OAM-" + number + "/" + type + "-" + year;
    }
}
