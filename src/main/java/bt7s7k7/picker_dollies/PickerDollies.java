package bt7s7k7.picker_dollies;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import bt7s7k7.picker_dollies.network.CommandHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(PickerDollies.MODID)
public class PickerDollies {
	// Define mod id in a common place for everything to reference
	public static final String MODID = "picker_dollies";
	// Directly reference a slf4j logger
	public static final Logger LOGGER = LogUtils.getLogger();

	// The constructor for the mod class is the first code that is run when your mod is loaded.
	// FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
	public PickerDollies(IEventBus modEventBus, ModContainer modContainer) {
		Config.register(modContainer);
		CommandHandler.register();
	}
}
