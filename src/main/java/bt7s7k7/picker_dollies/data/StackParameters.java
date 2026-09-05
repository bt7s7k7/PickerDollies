package bt7s7k7.picker_dollies.data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class StackParameters {
	public final ResourceKey<Level> dimension;
	public final BoundingBox bounds;
	public Vec3i gap = Vec3i.ZERO;

	public ResourceKey<Level> getDimension() {
		return this.dimension;
	}

	public BoundingBox getBounds() {
		return this.bounds;
	}

	public Vec3i getGap() {
		return this.gap;
	}

	public final int[] extend = new int[6];
	public final int[] shift = new int[3];

	protected List<Integer> getParametersForSerialisation() {
		return IntStream.concat(Arrays.stream(this.extend), Arrays.stream(this.shift)).boxed().toList();
	}

	public StackParameters(ResourceKey<Level> dimension, BoundingBox bounds) {
		this.dimension = dimension;
		this.bounds = bounds;
	}

	protected StackParameters(ResourceKey<Level> dimension, BoundingBox bounds, Vec3i gap, List<Integer> parameters) {
		this.dimension = dimension;
		this.bounds = bounds;
		this.gap = gap;

		if (parameters.size() != this.extend.length + this.shift.length) {
			throw new IllegalArgumentException("Expected parameters list to be " + (this.extend.length + this.shift.length) + " elements");
		}

		for (int i = 0; i < this.extend.length; i++) {
			this.extend[i] = parameters.get(i).intValue();
		}

		for (int i = 0; i < this.shift.length; i++) {
			this.shift[i] = parameters.get(i + this.extend.length).intValue();
		}
	}

	public BlockPos getPos() {
		return new BlockPos(this.bounds.minX(), this.bounds.minY(), this.bounds.minZ());
	}

	public GlobalPos getAnchor() {
		return new GlobalPos(this.dimension, this.getPos());
	}

	public Stream<BlockPos> getDestinations() {
		var yStart = -this.extend[Direction.DOWN.get3DDataValue()];
		var yEnd = this.extend[Direction.UP.get3DDataValue()];
		var xStart = -this.extend[Direction.WEST.get3DDataValue()];
		var xEnd = this.extend[Direction.EAST.get3DDataValue()];
		var zStart = -this.extend[Direction.NORTH.get3DDataValue()];
		var zEnd = this.extend[Direction.SOUTH.get3DDataValue()];

		var gap = this.gap;
		var origin = this.getPos();
		var shift = new Vec3i(
				xStart == 0 && xEnd == 0 ? this.shift[Direction.Axis.X.ordinal()] * gap.getX() : 0,
				yStart == 0 && yEnd == 0 ? this.shift[Direction.Axis.Y.ordinal()] * gap.getY() : 0,
				zStart == 0 && zEnd == 0 ? this.shift[Direction.Axis.Z.ordinal()] * gap.getZ() : 0);

		return BlockPos.betweenClosedStream(new BlockPos(xStart, yStart, zStart), new BlockPos(xEnd, yEnd, zEnd))
				.filter(pos -> pos.getZ() != 0 || pos.getY() != 0 || pos.getX() != 0)
				.map(pos -> new BlockPos(
						pos.getX() * this.bounds.getXSpan() + pos.getX() * gap.getX() + (Math.abs(pos.getZ()) + Math.abs(pos.getY())) * shift.getX(),
						pos.getY() * this.bounds.getYSpan() + pos.getY() * gap.getY() + (Math.abs(pos.getX()) + Math.abs(pos.getZ())) * shift.getY(),
						pos.getZ() * this.bounds.getZSpan() + pos.getZ() * gap.getZ() + (Math.abs(pos.getX()) + Math.abs(pos.getY())) * shift.getZ())
								.offset(origin));
	}

	public static final Codec<StackParameters> CODEC = RecordCodecBuilder.create(instance -> (instance.group(
			Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(StackParameters::getDimension),
			BoundingBox.CODEC.fieldOf("bounds").forGetter(StackParameters::getBounds),
			Vec3i.CODEC.fieldOf("gap").forGetter(StackParameters::getGap),
			Codec.list(Codec.INT, 9, 9).fieldOf("parameters").forGetter(StackParameters::getParametersForSerialisation)))
					.apply(instance, StackParameters::new));

	public static final StreamCodec<ByteBuf, StackParameters> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
