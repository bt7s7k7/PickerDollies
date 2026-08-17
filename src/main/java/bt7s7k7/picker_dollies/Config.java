package bt7s7k7.picker_dollies;

import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.IntValue MAX_BLOCKS = BUILDER
			.comment("Maximum number of blocks supported per operation. In practice this limits the size of the initial selection.")
			.defineInRange("maxBlocks", 64 * 64 * 64, 0, Integer.MAX_VALUE);

	static final ModConfigSpec SPEC = BUILDER.build();
}
