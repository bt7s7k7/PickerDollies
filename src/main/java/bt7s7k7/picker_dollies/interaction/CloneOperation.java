package bt7s7k7.picker_dollies.interaction;

import bt7s7k7.picker_dollies.Config;
import bt7s7k7.picker_dollies.data.SharedClientData;
import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.network.CopyCommand;
import bt7s7k7.picker_dollies.network.StampCommand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class CloneOperation extends BaseDestinationOperation {
	public CloneOperation(DestinationArea destination) {
		super(destination);
	}

	public CloneOperation(Selection source) {
		super(source);
	}

	@Override
	public int getColor() {
		return 0xff00ff00;
	}

	@Override
	public void apply() {
		PacketDistributor.sendToServer(new StampCommand(this.destination));

		var data = WorldClientData.getInstance();

		if (!Config.CLONE_CONTINUE.getAsBoolean()) {
			data.activeOperation = null;
		}
	}

	public static final OperationActivator ACTIVATOR = new OperationActivator() {
		@Override
		public ActiveOperation activate() {
			var data = WorldClientData.getInstance();
			var operation = new CloneOperation(data.selection);
			data.activeOperation = operation;

			// Reset the structure to remove potentially stale data that would be displayed until we get a response from the server
			SharedClientData.setStructure(null);

			PacketDistributor.sendToServer(new CopyCommand(data.selection));
			return operation;
		}

		@Override
		public Component getName() {
			return Component.translatable("operation.picker_dollies.clone");
		}

		@Override
		public boolean canActivate(Player player) {
			return !Config.DISABLE_FREE_OPERATIONS_IN_SURVIVAL.getAsBoolean() || player.isCreative();
		}
	};
}
