package debug;

/**
 * This class is used to display debug messages.
 * <p>
 * When the program is compiled in production mode,
 * don't forget to set isDebugging to false !
 */
public class Debugger {

    // Reset
    public static final String RESET = "\033[0m";  // Text Reset

    // Regular Colors
    public static final String BLACK = "\033[0;30m";
    public static final String RED = "\033[0;31m";
    public static final String GREEN = "\033[0;32m";
    public static final String YELLOW = "\033[0;33m";
    public static final String BLUE = "\033[0;34m";
    public static final String PURPLE = "\033[0;35m";
    public static final String CYAN = "\033[0;36m";
    public static final String WHITE = "\033[0;37m";

    public static Boolean isDebugging = false;

    /**
     * Displays a debug message as "from: message"
     *
     * @param from    From where you display the message (e.g the caller class)
     * @param message The message you want to display
     */
    public static void logMessage(String from, String message) {
        if (isDebugging) {
            System.out.println(from + ": " + message);
        }
    }

    /**
     * Displays a debug message as "from: message" with the chosen color
     *
     * @param color   The color in which you want to display the message
     * @param from    From where you display the message (e.g the caller class)
     * @param message The message you want to display
     */
    public static void logColorMessage(final String color, String from, String message) {
        if (isDebugging) {
            System.out.println(color + from + ": " + message + RESET);
        }
    }

}
