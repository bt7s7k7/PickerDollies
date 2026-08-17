package bt7s7k7.picker_dollies.interaction;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.picker_dollies.Config;
import bt7s7k7.picker_dollies.PickerDollies;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class Selection implements Area, Cloneable {
	protected ResourceKey<Level> dimension = null;
	protected BoundingBox bounds = null;

	@Override
	public ResourceKey<Level> getDimension() {
		if (this.dimension == null) throw new NullPointerException("Tried to get dimension of an inactive Selection");
		return this.dimension;
	}

	@Override
	public BoundingBox getBounds() {
		if (this.bounds == null) throw new NullPointerException("Tried to get bounds of an inactive Selection");
		return this.bounds;
	}

	public Selection() {}

	public Selection(ResourceKey<Level> dimension, BoundingBox boundingBox) {
		this.dimension = dimension;
		this.bounds = boundingBox;
	}

	public boolean isActive() {
		return this.bounds != null;
	}

	public Selection activeOrNull() {
		if (this.isActive()) return this;
		return null;
	}

	public void clear() {
		this.dimension = null;
		this.bounds = null;
	}

	public Selection reset(GlobalPos start) {
		return this.reset(start.dimension(), new BoundingBox(start.pos()));
	}

	public Selection reset(ResourceKey<Level> dimension, BoundingBox boundingBox) {
		this.dimension = dimension;
		this.bounds = boundingBox;
		return this;
	}

	public Selection expand(GlobalPos position) {
		if (position.dimension() != this.dimension) {
			this.reset(position);
			return this;
		}

		this.bounds = BoundingBox.encapsulatingBoxes(List.of(this.bounds, new BoundingBox(position.pos()))).get();
		return this;
	}

	@Override
	public Selection clone() {
		try {
			return (Selection) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ServerLevel getLevel() {
		if (!this.isActive()) {
			PickerDollies.LOGGER.error("Received command with an empty selection");
			return null;
		}

		return Area.super.getLevel();
	}

	public StructureTemplate getStructure() {
		var level = this.getLevel();
		if (level == null) return null;

		var structure = new StructureTemplate();
		structure.fillFromWorld(level, this.getPos(), this.getSize(), false, Blocks.AIR);

		return structure;
	}

	public boolean isWithinLimits() {
		var sizeX = this.bounds.getXSpan();
		var sizeY = this.bounds.getYSpan();
		var sizeZ = this.bounds.getZSpan();
		return sizeX * sizeY * sizeZ <= Config.MAX_BLOCKS.getAsInt();
	}

	public static Codec<Selection> CODEC = RecordCodecBuilder.create(instance -> (instance.group(
			Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Selection::getDimension),
			BoundingBox.CODEC.fieldOf("bounds").forGetter(Selection::getBounds))).apply(instance, Selection::new));

	public static StreamCodec<ByteBuf, Selection> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
