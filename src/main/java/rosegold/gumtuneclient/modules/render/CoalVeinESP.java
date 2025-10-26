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

    private final List<VeinInfo> coalVeins = new CopyOnWriteArrayList<>();

    private static class VeinInfo {
        public final Set<BlockPos> blocks;
        public final BlockPos center;
        public final double distanceSqToPlayer;
        public final int lowestY;

        public VeinInfo(Set<BlockPos> blocks, BlockPos playerPos) {
            this.blocks = blocks;
            BlockPos sumPos = new BlockPos(0, 0, 0);
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

            for (BlockPos block : blocks) {
                sumPos = sumPos.add(block);
                minX = Math.min(minX, block.getX());
                minY = Math.min(minY, block.getY());
                minZ = Math.min(minZ, block.getZ());
                maxX = Math.max(maxX, block.getX());
                maxY = Math.max(maxY, block.getY());
                maxZ = Math.max(maxZ, block.getZ());
            }

            // Calculate center of the vein
            this.center = new BlockPos(
                (minX + maxX) / 2,
                (minY + maxY) / 2,
                (minZ + maxZ) / 2
            );
            this.distanceSqToPlayer = playerPos.distanceSq(this.center);
            this.lowestY = minY;
        }
    }

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
        final BlockPos playerPos = GumTuneClient.mc.thePlayer.getPosition();
        BlockPos lastPos = playerPos;

        // Render ESP boxes for the highlighted veins
        for (int i = 0; i < Math.min(coalVeins.size(), GumTuneClientConfig.maxHighlightedVeins); i++) {
            VeinInfo veinInfo = coalVeins.get(i);
            for (BlockPos block : veinInfo.blocks) {
                RenderUtils.renderEspBox(block, event.partialTicks, GumTuneClientConfig.coalVeinHighlightColor.getRGB());
            }
            if (i == 0) {
                RenderUtils.renderTracer(veinInfo.center, GumTuneClientConfig.coalVeinTracerColor.toJavaColor(), event.partialTicks);
            } else {
                RenderUtils.renderTracer(lastPos, veinInfo.center, GumTuneClientConfig.coalVeinTracerColor.toJavaColor(), event.partialTicks);
            }
            lastPos = veinInfo.center;
        }
    }

    private void findCoalVeins() {
        if (GumTuneClient.mc.thePlayer == null) return;

        BlockPos playerPos = GumTuneClient.mc.thePlayer.getPosition();
        Set<BlockPos> visited = new HashSet<>();
        List<VeinInfo> foundVeins = new ArrayList<>();

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
                            foundVeins.add(new VeinInfo(vein, playerPos));
                        }
                    }
                }
            }
        }

        // Create a path of the best veins
        List<VeinInfo> path = new ArrayList<>();
        if (!foundVeins.isEmpty()) {
            // Find the closest vein to the player to start the path
            foundVeins.sort((v1, v2) -> Double.compare(v1.distanceSqToPlayer, v2.distanceSqToPlayer));
            VeinInfo currentVein = foundVeins.remove(0);
            path.add(currentVein);

            // Iteratively find the next closest vein
            for (int i = 0; i < GumTuneClientConfig.maxHighlightedVeins - 1 && !foundVeins.isEmpty(); i++) {
                final BlockPos lastVeinCenter = currentVein.center;
                foundVeins.sort((v1, v2) -> Double.compare(v1.center.distanceSq(lastVeinCenter), v2.center.distanceSq(lastVeinCenter)));
                currentVein = foundVeins.remove(0);
                path.add(currentVein);
            }
        }

        coalVeins.clear();
        coalVeins.addAll(path);
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