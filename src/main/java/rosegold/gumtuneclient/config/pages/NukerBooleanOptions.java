package rosegold.gumtuneclient.config.pages;

import cc.polyfrost.oneconfig.config.annotations.Switch;

public class NukerBooleanOptions {
    @Switch(
            name = "Mine ALL Blocks In Front",
            description = "Mine all blocks in the way of the player"
    )
    public static boolean mineBlocksInFront = false;

    @Switch(
            name = "On Ground Only",
            description = "Mine only while the player is grounded"
    )
    public static boolean onGroundOnly = false;

    @Switch(
            name = "Preview",
            description = "Show which blocks are going to be mined"
    )
    public static boolean preview = false;

    @Switch(
            name = "Pickaxe Ability",
            description = "Use pickaxe ability when ready"
    )
    public static boolean pickaxeAbility = false;

    @Switch(
            name = "Only Visible Blocks",
            description = "Only mine blocks that are visible"
    )
    public static boolean onlyVisibleBlocks = false;
    
    @Switch(
            name = "Precision Mining",
            description = "Automaticly Looks at the particles spawned by precision mining"
    )
    public static boolean precisionMining = false;
}
