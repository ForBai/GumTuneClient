package rosegold.gumtuneclient.modules.mining;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import rosegold.gumtuneclient.GumTuneClient;
import rosegold.gumtuneclient.config.GumTuneClientConfig;
import rosegold.gumtuneclient.config.pages.NukerBlockFilter;
import rosegold.gumtuneclient.config.pages.NukerBooleanOptions;
import rosegold.gumtuneclient.config.pages.NukerSliderOptions;
import rosegold.gumtuneclient.events.MillisecondEvent;
import rosegold.gumtuneclient.events.PacketReceivedEvent;
import rosegold.gumtuneclient.events.PlayerMoveEvent;
import rosegold.gumtuneclient.events.SecondEvent;
import rosegold.gumtuneclient.modules.macro.GemstoneMacro;
import rosegold.gumtuneclient.modules.render.ESPs;
import rosegold.gumtuneclient.utils.*;
import rosegold.gumtuneclient.utils.objects.BrokenBlock;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Nuker {
    public static boolean enabled;
    private final ArrayList<BlockPos> broken = new ArrayList<>();
    public static BlockPos blockPos;
    private long lastBroken = 0;
    private long stuckTimestamp = 0;
    private long abilityTimestamp = 0;
    private BlockPos current;
    private final ArrayList<BlockPos> blocksInRange = new ArrayList<>();
    private boolean particleSpawned;
    private final List<BrokenBlock> brokenBlocks = new ArrayList<>();
    private Vec3 particlePosition;
    private Vec3 cachedEyes;


    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (!GumTuneClientConfig.nuker) return;
        if (Keyboard.getEventKeyState()) return;
        int eventKey = Keyboard.getEventKey();
        ArrayList<Integer> keyBinds = GumTuneClientConfig.nukerKeyBind.getKeyBinds();
        if (keyBinds.size() > 0 && keyBinds.get(0) == eventKey) {
            enabled = !enabled;
            ModUtils.sendMessage((enabled ? "Enabled" : "Disabled") + " Nuker");
            if (enabled) {
                if (NukerBlockFilter.nukerBlockFilterFrozenTreasure) {
                    if (GumTuneClientConfig.frozenTreasureESP) {
                        ModUtils.sendMessage("&cIf nuker doesn't work for frozen treasures, try disabling entity culling in patcher!");
                    } else {
                        ModUtils.sendMessage("&cEnable frozen treasure ESP for frozen treasure nuker to work!");
                    }
                }
                if (GumTuneClientConfig.nukerShape == 2 && (NukerBlockFilter.nukerBlockFilterHardstone || NukerBlockFilter.nukerBlockFilterStone) && !GumTuneClientConfig.phaseCameraThroughBlocks) {
                    ModUtils.sendMessage("&cRecommended to turn on Phase Camera Through Blocks when using tunnel shape and hardstone filter!");
                }
            } else {
                if (current != null && GumTuneClient.mc.thePlayer != null) {
                    GumTuneClient.mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK,
                            blockPos,
                            EnumFacing.DOWN)
                    );
                }
                RotationUtils.reset();
                blockPos = null;
                current = null;
                particleSpawned = false;
            }
        }
    }

    @SubscribeEvent
    public void onSecond(SecondEvent event) {
        if (GumTuneClient.mc.thePlayer == null) return;
        if (!isEnabled()) return;
        if (broken.size() > 0) broken.clear();
    }

    @SubscribeEvent
    public void onOverlayRender(RenderGameOverlayEvent.Post event) {
        if (!isEnabled()) return;
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            FontUtils.drawScaledString("Stuck timer: " + (System.currentTimeMillis() - stuckTimestamp), 1, 300, 100, true);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (!isEnabled()) return;

        blocksInRange.clear();
        EntityPlayerSP player = GumTuneClient.mc.thePlayer;
        BlockPos playerPos = new BlockPos((int) Math.floor(player.posX), (int) Math.floor(player.posY) + 1, (int) Math.floor(player.posZ));
        Vec3i vec3Top = new Vec3i(NukerSliderOptions.nukerRange, NukerSliderOptions.nukerHeight, NukerSliderOptions.nukerRange);
        Vec3i vec3Bottom = new Vec3i(NukerSliderOptions.nukerRange, NukerSliderOptions.nukerDepth, NukerSliderOptions.nukerRange);

        for (BlockPos blockPos : BlockPos.getAllInBox(playerPos.subtract(vec3Bottom), playerPos.add(vec3Top))) {
            Vec3 target = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
            if (Math.abs(RotationUtils.wrapAngleTo180(RotationUtils.fovToVec3(target) - RotationUtils.wrapAngleTo180(GumTuneClient.mc.thePlayer.rotationYaw))) < (float) NukerSliderOptions.nukerFieldOfView / 2)
                if (NukerBooleanOptions.onlyVisibleBlocks) {
                    if (BlockUtils.isBlockVisible(blockPos))
                        blocksInRange.add(blockPos);
                } else blocksInRange.add(blockPos);
        }

        if (System.currentTimeMillis() - stuckTimestamp > NukerSliderOptions.nukerStuckTimer) {
            stuckTimestamp = System.currentTimeMillis();
            blockPos = null;
            current = null;
            particleSpawned = false;
        }

        if (current != null) GumTuneClient.mc.thePlayer.swingItem();
    }

    @SubscribeEvent
    public void onMillisecond(MillisecondEvent event) {
        if (!isEnabled()) {
            current = null;
            if (broken.size() > 0) broken.clear();
            particleSpawned = false;
            return;
        }

        if (NukerBooleanOptions.onGroundOnly && !GumTuneClient.mc.thePlayer.onGround) return;

        if (NukerBooleanOptions.pickaxeAbility && PlayerUtils.pickaxeAbilityReady && System.currentTimeMillis() - abilityTimestamp > 1000) {
            abilityTimestamp = System.currentTimeMillis();
            GumTuneClient.mc.playerController.sendUseItem(
                    GumTuneClient.mc.thePlayer,
                    GumTuneClient.mc.theWorld,
                    GumTuneClient.mc.thePlayer.getHeldItem()
            );

            return;
        }

        if (event.timestamp - lastBroken > 1000f / NukerSliderOptions.nukerSpeed) {
            lastBroken = event.timestamp;
            if (broken.size() > NukerSliderOptions.nukerPinglessCutoff) broken.clear();

            if (NukerBooleanOptions.mineBlocksInFront) {
                blockPos = blockInFront();

                if (blockPos != null) {
                    if (current != null && (current.compareTo(blockPos) != 0 || (getBlockState(current).getBlock() != getBlockState(blockPos).getBlock()))) {
                        current = null;
                    }
                    if (isSlow(getBlockState(blockPos))) {
                        if (current == null) {
                            mineBlock(blockPos);
                        }
                    } else {
                        pinglessMineBlock(blockPos);
                        current = null;
                        particleSpawned = false;
                    }
                    return;
                }
            }

            if (current == null) {
                switch (GumTuneClientConfig.nukerAlgorithm) {
                    case 0:
                        blockPos = BlockUtils.getClosestBlock(NukerSliderOptions.nukerRange, NukerSliderOptions.nukerHeight, NukerSliderOptions.nukerDepth, this::canMine);
                        break;
                    case 1:
                        blockPos = BlockUtils.getEasiestBlock(NukerSliderOptions.nukerRange, NukerSliderOptions.nukerHeight, NukerSliderOptions.nukerDepth, this::canMine);
                        break;
                }
            }

            if (blockPos != null) {
                if (current != null && (current.compareTo(blockPos) != 0 || getBlockState(current).getBlock() != getBlockState(blockPos).getBlock())) {
                    current = null;
                }
                if (isSlow(getBlockState(blockPos))) {
                    if (current == null) {
                        mineBlock(blockPos);
                    }
                } else {
                    pinglessMineBlock(blockPos);
                    current = null;
                    particleSpawned = false;
                }
            } else {
                current = null;
            }
        }
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (!isEnabled()) return;

        // Render the target block in gray
        RenderUtils.renderEspBox(blockPos, event.partialTicks, Color.GRAY.getRGB());

        // Render the currently mining block with expanding effect
        /*if (current != null) {
            float breakProgress = getBlockBreakingProgress(current);
            double expansion = 0.5 * breakProgress; // Expands to half the block size
            System.out.println(breakProgress);
            AxisAlignedBB blockAABB = new AxisAlignedBB(
                    current.getX() + 0.5 - expansion,
                    current.getY() + 0.5 - expansion,
                    current.getZ() + 0.5 - expansion,
                    current.getX() + 0.5 + expansion,
                    current.getY() + 0.5 + expansion,
                    current.getZ() + 0.5 + expansion
            );

            RenderUtils.renderBoundingBox(blockAABB, Color.BLUE.getRGB(), 0.5f);
        }*/

        // Render fading boxes for broken blocks
        /*long currentTime = System.currentTimeMillis();
        brokenBlocks.removeIf(brokenBlock -> currentTime - brokenBlock.time > NukerSliderOptions.nukerFadeTime);
        for (BrokenBlock brokenBlock : brokenBlocks) {
            long timeSinceBroken = currentTime - brokenBlock.time;
            float opacity = 1.0f - (float) timeSinceBroken / NukerSliderOptions.nukerFadeTime;
            if (opacity > 0) {
                AxisAlignedBB blockAABB = new AxisAlignedBB(
                        brokenBlock.pos.getX(),
                        brokenBlock.pos.getY(),
                        brokenBlock.pos.getZ(),
                        brokenBlock.pos.getX() + 1,
                        brokenBlock.pos.getY() + 1,
                        brokenBlock.pos.getZ() + 1
                );
                RenderUtils.renderBoundingBox(blockAABB, brokenBlock.color, opacity);
            }
        }*/

        if (NukerBooleanOptions.preview) {
            RenderUtils.renderEspBlocks(blocksInRange);
        }

        // Render small white box at particle location
        /*if (particlePosition != null) {
            RenderUtils.renderSmallBox(particlePosition, Color.WHITE.getRGB());
        }*/
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onUpdatePre(PlayerMoveEvent.Pre pre) {
        if (!isEnabled()) return;
        if (GumTuneClientConfig.nukerRotationType == 0) return;
        if (GumTuneClientConfig.powderChestPauseNukerMode == 2 && PowderChestSolver.particle != null) return;
        switch (GumTuneClientConfig.nukerRotationType) {
            case 1:
                if (blockPos != null) {
                    RotationUtils.look(RotationUtils.getRotation(blockPos));
                }
                if (current != null) {
                    RotationUtils.look(RotationUtils.getRotation(current));
                }
                break;
            case 2:
                RotationUtils.updateServerLook();
                break;
        }
    }

    @SubscribeEvent
    public void onParticelPacket(PacketReceivedEvent event) {
        if (!isEnabled() || current == null || !NukerBooleanOptions.precisionMining) return;
        if (event.packet instanceof S2APacketParticles) {
            S2APacketParticles packet = (S2APacketParticles) event.packet;
            if (packet.getParticleType().equals(EnumParticleTypes.CRIT)) {
                double x = packet.getXCoordinate();
                double y = packet.getYCoordinate();
                double z = packet.getZCoordinate();

                // Check if particle is close to the current block
                if (Math.abs(x - (current.getX() + 0.5)) < 0.7 &&
                        Math.abs(y - (current.getY() + 0.5)) < 0.7 &&
                        Math.abs(z - (current.getZ() + 0.5)) < 0.7) {


                    // Get player eye position
                    Vec3 eyes = GumTuneClient.mc.thePlayer.getPositionEyes(1f);
                    cachedEyes = eyes;
                    // Create vector for particle position
                    Vec3 particlePos = new Vec3(x, y, z);
                    particlePosition = particlePos;

                    ArrayList<Vec3> points = new ArrayList<>();
                    //add the corner points of the block
                    points.add(new Vec3(current.getX(), current.getY(), current.getZ()));
                    points.add(new Vec3(current.getX(), current.getY() + 1, current.getZ()));
                    points.add(new Vec3(current.getX() + 1, current.getY(), current.getZ()));
                    points.add(new Vec3(current.getX() + 1, current.getY() + 1, current.getZ()));
                    points.add(new Vec3(current.getX() + 1, current.getY() + 1, current.getZ() + 1));
                    points.add(new Vec3(current.getX(), current.getY() + 1, current.getZ() + 1));
                    points.add(new Vec3(current.getX(), current.getY(), current.getZ() + 1));
                    points.add(new Vec3(current.getX() + 1, current.getY(), current.getZ() + 1));
                    //ray trace all points and remove the ones that are not in sight
                    for (int i = 0; i < points.size(); i++) {
                        Vec3 point = points.get(i);
                        if (GumTuneClient.mc.theWorld.rayTraceBlocks(eyes, point) != null) {
                            points.remove(i);
                            i--;
                        }
                    }
                    points.add(eyes);

                    if (BlockUtils.isPointInVolume(particlePos, points)) {
                        particleSpawned = true;
                        RotationUtils.serverSmoothLook(RotationUtils.getRotation(particlePos), NukerSliderOptions.precisionMiningAimTime);
                    }
                }
            }
        }
    }


    private void mineBlock(BlockPos blockPos) {
        if (PowderChestSolver.particle == null) {
            RotationUtils.serverSmoothLook(RotationUtils.getRotation(blockPos), GumTuneClientConfig.nukerRotationSpeed);
        }
        breakBlock(blockPos);
        stuckTimestamp = System.currentTimeMillis();
        current = blockPos;
        brokenBlocks.add(new BrokenBlock(blockPos, System.currentTimeMillis(), Color.BLUE.getRGB()));
    }

    private void pinglessMineBlock(BlockPos blockPos) {
        if (PowderChestSolver.particle == null) {
            RotationUtils.serverSmoothLook(RotationUtils.getRotation(blockPos), GumTuneClientConfig.nukerRotationSpeed);
        }
        GumTuneClient.mc.thePlayer.swingItem();
        stuckTimestamp = System.currentTimeMillis();
        breakBlock(blockPos);
        broken.add(blockPos);
        brokenBlocks.add(new BrokenBlock(blockPos, System.currentTimeMillis(), Color.green.getRGB()));
    }

    private void breakBlock(BlockPos blockPos) {
        EnumFacing enumFacing = BlockUtils.calculateEnumfacing(new Vec3(blockPos).add(RandomUtils.randomVec()));
        if (enumFacing != null) {
            GumTuneClient.mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                    blockPos,
                    enumFacing
            ));
        } else {
            GumTuneClient.mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                    blockPos,
                    GumTuneClient.mc.thePlayer.getHorizontalFacing().getOpposite()
            ));
        }
    }

    public static boolean isEnabled() {
        return (GumTuneClientConfig.powderChestPauseNukerMode != 1 || PowderChestSolver.particle == null) && enabled && GumTuneClientConfig.nuker && GumTuneClient.mc.thePlayer != null && GumTuneClient.mc.theWorld != null;
    }

    private boolean isLookingAtBlock(BlockPos blockPos) {
        AxisAlignedBB aabb = AxisAlignedBB.fromBounds(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ() + 1);
        Vec3 position = GumTuneClient.mc.thePlayer.getPositionEyes(1f);
        Vec3 look = VectorUtils.scaleVec(GumTuneClient.mc.thePlayer.getLook(1f), 0.2f);
        for (int i = 0; i < 25; i++) {
            if (aabb.minX <= position.xCoord && aabb.maxX >= position.xCoord && aabb.minY <= position.yCoord && aabb.maxY >= position.yCoord && aabb.minZ <= position.zCoord && aabb.maxZ >= position.zCoord) {
                return true;
            }
            position = position.add(look);
        }

        return false;
    }

    private BlockPos blockInFront() {
        EntityPlayerSP player = GumTuneClient.mc.thePlayer;
        BlockPos playerPos = new BlockPos((int) Math.floor(player.posX), (int) Math.floor(player.posY), (int) Math.floor(player.posZ));
        Vec3i axisVector = player.getHorizontalFacing().getDirectionVec();

        if (getBlockState(playerPos).getBlock() != Blocks.air && !(getBlockState(playerPos).getBlock() instanceof BlockLiquid) &&
                getBlockState(playerPos).getBlock() != Blocks.bedrock && !broken.contains(playerPos)) {
            return playerPos;
        }
        if (getBlockState(playerPos.add(new Vec3i(0, 1, 0))).getBlock() != Blocks.air && !(getBlockState(playerPos).getBlock() instanceof BlockLiquid) &&
                getBlockState(playerPos).getBlock() != Blocks.bedrock && !broken.contains(playerPos.add(new Vec3i(0, 1, 0)))) {
            return playerPos.add(new Vec3i(0, 1, 0));
        }
        if (getBlockState(playerPos.add(axisVector)).getBlock() != Blocks.air && !(getBlockState(playerPos).getBlock() instanceof BlockLiquid) &&
                getBlockState(playerPos).getBlock() != Blocks.bedrock && !broken.contains(playerPos.add(axisVector))) {
            return playerPos.add(axisVector);
        }
        if (getBlockState(playerPos.add(axisVector).add(new Vec3i(0, 1, 0))).getBlock() != Blocks.air && !(getBlockState(playerPos).getBlock() instanceof BlockLiquid) &&
                getBlockState(playerPos).getBlock() != Blocks.bedrock && !broken.contains(playerPos.add(axisVector).add(new Vec3i(0, 1, 0)))) {
            return playerPos.add(axisVector).add(new Vec3i(0, 1, 0));
        }
        return null;
    }

    private boolean canMine(BlockPos blockPos) {
        if (canMineBlockType(blockPos) && !broken.contains(blockPos) && blocksInRange.contains(blockPos)) {
            EntityPlayerSP player = GumTuneClient.mc.thePlayer;
            EnumFacing axis = player.getHorizontalFacing();

            Vec3i ray = VectorUtils.addVector(VectorUtils.addVector(new Vec3i((int) Math.floor(player.posX), 0, (int) Math.floor(player.posZ)), VectorUtils.scaleVec(axis.getDirectionVec(), NukerSliderOptions.nukerForwardsOffset)), VectorUtils.scaleVec(axis.rotateY().getDirectionVec(), NukerSliderOptions.nukerSidewaysOffset));

            switch (GumTuneClientConfig.nukerShape) {
                case 1:
                    for (int i = 0; i < 6; i++) {
                        if (ray.getX() == blockPos.getX() && ray.getZ() == blockPos.getZ()) {
                            return true;
                        }

                        ray = VectorUtils.addVector(ray, axis.getDirectionVec());
                    }

                    return false;
                case 2:
                    for (int i = 0; i < 6; i++) {
                        if (ray.getX() == blockPos.getX() && ray.getZ() == blockPos.getZ()) {
                            return true;
                        }

                        if (axis.getAxis() == EnumFacing.Axis.Z) {
                            if (ray.getX() + 2 == blockPos.getX() && ray.getZ() == blockPos.getZ()) {
                                return true;
                            }
                            if (ray.getX() - 2 == blockPos.getX() && ray.getZ() == blockPos.getZ()) {
                                return true;
                            }
                        } else if (axis.getAxis() == EnumFacing.Axis.X) {
                            if (ray.getX() == blockPos.getX() && ray.getZ() + 2 == blockPos.getZ()) {
                                return true;
                            }
                            if (ray.getX() == blockPos.getX() && ray.getZ() - 2 == blockPos.getZ()) {
                                return true;
                            }
                        }

                        ray = VectorUtils.addVector(ray, axis.getDirectionVec());
                    }

                    return false;
                case 3:
                    return isLookingAtBlock(blockPos);
                case 4:
                    return GemstoneMacro.extraBlocksInTheWay.contains(blockPos);
            }

            return true;
        }

        return false;
    }

    private boolean canMineBlockType(BlockPos bp) {
        IBlockState blockState = getBlockState(bp);
        Block block = blockState.getBlock();
        if (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS &&
                NukerBlockFilter.nukerBlockFilterHardstone &&
                (block == Blocks.stone || block == Blocks.stained_hardened_clay)) return true;

        if (
                (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS ||
                        LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES ||
                        LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterGemstones &&
                        (block == Blocks.stained_glass_pane ||
                                block == Blocks.stained_glass)
        ) return true;

        if (
                (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS ||
                        LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES ||
                        LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterJade &&
                        ((block == Blocks.stained_glass_pane && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.LIME) ||
                                (block == Blocks.stained_glass && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.LIME))
        ) return true;
        if (
                (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS ||
                        LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES ||
                        LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterAmber &&
                        ((block == Blocks.stained_glass_pane && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.ORANGE) ||
                                (block == Blocks.stained_glass && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.ORANGE))
        ) return true;
        if (
                (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS ||
                        LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES ||
                        LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterTopaz &&
                        ((block == Blocks.stained_glass_pane && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.YELLOW) ||
                                (block == Blocks.stained_glass && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.YELLOW))
        ) return true;
        if (
                (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS ||
                        LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES ||
                        LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterSapphire &&
                        ((block == Blocks.stained_glass_pane && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.LIGHT_BLUE) ||
                                (block == Blocks.stained_glass && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.LIGHT_BLUE))
        ) return true;
        if (
                (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS ||
                        LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES ||
                        LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterAmethyst &&
                        ((block == Blocks.stained_glass_pane && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.PURPLE) ||
                                (block == Blocks.stained_glass && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.PURPLE))
        ) return true;
        if (
                (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS ||
                        LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES ||
                        LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterJasper &&
                        ((block == Blocks.stained_glass_pane && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.PINK) ||
                                (block == Blocks.stained_glass && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.PINK))
        ) return true;
        if (
                (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS ||
                        LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES ||
                        LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterRuby &&
                        ((block == Blocks.stained_glass_pane && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.RED) ||
                                (block == Blocks.stained_glass && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.RED))
        ) return true;
        if (
                (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS ||
                        LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES ||
                        LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterOpal &&
                        ((block == Blocks.stained_glass_pane && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.WHITE) ||
                                (block == Blocks.stained_glass && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.WHITE))
        ) return true;
        if (
                (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS ||
                        LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES ||
                        LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterOnyx &&
                        ((block == Blocks.stained_glass_pane && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.BLACK) ||
                                (block == Blocks.stained_glass && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.BLACK))
        ) return true;
        if (
                (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS ||
                        LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES ||
                        LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterAquamarine &&
                        ((block == Blocks.stained_glass_pane && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.BLUE) ||
                                (block == Blocks.stained_glass && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.BLUE))
        ) return true;
        if (
                (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS ||
                        LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES ||
                        LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterCitrine &&
                        ((block == Blocks.stained_glass_pane && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.BROWN) ||
                                (block == Blocks.stained_glass && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.BROWN))

        ) return true;
        if (
                (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS ||
                        LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES ||
                        LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterPeridot &&
                        ((block == Blocks.stained_glass_pane && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.GREEN) ||
                                (block == Blocks.stained_glass && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.GREEN))

        ) return true;

        if (
                (LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES || LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterUmber &&
                        ((block == Blocks.double_stone_slab2 && blockState.getValue(BlockStoneSlabNew.VARIANT) == BlockStoneSlabNew.EnumType.RED_SANDSTONE) ||
                                block == Blocks.hardened_clay ||
                                (block == Blocks.stained_hardened_clay && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.BROWN))
        ) return true;

        if (
                (LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES || LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterTungsten &&
                        (block == Blocks.cobblestone || block == Blocks.clay || (block == Blocks.stone_slab && blockState.getValue(BlockStoneSlab.VARIANT) == BlockStoneSlab.EnumType.COBBLESTONE) || block == Blocks.stone_stairs)
        ) return true;

        if (
                (LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES || LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) &&
                        NukerBlockFilter.nukerBlockFilterGlacite &&
                        block == Blocks.packed_ice

        ) return true;


        if (NukerBlockFilter.nukerBlockFilterMithril &&
                (
                        (LocationUtils.currentIsland == LocationUtils.Island.CRYSTAL_HOLLOWS &&
                                (block == Blocks.prismarine ||
                                        (block == Blocks.wool && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.LIGHT_BLUE))
                        ) || (LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES &&
                                (block == Blocks.prismarine ||
                                        (block == Blocks.wool && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.LIGHT_BLUE) ||
                                        (block == Blocks.wool && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.GRAY) ||
                                        (block == Blocks.stained_hardened_clay && blockState.getValue(BlockColored.COLOR) == EnumDyeColor.CYAN)
                                )
                        )
                )
        ) return true;

        if (NukerBlockFilter.nukerBlockFilterTitanium &&
                LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES && block == Blocks.stone && blockState.getValue(BlockStone.VARIANT) == BlockStone.EnumType.DIORITE_SMOOTH
        ) return true;

        if (LocationUtils.currentIsland == LocationUtils.Island.CRIMSON_ISLE &&
                NukerBlockFilter.nukerBlockFilterExcavatables &&
                (block == Blocks.sand ||
                        block == Blocks.mycelium)) return true;

        if (NukerBlockFilter.nukerBlockFilterGold &&
                block == Blocks.gold_block) return true;

        if (NukerBlockFilter.nukerBlockFilterStone &&
                (block == Blocks.stone ||
                        block == Blocks.cobblestone)) return true;

        if (NukerBlockFilter.nukerBlockFilterOres &&
                (block == Blocks.coal_ore ||
                        block == Blocks.lapis_ore ||
                        block == Blocks.iron_ore ||
                        block == Blocks.gold_ore ||
                        block == Blocks.redstone_ore ||
                        block == Blocks.lit_redstone_ore ||
                        block == Blocks.diamond_ore ||
                        block == Blocks.emerald_ore ||
                        block == Blocks.quartz_ore)) return true;

        if (NukerBlockFilter.nukerBlockFilterObsidian &&
                block == Blocks.obsidian) return true;

        if (NukerBlockFilter.nukerBlockFilterCrops &&
                ((block == Blocks.carrots && blockState.getValue(BlockCrops.AGE) == 7) ||
                        (block == Blocks.potatoes && blockState.getValue(BlockCrops.AGE) == 7) ||
                        (block == Blocks.reeds && getBlockState(bp.add(0, -1, 0)).getBlock() == Blocks.reeds && getBlockState(bp.add(0, 1, 0)).getBlock() == Blocks.reeds) ||
                        (block == Blocks.cocoa && blockState.getValue(BlockCocoa.AGE) == 2) ||
                        block == Blocks.melon_block ||
                        block == Blocks.pumpkin ||
                        (block == Blocks.cactus && getBlockState(bp.add(0, -1, 0)).getBlock() == Blocks.cactus) ||
                        block == Blocks.brown_mushroom ||
                        block == Blocks.red_mushroom ||
                        (block == Blocks.nether_wart && blockState.getValue(BlockNetherWart.AGE) == 3) ||
                        (block == Blocks.wheat && blockState.getValue(BlockCrops.AGE) == 7))) return true;

        if (NukerBlockFilter.nukerBlockFilterFoliage &&
                (block == Blocks.leaves ||
                        block == Blocks.leaves2 ||
                        block == Blocks.tallgrass ||
                        block == Blocks.red_flower ||
                        block == Blocks.yellow_flower ||
                        block == Blocks.double_plant ||
                        block == Blocks.deadbush)) return true;

        if (NukerBlockFilter.nukerBlockFilterWood &&
                (block == Blocks.log ||
                        block == Blocks.log2)) return true;

        if (NukerBlockFilter.nukerBlockFilterSand &&
                (block == Blocks.sand || block == Blocks.gravel)) return true;

        if (NukerBlockFilter.nukerBlockFilterDirt &&
                (block == Blocks.dirt ||
                        block == Blocks.grass ||
                        block == Blocks.farmland)) return true;

        if (NukerBlockFilter.nukerBlockFilterGlowstone &&
                block == Blocks.glowstone) return true;

        if (NukerBlockFilter.nukerBlockFilterIce &&
                block == Blocks.ice) return true;

        if (NukerBlockFilter.nukerBlockFilterFrozenTreasure &&
                LocationUtils.currentIsland == LocationUtils.Island.JERRY_WORKSHOP &&
                ESPs.frozenTreasures.contains(bp)) {
            ESPs.frozenTreasures.remove(bp);
            ESPs.checked.clear(); // make nuker faster
            return true;
        }

        return NukerBlockFilter.nukerBlockFilterNetherrack &&
                block == Blocks.netherrack;
    }

    private float getBlockBreakingProgress(BlockPos blockPos) {
        if (GumTuneClient.mc.playerController == null) return 0.0f;
        if (current == null) return 0.0f;
        if (!current.equals(blockPos)) return 0.0f;
        Object damage = ReflectionUtils.field(GumTuneClient.mc.playerController, "curBlockDamageMP");
        if (damage instanceof Float) {
            return (Float) damage;
        }
        return 0.0f;
    }

    private boolean isSlow(IBlockState blockState) {
        Block block = blockState.getBlock();
        return block == Blocks.prismarine || block == Blocks.wool || block == Blocks.stained_hardened_clay ||
                block == Blocks.gold_block || block == Blocks.stained_glass_pane || block == Blocks.stained_glass ||
                block == Blocks.glowstone || block == Blocks.chest ||
                (block == Blocks.stone && blockState.getValue(BlockStone.VARIANT) == BlockStone.EnumType.DIORITE_SMOOTH) ||
                block == Blocks.obsidian || (LocationUtils.currentIsland == LocationUtils.Island.THE_RIFT && block == Blocks.lapis_ore) ||
                block == Blocks.packed_ice || block == Blocks.clay ||
                ((LocationUtils.currentIsland == LocationUtils.Island.THE_HUB) && block == Blocks.sand) ||
                ((LocationUtils.currentIsland == LocationUtils.Island.THE_HUB) && block == Blocks.gravel) ||
                ((LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT || LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES) && block == Blocks.double_stone_slab2 && blockState.getValue(BlockStoneSlabNew.VARIANT) == BlockStoneSlabNew.EnumType.RED_SANDSTONE) ||
                ((LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT || LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES) && block == Blocks.hardened_clay) ||
                ((LocationUtils.currentIsland == LocationUtils.Island.DWARVEN_MINES || LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT) && NukerBlockFilter.nukerBlockFilterTungsten && block == Blocks.cobblestone) ||
                (LocationUtils.currentIsland == LocationUtils.Island.MINESHAFT && block == Blocks.cobblestone);
    }

    private IBlockState getBlockState(BlockPos blockPos) {
        return GumTuneClient.mc.theWorld.getBlockState(blockPos);
    }

    private boolean isPositionBetween(Vec3 position, Vec3 pos1, Vec3 pos2, double tolerance) {
        // If pos1 and pos2 are the same, check if the position is within the tolerance
        if (pos1.equals(pos2)) {
            return position.distanceTo(pos1) <= tolerance;
        }

        // Calculate the distance from the point to the line
        double distance = position.distanceTo(getClosestPointOnLine(position, pos1, pos2));

        return distance <= tolerance;
    }

    private Vec3 getClosestPointOnLine(Vec3 point, Vec3 lineStart, Vec3 lineEnd) {
        Vec3 lineDirection = lineEnd.subtract(lineStart).normalize();
        Vec3 pointToLineStart = point.subtract(lineStart);
        double dotProduct = pointToLineStart.dotProduct(lineDirection);
        return lineStart.add(new Vec3(lineDirection.xCoord * dotProduct, lineDirection.yCoord * dotProduct, lineDirection.zCoord * dotProduct));
    }
}
