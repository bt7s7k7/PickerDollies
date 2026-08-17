package bt7s7k7.picker_dollies;

import java.util.List;
import java.util.stream.Stream;

import com.mojang.blaze3d.platform.InputConstants;

import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.interaction.MovementOperation;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = PickerDollies.MODID, value = Dist.CLIENT)
public class ClientInputEvents {
	private static GlobalPos getTargetedBlock(Player player) {
		// 1. Get the level (dimension) directly from the player
		var level = player.level();

		// 2. Perform a raycast along the player's line of sight
		// Max reach distance (in blocks), includeFluids (boolean)
		var reachDistance = 100.0f;
		var hitResult = player.pick(reachDistance, 0.0f, false);

		// 3. Verify the raycast hit a block (not air or an entity)
		if (hitResult.getType() != HitResult.Type.BLOCK) return null;

		var blockHitResult = (BlockHitResult) hitResult;

		// 4. Extract the BlockPos
		var targetPos = blockHitResult.getBlockPos();

		return new GlobalPos(level.dimension(), targetPos);
	}

	@SubscribeEvent
	public static void registerGuiLayers(RegisterGuiLayersEvent event) {
		event.registerBelow(VanillaGuiLayers.OVERLAY_MESSAGE, ResourceLocation.fromNamespaceAndPath(PickerDollies.MODID, "selection_help"), (guiGraphics, delta) -> {
			var helpMessage = getHelpMessage();
			if (helpMessage == null) return;

			var gui = Minecraft.getInstance().gui;
			var font = gui.getFont();

			// Include a shift based on the bar height plus the difference between the height that renderSelectedItemName
			// renders at (59) and the height that the overlay/status bar renders at (68) by default
			// int yShift = Math.max(gui.leftHeight, gui.rightHeight) + (68 - 59);
			guiGraphics.pose().pushPose();
			// If y shift is smaller less than the default y level, just render it at the base y level
			// guiGraphics.pose().translate((float) (guiGraphics.guiWidth() / 2), (float) (guiGraphics.guiHeight() - Math.max(yShift, 68)), 0.0F);
			guiGraphics.pose().translate((float) (guiGraphics.guiWidth() / 2), (float) (guiGraphics.guiHeight() / 2), 0.0F);
			int baseColor = 0xffffffff;

			var y = 8 + font.lineHeight;

			for (var line : Support.getIterable(helpMessage::iterator)) {
				int width = font.width(line);
				guiGraphics.drawStringWithBackdrop(font, line, -width / 2, y, width, baseColor);
				y += font.lineHeight;
			}

			guiGraphics.pose().popPose();
		});
	}

	public static final List<Component> START_SELECTION_HELP = List.of(
			Component.literal("Press ").withStyle(ChatFormatting.GRAY).append(Component.literal("[Left Click]").withStyle(ChatFormatting.WHITE)).append(Component.literal(" to start selection").withStyle(ChatFormatting.GRAY)));
	public static final List<Component> BASE_SELECTION_HELP = List.<Component>of(
			Component.literal("Press ").withStyle(ChatFormatting.GRAY).append(Component.literal("[Left Click]").withStyle(ChatFormatting.WHITE)).append(Component.literal(" to expand selection").withStyle(ChatFormatting.GRAY)),
			Component.literal("Press ").withStyle(ChatFormatting.GRAY).append(Component.literal("[Right Click]").withStyle(ChatFormatting.WHITE)).append(Component.literal(" to clear selection").withStyle(ChatFormatting.GRAY)));
	public static final List<Component> BASE_OPERATION_HELP = List.<Component>of(
			Component.literal("Press ").withStyle(ChatFormatting.GRAY).append(Component.literal("[Left Click]").withStyle(ChatFormatting.WHITE)).append(Component.literal(" to apply").withStyle(ChatFormatting.GRAY)),
			Component.literal("Press ").withStyle(ChatFormatting.GRAY).append(Component.literal("[Right Click]").withStyle(ChatFormatting.WHITE)).append(Component.literal(" to cancel").withStyle(ChatFormatting.GRAY)));

