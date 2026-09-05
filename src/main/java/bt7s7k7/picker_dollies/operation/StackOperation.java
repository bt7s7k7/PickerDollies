package bt7s7k7.picker_dollies.operation;

import java.util.Arrays;
import java.util.stream.Stream;

import bt7s7k7.picker_dollies.Config;
import bt7s7k7.picker_dollies.PickerDolliesClient;
import bt7s7k7.picker_dollies.data.Area;
import bt7s7k7.picker_dollies.data.DestinationArea;
import bt7s7k7.picker_dollies.data.Selection;
import bt7s7k7.picker_dollies.data.SharedClientData;
import bt7s7k7.picker_dollies.data.StackParameters;
import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.network.CopyCommand;
import bt7s7k7.picker_dollies.network.StackCommand;
import bt7s7k7.picker_dollies.support.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.network.PacketDistributor;

public class StackOperation implements ActiveOperation {
	protected final StackParameters parameters;

	public StackOperation(Selection selection) {
		this.parameters = new StackParameters(selection.getDimension(), selection.getBounds());
	}

	@Override
	public boolean supportsMove() {
		return true;
	}

	@Override
	public boolean supportsMoveTo() {
		return false;
	}

	@Override
	public GlobalPos getAnchor() {
		return this.parameters.getAnchor();
	}

	@Override
	public Rotation getRotation() {
		return Rotation.NONE;
	}

	@Override
	public Stream<Component> getHelpMessage() {
		var extend = Arrays.toString(this.parameters.extend);
		extend = extend.substring(1, extend.length() - 1);

		var shift = Arrays.toString(this.parameters.shift);
		shift = shift.substring(1, shift.length() - 1);

		return Stream.concat(Stream.of(
				Component.translatable("gui.picker_dollies.stack_state_extends",
						Component.literal(extend).withStyle(ChatFormatting.GOLD)).withStyle(PickerDolliesClient.ALTERNATE_INPUT.get().isDown() ? ChatFormatting.DARK_PURPLE : ChatFormatting.LIGHT_PURPLE),
				Component.translatable("gui.picker_dollies.stack_state_gap",
						Component.literal(this.parameters.gap.toShortString()).withStyle(ChatFormatting.GOLD),
						Component.literal(shift).withStyle(ChatFormatting.GOLD),
						Component.literal("[").withStyle(ChatFormatting.WHITE)
								.append(Component.keybind(PickerDolliesClient.ALTERNATE_INPUT.get().getName()))
								.append(Component.literal("]")))
						.withStyle(PickerDolliesClient.ALTERNATE_INPUT.get().isDown() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_PURPLE)),
				Messages.baseOperationHelp());
	}

	@Override
	public void cancel() {
		WorldClientData.getInstance().activeOperation = null;
	}

	@Override
	public void move(Vec3i offset, Direction direction, int amount) {
		if (PickerDolliesClient.ALTERNATE_INPUT.get().isDown()) {
			var newGap = this.parameters.gap.relative(direction.getAxis(), amount);
			this.parameters.gap = newGap;
			this.parameters.shift[direction.getAxis().ordinal()] = direction.getAxisDirection().getStep();
		} else {
			var idx = direction.get3DDataValue();
			this.parameters.extend[idx] += amount;
			if (this.parameters.extend[idx] < 0) {
				this.parameters.extend[idx] = 0;
				this.parameters.extend[direction.getOpposite().get3DDataValue()]++;
			}
		}
	}

	@Override
	public void moveTo(GlobalPos globalPos) {
		// Not applicable
	}

	@Override
	public void apply() {
		PacketDistributor.sendToServer(new StackCommand(this.parameters));
		var data = WorldClientData.getInstance();
		data.activeOperation = null;
		data.selection.clear();
	}

	@Override
	public Stream<DestinationBox> getDestinationBoxes() {
		return this.parameters.getDestinations().map(pos -> {
			var delta = pos.subtract(this.parameters.getPos());
			return new DestinationBox(new Area.Simple(
					this.parameters.getDimension(),
					this.parameters.getBounds().moved(delta.getX(), delta.getY(), delta.getZ())), 0xffff00ff);
		});
	}

	@Override
	public Stream<PreviewBox> getPreviewRenderPositions() {
		return this.parameters.getDestinations().map(pos -> new PreviewBox(new DestinationArea(
				this.parameters.getDimension(),
				this.parameters.getBounds(),
				Mirror.NONE,
				Rotation.NONE), pos));
	}

	@Override
	public void applyMirror(Mirror mirror) {
		// Not applicable
	}

	@Override
	public void applyRotation(Rotation rotation) {
		// Not applicable
	}

	public static final OperationActivator ACTIVATOR = new OperationActivator() {
		@Override
		public ActiveOperation activate() {
			var data = WorldClientData.getInstance();
			var operation = new StackOperation(data.selection);
			data.activeOperation = operation;

			// Reset the structure to remove potentially stale data that would be displayed until we get a response from the server
			SharedClientData.setStructure(null);

			PacketDistributor.sendToServer(new CopyCommand(data.selection));
			return operation;
		}

		@Override
		public boolean supportsMoveTo() {
			return false;
		}

		@Override
		public boolean supportsMove() {
			return true;
		}

		@Override
		public Component getName() {
			return Component.translatable("operation.picker_dollies.stack");
		}

		@Override
		public boolean canActivate(Player player) {
			return !Config.DISABLE_FREE_OPERATIONS_IN_SURVIVAL.getAsBoolean() || player.isCreative();
		}
	};
}
