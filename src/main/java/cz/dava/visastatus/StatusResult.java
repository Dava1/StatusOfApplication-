package cz.dava.visastatus;

/**
 * Message the website shows after the form has been submitted.
 *
 * @param level   how the website styled the message
 * @param message the text, e.g. "Proceedings OAM-21880/DP-2020 were finished with positive result."
 */
public record StatusResult(Level level, String message) {

    public enum Level {
        SUCCESS, INFO, WARNING, ERROR
    }

    @Override
    public String toString() {
        return "[" + level + "] " + message;
    }
}
