package bt7s7k7.picker_dollies.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber
public class ServerData {
	private ServerData() {}

	private static ServerData instance;

	public final Map<UUID, ServerPlayerData> serverPlayerData = new HashMap<>();

	public static ServerData getInstance() {
		if (instance == null) instance = new ServerData();
		return instance;
	}

	@SubscribeEvent
	public static void onServerShutdown(ServerStoppingEvent event) {
		instance = null;
	}
}
