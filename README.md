# Visa Status Checker

A small command-line tool in Java that checks the status of a Czech residence permit application.
You give it your reference number; it loads the Ministry of the Interior's web form, fills it in
the way a browser would, and prints the result.

How a successful check looks against the original form (see [Limitations](#limitations) for the live site today):

```
$ java -jar visa-status-checker.jar OAM-21880/DP-2020 --verbose
Checking OAM-21880/DP-2020 ...
  GET https://frs.gov.cz/en/ioff/application-status -> 200
  POST https://frs.gov.cz/en/ioff/application-status -> 302 (Location: /en/ioff/application-status)
  GET https://frs.gov.cz/en/ioff/application-status -> 200
[SUCCESS] Proceedings OAM-21880/DP-2020 were finished with positive result.
```

## Build and run

Requires Java 17+ and Maven.

```bash
mvn package                                    # compiles, runs the tests, builds one runnable jar
java -jar target/visa-status-checker.jar OAM-21880/DP-2020
java -jar target/visa-status-checker.jar OAM-21880/DP-2020 --verbose   # log every HTTP step
```

Exit codes: `0` status printed, `1` check failed, `2` wrong arguments.

## How it works

The website uses the **Post/Redirect/Get** pattern:

1. `GET` the form page. The server sets a session cookie and puts one-time hidden tokens
   (`form_build_id`, `honeypot_time`) into the form.
2. `POST` the form with those tokens. Accepted → `302 Found` + `Location`. Rejected → `200 OK`
   with the form shown again and an error message.
3. `GET` the `Location`. The result message is stored in the session and shown **once**.

| Class | Responsibility |
|---|---|
| `ApplicationNumber` | Parses and validates `OAM-21880/DP-2020` |
| `FormParser` | Finds the form, copies every field a browser would send (including fresh tokens), fills in the number |
| `FormEncoder` | Builds the `application/x-www-form-urlencoded` body |
| `StatusChecker` | HTTP: cookies, honeypot delay, redirects followed by hand and logged |
| `ResultParser` | Extracts the status message from the result page |
| `Main` | Command-line interface |

Built on `java.net.http.HttpClient` (no HTTP library) and [jsoup](https://jsoup.org/) for HTML parsing.

## Tests

`mvn test` runs 24 tests, none of which need the internet. `StatusCheckerTest` starts a small local
server (`FakeDrupalServer`) that copies how the real site behaved: session cookie, one-time token,
honeypot timer, Post/Redirect/Get, and a message shown only once. The tests check the whole flow
end to end, including the cases that broke the first version.

## Problems I solved

The first version of this tool (still in the git history as `src/Main/StatusOfApplication.java`)
always got `200` back after the POST instead of the expected `302`, and never saw the result.
Debugging it turned up several separate causes:

1. **Broken URL encoding.** The code ran `URLEncoder.encode("form_id=ioff_application_status_form")`
   on the whole pair, which turned `=` into `%3D`. The server saw one meaningless field instead of
   `form_id`. Fix: encode the key and the value separately (`FormEncoder`, covered by a test).
2. **Hard-coded one-time tokens.** `form_build_id` and `honeypot_time` had been copied from the
   browser once. Drupal creates new ones on every page load and rejects old ones, answering `200`
   with an error. Fix: read every hidden field from the page that was just loaded (`FormParser`).
3. **Honeypot timing.** Drupal's honeypot module rejects forms sent a few milliseconds after the
   page loads, because that is how bots behave. Fix: wait 6 seconds between `GET` and `POST`.
4. **The 302 was invisible.** `HttpURLConnection.setInstanceFollowRedirects(true)` followed the
   redirect automatically, so only the final `200` was ever reported. Fix: `Redirect.NEVER` and
   follow redirects by hand, logging each step.
5. **The result was thrown away.** The message is shown once, on the page after the redirect. The
   old code then loaded the form a third time and printed that clean page. Fix: parse the page the
   redirect leads to.
6. **Smaller bugs:** `cookie.split(";", 1)` never splits (limit 1 means "one piece"); `<select>`
   has no `value` attribute (the value comes from the selected `<option>`); the response was read
   without a charset, which breaks Czech characters.

## Limitations

In 2025–2026 the service moved from `frs.gov.cz` to `ipc.gov.cz`. The old URL now redirects to a
JavaScript application that checks the status through a JSON API protected by **Google reCAPTCHA**.
The old Drupal form no longer exists, so against the live site this tool now stops with a clear
error that says where the address redirects. Getting around a CAPTCHA would go against its purpose,
so this project does not try to. Use the website in a browser to check a real application.

The project stays useful as a worked example of form scraping, session handling and
Post/Redirect/Get in plain Java, tested end to end against a local copy of the old behaviour.
