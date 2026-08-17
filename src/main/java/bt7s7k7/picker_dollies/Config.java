package bt7s7k7.picker_dollies;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
	private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.IntValue MAX_BLOCKS = SERVER_BUILDER
			.comment("Maximum number of blocks supported per operation. In practice this limits the size of the initial selection.")
			.defineInRange("maxBlocks", 64 * 64 * 64, 0, Integer.MAX_VALUE);

	static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

	private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.ConfigValue<String> WAND_ITEM = CLIENT_BUILDER
			.comment("Item that will be used to perform operations using this mod. All the functionality of this item will be replaced, so pick something you won't be using.")
			.define("wandItem", "minecraft:wooden_axe", value -> value instanceof String id && ResourceLocation.tryParse(id) != null);

	public static final ModConfigSpec.BooleanValue SHOULD_RENDER_PREVIEW = CLIENT_BUILDER
			.comment("Determines if a preview for an operation result should be displayed, for example for a move operation this will be the blocks that will be placed. This feature is not super optimised, so disable this if you get frame drops.")
			.define("shouldRenderPreview", true);

	static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

	public static void register(ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
		modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
	}
}
