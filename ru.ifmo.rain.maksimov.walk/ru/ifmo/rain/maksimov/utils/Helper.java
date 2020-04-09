package ru.ifmo.rain.maksimov.utils;

/**
 * Helper with some useful methods
 */
public class Helper {

    /**
     * Log a message to {@link System#err}, if exception is not null
     * log {@link Exception#getMessage()} too
     *
     * @param mess {@link String} message to log
     * @param e {@link Exception} to get more information from
     */
    public static void log(String mess, Exception e) {
        String out = mess + System.lineSeparator();
        if (e != null) {
            out += "Error message: " + e.getMessage() + System.lineSeparator();
        }
        System.err.println(out);
    }


    /**
     * As same method as {@link #log(String, Exception)} but without exception
     *
     * @param mess {@link String} message to log
     */
    public static void log(String mess) {
        log(mess, null);
    }


    /**
     * Get hash of file as {@link String} in special format
     *
     * @param filename {@link String} filename
     * @param hash hash of file with this filename
     * @return representation of hash
     */
    public static String getOutputFormat(String filename, int hash) {
        return String.format("%08x", hash) + ' ' + filename + System.lineSeparator();
    }
}
