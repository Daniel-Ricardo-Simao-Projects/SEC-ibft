package pt.ulisboa.tecnico.hdsledger.utilities;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class CustomLogger {

    private static Logger LOGGER;

    public CustomLogger(String name) {
        LOGGER = Logger.getLogger(name);
        LOGGER.setLevel(Level.ALL);
        LOGGER.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();

        Formatter formatter = new CustomLog();
        handler.setFormatter(formatter);

        LOGGER.addHandler(handler);
    }

    public void log(Level level, String message) {
        LOGGER.log(level, message);
    }

}

class CustomLog extends Formatter {

    // ANSI escape code
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        // ANSI escape code for color
        Level level = record.getLevel();
        if (level == Level.INFO) {
            sb.append(ANSI_WHITE);
        } else if (level == Level.SEVERE) {
            sb.append(ANSI_BLUE);
        } else if (level == Level.WARNING) {
            sb.append(ANSI_RED);
            sb.append("[WARNING] ");
        } else {
            sb.append(ANSI_WHITE);
        }

        sb.append(record.getMessage()).append('\n');

        return sb.toString();
    }
}
