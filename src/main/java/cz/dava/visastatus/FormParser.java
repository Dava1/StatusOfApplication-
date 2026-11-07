package cz.dava.visastatus;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads the status form from the HTML page and builds the fields for the POST request.
 *
 * <p>The website is built on Drupal. Every time the page is loaded, Drupal puts new one-time
 * hidden fields into the form ({@code form_build_id}, {@code honeypot_time}). They must be copied
 * from the page we just loaded; old values are rejected. That is why nothing here is hard-coded.
 */
public class FormParser {

    static final String NUMBER_FIELD = "ioff_application_number";
    static final String TYPE_FIELD = "ioff_application_code";
    static final String YEAR_FIELD = "ioff_application_year";

    /** The form to submit: where to send it and which fields to send. */
    public record StatusForm(URI action, Map<String, String> fields) {
    }

    /**
     * @param html    the page with the form
     * @param pageUrl the URL the page was loaded from; used to resolve a relative form action
     * @param number  the application number to fill in
     */
    public StatusForm parse(String html, URI pageUrl, ApplicationNumber number) throws StatusCheckException {
        Document doc = Jsoup.parse(html, pageUrl.toString());
        Element form = findStatusForm(doc);

        Map<String, String> fields = readFields(form);
        if (!fields.containsKey("form_build_id")) {
            throw new StatusCheckException("The form has no form_build_id token; the page has probably changed");
        }

        fields.put(NUMBER_FIELD, number.number());
        fields.put(TYPE_FIELD, number.type());
        fields.put(YEAR_FIELD, number.year());

        String action = form.absUrl("action");
        URI actionUrl = action.isEmpty() ? pageUrl : URI.create(action);
        return new StatusForm(actionUrl, fields);
    }

    private static Element findStatusForm(Document doc) throws StatusCheckException {
        // Look for the whole <form>, not only the #edit-oam block: the hidden tokens sit outside it.
        for (Element form : doc.select("form")) {
            if (!form.select("[name=" + NUMBER_FIELD + "]").isEmpty()) {
                return form;
            }
        }
        throw new StatusCheckException("Status form not found on the page; the website has probably changed");
    }

    /** Collects the fields a browser would send, in page order. */
    private static Map<String, String> readFields(Element form) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (Element el : form.select("input[name], select[name], textarea[name], button[name]")) {
            String name = el.attr("name");
            switch (el.normalName()) {
                case "select" -> fields.put(name, selectedValue(el));
                case "textarea" -> fields.put(name, el.text());
                case "button" -> fields.putIfAbsent(name, el.attr("value"));
                default -> {
                    String type = el.attr("type").toLowerCase();
                    if ((type.equals("checkbox") || type.equals("radio")) && !el.hasAttr("checked")) {
                        continue;
                    }
                    // Only the first submit button is "clicked" (the website's one is op=Verify).
                    if (type.equals("submit")) {
                        fields.putIfAbsent(name, el.attr("value"));
                    } else {
                        fields.put(name, el.attr("value"));
                    }
                }
            }
        }
        return fields;
    }

    /** A {@code <select>} has no value attribute: the value comes from the selected {@code <option>}. */
    private static String selectedValue(Element select) {
        Element option = select.selectFirst("option[selected]");
        if (option == null) {
            option = select.selectFirst("option");
        }
        if (option == null) {
            return "";
        }
        return option.hasAttr("value") ? option.attr("value") : option.text();
    }
}
