package cz.dava.visastatus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A tiny local server that behaves like the original Drupal status form, including the parts
 * that broke the first version of this tool:
 * <ul>
 *     <li>a session cookie,</li>
 *     <li>a one-time {@code form_build_id} token per page load,</li>
 *     <li>a honeypot that rejects forms sent too quickly,</li>
 *     <li>Post/Redirect/Get: accepted form = 302, rejected form = 200 with an error,</li>
 *     <li>the result message is shown once, after the redirect.</li>
 * </ul>
 */
final class FakeDrupalServer implements AutoCloseable {

    static final String FORM_PATH = "/en/ioff/application-status";

    private static final class Session {
        String token;
        long formLoadedAtMillis;
        String pendingMessage;
    }

    private final HttpServer server;
    private final Duration honeypotMinDelay;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    FakeDrupalServer(Duration honeypotMinDelay) throws IOException {
        this.honeypotMinDelay = honeypotMinDelay;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(FORM_PATH, this::handleForm);
        server.createContext("/moved", exchange -> {
            exchange.getResponseHeaders().add("Location", "/new-site");
            send(exchange, 301, "");
        });
        server.createContext("/new-site", exchange -> send(exchange, 200, "<html><body>New JS app</body></html>"));
        server.start();
    }

    URI uri(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handleForm(HttpExchange exchange) throws IOException {
        Session session = session(exchange);
        if (exchange.getRequestMethod().equals("POST")) {
            handlePost(exchange, session);
        } else {
            String message = session.pendingMessage;
            session.pendingMessage = null;
            String alert = message == null ? "" : "<div class=\"alert alert-success\">"
                    + "<button class=\"close\">×</button><h2 class=\"sr-only\">Status message</h2>"
                    + message + "</div>";
            send(exchange, 200, page(session, alert));
        }
    }

    private void handlePost(HttpExchange exchange, Session session) throws IOException {
        Map<String, String> form = decode(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

        String error = null;
        if (!"ioff_application_status_form".equals(form.get("form_id"))) {
            error = "Unknown form.";
        } else if (session.token == null || !session.token.equals(form.get("form_build_id"))) {
            error = "The form has become outdated. Copy any unsaved work in the form below and then reload this page.";
        } else if (!"".equals(form.get("surname"))) {
            error = "There was a problem with your form submission.";
        } else if (System.currentTimeMillis() - session.formLoadedAtMillis < honeypotMinDelay.toMillis()) {
            error = "There was a problem with your form submission. Please wait 6 seconds and try again.";
        } else if (form.getOrDefault("ioff_application_number", "").isEmpty()) {
            error = "Application number field is required.";
        }

        if (error != null) {
            send(exchange, 200, page(session, "<div class=\"alert alert-danger\">" + error + "</div>"));
            return;
        }
        session.pendingMessage = "Proceedings OAM-" + form.get("ioff_application_number") + "/"
                + form.get("ioff_application_code") + "-" + form.get("ioff_application_year")
                + " were finished with positive result.";
        exchange.getResponseHeaders().add("Location", FORM_PATH);
        send(exchange, 302, "");
    }

    /** Renders the form with a fresh one-time token, like Drupal does on every page load. */
    private String page(Session session, String alert) {
        session.token = "form-" + UUID.randomUUID();
        session.formLoadedAtMillis = System.currentTimeMillis();
        return """
                <html><body>%s
                <form action="%s" method="post">
                  <div id="edit-oam">
                    <input type="text" name="ioff_application_number" value="">
                    <select name="ioff_application_code"><option value="DP">DP</option><option value="TP">TP</option></select>
                    <select name="ioff_application_year"><option value="2020">2020</option></select>
                  </div>
                  <input type="submit" name="op" value="Verify">
                  <input type="hidden" name="form_build_id" value="%s">
                  <input type="hidden" name="form_id" value="ioff_application_status_form">
                  <input type="hidden" name="honeypot_time" value="%d|sig">
                  <input type="text" name="surname" value="">
                </form></body></html>
                """.formatted(alert, FORM_PATH, session.token, session.formLoadedAtMillis / 1000);
    }

    private Session session(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) {
            for (String part : cookie.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals("SESS") && sessions.containsKey(kv[1])) {
                    return sessions.get(kv[1]);
                }
            }
        }
        String id = UUID.randomUUID().toString();
        Session session = new Session();
        sessions.put(id, session);
        exchange.getResponseHeaders().add("Set-Cookie", "SESS=" + id + "; Path=/; HttpOnly");
        return session;
    }

    /** Strict decoding: a pair without '=' (the old "%3D" bug) is simply not a field. */
    private static Map<String, String> decode(String body) {
        Map<String, String> fields = new HashMap<>();
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                fields.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return fields;
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
