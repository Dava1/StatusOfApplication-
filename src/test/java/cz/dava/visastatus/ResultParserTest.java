package cz.dava.visastatus;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultParserTest {

    private final ResultParser parser = new ResultParser();

    @Test
    void readsSuccessMessageWithoutCloseButtonAndHiddenHeading() {
        String html = """
                <div class="alert alert-success" role="alert">
                  <button type="button" class="close">×</button>
                  <h2 class="sr-only">Status message</h2>
                  Proceedings OAM-21880/DP-2020 were finished with positive result.
                </div>""";

        assertEquals(Optional.of(new StatusResult(StatusResult.Level.SUCCESS,
                "Proceedings OAM-21880/DP-2020 were finished with positive result.")), parser.parse(html));
    }

    @Test
    void mapsBootstrapClassesToLevels() {
        assertEquals(StatusResult.Level.WARNING, level("alert alert-warning"));
        assertEquals(StatusResult.Level.ERROR, level("alert alert-danger"));
        assertEquals(StatusResult.Level.INFO, level("alert alert-info"));
    }

    @Test
    void returnsEmptyWhenThereIsNoMessage() {
        assertTrue(parser.parse("<form><input name='x'></form>").isEmpty());
    }

    private StatusResult.Level level(String cssClass) {
        return parser.parse("<div class='" + cssClass + "'>text</div>").orElseThrow().level();
    }
}
