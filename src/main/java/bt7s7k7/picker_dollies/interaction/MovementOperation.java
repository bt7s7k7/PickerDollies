package bt7s7k7.picker_dollies.interaction;

import java.util.stream.Stream;

import bt7s7k7.picker_dollies.data.SharedClientData;
import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.network.MovementCommand;
import bt7s7k7.picker_dollies.network.SelectionContentRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.network.PacketDistributor;

public class MovementOperation implements ActiveOperation {
	public Selection source;
	public DestinationArea destination;

	public MovementOperation(Selection source, DestinationArea destination) {
		this.source = source;
		this.destination = destination;
	}

	public MovementOperation(Selection source) {
		this.source = source;
		this.destination = DestinationArea.from(source);
	}

	@Override
	public Stream<BlockPos> getPreviewRenderPositions() {
		return Stream.of(this.destination.getUntransformedArea().getPos());
	}

	@Override
	public DestinationArea getDestination() {
		return this.destination;
	}

	@Override
	public int getColor() {
		return 0xffffff00;
	}

	@Override
	public void cancel() {
		WorldClientData.getInstance().activeOperation = null;
	}

	@Override
	public void move(Vec3i offset) {
		this.destination.applyOffset(offset);
	}

	@Override
	public void applyMirror(Mirror mirror) {
		this.destination.applyMirror(mirror);
	}

	@Override
	public void applyRotation(Rotation rotation) {
		this.destination.applyRotation(rotation);
	}

	@Override
	public void apply() {
		PacketDistributor.sendToServer(new MovementCommand(this.source.clone(), this.destination));
		var data = WorldClientData.getInstance();
		data.activeOperation = null;
		data.selection.clear();
	}

	public static final OperationActivator ACTIVATOR = new OperationActivator() {
		@Override
		public ActiveOperation activate() {
			var data = WorldClientData.getInstance();
			var operation = new MovementOperation(data.selection);
			data.activeOperation = operation;

			// Reset the structure to remove potentially stale data that would be displayed until we get a response from the server
			SharedClientData.setStructure(null);

			PacketDistributor.sendToServer(new SelectionContentRequest(data.selection));
			return operation;
		}

		@Override
		public Component getName() {
			return Component.translatable("operation.picker_dollies.move");
		}
	};
}