	private static Stream<Component> getHelpMessage() {
		var activeOperation = WorldClientData.getInstance().activeOperation;

		if (activeOperation != null) {
			return activeOperation.getHelpMessage();
		}

		var mc = Minecraft.getInstance();
		var player = mc.player;

		// Ensure the player is actually in-game
		if (player == null) return null;
		if (!hasActivator(player)) return null;

		var selection = WorldClientData.getInstance().selection;
		if (!selection.isActive()) {
			return START_SELECTION_HELP.stream();
		} else {
			var sizeX = selection.getBounds().getXSpan();
			var sizeY = selection.getBounds().getYSpan();
			var sizeZ = selection.getBounds().getZSpan();

			if (sizeX + sizeY + sizeZ == 3) {
				return BASE_SELECTION_HELP.stream();
			} else {
				if (!selection.isWithinLimits()) {
					return Stream.concat(Stream.of(Component.literal("Selection too large (Max: " + Config.MAX_BLOCKS.getAsInt() + ")").withStyle(ChatFormatting.RED)), BASE_SELECTION_HELP.stream());
				}

				return Stream.concat(Stream.of(Component.literal("Selection: [").withStyle(ChatFormatting.AQUA)
						.append(Component.literal("" + sizeX).withStyle(ChatFormatting.GOLD)).append(Component.literal(", "))
						.append(Component.literal("" + sizeY).withStyle(ChatFormatting.GOLD)).append(Component.literal(", "))
						.append(Component.literal("" + sizeZ).withStyle(ChatFormatting.GOLD)).append(Component.literal("]"))),
						BASE_SELECTION_HELP.stream());
			}
		}
	}

	@SubscribeEvent
	public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
		var mc = Minecraft.getInstance();
		if (mc.screen != null) return;
		var player = mc.player;

		// Ensure the player is actually in-game
		if (player == null) return;

		double scrollDelta = event.getScrollDeltaY();

		// Ignore horizontal scrolling or zero deltas
		if (scrollDelta == 0.0) return;
		if (!hasActivator(player)) return;

		var selection = WorldClientData.getInstance().selection;
		var activeOperation = WorldClientData.getInstance().activeOperation;

		// If there is no active operation, but we have a selection, activate an operation
		if (selection.isActive() && selection.isWithinLimits() && activeOperation == null) {
			activeOperation = MovementOperation.activate();
		}

		if (activeOperation == null) return;

		var forward = player.getForward().scale(scrollDelta);
		var direction = Direction.getNearest(forward);
		var offset = direction.getNormal();
		activeOperation.move(offset);
		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onMouseButton(InputEvent.MouseButton.Pre event) {
		var mc = Minecraft.getInstance();
		if (mc.screen != null) return;
		var player = mc.player;

		// Ensure the player is actually in-game
		if (player == null) return;

		var activeOperation = WorldClientData.getInstance().activeOperation;
		if (!hasActivator(player)) return;

		if (event.getAction() == InputConstants.PRESS) {
			if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT) {
				event.setCanceled(true);

				if (activeOperation != null) {
					activeOperation.apply();
					return;
				}

				var target = getTargetedBlock(player);
				if (target == null) return;
				player.displayClientMessage(Component.literal("Expanded selection"), true);
				WorldClientData.getInstance().selection.expand(target);
			}

			if (event.getButton() == InputConstants.MOUSE_BUTTON_RIGHT) {
				event.setCanceled(true);

				if (activeOperation != null) {
					activeOperation.cancel();
					return;
				}

				var target = getTargetedBlock(player);
				if (target == null) return;
				player.displayClientMessage(Component.literal("Clearer selection"), true);
				WorldClientData.getInstance().selection.clear();
			}

			if (event.getButton() == InputConstants.MOUSE_BUTTON_MIDDLE) {
				event.setCanceled(true);

				if (activeOperation != null) {
					return;
				}

				var target = getTargetedBlock(player);
				if (target == null) return;
				player.displayClientMessage(Component.literal("Reset selection"), true);
				WorldClientData.getInstance().selection.reset(target);
			}
		}
	}

	public static boolean hasActivator(Player player) {
		var heldStack = player.getMainHandItem();
		return heldStack != null && heldStack.is(Items.STICK);
	}

	public static void register() {
		// Nothing
	}
}
