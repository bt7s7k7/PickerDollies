package bt7s7k7.picker_dollies.operation;

import static bt7s7k7.picker_dollies.PickerDolliesClient.keyMappingToComponent;

import java.util.stream.Stream;

import bt7s7k7.picker_dollies.PickerDolliesClient;
import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.network.FillCommand;
import bt7s7k7.picker_dollies.support.LookingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.network.PacketDistributor;

public class FillOperation implements ActiveOperation {

	@Override
	public boolean supportsMoveTo() {
		return true;
	}

	@Override
	public boolean supportsMove() {
		return false;
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
		return Stream.empty();
	}

	@Override
	public void cancel() {
		WorldClientData.getInstance().activeOperation = null;
	}

	@Override
	public void move(Vec3i offset, Direction direction, int amount) {
		// Not applicable
	}

	@Override
	public void moveTo(GlobalPos globalPos) {

	}

	@Override
	public void apply() {
		this.cancel();
	}

	@Override
	public Stream<DestinationBox> getDestinationBoxes() {
		return Stream.empty();
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
			var mc = Minecraft.getInstance();
			var player = mc.player;
			var target = LookingUtil.getTargetedBlock(player, false);

			if (target != null) {
				PacketDistributor.sendToServer(new FillCommand(WorldClientData.getInstance().selection.clone(), target));
			}

			return null;
		}

		@Override
		public Component getName() {
			return Component.translatable("operation.picker_dollies.fill");
		}

		@Override
		public boolean supportsMove() {
			return false;
		}

		@Override
		public boolean supportsMoveTo() {
			return true;
		}

		@Override
		public boolean canActivate(Player player) {
			return CloneOperation.ACTIVATOR.canActivate(player);
		}

		@Override
		public Component getMoveToMessage() {
			return null;
		}

		@Override
		public Component getStartMessage(boolean lookingAtSelection) {
			var target = LookingUtil.getTargetedBlock(Minecraft.getInstance().player, false);
			if (target == null) return Component.translatable("gui.picker_dollies.fill_with_target_hint").withStyle(ChatFormatting.DARK_GREEN);
			return Component.translatable("gui.picker_dollies.fill_with_target", keyMappingToComponent(PickerDolliesClient.OPERATION_PICK)).withStyle(ChatFormatting.GREEN);
		};
	};

}
