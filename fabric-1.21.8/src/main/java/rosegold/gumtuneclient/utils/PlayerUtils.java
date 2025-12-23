package rosegold.gumtuneclient.utils;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.util.hit.HitResult;
import rosegold.gumtuneclient.GumTuneClient;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for player-related operations
 * Ported from 1.8.9 Forge to 1.21.8 Fabric
 */
public class PlayerUtils {
    private static final Random random = new Random();
    private static ScheduledExecutorService scheduler;
    
    public static boolean pickaxeAbilityReady = false;

    /**
     * Initialize player utilities
     */
    public static void init() {
        scheduler = Executors.newScheduledThreadPool(1);
        // Listen for pickaxe ability messages
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = StringUtils.removeFormatting(message.getString());
            if (text.contains(":") || text.contains(">")) return;
            
            if (text.startsWith("You used your Mining Speed Boost Pickaxe Ability!") || 
                text.startsWith("You used your Maniac Miner Pickaxe Ability!")) {
                pickaxeAbilityReady = false;
            } else if (text.equals("Mining Speed Boost is now available!") || 
                       text.equals("Maniac Miner is now available!")) {
                scheduler.schedule(() -> pickaxeAbilityReady = true, 
                                 random.nextInt(500) + 500, TimeUnit.MILLISECONDS);
            }
        });
    }

    /**
     * Swing the player's hand
     */
    public static void swingHand() {
        if (GumTuneClient.mc.player == null) return;
        GumTuneClient.mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }

    /**
     * Perform a right click action
     */
    public static void rightClick() {
        if (GumTuneClient.mc.interactionManager == null) return;
        // In modern Minecraft, right-click is handled through the interaction manager
        // This is a simplified version - full implementation would need more context
        if (GumTuneClient.mc.crosshairTarget != null && 
            GumTuneClient.mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            // Right click on block
            GumTuneClient.mc.interactionManager.interactBlock(
                GumTuneClient.mc.player,
                net.minecraft.util.Hand.MAIN_HAND,
                ((net.minecraft.util.hit.BlockHitResult) GumTuneClient.mc.crosshairTarget)
            );
        } else {
            // Right click in air
            GumTuneClient.mc.interactionManager.interactItem(
                GumTuneClient.mc.player,
                net.minecraft.util.Hand.MAIN_HAND
            );
        }
    }

    /**
     * Perform a left click action
     */
    public static void leftClick() {
        if (GumTuneClient.mc.interactionManager == null || GumTuneClient.mc.player == null) return;
        
        if (GumTuneClient.mc.crosshairTarget != null && 
            GumTuneClient.mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            // Attack block
            GumTuneClient.mc.interactionManager.attackBlock(
                ((net.minecraft.util.hit.BlockHitResult) GumTuneClient.mc.crosshairTarget).getBlockPos(),
                ((net.minecraft.util.hit.BlockHitResult) GumTuneClient.mc.crosshairTarget).getSide()
            );
        } else if (GumTuneClient.mc.crosshairTarget != null && 
                   GumTuneClient.mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            // Attack entity
            GumTuneClient.mc.interactionManager.attackEntity(
                GumTuneClient.mc.player,
                ((net.minecraft.util.hit.EntityHitResult) GumTuneClient.mc.crosshairTarget).getEntity()
            );
        }
        
        swingHand();
    }

    /**
     * Reset state on world change
     */
    public static void onWorldUnload() {
        pickaxeAbilityReady = false;
    }

    /**
     * Shutdown the scheduler to prevent thread leaks
     */
    public static void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
