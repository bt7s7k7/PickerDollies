package bt7s7k7.picker_dollies.operation;

import static bt7s7k7.picker_dollies.PickerDolliesClient.keyMappingToComponent;

import java.util.stream.Stream;

import bt7s7k7.picker_dollies.Config;
import bt7s7k7.picker_dollies.PickerDolliesClient;
import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.support.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public class AdjustSelectionOperation implements ActiveOperation {
	@Override
	public boolean supportsMove() {
		return true;
	}

	@Override
	public boolean supportsMoveTo() {
		return false;
	}

	@Override
	public boolean ignoreInvalidation() {
		return true;
	}

	@Override
	public GlobalPos getAnchor() {
		var selection = WorldClientData.getInstance().selection;
		return new GlobalPos(selection.getDimension(), selection.getPos());
	}

	@Override
	public Rotation getRotation() {
		return Rotation.NONE;
	}

	@Override
	public Stream<Component> getHelpMessage() {
		var selection = WorldClientData.getInstance().selection;

		var sizeX = selection.getBounds().getXSpan();
		var sizeY = selection.getBounds().getYSpan();
		var sizeZ = selection.getBounds().getZSpan();

		return Stream.concat(
				selection.isWithinLimits()
						? Stream.of(Component.translatable("gui.picker_dollies.selection",
								Component.literal("" + sizeX).withStyle(ChatFormatting.GOLD),
								Component.literal("" + sizeY).withStyle(ChatFormatting.GOLD),
								Component.literal("" + sizeZ).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.AQUA))
						: Stream.of(Component.translatable("gui.picker_dollies.selection_too_large", Component.literal("" + Config.MAX_BLOCKS.getAsInt())).withStyle(ChatFormatting.RED)),
				Stream.concat(
						Stream.of(
								Component.translatable("gui.picker_dollies.hold_adjust_size",
										keyMappingToComponent(PickerDolliesClient.ALTERNATE_INPUT))
										.withStyle(PickerDolliesClient.ALTERNATE_INPUT.get().isDown() ? ChatFormatting.AQUA : ChatFormatting.BLUE)),
						Messages.baseOperationHelp()));
	}

	@Override
	public void cancel() {
		WorldClientData.getInstance().activeOperation = null;
	}

	@Override
	public void move(Vec3i offset, Direction direction, int amount) {
		var selection = WorldClientData.getInstance().selection;
		if (!selection.isActive()) {
			this.cancel();
			return;
		}

		if (PickerDolliesClient.ALTERNATE_INPUT.get().isDown()) {
			var pos = selection.getPos();
			var size = selection.getSize();

			if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
				size = size.offset(direction.getNormal().multiply(amount));
			} else {
				size = size.subtract(direction.getNormal().multiply(amount));
				pos = pos.offset(direction.getNormal().multiply(amount));
			}

			if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) return;

			selection.reset(new GlobalPos(selection.getDimension(), pos));
			selection.expand(new GlobalPos(selection.getDimension(), pos.offset(size.offset(-1, -1, -1))));
		} else {
			selection.applyOffset(offset);
		}
	}

	@Override
	public void moveTo(GlobalPos globalPos) {
		// Not applicable
	}

	@Override
	public void apply() {
		this.cancel();
	}

	@Override
	public Stream<DestinationBox> getDestinationBoxes() {
		return Stream.of(new DestinationBox(WorldClientData.getInstance().selection, 0xff00ffff));
	}

	@Override
	public Stream<PreviewBox> getPreviewRenderPositions() {
		return Stream.empty();
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
			return WorldClientData.getInstance().activeOperation = new AdjustSelectionOperation();
		}

		@Override
		public Component getName() {
			return Component.translatable("operation.picker_dollies.adjust_selection");
		}

		@Override
		public boolean supportsMoveTo() {
			return false;
		}

		@Override
		public boolean supportsMove() {
			return true;
		}
	};
}
