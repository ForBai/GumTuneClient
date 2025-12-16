package rosegold.gumtuneclient.modules.macro;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import rosegold.gumtuneclient.GumTuneClient;
import rosegold.gumtuneclient.config.GumTuneClientConfig;
import rosegold.gumtuneclient.utils.InventoryUtils;
import rosegold.gumtuneclient.utils.PlayerUtils;
import rosegold.gumtuneclient.utils.RotationUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static rosegold.gumtuneclient.GumTuneClient.mc;

public class AutoFish {
    private enum State {
        FISHING,
        WAITING_FOR_MOB,
        KILLING,
        WAITING_FOR_SPLIT,
        RECASTING
    }

    private State currentState = State.FISHING;
    private int delay = 0;
    private int mobWaitTicks = 0;
    private float savedYaw;
    private float savedPitch;
    private int rodSlot;


    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!GumTuneClientConfig.autoFish || mc.thePlayer == null || mc.theWorld == null) {
            currentState = State.FISHING;
            delay = 0;
            return;
        }
        if (event.phase != TickEvent.Phase.START) return;

        if (delay > 0) {
            delay--;
            return;
        }

        switch (currentState) {
            case FISHING:
                handleFishing();
                break;
            case WAITING_FOR_MOB:
                handleWaitingForMob();
                break;
            case KILLING:
                handleKilling();
                break;
            case WAITING_FOR_SPLIT:
                handleWaitingForSplit();
                break;
            case RECASTING:
                handleRecasting();
                break;
        }
    }

    private void handleFishing() {
        EntityFishHook myHook = null;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityFishHook) {
                EntityFishHook hook = (EntityFishHook) entity;
                if (hook.angler == mc.thePlayer) {
                    myHook = hook;
                    break;
                }
            }
        }

        if (myHook == null) {
            return;
        }

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityArmorStand) {
                EntityArmorStand armorStand = (EntityArmorStand) entity;
                if (armorStand.getDistanceSqToEntity(myHook) < 1.0) {
                    String customNameTag = armorStand.getCustomNameTag();
                    if (customNameTag != null && customNameTag.contains("!!!")) {
                        // Trigger catch
                        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem()); // Pull
                        
                        if (GumTuneClientConfig.autoFishKillMobs) {
                            currentState = State.WAITING_FOR_MOB;
                            mobWaitTicks = GumTuneClientConfig.autoFishMobWaitTime; 
                            delay = 5; 
                        } else {
                            scheduleRecast();
                        }
                        break;
                    }
                }
            }
        }
    }

    private EntityArmorStand targetMob;

    private EntityArmorStand findTarget() {
        List<EntityArmorStand> candidates = mc.theWorld.loadedEntityList.stream()
            .filter(e -> e instanceof EntityArmorStand)
            .filter(e -> e.getDistanceToEntity(mc.thePlayer) < 10)
            .map(e -> (EntityArmorStand) e)
            .filter(e -> e.hasCustomName() && e.getCustomNameTag().contains("❤"))
            .sorted(Comparator.comparingDouble(e -> e.getDistanceToEntity(mc.thePlayer)))
            .collect(Collectors.toList());
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private void handleWaitingForMob() {
        EntityArmorStand candidate = findTarget();

        if (candidate != null) {
            targetMob = candidate;
            savedYaw = mc.thePlayer.rotationYaw;
            savedPitch = mc.thePlayer.rotationPitch;
            rodSlot = mc.thePlayer.inventory.currentItem;
            
            // Switch to Hyperion
            int hyperionSlot = InventoryUtils.findItemInHotbar("Hyperion");
            if (hyperionSlot == -1) {
                 hyperionSlot = InventoryUtils.findItemInHotbar("Astraea");
                 if (hyperionSlot == -1) hyperionSlot = InventoryUtils.findItemInHotbar("Scylla");
                 if (hyperionSlot == -1) hyperionSlot = InventoryUtils.findItemInHotbar("Valkyrie");
                 if (hyperionSlot == -1) hyperionSlot = InventoryUtils.findItemInHotbar("Blade"); 
            }
            
            if (hyperionSlot != -1) {
                mc.thePlayer.inventory.currentItem = hyperionSlot;
            }
            
            // Start looking down smoothly
            RotationUtils.smoothLook(new RotationUtils.Rotation(90.0f, savedYaw), GumTuneClientConfig.autoFishLookSpeed);
            
            currentState = State.KILLING;
            delay = 0;
        } else {
            mobWaitTicks--;
            if (mobWaitTicks <= 0) {
                scheduleRecast();
            }
        }
    }

    private void handleKilling() {
         // Wait for rotation to finish
        if (!RotationUtils.done) return;

        // Check if mob is still alive (ArmorStand still exists and has Heart)
        if (targetMob == null || targetMob.isDead || !targetMob.hasCustomName() || !targetMob.getCustomNameTag().contains("❤") || targetMob.getDistanceToEntity(mc.thePlayer) > 12) {
            // Mob dead or lost
            // Check for split or new mobs
            currentState = State.WAITING_FOR_SPLIT;
            mobWaitTicks = GumTuneClientConfig.autoFishMobWaitTime;
            delay = 0;
            return;
        }

        PlayerUtils.rightClick();
        delay = GumTuneClientConfig.autoFishHyperionClickDelay; 
    }
    
    private void handleWaitingForSplit() {
        EntityArmorStand candidate = findTarget();
        if (candidate != null) {
            targetMob = candidate;
            currentState = State.KILLING;
            delay = 0;
        } else {
            mobWaitTicks--;
            if (mobWaitTicks <= 0) {
                 // No new mob found, finish up
                // Switch back to Rod
                mc.thePlayer.inventory.currentItem = rodSlot;
                
                // Look back smoothly
                RotationUtils.smoothLook(new RotationUtils.Rotation(savedPitch, savedYaw), GumTuneClientConfig.autoFishLookSpeed);
                
                scheduleRecast();
            }
        }
    }

    private void scheduleRecast() {
        currentState = State.RECASTING;
        long sleepTime = GumTuneClientConfig.autoFishRecastDelay;
        if (GumTuneClientConfig.autoFishRandomize) {
             sleepTime += (long) (Math.random() * GumTuneClientConfig.autoFishRandomness);
        }
        // Convert ms to ticks (approx 50ms per tick)
        delay = (int) (sleepTime / 50.0);
    }
    
    private void handleRecasting() {
        // Wait for rotation to finish
        if (!RotationUtils.done) return;
        
        // Ensure we are holding the rod
        if (rodSlot != -1 && mc.thePlayer.inventory.currentItem != rodSlot) {
            mc.thePlayer.inventory.currentItem = rodSlot;
        }
        
        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem()); // Throw
        currentState = State.FISHING;
        delay = 10;
    }
}
