package rosegold.gumtuneclient.utils;

/**
 * Utility class for string operations
 * Ported from 1.8.9 Forge to 1.21.8 Fabric
 */
public class StringUtils {
    
    /**
     * Remove Minecraft formatting codes from a string
     * @param input String with formatting codes (§)
     * @return String without formatting codes
     */
    public static String removeFormatting(String input) {
        return input.replaceAll("§[0-9a-fk-or]", "");
    }

    /**
     * Format milliseconds into HH:MM:SS format
     * @param milliseconds Time in milliseconds
     * @return Formatted time string
     */
    public static String millisecondFormatTime(long milliseconds) {
        long second = (milliseconds / 1000) % 60;
        long minute = (milliseconds / (1000 * 60)) % 60;
        long hour = (milliseconds / (1000 * 60 * 60)) % 24;

        return String.format("%02d:%02d:%02d", hour, minute, second);
    }
}
