package bt7s7k7.picker_dollies;

import static bt7s7k7.picker_dollies.PickerDolliesClient.keyMappingToComponent;

import java.util.stream.Stream;

import org.joml.Intersectiond;
import org.joml.Matrix4d;
import org.joml.RoundingMode;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.spongepowered.include.com.google.common.base.Objects;

import com.mojang.blaze3d.platform.InputConstants;

import bt7s7k7.picker_dollies.data.DragState;
import bt7s7k7.picker_dollies.data.SharedClientData;
import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.interaction.CloneOperation;
import bt7s7k7.picker_dollies.interaction.DestinationArea;
import bt7s7k7.picker_dollies.interaction.OperationActivator;
import bt7s7k7.picker_dollies.interaction.SelectionRenderer;
import bt7s7k7.picker_dollies.network.CopyCommand;
import bt7s7k7.picker_dollies.network.CutCommand;
import bt7s7k7.picker_dollies.network.PasteCommand;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = PickerDollies.MODID, value = Dist.CLIENT)
public class ClientInputEvents {
	private static GlobalPos getTargetedBlock(Player player, boolean above) {
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
		if (above) {
			targetPos = targetPos.offset(blockHitResult.getDirection().getNormal());
		}

		return new GlobalPos(level.dimension(), targetPos);
	}

