package bt7s7k7.picker_dollies;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import bt7s7k7.picker_dollies.data.SharedClientData;
import bt7s7k7.picker_dollies.extra.CreativeFlightNoclip;
import bt7s7k7.picker_dollies.network.CommandHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(PickerDollies.MODID)
@EventBusSubscriber
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
		CreativeFlightNoclip.register();
	}

	@SubscribeEvent
	public static void registerCommands(RegisterClientCommandsEvent event) {
		var commands = event.getDispatcher();

		var main = Commands.literal("picker");

		commands.register(main.then(Commands.literal("clipboard").executes(ctx -> {
			var source = ctx.getSource();

			var structure = SharedClientData.getStructure();
			if (structure == null) {
				source.sendFailure(Component.translatable("command.picker_dollies.clipboard.empty"));
				return 0;
			}

			source.sendSuccess(() -> Component.translatable("command.picker_dollies.clipboard.structure",
					Component.literal("" + structure.getSize().getX()).withStyle(ChatFormatting.GOLD),
					Component.literal("" + structure.getSize().getY()).withStyle(ChatFormatting.GOLD),
					Component.literal("" + structure.getSize().getZ()).withStyle(ChatFormatting.GOLD)), false);
			return 1;
		})));

		commands.register(main.then(Commands.literal("clipboard").then(Commands.literal("save").executes(ctx -> {
			var source = ctx.getSource();

			var structure = SharedClientData.getStructure();
			if (structure == null) {
				source.sendFailure(Component.translatable("command.picker_dollies.clipboard.empty"));
				return 0;
			}

			if (!SharedClientData.saveStructureNow()) {
				source.sendFailure(Component.translatable("command.picker_dollies.clipboard.internal_error"));
				return 0;
			}

			source.sendSuccess(() -> Component.translatable("command.picker_dollies.clipboard.saved"), false);

			return 1;
		}))));

		commands.register(main.then(Commands.literal("clipboard").then(Commands.literal("clear").executes(ctx -> {
			var source = ctx.getSource();

			var structure = SharedClientData.getStructure();
			if (structure == null) {
				source.sendFailure(Component.translatable("command.picker_dollies.clipboard.empty"));
				return 0;
			}

			SharedClientData.setStructure(null);

			source.sendSuccess(() -> Component.translatable("command.picker_dollies.clipboard.cleared"), false);

			return 1;
		}))));

		commands.register(main.then(Commands.literal("clipboard").then(Commands.literal("load").executes(ctx -> {
			var source = ctx.getSource();

			if (!SharedClientData.loadStructureNow()) {
				source.sendFailure(Component.translatable("command.picker_dollies.clipboard.internal_error"));
				return 0;
			}

			var structure = SharedClientData.getStructure();
			source.sendSuccess(() -> Component.translatable("command.picker_dollies.clipboard.structure",
					Component.literal("" + structure.getSize().getX()).withStyle(ChatFormatting.GOLD),
					Component.literal("" + structure.getSize().getY()).withStyle(ChatFormatting.GOLD),
					Component.literal("" + structure.getSize().getZ()).withStyle(ChatFormatting.GOLD)), false);

			return 1;
		}))));
	}
}
