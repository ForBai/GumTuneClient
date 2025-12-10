package rosegold.gumtuneclient.modules;

/**
 * Base class for all mod modules
 * Provides common functionality and lifecycle management
 */
public abstract class Module {
    protected boolean enabled = false;
    protected String name;

    public Module(String name) {
        this.name = name;
    }

    /**
     * Called when the module is enabled
     */
    public void onEnable() {
        enabled = true;
    }

    /**
     * Called when the module is disabled
     */
    public void onDisable() {
        enabled = false;
    }

    /**
     * Toggle the module on/off
     */
    public void toggle() {
        if (enabled) {
            onDisable();
        } else {
            onEnable();
        }
    }

    /**
     * Called every client tick (20 times per second)
     */
    public void onTick() {
    }

    /**
     * Called every second
     */
    public void onSecond() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getName() {
        return name;
    }
}
