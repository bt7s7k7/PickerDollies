package bt7s7k7.picker_dollies.operation;

import java.util.Arrays;
import java.util.stream.Stream;

import bt7s7k7.picker_dollies.Config;
import bt7s7k7.picker_dollies.PickerDolliesClient;
import bt7s7k7.picker_dollies.data.Area;
import bt7s7k7.picker_dollies.data.DestinationArea;
import bt7s7k7.picker_dollies.data.Selection;
import bt7s7k7.picker_dollies.data.SharedClientData;
import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.network.CopyCommand;
import bt7s7k7.picker_dollies.network.StampManyCommand;
import bt7s7k7.picker_dollies.support.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.network.PacketDistributor;

public class StackOperation implements ActiveOperation {
	protected final DestinationArea destination;

	protected Vec3i gap = Vec3i.ZERO;
	protected int[] extend = new int[6];
	protected int[] shift = new int[3];

	public StackOperation(Selection selection) {
		this.destination = DestinationArea.from(selection);
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
		return new GlobalPos(this.destination.getDimension(), this.destination.getPos());
	}

	@Override
	public Rotation getRotation() {
		return this.destination.getRotation();
	}

	@Override
	public Stream<Component> getHelpMessage() {
		var extend = Arrays.toString(this.extend);
		extend = extend.substring(1, extend.length() - 1);

		var shift = Arrays.toString(this.shift);
		shift = shift.substring(1, shift.length() - 1);

		return Stream.concat(Stream.of(
				Component.translatable("gui.picker_dollies.stack_state_extends",
						Component.literal(extend).withStyle(ChatFormatting.GOLD)).withStyle(PickerDolliesClient.ALTERNATE_INPUT.get().isDown() ? ChatFormatting.DARK_PURPLE : ChatFormatting.LIGHT_PURPLE),
				Component.translatable("gui.picker_dollies.stack_state_gap",
						Component.literal(this.gap.toShortString()).withStyle(ChatFormatting.GOLD),
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
			var newGap = this.gap.relative(direction.getAxis(), amount);
			this.gap = newGap;
			this.shift[direction.getAxis().ordinal()] = direction.getAxisDirection().getStep();
		} else {
			var idx = direction.get3DDataValue();
			this.extend[idx] += amount;
			if (this.extend[idx] < 0) {
				this.extend[idx] = 0;
				this.extend[direction.getOpposite().get3DDataValue()]++;
			}
		}
	}

	@Override
	public void moveTo(GlobalPos globalPos) {
		// Not applicable
	}

	@Override
	public void apply() {
		PacketDistributor.sendToServer(new StampManyCommand(this.destination, this.getDestinations().toList()));
		var data = WorldClientData.getInstance();
		data.activeOperation = null;
		data.selection.clear();
	}

	public Stream<BlockPos> getDestinations() {
		var yStart = -this.extend[Direction.DOWN.get3DDataValue()];
		var yEnd = this.extend[Direction.UP.get3DDataValue()];
		var xStart = -this.extend[Direction.WEST.get3DDataValue()];
		var xEnd = this.extend[Direction.EAST.get3DDataValue()];
		var zStart = -this.extend[Direction.NORTH.get3DDataValue()];
		var zEnd = this.extend[Direction.SOUTH.get3DDataValue()];

		var gap = this.gap;
		var bounds = this.destination.getBounds();
		var origin = this.destination.getPos();
		var shift = new Vec3i(
				xStart == 0 && xEnd == 0 ? this.shift[Direction.Axis.X.ordinal()] * gap.getX() : 0,
				yStart == 0 && yEnd == 0 ? this.shift[Direction.Axis.Y.ordinal()] * gap.getY() : 0,
				zStart == 0 && zEnd == 0 ? this.shift[Direction.Axis.Z.ordinal()] * gap.getZ() : 0);

		return BlockPos.betweenClosedStream(new BlockPos(xStart, yStart, zStart), new BlockPos(xEnd, yEnd, zEnd))
				.filter(pos -> pos.getZ() != 0 || pos.getY() != 0 || pos.getX() != 0)
				.map(pos -> new BlockPos(
						pos.getX() * bounds.getXSpan() + pos.getX() * gap.getX() + (Math.abs(pos.getZ()) + Math.abs(pos.getY())) * shift.getX(),
						pos.getY() * bounds.getYSpan() + pos.getY() * gap.getY() + (Math.abs(pos.getX()) + Math.abs(pos.getZ())) * shift.getY(),
						pos.getZ() * bounds.getZSpan() + pos.getZ() * gap.getZ() + (Math.abs(pos.getX()) + Math.abs(pos.getY())) * shift.getZ())
								.offset(origin));
	}

	@Override
	public Stream<DestinationBox> getDestinationBoxes() {
		return this.getDestinations().map(pos -> {
			var delta = pos.subtract(this.destination.getPos());
			return new DestinationBox(new Area.Simple(
					this.destination.getDimension(),
					this.destination.getBounds().moved(delta.getX(), delta.getY(), delta.getZ())), 0xffff00ff);
		});
	}

	@Override
	public Stream<PreviewBox> getPreviewRenderPositions() {
		return this.getDestinations().map(pos -> new PreviewBox(new DestinationArea(
				this.destination.getDimension(),
				this.destination.getBounds(),
				this.destination.getMirror(),
				this.destination.getRotation()), pos));
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
