package si.um.feri.GeoMetrics.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AppLogger {

    private static PrintWriter logWriter;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static String logFilePath;

    static {
        try {
            String userHome = System.getProperty("user.home");
            logFilePath = userHome + System.getProperty("file.separator") + "geometrics_debug.log";
            logWriter = new PrintWriter(new FileWriter(logFilePath, true), true);

            log("INFO", "=================================================");
            log("INFO", "Application started");
            log("INFO", "Log file: " + logFilePath);
            log("INFO", "Java Version: " + System.getProperty("java.version"));
            log("INFO", "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            log("INFO", "=================================================");
        } catch (IOException e) {
            System.err.println("Failed to create log file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void log(String level, String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);

        if (level.equals("ERROR")) {
            System.err.println(logMessage);
        } else {
            System.out.println(logMessage);
        }

        if (logWriter != null) {
            logWriter.println(logMessage);
            logWriter.flush();
        }
    }

    public static void info(String message) {
        log("INFO", message);
    }

    public static void debug(String message) {
        log("DEBUG", message);
    }

    public static void warn(String message) {
        log("WARN", message);
    }

    public static void error(String message) {
        log("ERROR", message);
    }

    public static void error(String message, Throwable throwable) {
        error(message);
        if (throwable != null && logWriter != null) {
            throwable.printStackTrace(logWriter);
            logWriter.flush();
            throwable.printStackTrace(System.err);
        }
    }

    public static String getLogFilePath() {
        return logFilePath;
    }

    public static void close() {
        if (logWriter != null) {
            log("INFO", "Application shutting down");
            logWriter.close();
        }
    }
}
