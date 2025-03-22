package rosegold.gumtuneclient.utils.objects;

import net.minecraft.util.BlockPos;

public class BrokenBlock {
    public BlockPos pos;
    public long time;
    public int color;

    public BrokenBlock(BlockPos pos, long time, int color) {
        this.pos = pos;
        this.time = time;
        this.color = color;
    }
}