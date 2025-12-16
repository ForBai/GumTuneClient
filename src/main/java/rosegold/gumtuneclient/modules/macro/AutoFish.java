package rosegold.gumtuneclient.modules.macro;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import rosegold.gumtuneclient.GumTuneClient;
import rosegold.gumtuneclient.config.GumTuneClientConfig;

import static rosegold.gumtuneclient.GumTuneClient.mc;

public class AutoFish {
    private int delay = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!GumTuneClientConfig.autoFish || mc.thePlayer == null || mc.theWorld == null) return;
        if (event.phase != TickEvent.Phase.START) return;

        if (delay > 0) {
            delay--;
            return;
        }

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

        if (myHook == null) return;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityArmorStand) {
                EntityArmorStand armorStand = (EntityArmorStand) entity;
                if (armorStand.getDistanceSqToEntity(myHook) < 1.0) { // Distance squared check
                    String customNameTag = armorStand.getCustomNameTag();
                    if (customNameTag != null && customNameTag.contains("!!!")) {
                        // Trigger catch
                        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem());  // Pull
                        delay = 20; // Set check delay to 1 second
                        
                        new Thread(() -> {
                            try {
                                long sleepTime = GumTuneClientConfig.autoFishRecastDelay;
                                if (GumTuneClientConfig.autoFishRandomize) {
                                    sleepTime += (long) (Math.random() * GumTuneClientConfig.autoFishRandomness);
                                }
                                Thread.sleep(sleepTime);
                                if (GumTuneClientConfig.autoFish && mc.thePlayer != null) {
                                     mc.addScheduledTask(() -> {
                                         mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem()); // Throw
                                     });
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                        
                        break;
                    }
                }
            }
        }
    }
}
