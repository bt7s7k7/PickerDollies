package bt7s7k7.picker_dollies.interaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class DestinationArea implements Area {
	protected ResourceKey<Level> dimension;
	protected BoundingBox bounds;

	// This field is only used client-side, so it is not part of the codec
	protected Vec3i offset = Vec3i.ZERO;

	public DestinationArea(ResourceKey<Level> dimension, BoundingBox boundingBox) {
		this.dimension = dimension;
		this.bounds = boundingBox;
	}

	@Override
	public ResourceKey<Level> getDimension() {
		return this.dimension;
	}

	@Override
	public BoundingBox getBounds() {
		return this.bounds;
	}

	public void applyStructure(StructureTemplate structure, boolean destroyExistingBlocks) {
		var level = this.getLevel();
		if (level == null) return;

		var settings = new StructurePlaceSettings();

		if (destroyExistingBlocks) {
			settings.addProcessor(new StructureProcessor() {
				@Override
				public StructureBlockInfo process(LevelReader level_1, BlockPos offset, BlockPos pos, StructureBlockInfo blockInfo, StructureBlockInfo relativeBlockInfo, StructurePlaceSettings settings, StructureTemplate template) {
					level.destroyBlock(relativeBlockInfo.pos(), true);
					return super.process(level_1, offset, pos, blockInfo, relativeBlockInfo, settings, template);
				}

				@Override
				protected StructureProcessorType<?> getType() {
					throw new UnsupportedOperationException("Unimplemented method 'getType'");
				}
			});
		}

		structure.placeInWorld(level, this.getPos(), this.getPos(), settings, level.getRandom(), Block.UPDATE_CLIENTS);
	}

	public DestinationArea applyOffset(Vec3i offset) {
		this.bounds = this.bounds.moved(offset.getX(), offset.getY(), offset.getZ());
		this.offset = this.offset.offset(offset);
		return this;
	}

	public static Codec<DestinationArea> CODEC = RecordCodecBuilder.create(instance -> (instance.group(
			Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(DestinationArea::getDimension),
			BoundingBox.CODEC.fieldOf("bounds").forGetter(DestinationArea::getBounds))).apply(instance, DestinationArea::new));

	public static StreamCodec<ByteBuf, DestinationArea> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public static DestinationArea from(Area area) {
		return new DestinationArea(area.getDimension(), area.getBounds());
	}
}
