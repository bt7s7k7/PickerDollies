package bt7s7k7.picker_dollies.data;

import bt7s7k7.picker_dollies.Config;
import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.support.VectorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public interface Area {
	public static class Simple implements Area {
		protected ResourceKey<Level> dimension;
		protected BoundingBox bounds;

		@Override
		public ResourceKey<Level> getDimension() {
			return this.dimension;
		}

		@Override
		public BoundingBox getBounds() {
			return this.bounds;
		}

		public Simple(ResourceKey<Level> dimension, BoundingBox bounds) {
			this.dimension = dimension;
			this.bounds = bounds;
		}

		public Simple(Area area) {
			this.dimension = area.getDimension();
			this.bounds = area.getBounds();
		}
	}

	public ResourceKey<Level> getDimension();

	public BoundingBox getBounds();

	public default BlockPos getPos() {
		var bounds = this.getBounds();
		return VectorUtil.blockPosMin(bounds);
	}

	public default Vec3i getSize() {
		var bounds = this.getBounds();
		return VectorUtil.vec3iSize(bounds);
	}

	public default ServerLevel getLevel() {
		var server = ServerLifecycleHooks.getCurrentServer();

		var level = server.getLevel(this.getDimension());
		if (level == null) {
			PickerDollies.LOGGER.error("Received command with an invalid dimension");
			return null;
		}

		return level;
	}

	public default void destroyBlocks() {
		var level = this.getLevel();
		if (level == null) return;

		for (var pos : BlockPos.betweenClosed(this.getPos(), this.getPos().offset(this.getSize()).offset(-1, -1, -1))) {
			level.destroyBlock(pos, true);
		}
	}

	public default void fillBlocks(BlockState blockState) {
		var level = this.getLevel();
		if (level == null) return;

		// Two phase block setting to prevent self-interference: for example if we have redstone
		// dust inside the selection, we don't want it to pop off when its supporting blocks are
		// destroyed, but if we have redstone dust outside the selection, but supported by the
		// selection we want it to pop off.

		var placeholder = Blocks.BARRIER.defaultBlockState();
		for (var pos : BlockPos.betweenClosed(this.getPos(), this.getPos().offset(this.getSize()).offset(-1, -1, -1))) {
			var blockEntity = level.getBlockEntity(pos);
			Clearable.tryClear(blockEntity);

			level.setBlock(pos, placeholder, Block.UPDATE_KNOWN_SHAPE);
		}

		for (var pos : BlockPos.betweenClosed(this.getPos(), this.getPos().offset(this.getSize()).offset(-1, -1, -1))) {
			level.setBlock(pos, blockState, Block.UPDATE_ALL);
		}
	}

	public default boolean isWithinLimits() {
		var maxBlocks = Config.MAX_BLOCKS.getAsInt();
		// Check intermediates to prevent false positive due to overflow
		var bounds = this.getBounds();
		var sizeX = bounds.getXSpan();
		if (sizeX > maxBlocks) return false;
		var sizeY = bounds.getYSpan();
		if (sizeY > maxBlocks) return false;
		var sizeZ = bounds.getZSpan();
		if (sizeZ > maxBlocks) return false;
		return sizeX * sizeY * sizeZ <= maxBlocks;
	}
}
