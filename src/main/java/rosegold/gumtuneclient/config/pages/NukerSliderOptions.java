package rosegold.gumtuneclient.config.pages;

import cc.polyfrost.oneconfig.config.annotations.Slider;

public class NukerSliderOptions {
    @Slider(
            name = "Speed",
            description = "Blocks per second",
            min = 0, max = 80
    )
    public static int nukerSpeed = 20;

    @Slider(
            name = "Range",
            description = "Range in blocks",
            min = 1, max = 5,
            step = 1
    )
    public static int nukerRange = 5;

    @Slider(
            name = "Height",
            description = "Blocks above your head",
            min = 0, max = 5
    )
    public static int nukerHeight = 0;

    @Slider(
            name = "Depth",
            description = "Blocks below your head",
            min = 0, max = 4
    )
    public static int nukerDepth = 1;

    @Slider(
            name = "Field of View",
            description = "Change fov of sphere shape nuker",
            min = 0, max = 361, // bruh moment
            step = 20
    )
    public static int nukerFieldOfView = 180;

    @Slider(
            name = "Sideways Offset",
            description = "For facing axis mode (positive - offset to the right)",
            min = -4, max = 4,
            step = 1
    )
    public static int nukerSidewaysOffset = 0;

    @Slider(
            name = "Forwards-Backwards Offset",
            description = "For facing axis mode (positive - forwards)",
            min = -4, max = 4,
            step = 1
    )
    public static int nukerForwardsOffset = 0;

    @Slider(
            name = "Pingless Reset Cutoff",
            description = "Mess with this slider and see if it makes nuker faster",
            min = 0f, max = 20,
            step = 1
    )
    public static int nukerPinglessCutoff = 10;

    @Slider(
            name = "Stuck Timer",
            description = "Maximum amount of time to spend mining a block",
            min = 0f, max = 10000
    )
    public static int nukerStuckTimer = 4000;

    @Slider(
            name = "Fade Time",
            description = "Time in milliseconds for the broken block box to fade",
            min = 0, max = 2000
    )
    public static int nukerFadeTime = 500;
    
    @Slider(
            name = "Precision Mining Aim Time",
            description = "Time to look at the Precision Mining particle before breaking the block",
            min = 50, max = 500
    )
    public static int precisionMiningAimTime = 200;
}
