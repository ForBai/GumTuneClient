package rosegold.gumtuneclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main mod class for GumTuneClient Fabric 1.21.8
 * 
 * This is a port from the 1.8.9 Forge version. Many features are being migrated incrementally.
 * Note: OneConfig dependency has been removed as it's Forge-only.
 */
public class GumTuneClient implements ClientModInitializer {
	public static final String MOD_ID = "gumtuneclient";
	public static final String NAME = "GumTuneClient";
	public static final String VERSION = "0.7.6-beta3-fabric";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static GumTuneClient INSTANCE;
	public static MinecraftClient mc;
	public static boolean debug = false;

	private boolean initialized = false;

	@Override
	public void onInitializeClient() {
		INSTANCE = this;
		mc = MinecraftClient.getInstance();
		
		LOGGER.info("GumTuneClient {} initializing for Minecraft 1.21.8!", VERSION);

		// Register event handlers
		registerEvents();

		// Start scheduled tasks (equivalent to Forge's scheduled executors)
		startScheduledTasks();

		LOGGER.info("GumTuneClient initialized successfully!");
	}

	private void registerEvents() {
		// Client tick event - equivalent to TickEvent.ClientTickEvent
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.world == null) {
				initialized = false;
				return;
			}
			
			if (!initialized) {
				initialized = true;
				onPlayerJoinWorld();
			}
		});

		// World render event - equivalent to RenderWorldLastEvent
		WorldRenderEvents.LAST.register(context -> {
			// World rendering logic will be added here
			// This is where ESPs and other world overlays will be rendered
		});
	}

	private void startScheduledTasks() {
		ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(1);
		
		// Schedule tick events at different intervals
		// Second event - fires every second
		threadPool.scheduleAtFixedRate(() -> {
			// Fire second event to modules that need it
			// TODO: Implement event bus for modules
		}, 0, 1, TimeUnit.SECONDS);

		// Millisecond event - fires every millisecond
		threadPool.scheduleAtFixedRate(() -> {
			// Fire millisecond event to modules that need it
			// TODO: Implement event bus for modules
		}, 0, 1, TimeUnit.MILLISECONDS);
	}

	private void onPlayerJoinWorld() {
		LOGGER.info("Player joined world - initializing mod features");
		// Initialize modules and features here
		// TODO: Load configuration
		// TODO: Initialize modules
	}
}
