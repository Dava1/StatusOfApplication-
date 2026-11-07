package cz.dava.visastatus;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Command line entry point.
 *
 * <pre>
 *   java -jar visa-status-checker.jar OAM-21880/DP-2020
 *   java -jar visa-status-checker.jar OAM-21880/DP-2020 --verbose
 * </pre>
 *
 * Exit codes: 0 = status printed, 1 = the check failed, 2 = wrong arguments.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        String numberArg = null;
        boolean verbose = false;
        for (String arg : args) {
            if (arg.equals("-v") || arg.equals("--verbose")) {
                verbose = true;
            } else if (numberArg == null) {
                numberArg = arg;
            } else {
                return usage("Unexpected argument: " + arg);
            }
        }
        if (numberArg == null) {
            return usage(null);
        }

        ApplicationNumber number;
        try {
            number = ApplicationNumber.parse(numberArg);
        } catch (IllegalArgumentException e) {
            return usage("Invalid application number. " + e.getMessage());
        }

        if (verbose) {
            enableHttpLog();
        }

        System.out.println("Checking " + number + " ...");
        try {
            StatusResult result = new StatusChecker().check(number);
            System.out.println(result);
            return 0;
        } catch (StatusCheckException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private static int usage(String error) {
        if (error != null) {
            System.err.println(error);
        }
        System.err.println("Usage: java -jar visa-status-checker.jar OAM-<number>/<type>-<year> [--verbose]");
        return 2;
    }

    private static void enableHttpLog() {
        System.setProperty("java.util.logging.SimpleFormatter.format", "  %5$s%n");
        Logger log = Logger.getLogger(StatusChecker.class.getPackageName());
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
        handler.setFormatter(new SimpleFormatter());
        log.addHandler(handler);
        log.setLevel(Level.FINE);
        log.setUseParentHandlers(false);
    }
}
