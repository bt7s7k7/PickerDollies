package bt7s7k7.picker_dollies;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.rendering.SelectionRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.util.Lazy;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = PickerDollies.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = PickerDollies.MODID, value = Dist.CLIENT)
public class PickerDolliesClient {
	public static final Lazy<KeyMapping> CONFIRM_OPERATION = Lazy.of(() -> new KeyMapping(
			"key.picker_dollies.confirm_operation",
			InputConstants.Type.MOUSE,
			GLFW.GLFW_MOUSE_BUTTON_LEFT,
			"key.categories.picker_dollies.picker_dollies"));

	public static final Lazy<KeyMapping> CANCEL_OPERATION = Lazy.of(() -> new KeyMapping(
			"key.picker_dollies.cancel_operation",
			InputConstants.Type.MOUSE,
			GLFW.GLFW_MOUSE_BUTTON_RIGHT,
			"key.categories.picker_dollies.picker_dollies"));

	public static final Lazy<KeyMapping> OPERATION_PICK = Lazy.of(() -> new KeyMapping(
			"key.picker_dollies.operation_pick",
			InputConstants.Type.MOUSE,
			GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
			"key.categories.picker_dollies.picker_dollies"));

	public static final Lazy<KeyMapping> ALTERNATE_INPUT = Lazy.of(() -> new KeyMapping(
			"key.picker_dollies.alternate_input",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_X,
			"key.categories.picker_dollies.picker_dollies"));

	public static final Lazy<KeyMapping> ROTATE = Lazy.of(() -> new KeyMapping(
			"key.picker_dollies.rotate",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_PAGE_UP,
			"key.categories.picker_dollies.picker_dollies"));

	public static final Lazy<KeyMapping> MIRROR = Lazy.of(() -> new KeyMapping(
			"key.picker_dollies.mirror",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_HOME,
			"key.categories.picker_dollies.picker_dollies"));

	public static final Lazy<KeyMapping> COPY = Lazy.of(() -> new KeyMapping(
			"key.picker_dollies.copy",
			KeyConflictContext.UNIVERSAL,
			KeyModifier.CONTROL,
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_C,
			"key.categories.picker_dollies.picker_dollies"));

	public static final Lazy<KeyMapping> CUT = Lazy.of(() -> new KeyMapping(
			"key.picker_dollies.cut",
			KeyConflictContext.UNIVERSAL,
			KeyModifier.CONTROL,
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_X,
			"key.categories.picker_dollies.picker_dollies"));

	public static final Lazy<KeyMapping> PASTE = Lazy.of(() -> new KeyMapping(
			"key.picker_dollies.paste",
			KeyConflictContext.UNIVERSAL,
			KeyModifier.CONTROL,
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_V,
			"key.categories.picker_dollies.picker_dollies"));

	public PickerDolliesClient(ModContainer container) {
		// Allows NeoForge to create a config screen for this mod's configs.
		// The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
		// Do not forget to add translations for your config options to the en_us.json file.
		container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

		ClientInputEvents.register();
		WorldClientData.register();
		SelectionRenderer.register();
	}

	@SubscribeEvent
	public static void registerBindings(RegisterKeyMappingsEvent event) {
		event.register(CONFIRM_OPERATION.get());
		event.register(CANCEL_OPERATION.get());
		event.register(OPERATION_PICK.get());
		event.register(ALTERNATE_INPUT.get());
		event.register(ROTATE.get());
		event.register(MIRROR.get());
		event.register(CUT.get());
		event.register(COPY.get());
		event.register(PASTE.get());
	}

	public static Component keyMappingToComponent(Lazy<KeyMapping> mapping) {
		return keyMappingToComponent(mapping.get());
	}

	public static Component keyMappingToComponent(KeyMapping mapping) {
		return Component.literal("[").withStyle(ChatFormatting.WHITE)
				.append(Component.keybind(mapping.getName()))
				.append(Component.literal("]"));
	}
}
