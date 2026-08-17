package bt7s7k7.picker_dollies.interaction;

import bt7s7k7.picker_dollies.data.SharedClientData;
import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.network.CopyCommand;
import bt7s7k7.picker_dollies.network.MovementCommand;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class MovementOperation extends BaseDestinationOperation {
	public Selection source;

	public MovementOperation(Selection source, DestinationArea destination) {
		super(destination);
		this.source = source;
	}

	public MovementOperation(Selection source) {
		super(source);
		this.source = source;
	}

	@Override
	public int getColor() {
		return 0xffffff00;
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

			PacketDistributor.sendToServer(new CopyCommand(data.selection));
			return operation;
		}

		@Override
		public Component getName() {
			return Component.translatable("operation.picker_dollies.move");
		}
	};
}
