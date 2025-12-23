package rosegold.gumtuneclient.utils;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import rosegold.gumtuneclient.GumTuneClient;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

import static rosegold.gumtuneclient.GumTuneClient.mc;

/**
 * Utility class for block-related operations
 * Ported from 1.8.9 Forge to 1.21.8 Fabric
 * 
 * Note: Some advanced features like pathfinding are simplified/pending
 */
public class BlockUtils {

    // Queue for debug rendering of blocks
    public static ConcurrentLinkedQueue<BlockPos> blockPosConcurrentLinkedQueue = new ConcurrentLinkedQueue<>();
    
    // For debug line rendering
    public static Vec3d source;
    public static Vec3d destination;

    // Block side positions for targeting specific faces
    private static final HashMap<Direction, float[]> BLOCK_SIDES = new HashMap<Direction, float[]>() {{
        put(Direction.DOWN, new float[]{0.5f, 0.01f, 0.5f});
        put(Direction.UP, new float[]{0.5f, 0.99f, 0.5f});
        put(Direction.WEST, new float[]{0.01f, 0.5f, 0.5f});
        put(Direction.EAST, new float[]{0.99f, 0.5f, 0.5f});
        put(Direction.NORTH, new float[]{0.5f, 0.5f, 0.01f});
        put(Direction.SOUTH, new float[]{0.5f, 0.5f, 0.99f});
    }};

    /**
     * Find the closest block matching a predicate within a radius
     */
    public static BlockPos getClosestBlock(int radius, int height, int depth, Predicate<? super BlockPos> predicate) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return null;
        
        BlockPos playerPos = BlockPos.ofFloored(player.getX(), player.getY() + 1, player.getZ());
        BlockPos closest = null;

        for (BlockPos blockPos : BlockPos.iterate(
                playerPos.add(-radius, -depth, -radius),
                playerPos.add(radius, height, radius))) {
            if (predicate.test(blockPos)) {
                if (closest == null || player.squaredDistanceTo(blockPos.toCenterPos()) < player.squaredDistanceTo(closest.toCenterPos())) {
                    closest = blockPos.toImmutable();
                }
            }
        }

        return closest;
    }

    /**
     * Find the furthest block matching a predicate within a radius
     */
    public static BlockPos getFurthestBlock(int radius, int height, int depth, Predicate<? super BlockPos> predicate) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return null;
        
        BlockPos playerPos = BlockPos.ofFloored(player.getX(), player.getY() + 1, player.getZ());
        BlockPos furthest = null;

        for (BlockPos blockPos : BlockPos.iterate(
                playerPos.add(-radius, -depth, -radius),
                playerPos.add(radius, height, radius))) {
            if (predicate.test(blockPos)) {
                if (furthest == null || player.squaredDistanceTo(blockPos.toCenterPos()) > player.squaredDistanceTo(furthest.toCenterPos())) {
                    furthest = blockPos.toImmutable();
                }
            }
        }

        return furthest;
    }

    /**
     * Get the block state at a position
     */
    public static BlockState getBlockState(BlockPos pos) {
        if (mc.world == null) return null;
        return mc.world.getBlockState(pos);
    }

    /**
     * Check if a position is air
     */
    public static boolean isAir(BlockPos pos) {
        if (mc.world == null) return true;
        return mc.world.getBlockState(pos).isAir();
    }

    /**
     * Get the center of a block as a Vec3d
     */
    public static Vec3d getBlockCenter(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /**
     * Get distance from player to block
     */
    public static double getDistanceToBlock(BlockPos pos) {
        if (mc.player == null) return Double.MAX_VALUE;
        return mc.player.squaredDistanceTo(getBlockCenter(pos));
    }

    /**
     * Clear debug rendering queues
     */
    public static void clearDebugRendering() {
        blockPosConcurrentLinkedQueue.clear();
        source = null;
        destination = null;
    }
}
