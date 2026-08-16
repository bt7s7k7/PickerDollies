package bt7s7k7.picker_dollies.interaction;

import bt7s7k7.picker_dollies.PickerDollies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public interface Area {
	public ResourceKey<Level> getDimension();

	public BoundingBox getBounds();

	public default BlockPos getPos() {
		var bounds = this.getBounds();
		return new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
	}

	public default Vec3i getSize() {
		var bounds = this.getBounds();
		return new Vec3i(bounds.getXSpan(), bounds.getYSpan(), bounds.getZSpan());
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
}