	private static DragState tryStartDrag(Player player) {
		var dir = player.getViewVector(0.0f);
		var playerPosition = player.getEyePosition(0.0f);

		var hitDistance = 10.0;
		// Rendered areas are positioned relative to the camera
		var start = Vec3.ZERO;

		var bestHit = (SelectionRenderer.RenderedArea.HitResult) null;
		var bestHitDistance = Double.POSITIVE_INFINITY;
		var hitSelection = false;

		if (WorldClientData.getInstance().activeOperation == null) {
			if (SelectionRenderer.renderedSelection != null) {
				var hit = SelectionRenderer.renderedSelection.clip(start, dir, hitDistance);
				if (hit != null) {
					var distance = hit.position().distanceSquared(start.x, start.y, start.z);
					if (distance < bestHitDistance) {
						bestHitDistance = distance;
						bestHit = hit;
						hitSelection = true;
					}
				}
			}
		} else {
			for (var area : SelectionRenderer.renderedActiveAreas) {
				var hit = area.clip(start, dir, hitDistance);
				if (hit == null) continue;

				var distance = hit.position().distanceSquared(start.x, start.y, start.z);
				if (distance >= bestHitDistance) continue;

				bestHitDistance = distance;
				bestHit = hit;
				hitSelection = false;
			}
		}

		if (bestHit == null) return null;

		var worldHitPosition = new Vector3d(bestHit.position()).add(playerPosition.x, playerPosition.y, playerPosition.z);

		if (!hitSelection && WorldClientData.getInstance().activeOperation == null) throw new NullPointerException();

		return new DragState(hitSelection ? null : WorldClientData.getInstance().activeOperation, worldHitPosition, bestHit.normal(), bestHit.primaryDirection(), bestHit.secondaryDirection());
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
			var anchor = (GlobalPos) null;
			if (data.activeOperation != null) {
				anchor = data.activeOperation.getAnchor();
			} else if (data.selection.isActive()) {
				anchor = new GlobalPos(data.selection.getDimension(), data.selection.getPos());
			}

			if (Config.DISPLAY_DIRECTION_INDICATOR.getAsBoolean()) {
				var direction = getPlayerDirection(anchor);
				var axis = switch (direction.getAxis()) {
					case X -> Component.literal("X").withStyle(ChatFormatting.RED);
					case Y -> Component.literal("Y").withStyle(ChatFormatting.GREEN);
					case Z -> Component.literal("Z").withStyle(ChatFormatting.BLUE);
				};

				guiGraphics.drawCenteredString(font, axis, -16, -font.lineHeight / 2, 0xaaffffff);
			}

			var y = 8 + font.lineHeight;

			for (var line : Support.getIterable(helpMessage::iterator)) {
				if (line == null) continue;
				guiGraphics.drawCenteredString(font, line, 0, y, baseColor);
				y += font.lineHeight;
			}

			guiGraphics.pose().popPose();
		});
	}

	public static final Stream<Component> startSelectionHelp() {
		return Stream.<Component>of(
				Component.translatable("gui.picker_dollies.start_selection", keyMappingToComponent(PickerDolliesClient.CONFIRM_OPERATION)).withStyle(ChatFormatting.GRAY),
				SharedClientData.getStructureData() != null && CloneOperation.ACTIVATOR.canActivate()
						? Component.translatable("gui.picker_dollies.paste_prompt", keyMappingToComponent(PickerDolliesClient.PASTE)).withStyle(ChatFormatting.GRAY)
						: null);
	}

	public static final Stream<Component> baseSelectionHelp() {
		var hitSelection = tryStartDrag(Minecraft.getInstance().player) != null;

		return Stream.<Component>of(
				Component.translatable("gui.picker_dollies.expand_selection", keyMappingToComponent(PickerDolliesClient.CONFIRM_OPERATION)).withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.picker_dollies.clear_selection", keyMappingToComponent(PickerDolliesClient.CANCEL_OPERATION)).withStyle(ChatFormatting.GRAY),
				!hitSelection && SharedClientData.getSelectedOperation().supportsMoveTo()
						? Component.translatable("gui.picker_dollies.move_to_mouse", keyMappingToComponent(PickerDolliesClient.OPERATION_PICK)).withStyle(ChatFormatting.GRAY)
						: null,
				Component.translatable("gui.picker_dollies.copy_or_cut_prompt",
						keyMappingToComponent(PickerDolliesClient.COPY),
						keyMappingToComponent(PickerDolliesClient.CUT))
						.withStyle(ChatFormatting.GRAY),
				hitSelection
						? Component.translatable("gui.picker_dollies.start_operation_drag",
								keyMappingToComponent(PickerDolliesClient.OPERATION_PICK),
								Component.empty().append(SharedClientData.getSelectedOperation().getName()).withStyle(ChatFormatting.GOLD),
								keyMappingToComponent(PickerDolliesClient.ALTERNATE_INPUT))
								.withStyle(ChatFormatting.GREEN)
						: Component.translatable("gui.picker_dollies.start_operation",
								Component.empty().append(SharedClientData.getSelectedOperation().getName()).withStyle(ChatFormatting.GOLD),
								keyMappingToComponent(PickerDolliesClient.ALTERNATE_INPUT))
								.withStyle(ChatFormatting.GREEN));
	}

	public static final Stream<Component> baseOperationHelp() {
		var pickHint = (Component) null;
		var activeOperation = WorldClientData.getInstance().activeOperation;
		if (activeOperation != null && activeOperation.supportsMoveTo()) {
			var hit = tryStartDrag(Minecraft.getInstance().player);
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

		if (activeOperation == null && PickerDolliesClient.ALTERNATE_INPUT.get().isDown()) {
			if (scrollDelta > 0.0) SharedClientData.selectPreviousOperation();
			if (scrollDelta < 0.0) SharedClientData.selectNextOperation();
			event.setCanceled(true);
			return;
		}

		// If there is no active operation, but we have a selection, activate an operation
		if (selection.isActive() && selection.isWithinLimits() && activeOperation == null) {
			activeOperation = SharedClientData.getSelectedOperation().activate();
		}

		if (activeOperation == null) return;
		var anchor = activeOperation.getAnchor();
		if (anchor.dimension() != player.level().dimension()) return;

		var direction = getPlayerDirection(anchor);
		if (scrollDelta < 0.0) direction = direction.getOpposite();

		var offset = direction.getNormal();
		activeOperation.move(offset, scrollDelta > 0.0 ? direction : direction.getOpposite(), scrollDelta > 0.0 ? 1 : -1);
		event.setCanceled(true);
	}

	public static Direction getPlayerDirection(GlobalPos anchor) {
		var player = Minecraft.getInstance().player;
		return getDirectionFromVector(player.getForward(), player.level(), anchor);
	}

	public static Direction getDirectionFromVector(Vec3 forward, Level level, GlobalPos anchor) {
		return getDirectionFromVector(new Vector3d(forward.x, forward.y, forward.z), level, anchor);
	}

	public static Direction getDirectionFromVector(Vector3d forward, Level level, GlobalPos anchor) {
		return getDirectionFromVector(forward, new Matrix4d(), level, anchor);
	}

	public static Direction getDirectionFromVector(Vector3d forward, Matrix4d pose, Level level, GlobalPos anchor) {
		if (anchor != null) {
			var position = anchor.pos();
			var sublevel = SableCompanion.INSTANCE.getContaining(level, position);

			if (sublevel != null) {
				sublevel.logicalPose().bakeIntoMatrix(pose).invert().transformDirection(forward);
			}
		}

		var direction = Direction.getNearest(forward.x, forward.y, forward.z);
		return direction;
	}

	public static void handleInput(int action, InputConstants.Key key, ICancellableEvent event) {
		if (action == InputConstants.RELEASE) {
			if (PickerDolliesClient.OPERATION_PICK.get().isActiveAndMatches(key)) {
				WorldClientData.getInstance().dragState = null;
				return;
			}
		}

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

			var target = getTargetedBlock(player, false);
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

			var target = getTargetedBlock(player, false);
			if (target == null) return;
			WorldClientData.getInstance().selection.clear();
		}

		if (PickerDolliesClient.OPERATION_PICK.get().isActiveAndMatches(key)) {
			if (event != null) event.setCanceled(true);

			var newDrag = tryStartDrag(player);

			if (activeOperation == null) {
				var selectedOperation = SharedClientData.getSelectedOperation();
				if (newDrag == null && !selectedOperation.supportsMoveTo()) return;
				activeOperation = selectedOperation.activate();
			}

			if (newDrag != null) {
				if (newDrag.target != activeOperation) newDrag = newDrag.withTarget(activeOperation);
				WorldClientData.getInstance().dragState = newDrag;
				return;
			}

			var target = getTargetedBlock(player, true);
			if (target == null) return;
			activeOperation.moveTo(target);
			return;
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

			var anchor = activeOperation.getAnchor();
			var direction = getPlayerDirection(anchor);
			var rotation = activeOperation.getRotation();
			var isRotated = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;

			var mirror = switch (direction) {
				case DOWN, UP -> null;
				case NORTH, SOUTH -> isRotated ? Mirror.FRONT_BACK : Mirror.LEFT_RIGHT;
				case EAST, WEST -> isRotated ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
			};

			if (mirror == null) return;

			activeOperation.applyMirror(mirror);
		}

		if (PickerDolliesClient.COPY.get().isActiveAndMatches(key)) {
			if (activeOperation != null) return;
			var selection = WorldClientData.getInstance().selection;
			if (!selection.isActive()) return;

			if (event != null) event.setCanceled(true);

			PacketDistributor.sendToServer(new CopyCommand(selection.clone()));
		}

		if (PickerDolliesClient.CUT.get().isActiveAndMatches(key)) {
			if (activeOperation != null) return;
			var selection = WorldClientData.getInstance().selection;
			if (!selection.isActive()) return;

			if (event != null) event.setCanceled(true);

			PacketDistributor.sendToServer(new CutCommand(selection.clone()));
			selection.clear();
		}

		if (PickerDolliesClient.PASTE.get().isActiveAndMatches(key)) {
			if (!CloneOperation.ACTIVATOR.canActivate()) return;

			if (activeOperation != null) return;

			if (event != null) event.setCanceled(true);

			var target = getTargetedBlock(player, true);
			if (target == null) return;

			var structure = SharedClientData.getStructureData();

			if (structure == null) {
				player.sendSystemMessage(Component.translatable("command.picker_dollies.clipboard.empty").withStyle(ChatFormatting.RED));
				return;
			}

			var selection = WorldClientData.getInstance().selection;
			if (selection.isActive()) {
				selection.clear();
			}

			var boundingBox = BoundingBox.fromCorners(Vec3i.ZERO, structure.template().getSize().offset(-1, -1, -1))
					.moved(target.pos().getX(), target.pos().getY(), target.pos().getZ());

			PacketDistributor.sendToServer(new PasteCommand(structure));
			var destinationArea = new DestinationArea(target.dimension(), boundingBox);
			var operation = new CloneOperation(destinationArea);
			WorldClientData.getInstance().activeOperation = operation;
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

	@SubscribeEvent
	public static void onClientTick(RenderFrameEvent.Pre event) {
		var mc = Minecraft.getInstance();
		var player = mc.player;
		if (player == null) return;

		var partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		var dir = player.getViewVector(partialTicks);
		var pos = player.getEyePosition(partialTicks);

		var data = WorldClientData.getInstance();
		if (data.dragState == null) return;

		var dragState = data.dragState;
		// Stop the drag if an operation starts or ends
		if (dragState.target != data.activeOperation && (dragState.target == null || !dragState.target.ignoreInvalidation())) {
			data.dragState = null;
			return;
		}

		SelectionRenderer.debugShapes.add(new SelectionRenderer.PointDebugShape(SelectionRenderer.DebugShape.getPoseWorldToView(), dragState.origin, 0xffffff00));
		var hit = Intersectiond.intersectRayPlane(new Vector3d(pos.x, pos.y, pos.z), new Vector3d(dir.x, dir.y, dir.z), dragState.origin, dragState.normal, partialTicks);

		if (hit == -1) return;

		var newPosition = new Vector3d(dir.x, dir.y, dir.z).mul(hit).add(pos.x, pos.y, pos.z);

		var delta = newPosition.sub(dragState.lastPosition, new Vector3d());
		var pose = new Matrix4d();
		var direction = getDirectionFromVector(delta, pose, player.level(), dragState.target.getAnchor());
		var deltaFloored = delta.get(RoundingMode.HALF_DOWN, new Vector3i());
		if (deltaFloored.lengthSquared() == 0) return;

		var worldDeltaFloored = pose.invert().transformDirection(new Vector3d(deltaFloored.x, deltaFloored.y, deltaFloored.z));
		dragState.lastPosition.add(worldDeltaFloored);

		int amount;
		Direction actualDirection;
		if (direction == dragState.primaryDirection) {
			amount = 1;
			actualDirection = dragState.primaryDirection;
		} else if (direction == dragState.primaryDirection.getOpposite()) {
			amount = -1;
			actualDirection = dragState.primaryDirection;
		} else if (direction == dragState.secondaryDirection) {
			amount = 1;
			actualDirection = dragState.secondaryDirection;
		} else if (direction == dragState.secondaryDirection.getOpposite()) {
			amount = -1;
			actualDirection = dragState.secondaryDirection;
		} else {
			PickerDollies.LOGGER.error("Failed to find the correct direction from {} for drag event {}", direction, dragState);
			return;
		}

		if (dragState.target == null) {
			data.selection.applyOffset(actualDirection.getNormal().multiply(amount));
		} else {
			dragState.target.move(new Vec3i(deltaFloored.x, deltaFloored.y, deltaFloored.z), actualDirection, amount);
		}
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
