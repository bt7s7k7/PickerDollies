package bt7s7k7.picker_dollies.support;

import static bt7s7k7.picker_dollies.PickerDolliesClient.keyMappingToComponent;

import java.util.stream.Stream;

import bt7s7k7.picker_dollies.Config;
import bt7s7k7.picker_dollies.PickerDolliesClient;
import bt7s7k7.picker_dollies.data.DragState;
import bt7s7k7.picker_dollies.data.SharedClientData;
import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.operation.CloneOperation;
import bt7s7k7.picker_dollies.operation.OperationActivator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class Messages {
	private Messages() {}

	public static final Stream<Component> startSelectionHelp() {
		return Stream.<Component>of(
				Component.translatable("gui.picker_dollies.start_selection", keyMappingToComponent(PickerDolliesClient.CONFIRM_OPERATION)).withStyle(ChatFormatting.GRAY),
				SharedClientData.getStructureData() != null && CloneOperation.ACTIVATOR.canActivate()
						? Component.translatable("gui.picker_dollies.paste_prompt", keyMappingToComponent(PickerDolliesClient.PASTE)).withStyle(ChatFormatting.GRAY)
						: null);
	}

	public static final Stream<Component> baseSelectionHelp() {
		var hitSelection = DragState.tryStart(Minecraft.getInstance().player) != null;
		var selectedOperation = SharedClientData.getSelectedOperation();
		return Stream.<Component>of(
				Component.translatable("gui.picker_dollies.expand_selection", keyMappingToComponent(PickerDolliesClient.CONFIRM_OPERATION)).withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.picker_dollies.clear_selection", keyMappingToComponent(PickerDolliesClient.CANCEL_OPERATION)).withStyle(ChatFormatting.GRAY),
				!hitSelection && selectedOperation.supportsMoveTo()
						? selectedOperation.getMoveToMessage()
						: null,
				Component.translatable("gui.picker_dollies.copy_or_cut_prompt",
						keyMappingToComponent(PickerDolliesClient.COPY),
						keyMappingToComponent(PickerDolliesClient.CUT))
						.withStyle(ChatFormatting.GRAY),
				selectedOperation.getStartMessage(hitSelection));
	}

	public static final Stream<Component> baseOperationHelp() {
		var pickHint = (Component) null;
		var activeOperation = WorldClientData.getInstance().activeOperation;
		if (activeOperation != null && activeOperation.supportsMoveTo()) {
			var hit = DragState.tryStart(Minecraft.getInstance().player);
			if (hit != null) {
				pickHint = Component.translatable("gui.picker_dollies.drag_operation", keyMappingToComponent(PickerDolliesClient.OPERATION_PICK)).withStyle(ChatFormatting.GRAY);
			} else {
				pickHint = Component.translatable("gui.picker_dollies.move_to_mouse", keyMappingToComponent(PickerDolliesClient.OPERATION_PICK)).withStyle(ChatFormatting.GRAY);
			}
		}

		return Stream.of(
				Component.translatable("gui.picker_dollies.apply_operation", keyMappingToComponent(PickerDolliesClient.CONFIRM_OPERATION)).withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.picker_dollies.cancel_operation", keyMappingToComponent(PickerDolliesClient.CANCEL_OPERATION)).withStyle(ChatFormatting.GRAY),
				pickHint);
	}

	public static Stream<Component> getHelpMessage() {
		var activeOperation = WorldClientData.getInstance().activeOperation;

		if (activeOperation != null) {
			return activeOperation.getHelpMessage();
		}

		var mc = Minecraft.getInstance();
		var player = mc.player;

		// Ensure the player is actually in-game
		if (player == null) return null;
		if (!WandItem.inMainHand()) return null;

		if (PickerDolliesClient.ALTERNATE_INPUT.get().isDown()) {
			var selected = SharedClientData.getSelectedOperation();
			return Stream.concat(
					Stream.of(Component.translatable("gui.picker_dollies.select_operation_header").withStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.GOLD))),
					Stream.concat(
							SharedClientData.OPERATIONS.stream()
									.filter(OperationActivator::canActivate)
									.map(activator -> activator == selected
											? Component.literal("[").append(Component.empty().append(activator.getName()).withStyle(ChatFormatting.GREEN)).append(Component.literal("]"))
											: activator.getName()),
							Stream.of(Component.translatable("gui.picker_dollies.select_operation_footer").withStyle(ChatFormatting.GRAY))));
		}

		var selection = WorldClientData.getInstance().selection;
		if (!selection.isActive()) {
			return startSelectionHelp();
		} else {
			var sizeX = selection.getBounds().getXSpan();
			var sizeY = selection.getBounds().getYSpan();
			var sizeZ = selection.getBounds().getZSpan();

			if (sizeX + sizeY + sizeZ == 3) {
				return baseSelectionHelp();
			} else {
				if (!selection.isWithinLimits()) {
					return Stream.concat(Stream.of(Component.translatable("gui.picker_dollies.selection_too_large", Component.literal("" + Config.MAX_BLOCKS.getAsInt())).withStyle(ChatFormatting.RED)), baseSelectionHelp());
				}

				return Stream.concat(Stream.of(Component.translatable("gui.picker_dollies.selection",
						Component.literal("" + sizeX).withStyle(ChatFormatting.GOLD),
						Component.literal("" + sizeY).withStyle(ChatFormatting.GOLD),
						Component.literal("" + sizeZ).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.AQUA)),
						baseSelectionHelp());
			}
		}
	}
}
