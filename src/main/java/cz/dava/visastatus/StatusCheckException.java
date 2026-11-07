package cz.dava.visastatus;

/** Thrown when the status cannot be checked: network problem, changed page, or rejected form. */
public class StatusCheckException extends Exception {

    public StatusCheckException(String message) {
        super(message);
    }

    public StatusCheckException(String message, Throwable cause) {
        super(message, cause);
    }
}
