package bt7s7k7.picker_dollies.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.picker_dollies.support.VectorUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class DestinationArea implements Area {
	protected ResourceKey<Level> dimension;
	protected BoundingBox bounds;
	protected Mirror mirror = Mirror.NONE;
	protected Rotation rotation = Rotation.NONE;

	public DestinationArea(ResourceKey<Level> dimension, BoundingBox bounds, Mirror mirror, Rotation rotation) {
		this.dimension = dimension;
		this.bounds = bounds;
		this.mirror = mirror;
		this.rotation = rotation;
	}

	// This field is only used client-side, so it is not part of the codec
	protected Vec3i offset = Vec3i.ZERO;

	public Vec3i getOffset() {
		return this.offset;
	}

	public DestinationArea(ResourceKey<Level> dimension, BoundingBox boundingBox) {
		this.dimension = dimension;
		this.bounds = boundingBox;
	}

	public void setPos(BlockPos pos) {
		var size = this.getUntransformedArea().getSize();
		this.bounds = new BoundingBox(
				pos.getX(), pos.getY(), pos.getZ(),
				pos.getX() + size.getX(), pos.getY() + size.getY(), pos.getZ() + size.getZ());
	}

	@Override
	public ResourceKey<Level> getDimension() {
		return this.dimension;
	}

	public Area getUntransformedArea() {
		return new Area() {
			@Override
			public ResourceKey<Level> getDimension() {
				return DestinationArea.this.dimension;
			}

			@Override
			public BoundingBox getBounds() {
				return DestinationArea.this.bounds;
			}
		};
	}

	public static BlockPos transformPositionAccordingToMirrorAndRotate(BlockPos pos, BlockPos origin, Vec3i size, Rotation rotation, Mirror mirror) {
		var rectSize = Math.max(size.getX(), size.getZ());
		var start = origin.offset(
				-((rectSize - size.getX()) / 2),
				0,
				-((rectSize - size.getZ()) / 2));

		var relativePos = pos.subtract(start);
		var end = start.offset(
				rectSize - 1,
				0,
				rectSize - 1);

		if (mirror == Mirror.FRONT_BACK) {
			relativePos = new BlockPos(rectSize - relativePos.getX() - 1, relativePos.getY(), relativePos.getZ());
		} else if (mirror == Mirror.LEFT_RIGHT) {
			relativePos = new BlockPos(relativePos.getX(), relativePos.getY(), rectSize - relativePos.getZ() - 1);
		}

		if (rotation == Rotation.CLOCKWISE_180) {
			return new BlockPos(end.getX() - relativePos.getX(), pos.getY(), end.getZ() - relativePos.getZ());
		} else if (rotation == Rotation.CLOCKWISE_90) {
			return new BlockPos(end.getX() - relativePos.getZ(), pos.getY(), start.getZ() + relativePos.getX());
		} else if (rotation == Rotation.COUNTERCLOCKWISE_90) {
			return new BlockPos(start.getX() + relativePos.getZ(), pos.getY(), end.getZ() - relativePos.getX());
		}

		return start.offset(relativePos);
	}

	@Override
	public BoundingBox getBounds() {
		var originalPos = VectorUtil.blockPosMin(this.bounds);
		var originalSize = VectorUtil.vec3iSize(this.bounds);
		var originalEnd = originalPos.offset(originalSize).offset(-1, -1, -1);

		var start = transformPositionAccordingToMirrorAndRotate(originalPos, originalPos, originalSize, this.rotation, this.mirror);
		var end = transformPositionAccordingToMirrorAndRotate(originalEnd, originalPos, originalSize, this.rotation, this.mirror);

		return BoundingBox.fromCorners(start, end);
	}

	public Mirror getMirror() {
		return this.mirror;
	}

	public Rotation getRotation() {
		return this.rotation;
	}

	public String getRotationAngle() {
		return switch (this.rotation) {
			case CLOCKWISE_180 -> "180";
			case CLOCKWISE_90 -> "90";
			case COUNTERCLOCKWISE_90 -> "270";
			case NONE -> "0";
		};
	}

	public void applyStructure(StructureTemplate structure, boolean destroyExistingBlocks) {
		var level = this.getLevel();
		if (level == null) return;

		var settings = new StructurePlaceSettings();
		var rawArea = this.getUntransformedArea();
		var pos = rawArea.getPos();
		var size = rawArea.getSize();

		settings.setMirror(this.mirror);
		// Compensate for mirroring pivot
		if (this.mirror == Mirror.LEFT_RIGHT) {
			pos = pos.offset(0, 0, size.getZ() - 1);
		} else if (this.mirror == Mirror.FRONT_BACK) {
			pos = pos.offset(size.getX() - 1, 0, 0);
		}

		settings.setRotation(this.rotation);
		// Compensate for rotation pivot -- this could probably be done via
		// settings.setRotationPivot, but it's good to keep a united method.
		if (this.rotation == Rotation.CLOCKWISE_90) {
			pos = pos.offset(size.getX() - 1, 0, 0);
		} else if (this.rotation == Rotation.COUNTERCLOCKWISE_90) {
			pos = pos.offset(0, 0, size.getZ() - 1);
		} else if (this.rotation == Rotation.CLOCKWISE_180) {
			pos = pos.offset(size.getX() - 1, 0, size.getZ() - 1);
		}

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

		structure.placeInWorld(level, pos, pos, settings, level.getRandom(), Block.UPDATE_CLIENTS);
	}

	public DestinationArea applyOffset(Vec3i offset) {
		this.bounds = this.bounds.moved(offset.getX(), offset.getY(), offset.getZ());
		this.offset = this.offset.offset(offset);
		return this;
	}

	public DestinationArea moveTo(GlobalPos globalPos) {
		var pos = globalPos.pos();
		var bounds = this.getBounds();
		var closest = new BlockPos(
				Mth.clamp(pos.getX(), bounds.minX(), bounds.maxX()),
				Mth.clamp(pos.getY(), bounds.minY(), bounds.maxY()),
				Mth.clamp(pos.getZ(), bounds.minZ(), bounds.maxZ()));

		var delta = pos.subtract(closest);

		if (globalPos.dimension().equals(this.dimension)) {
			return this.applyOffset(delta);
		}

		this.offset = Vec3i.ZERO;
		this.dimension = globalPos.dimension();
		this.bounds = this.bounds.moved(delta.getX(), delta.getY(), delta.getZ());

		return this;
	}

	public DestinationArea applyMirror(Mirror mirror) {
		if (mirror == Mirror.NONE) return this;

		if (this.mirror == Mirror.NONE) {
			this.mirror = mirror;
		} else if (this.mirror == mirror) {
			this.mirror = Mirror.NONE;
		} else {
			this.mirror = Mirror.NONE;
			this.rotation = this.rotation.getRotated(Rotation.CLOCKWISE_180);
		}

		return this;
	}

	public DestinationArea applyRotation(Rotation rotation) {
		this.rotation = this.rotation.getRotated(rotation);
		return this;
	}

	public static Codec<DestinationArea> CODEC = RecordCodecBuilder.create(instance -> (instance.group(
			Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(DestinationArea::getDimension),
			BoundingBox.CODEC.fieldOf("bounds").forGetter(DestinationArea::getBounds),
			Mirror.CODEC.fieldOf("mirror").forGetter(DestinationArea::getMirror),
			Rotation.CODEC.fieldOf("rotation").forGetter(DestinationArea::getRotation))).apply(instance, DestinationArea::new));

	public static StreamCodec<ByteBuf, DestinationArea> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public static DestinationArea from(Area area) {
		return new DestinationArea(area.getDimension(), area.getBounds());
	}
}
