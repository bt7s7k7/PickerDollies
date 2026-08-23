package bt7s7k7.picker_dollies.operation;

import java.util.stream.Stream;

import bt7s7k7.picker_dollies.data.Area;
import bt7s7k7.picker_dollies.data.DestinationArea;
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

	public boolean supportsMove();

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

	public static abstract class Readonly implements ActiveOperation {

		@Override
		public boolean supportsMoveTo() {
			return false;
		}

		@Override
		public boolean supportsMove() {
			return false;
		}

		@Override
		public Rotation getRotation() {
			return Rotation.NONE;
		}

		@Override
		public Stream<Component> getHelpMessage() {
			return Stream.empty();
		}

		@Override
		public void cancel() {}

		@Override
		public void move(Vec3i offset, Direction direction, int amount) {}

		@Override
		public void moveTo(GlobalPos globalPos) {}

		@Override
		public void apply() {}

		@Override
		public Stream<DestinationBox> getDestinationBoxes() {
			return Stream.empty();
		}

		@Override
		public Stream<PreviewBox> getPreviewRenderPositions() {
			return Stream.empty();
		}

		@Override
		public void applyMirror(Mirror mirror) {}

		@Override
		public void applyRotation(Rotation rotation) {}
	}
}
