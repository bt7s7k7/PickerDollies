package bt7s7k7.picker_dollies.data;

import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.operation.ActiveOperation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = PickerDollies.MODID, value = Dist.CLIENT)
public class WorldClientData {
	private WorldClientData() {}

	public final Selection selection = new Selection();
	public ActiveOperation activeOperation;

	public DragState dragState;

	private static WorldClientData instance = null;

	public static WorldClientData getInstance() {
		if (instance == null) instance = new WorldClientData();
		return instance;
	}

	@SubscribeEvent
	public static void onLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
		instance = null;
	}

	public static void register() {
		// Nothing
	}
}
