package rosegold.gumtuneclient.modules.render;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import rosegold.gumtuneclient.GumTuneClient;
import rosegold.gumtuneclient.config.GumTuneClientConfig;
import rosegold.gumtuneclient.utils.RenderUtils;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class CoalVeinESP {

    private final List<Set<BlockPos>> coalVeins = new CopyOnWriteArrayList<>();
    private BlockPos closestVein = null;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (GumTuneClient.mc.thePlayer == null || GumTuneClient.mc.theWorld == null) return;
        if (!GumTuneClientConfig.coalVeinESP) return;

        if (GumTuneClient.mc.thePlayer.ticksExisted % 20 == 0) {
            new Thread(this::findCoalVeins).start();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!GumTuneClientConfig.coalVeinESP || coalVeins.isEmpty()) return;

        for (Set<BlockPos> vein : coalVeins) {
            for (BlockPos block : vein) {
                RenderUtils.renderEspBox(block, event.partialTicks, GumTuneClientConfig.coalVeinHighlightColor.getRGB());
            }
        }

        if (closestVein != null) {
            RenderUtils.renderTracer(closestVein, GumTuneClientConfig.coalVeinTracerColor.toJavaColor(), event.partialTicks);
        }
    }

    private void findCoalVeins() {
        if (GumTuneClient.mc.thePlayer == null) return;

        List<Set<BlockPos>> newCoalVeins = new ArrayList<>();
        BlockPos newClosestVein = null;
        BlockPos playerPos = GumTuneClient.mc.thePlayer.getPosition();
        Set<BlockPos> visited = new HashSet<>();
        double closestDistSq = Double.MAX_VALUE;

        for (int x = -GumTuneClientConfig.coalVeinScanRadius; x <= GumTuneClientConfig.coalVeinScanRadius; x++) {
            for (int y = -GumTuneClientConfig.coalVeinScanRadius; y <= GumTuneClientConfig.coalVeinScanRadius; y++) {
                for (int z = -GumTuneClientConfig.coalVeinScanRadius; z <= GumTuneClientConfig.coalVeinScanRadius; z++) {
                    BlockPos currentPos = playerPos.add(x, y, z);
                    if (visited.contains(currentPos)) continue;

                    IBlockState blockState = GumTuneClient.mc.theWorld.getBlockState(currentPos);
                    if (blockState.getBlock() == Blocks.coal_ore) {
                        Set<BlockPos> vein = new HashSet<>();
                        findVein(currentPos, vein, visited);
                        if (vein.size() >= GumTuneClientConfig.coalVeinMinSize) {
                            newCoalVeins.add(vein);
                            for (BlockPos blockPos : vein) {
                                double distSq = blockPos.distanceSq(playerPos);
                                if (distSq < closestDistSq) {
                                    closestDistSq = distSq;
                                    newClosestVein = blockPos;
                                }
                            }
                        }
                    }
                }
            }
        }
        coalVeins.clear();
        coalVeins.addAll(newCoalVeins);
        closestVein = newClosestVein;
    }

    private void findVein(BlockPos start, Set<BlockPos> vein, Set<BlockPos> visited) {
        if (vein.size() > 100) return; // safety break
        Set<BlockPos> toSearch = new HashSet<>();
        toSearch.add(start);
        visited.add(start);

        while (!toSearch.isEmpty()) {
            BlockPos current = toSearch.iterator().next();
            toSearch.remove(current);
            vein.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos neighbor = current.add(dx, dy, dz);
                        if(visited.contains(neighbor)) continue;
                        if(GumTuneClient.mc.theWorld.getBlockState(neighbor).getBlock() == Blocks.coal_ore) {
                            if (neighbor.distanceSq(start) > 200) continue; // performance
                            toSearch.add(neighbor);
                            visited.add(neighbor);
                        }
                    }
                }
            }
        }
    }
}