package bt7s7k7.picker_dollies.operation;

import static bt7s7k7.picker_dollies.PickerDolliesClient.keyMappingToComponent;

import bt7s7k7.picker_dollies.PickerDolliesClient;
import bt7s7k7.picker_dollies.data.SharedClientData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public interface OperationActivator {
	public ActiveOperation activate();

	public Component getName();

	public boolean supportsMove();

	public boolean supportsMoveTo();

	public default Component getMoveToMessage() {
		return Component.translatable("gui.picker_dollies.move_to_mouse", keyMappingToComponent(PickerDolliesClient.OPERATION_PICK)).withStyle(ChatFormatting.GRAY);
	}

	public default Component getStartMessage(boolean lookingAtSelection) {
		return lookingAtSelection
				? Component.translatable("gui.picker_dollies.start_operation_drag",
						keyMappingToComponent(PickerDolliesClient.OPERATION_PICK),
						Component.empty().append(SharedClientData.getSelectedOperation().getName()).withStyle(ChatFormatting.GOLD),
						keyMappingToComponent(PickerDolliesClient.ALTERNATE_INPUT))
						.withStyle(ChatFormatting.GREEN)
				: Component.translatable("gui.picker_dollies.start_operation",
						Component.empty().append(SharedClientData.getSelectedOperation().getName()).withStyle(ChatFormatting.GOLD),
						keyMappingToComponent(PickerDolliesClient.ALTERNATE_INPUT))
						.withStyle(ChatFormatting.GREEN);
	}

	public default boolean canActivate() {
		return this.canActivate(Minecraft.getInstance().player);
	}

	public default boolean canActivate(Player player) {
		return true;
	}
}
