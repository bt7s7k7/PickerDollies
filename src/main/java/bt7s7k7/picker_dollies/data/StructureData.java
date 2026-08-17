package bt7s7k7.picker_dollies.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class StructureData {
	protected StructureTemplate template;
	protected CompoundTag tag;

	public StructureData(StructureTemplate template) {
		this.template = template;
	}

	public StructureData(CompoundTag tag) {
		this.tag = tag;
	}

	public CompoundTag tag() {
		if (this.tag != null) return this.tag;
		return this.tag = this.template.save(new CompoundTag());
	}

	public StructureTemplate template() {
		if (this.template != null) return this.template;
		var structure = new StructureTemplate();
		structure.load(BuiltInRegistries.BLOCK.asLookup(), this.tag);
		return this.template = structure;
	}

	public static final StreamCodec<ByteBuf, StructureData> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.COMPOUND_TAG,
			StructureData::tag,
			StructureData::new);
}
