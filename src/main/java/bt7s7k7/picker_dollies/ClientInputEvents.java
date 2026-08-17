package bt7s7k7.picker_dollies;

import java.util.stream.Stream;

import org.joml.Matrix4d;
import org.joml.Vector3d;

import com.mojang.blaze3d.platform.InputConstants;

import bt7s7k7.picker_dollies.data.SharedClientData;
import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.interaction.Area;
import bt7s7k7.picker_dollies.interaction.OperationActivator;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.ICancellableEvent;
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

			var data = WorldClientData.getInstance();
			var area = (Area) null;
			if (data.activeOperation != null) {
				area = data.activeOperation.getDestination();
			} else if (data.selection.isActive()) {
				area = data.selection;
			}

			if (Config.DISPLAY_DIRECTION_INDICATOR.getAsBoolean()) {
				var direction = getPlayerDirection(baseColor, area);
				var axis = switch (direction.getAxis()) {
					case X -> Component.literal("X").withStyle(ChatFormatting.RED);
					case Y -> Component.literal("Y").withStyle(ChatFormatting.GREEN);
					case Z -> Component.literal("Z").withStyle(ChatFormatting.BLUE);
				};

				guiGraphics.drawCenteredString(font, axis, -16, -font.lineHeight / 2, 0xaaffffff);
			}

			var y = 8 + font.lineHeight;

			for (var line : Support.getIterable(helpMessage::iterator)) {
				guiGraphics.drawCenteredString(font, line, 0, y, baseColor);
				y += font.lineHeight;
			}

			guiGraphics.pose().popPose();
		});
	}

	public static final Stream<Component> startSelectionHelp() {
		return Stream.of(Component.translatable("gui.picker_dollies.start_selection", Component.literal("[").withStyle(ChatFormatting.WHITE)
				.append(Component.keybind(PickerDolliesClient.CONFIRM_OPERATION.get().getName()))
				.append(Component.literal("]"))).withStyle(ChatFormatting.GRAY));
	}

	public static final Stream<Component> baseSelectionHelp() {
		return Stream.of(
				Component.translatable("gui.picker_dollies.expand_selection", Component.literal("[").withStyle(ChatFormatting.WHITE)
						.append(Component.keybind(PickerDolliesClient.CONFIRM_OPERATION.get().getName()))
						.append(Component.literal("]"))).withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.picker_dollies.clear_selection", Component.literal("[").withStyle(ChatFormatting.WHITE)
						.append(Component.keybind(PickerDolliesClient.CANCEL_OPERATION.get().getName()))
						.append(Component.literal("]"))).withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.picker_dollies.new_selection", Component.literal("[").withStyle(ChatFormatting.WHITE)
						.append(Component.keybind(PickerDolliesClient.MISC_OPERATION_ACTION.get().getName()))
						.append(Component.literal("]"))).withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.picker_dollies.start_operation",
						Component.empty().append(SharedClientData.getSelectedOperation().getName()).withStyle(ChatFormatting.GOLD),
						Component.literal("[").withStyle(ChatFormatting.WHITE)
								.append(Component.keybind(PickerDolliesClient.SELECT_OPERATION.get().getName()))
								.append(Component.literal("]")))
						.withStyle(ChatFormatting.GREEN));
	}

	public static final Stream<Component> baseOperationHelp() {
		return Stream.of(
				Component.translatable("gui.picker_dollies.apply_operation", Component.literal("[").withStyle(ChatFormatting.WHITE)
						.append(Component.keybind(PickerDolliesClient.CONFIRM_OPERATION.get().getName()))
						.append(Component.literal("]"))).withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.picker_dollies.cancel_operation", Component.literal("[").withStyle(ChatFormatting.WHITE)
						.append(Component.keybind(PickerDolliesClient.CANCEL_OPERATION.get().getName()))
						.append(Component.literal("]"))).withStyle(ChatFormatting.GRAY));
	}

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

		if (PickerDolliesClient.SELECT_OPERATION.get().isDown()) {
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

		if (activeOperation == null && PickerDolliesClient.SELECT_OPERATION.get().isDown()) {
			if (scrollDelta < 0.0) SharedClientData.selectPreviousOperation();
			if (scrollDelta > 0.0) SharedClientData.selectNextOperation();
			event.setCanceled(true);
			return;
		}

		// If there is no active operation, but we have a selection, activate an operation
		if (selection.isActive() && selection.isWithinLimits() && activeOperation == null) {
			activeOperation = SharedClientData.getSelectedOperation().activate();
		}

		if (activeOperation == null) return;
		var destination = activeOperation.getDestination();
		if (destination.getDimension() != player.level().dimension()) return;

		var direction = getPlayerDirection(scrollDelta, destination);
		var offset = direction.getNormal();
		activeOperation.move(offset);
		event.setCanceled(true);
	}

	public static Direction getPlayerDirection(double mul, Area area) {
		var mc = Minecraft.getInstance();
		var player = mc.player;
		var forward = player.getForward().scale(mul);

		if (area != null) {
			var level = player.level();

			var position = area.getPos();
			var sublevel = SableCompanion.INSTANCE.getContaining(level, position);

			if (sublevel != null) {
				var newForward = sublevel.logicalPose().bakeIntoMatrix(new Matrix4d()).invert().transformDirection(new Vector3d(forward.x, forward.y, forward.z));
				forward = new Vec3(newForward.x, newForward.y, newForward.z);
			}
		}

		var direction = Direction.getNearest(forward);
		return direction;
	}

	public static void handleInput(int action, InputConstants.Key key, ICancellableEvent event) {
		if (action != InputConstants.PRESS) return;

		var mc = Minecraft.getInstance();
		if (mc.screen != null) return;
		var player = mc.player;

		// Ensure the player is actually in-game
		if (player == null) return;

		var activeOperation = WorldClientData.getInstance().activeOperation;
		if (!hasActivator(player)) return;

		if (PickerDolliesClient.CONFIRM_OPERATION.get().isActiveAndMatches(key)) {
			if (event != null) event.setCanceled(true);

			if (activeOperation != null) {
				activeOperation.apply();
				return;
			}

			var target = getTargetedBlock(player);
			if (target == null) return;
			var selection = WorldClientData.getInstance().selection;
			// Additional check to prevent expanding a selection between sublevels
			if (selection.isActive() && selection.getDimension().equals(player.level().dimension())) {
				var activeSublevel = SableCompanion.INSTANCE.getContaining(player.level(), selection.getPos());
				var newSublevel = SableCompanion.INSTANCE.getContaining(player.level(), target.pos());

				if (!Objects.equal(activeSublevel, newSublevel)) {
					selection.reset(target);
					return;
				}
			}
			selection.expand(target);
		}

		if (PickerDolliesClient.CANCEL_OPERATION.get().isActiveAndMatches(key)) {
			if (event != null) event.setCanceled(true);

			if (activeOperation != null) {
				activeOperation.cancel();
				return;
			}

			var target = getTargetedBlock(player);
			if (target == null) return;
			WorldClientData.getInstance().selection.clear();
		}

		if (PickerDolliesClient.MISC_OPERATION_ACTION.get().isActiveAndMatches(key)) {
			if (event != null) event.setCanceled(true);

			if (activeOperation != null) {
				return;
			}

			var target = getTargetedBlock(player);
			if (target == null) return;
			WorldClientData.getInstance().selection.reset(target);
		}

		if (PickerDolliesClient.ROTATE.get().isActiveAndMatches(key)) {
			if (activeOperation == null) {
				activeOperation = SharedClientData.getSelectedOperation().activate();
			}

			if (event != null) event.setCanceled(true);

			activeOperation.applyRotation(Rotation.CLOCKWISE_90);
		}

		if (PickerDolliesClient.MIRROR.get().isActiveAndMatches(key)) {
			if (activeOperation == null) {
				activeOperation = SharedClientData.getSelectedOperation().activate();
			}

			if (event != null) event.setCanceled(true);

			var destination = activeOperation.getDestination();
			var direction = getPlayerDirection(1.0, destination);
			var isRotated = destination.getRotation() == Rotation.CLOCKWISE_90 || destination.getRotation() == Rotation.COUNTERCLOCKWISE_90;

			var mirror = switch (direction) {
				case DOWN, UP -> null;
				case NORTH, SOUTH -> isRotated ? Mirror.FRONT_BACK : Mirror.LEFT_RIGHT;
				case EAST, WEST -> isRotated ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
			};

			if (mirror == null) return;

			activeOperation.applyMirror(mirror);
		}

	}

	@SubscribeEvent
	public static void onMouseButton(InputEvent.MouseButton.Pre event) {
		handleInput(event.getAction(), InputConstants.Type.MOUSE.getOrCreate(event.getButton()), event);
	}

	@SubscribeEvent
	public static void onKeyboardEvent(InputEvent.Key event) {
		handleInput(event.getAction(), InputConstants.getKey(event.getKey(), event.getScanCode()), null);
	}

	public static boolean hasActivator(Player player) {
		var wandItem = ResourceLocation.tryParse(Config.WAND_ITEM.get());
		if (wandItem == null) return false;

		var heldStack = player.getMainHandItem();
		return heldStack != null && BuiltInRegistries.ITEM.getKey(heldStack.getItem()).equals(wandItem);
	}

	public static void register() {
		// Nothing
	}
}
