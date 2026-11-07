package cz.dava.visastatus;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Optional;

/**
 * Finds the status message on the page that the website shows after the form is sent.
 *
 * <p>Drupal shows such messages in Bootstrap alert blocks, for example
 * {@code <div class="alert alert-success">...</div>}.
 */
public class ResultParser {

    public Optional<StatusResult> parse(String html) {
        Document doc = Jsoup.parse(html);
        Element alert = doc.selectFirst("div.alert");
        if (alert == null) {
            return Optional.empty();
        }
        // Drop the close button and the hidden "Status message" heading; keep only the message.
        alert = alert.clone();
        alert.select("button, .sr-only, .visually-hidden").remove();
        String text = alert.text().trim();
        if (text.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new StatusResult(levelOf(alert), text));
    }

    private static StatusResult.Level levelOf(Element alert) {
        if (alert.hasClass("alert-success")) {
            return StatusResult.Level.SUCCESS;
        }
        if (alert.hasClass("alert-warning")) {
            return StatusResult.Level.WARNING;
        }
        if (alert.hasClass("alert-danger") || alert.hasClass("alert-error")) {
            return StatusResult.Level.ERROR;
        }
        return StatusResult.Level.INFO;
    }
}
