package rosegold.gumtuneclient.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;

/**
 * Utility class for tracking player location in Hypixel Skyblock
 * Ported from 1.8.9 Forge to 1.21.8 Fabric
 */
public class LocationUtils {
    private static final Gson gson = new Gson();

    public enum Island {
        PRIVATE_ISLAND("Private Island"),
        THE_HUB("Hub"),
        THE_PARK("The Park"),
        THE_FARMING_ISLANDS("The Farming Islands"),
        SPIDER_DEN("Spider's Den"),
        THE_END("The End"),
        CRIMSON_ISLE("Crimson Isle"),
        GOLD_MINE("Gold Mine"),
        DEEP_CAVERNS("Deep Caverns"),
        DWARVEN_MINES("Dwarven Mines"),
        CRYSTAL_HOLLOWS("Crystal Hollows"),
        JERRY_WORKSHOP("Jerry's Workshop"),
        DUNGEON_HUB("Dungeon Hub"),
        LIMBO("UNKNOWN"),
        LOBBY("PROTOTYPE"),
        DUNGEON("Dungeon"),
        GARDEN("Garden"),
        THE_RIFT("The Rift"),
        MINESHAFT("Mineshaft");

        private final String name;

        public String getName() {
            return name;
        }

        Island(String name) {
            this.name = name;
        }
    }

    public static Island currentIsland;
    public static String serverName;
    public static boolean onSkyblock = false;
    public static boolean onHypixel = false;

    /**
     * Initialize location tracking
     * Registers chat message listener to detect /locraw responses
     */
    public static void init() {
        // Register chat message listener for /locraw responses
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String unformatted = message.getString();
            
            // Check for /locraw JSON response
            if (!unformatted.startsWith("{") || !unformatted.endsWith("}")) return;

            try {
                JsonObject obj = gson.fromJson(unformatted, JsonObject.class);
                if (!obj.has("gametype") || !obj.has("map")) return;

                // Detect Hypixel
                onHypixel = true;

                if (obj.getAsJsonPrimitive("gametype").getAsString().equals("limbo")) {
                    if (obj.getAsJsonPrimitive("server").getAsString().equals("limbo")) {
                        currentIsland = Island.LIMBO;
                    } else {
                        currentIsland = Island.LOBBY;
                    }
                } else {
                    onSkyblock = obj.getAsJsonPrimitive("gametype").getAsString().equals("SKYBLOCK");
                    if (onSkyblock) {
                        serverName = obj.getAsJsonPrimitive("server").getAsString();
                        for (Island island : Island.values()) {
                            if (obj.getAsJsonPrimitive("map").getAsString().equals(island.getName())) {
                                currentIsland = island;
                                break;
                            }
                        }
                    } else {
                        serverName = null;
                    }
                }
            } catch (Exception e) {
                // Silently fail for non-locraw JSON messages
            }
        });
    }

    /**
     * Check if currently on a specific island
     */
    public static boolean isInIsland(Island island) {
        return currentIsland == island;
    }

    /**
     * Check if in Crystal Hollows
     */
    public static boolean inCrystalHollows() {
        return currentIsland == Island.CRYSTAL_HOLLOWS;
    }

    /**
     * Check if in Dwarven Mines
     */
    public static boolean inDwarvenMines() {
        return currentIsland == Island.DWARVEN_MINES;
    }

    /**
     * Check if in a mining area
     */
    public static boolean inMiningIsland() {
        return currentIsland == Island.DWARVEN_MINES || 
               currentIsland == Island.CRYSTAL_HOLLOWS ||
               currentIsland == Island.DEEP_CAVERNS ||
               currentIsland == Island.GOLD_MINE ||
               currentIsland == Island.MINESHAFT;
    }
}
