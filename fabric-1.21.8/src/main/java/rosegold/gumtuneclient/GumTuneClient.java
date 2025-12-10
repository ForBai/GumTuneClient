package rosegold.gumtuneclient;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GumTuneClient implements ClientModInitializer {
	public static final String MOD_ID = "gumtuneclient";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("GumTuneClient initialized for Minecraft 1.21.8!");
		// TODO: Initialize mod components here
	}
}
