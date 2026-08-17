package bt7s7k7.picker_dollies.data;

import java.io.IOException;

import bt7s7k7.picker_dollies.PickerDollies;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
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
			new CompressedTagCodec(),
			StructureData::tag,
			StructureData::new);

	public static class CompressedTagCodec implements StreamCodec<ByteBuf, CompoundTag> {
		@Override
		public CompoundTag decode(ByteBuf buffer) {
			var tag = (CompoundTag) null;

			try {
				// Having unlimited heap over here is probably not that from a security standpoint, but it works
				tag = NbtIo.readCompressed(new ByteBufInputStream(buffer), NbtAccounter.unlimitedHeap());
				tag = tag.getId() == 0 ? null : tag;
			} catch (IOException ioexception) {
				throw new EncoderException(ioexception);
			}

			if (tag == null) {
				throw new DecoderException("Expected non-null compound tag");
			} else {
				return tag;
			}
		}

		@Override
		public void encode(ByteBuf buffer, CompoundTag tag) {
			if (tag == null) {
				throw new EncoderException("Expected non-null compound tag");
			} else {
				try {
					var start = buffer.writerIndex();
					NbtIo.writeCompressed(tag, new ByteBufOutputStream(buffer));
					var end = buffer.writerIndex();

					PickerDollies.LOGGER.debug("Compressed Nbt size: {}", end - start);
				} catch (IOException e) {
					throw new EncoderException(e);
				}
			}
		}
	}
}
