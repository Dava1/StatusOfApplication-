package cz.dava.visastatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end tests against a local server that behaves like the original website. */
class StatusCheckerTest {

    private static final Duration HONEYPOT = Duration.ofMillis(300);
    private static final ApplicationNumber NUMBER = ApplicationNumber.parse("OAM-21880/DP-2020");

    private FakeDrupalServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = new FakeDrupalServer(HONEYPOT);
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void getsResultThroughPostRedirectGet() throws Exception {
        StatusChecker checker = new StatusChecker(server.uri(FakeDrupalServer.FORM_PATH), HONEYPOT.plusMillis(200));

        StatusResult result = checker.check(NUMBER);

        assertEquals(new StatusResult(StatusResult.Level.SUCCESS,
                "Proceedings OAM-21880/DP-2020 were finished with positive result."), result);
    }

    @Test
    void reportsRejectedFormWhenSentTooFast() {
        StatusChecker checker = new StatusChecker(server.uri(FakeDrupalServer.FORM_PATH), Duration.ZERO);

        StatusCheckException e = assertThrows(StatusCheckException.class, () -> checker.check(NUMBER));

        assertTrue(e.getMessage().contains("200 instead of 302"), e.getMessage());
        assertTrue(e.getMessage().contains("wait 6 seconds"), e.getMessage());
    }

    @Test
    void explainsWhenTheFormHasMovedAway() {
        StatusChecker checker = new StatusChecker(server.uri("/moved"), Duration.ZERO);

        StatusCheckException e = assertThrows(StatusCheckException.class, () -> checker.check(NUMBER));

        assertTrue(e.getMessage().contains("not found"), e.getMessage());
    }
}
