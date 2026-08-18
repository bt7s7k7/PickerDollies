package bt7s7k7.picker_dollies.interaction;

import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public interface ActiveOperation {
	public static record DestinationBox(Area area, int color) {}

	public static record PreviewBox(DestinationArea area, BlockPos position) {}

	public boolean supportsMoveTo();

	public default boolean ignoreInvalidation() {
		return false;
	}

	public GlobalPos getAnchor();

	public Rotation getRotation();

	public Stream<Component> getHelpMessage();

	public void cancel();

	public void move(Vec3i offset, Direction direction, int amount);

	public void moveTo(GlobalPos globalPos);

	public void apply();

	public Stream<DestinationBox> getDestinationBoxes();

	public Stream<PreviewBox> getPreviewRenderPositions();

	public void applyMirror(Mirror mirror);

	public void applyRotation(Rotation rotation);
}
