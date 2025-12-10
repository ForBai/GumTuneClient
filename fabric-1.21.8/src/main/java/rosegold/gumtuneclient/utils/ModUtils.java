package rosegold.gumtuneclient.utils;

import net.minecraft.text.Text;
import rosegold.gumtuneclient.GumTuneClient;

/**
 * Utility class for mod-related functions
 * Ported from 1.8.9 Forge to 1.21.8 Fabric
 */
public class ModUtils {
    /**
     * Sends a message to the player's chat
     * @param object The message to send (will be converted to string)
     */
    public static void sendMessage(Object object) {
        String message = "null";
        if (object != null) {
            // Convert & color codes to § for Minecraft formatting
            message = object.toString().replace("&", "§");
        }
        
        if (GumTuneClient.mc != null && GumTuneClient.mc.player != null) {
            // Modern Minecraft uses Text instead of ChatComponentText
            String formattedMessage = "§7[§d" + GumTuneClient.NAME + "§7] §f" + message;
            GumTuneClient.mc.player.sendMessage(Text.literal(formattedMessage), false);
        }
    }
}
