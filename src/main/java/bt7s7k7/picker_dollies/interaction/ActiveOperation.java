package bt7s7k7.picker_dollies.interaction;

import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public interface ActiveOperation {
	public DestinationArea getDestination();

	public int getColor();

	public Stream<Component> getHelpMessage();

	public void cancel();

	public void move(Vec3i offset);

	public void apply();

	public Stream<BlockPos> getPreviewRenderPositions();

	public void applyMirror(Mirror mirror);

	public void applyRotation(Rotation rotation);
}
