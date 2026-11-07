package cz.dava.visastatus;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Checks an application status by filling in the website's form, the same way a browser does.
 *
 * <p>The website uses the Post/Redirect/Get pattern:
 * <ol>
 *     <li>{@code GET} the form page. The server sets a session cookie and puts one-time tokens into the form.</li>
 *     <li>{@code POST} the form with those tokens.
 *         If it is accepted, the server answers {@code 302 Found} with a {@code Location} header.
 *         If it is rejected, the server answers {@code 200 OK} and shows the form again with an error.</li>
 *     <li>{@code GET} the {@code Location}. The server shows the result message once, taken from the session.</li>
 * </ol>
 *
 * <p>Redirects are followed by hand ({@link HttpClient.Redirect#NEVER}) so that the 302 is visible
 * and logged. With automatic redirects the client only ever reports the final 200.
 */
public class StatusChecker {

    /** The original address of the form. */
    public static final URI DEFAULT_FORM_URL = URI.create("https://frs.gov.cz/en/ioff/application-status");

    /**
     * The website uses the Drupal "honeypot" module: a form sent faster than a few seconds after
     * the page was loaded is treated as a bot and rejected.
     */
    public static final Duration DEFAULT_HONEYPOT_DELAY = Duration.ofSeconds(6);

    private static final String USER_AGENT = "visa-status-checker/1.0 (+https://github.com/dava1/statusofapplication-)";
    private static final int MAX_REDIRECTS = 5;
    private static final Logger LOG = Logger.getLogger(StatusChecker.class.getName());

    private final HttpClient http;
    private final URI formUrl;
    private final Duration honeypotDelay;
    private final FormParser formParser = new FormParser();
    private final ResultParser resultParser = new ResultParser();

    public StatusChecker() {
        this(DEFAULT_FORM_URL, DEFAULT_HONEYPOT_DELAY);
    }

    public StatusChecker(URI formUrl, Duration honeypotDelay) {
        this.formUrl = Objects.requireNonNull(formUrl);
        this.honeypotDelay = Objects.requireNonNull(honeypotDelay);
        this.http = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER))
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public StatusResult check(ApplicationNumber number) throws StatusCheckException {
        try {
            HttpResponse<String> formPage = getFollowingRedirects(formUrl);
            FormParser.StatusForm form = parseForm(formPage, number);

            LOG.fine(() -> "Waiting " + honeypotDelay.toSeconds() + " s so the honeypot check does not reject the form");
            Thread.sleep(honeypotDelay.toMillis());

            HttpResponse<String> postResponse = post(form, formPage.uri());
            int status = postResponse.statusCode();

            if (isRedirect(status)) {
                URI next = location(postResponse);
                HttpResponse<String> resultPage = getFollowingRedirects(next);
                return resultParser.parse(resultPage.body()).orElseThrow(() ->
                        new StatusCheckException("The form was accepted, but the result page has no status message"));
            }
            if (status == 200) {
                String reason = resultParser.parse(postResponse.body())
                        .map(StatusResult::message)
                        .orElse("no error message on the page");
                throw new StatusCheckException("The website rejected the form (200 instead of 302): " + reason);
            }
            throw new StatusCheckException("Unexpected HTTP " + status + " after sending the form");
        } catch (IOException e) {
            throw new StatusCheckException("Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StatusCheckException("Interrupted", e);
        }
    }

    private FormParser.StatusForm parseForm(HttpResponse<String> page, ApplicationNumber number)
            throws StatusCheckException {
        try {
            return formParser.parse(page.body(), page.uri(), number);
        } catch (StatusCheckException e) {
            if (!page.uri().getHost().equals(formUrl.getHost())) {
                throw new StatusCheckException(e.getMessage() + ". The address now redirects to " + page.uri()
                        + "; the service has moved and the old form is no longer available", e);
            }
            throw e;
        }
    }

    private HttpResponse<String> getFollowingRedirects(URI url) throws IOException, InterruptedException,
            StatusCheckException {
        URI current = url;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            HttpRequest request = baseRequest(current).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            log(request, response);
            if (!isRedirect(response.statusCode())) {
                if (response.statusCode() != 200) {
                    throw new StatusCheckException("HTTP " + response.statusCode() + " from " + current);
                }
                return response;
            }
            current = location(response);
        }
        throw new StatusCheckException("Too many redirects starting from " + url);
    }

    private HttpResponse<String> post(FormParser.StatusForm form, URI referer) throws IOException,
            InterruptedException {
        HttpRequest request = baseRequest(form.action())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Referer", referer.toString())
                .POST(HttpRequest.BodyPublishers.ofString(FormEncoder.encode(form.fields())))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        log(request, response);
        return response;
    }

    private static HttpRequest.Builder baseRequest(URI url) {
        return HttpRequest.newBuilder(url)
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "en");
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static URI location(HttpResponse<?> response) throws StatusCheckException {
        String location = response.headers().firstValue("Location").orElseThrow(() ->
                new StatusCheckException("HTTP " + response.statusCode() + " without a Location header"));
        return response.uri().resolve(location);
    }

    private static void log(HttpRequest request, HttpResponse<?> response) {
        LOG.fine(() -> request.method() + " " + request.uri() + " -> " + response.statusCode()
                + response.headers().firstValue("Location").map(l -> " (Location: " + l + ")").orElse(""));
    }
}
