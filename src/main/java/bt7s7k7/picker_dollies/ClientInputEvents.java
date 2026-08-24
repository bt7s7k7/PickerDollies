package bt7s7k7.picker_dollies;

import java.util.Objects;

import org.joml.Intersectiond;
import org.joml.Matrix4d;
import org.joml.RoundingMode;
import org.joml.Vector3d;
import org.joml.Vector3i;

import com.mojang.blaze3d.platform.InputConstants;

import bt7s7k7.picker_dollies.data.DestinationArea;
import bt7s7k7.picker_dollies.data.DragState;
import bt7s7k7.picker_dollies.data.QuickFillState;
import bt7s7k7.picker_dollies.data.SharedClientData;
import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.network.BlockPlacedNotification;
import bt7s7k7.picker_dollies.network.CopyCommand;
import bt7s7k7.picker_dollies.network.CutCommand;
import bt7s7k7.picker_dollies.network.PasteCommand;
import bt7s7k7.picker_dollies.operation.AdjustSelectionOperation;
import bt7s7k7.picker_dollies.operation.CloneOperation;
import bt7s7k7.picker_dollies.rendering.DebugRenderer;
import bt7s7k7.picker_dollies.support.LookingUtil;
import bt7s7k7.picker_dollies.support.Messages;
import bt7s7k7.picker_dollies.support.Support;
import bt7s7k7.picker_dollies.support.VectorUtil;
import bt7s7k7.picker_dollies.support.WandItem;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
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
	@SubscribeEvent
	public static void registerGuiLayers(RegisterGuiLayersEvent event) {
		event.registerBelow(VanillaGuiLayers.OVERLAY_MESSAGE, ResourceLocation.fromNamespaceAndPath(PickerDollies.MODID, "selection_help"), (guiGraphics, delta) -> {
			var helpMessage = Messages.getHelpMessage();
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
				var direction = LookingUtil.getPlayerDirection(anchor);
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

		if (!WandItem.inMainHand()) {
			if (WandItem.isOffHand() && PickerDolliesClient.ALTERNATE_INPUT.get().isDown()) {
				if (scrollDelta > 0.0) SharedClientData.selectedQuickFillShape.selectPrevious();
				if (scrollDelta < 0.0) SharedClientData.selectedQuickFillShape.selectNext();
				event.setCanceled(true);
			}

			return;
		}

		var selection = WorldClientData.getInstance().selection;
		var activeOperation = WorldClientData.getInstance().activeOperation;

		if (activeOperation == null && PickerDolliesClient.ALTERNATE_INPUT.get().isDown()) {
			if (scrollDelta > 0.0) SharedClientData.selectedOperation.selectPrevious();
			if (scrollDelta < 0.0) SharedClientData.selectedOperation.selectNext();
			event.setCanceled(true);
			return;
		}

		// If there is no active operation, but we have a selection, activate an operation
		if (selection.isActive() && selection.isWithinLimits() && activeOperation == null) {
			var selectedOperation = SharedClientData.selectedOperation.get();
			if (!selectedOperation.supportsMove()) return;
			activeOperation = selectedOperation.activate();
		}

		if (activeOperation == null) return;
		var anchor = activeOperation.getAnchor();
		if (anchor.dimension() != player.level().dimension()) return;

		var direction = LookingUtil.getPlayerDirection(anchor);
		if (scrollDelta < 0.0) direction = direction.getOpposite();

		var offset = direction.getNormal();
		activeOperation.move(offset, scrollDelta > 0.0 ? direction : direction.getOpposite(), scrollDelta > 0.0 ? 1 : -1);
		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onBlockPlace(BlockPlacedNotification event) {
		if (!WandItem.isOffHand()) return;
		var player = Minecraft.getInstance().player;
		if (player.level().dimension() != event.pos().dimension()) return;
		if (!CloneOperation.ACTIVATOR.canActivate(player)) return;

		WorldClientData.getInstance().quickFill = QuickFillState.makeBuild(event.pos());
	}

	@SubscribeEvent
	public static void onBlockBreak(InputEvent.InteractionKeyMappingTriggered event) {
		if (!event.isAttack()) return;
		if (!WandItem.isOffHand()) return;
		var player = Minecraft.getInstance().player;
		var target = LookingUtil.getTargetedBlock(player, false);

		if (target == null) return;

		var pos = target.pos();
		var level = player.level();

		WorldClientData.getInstance().quickFill = QuickFillState.makeDestroy(new GlobalPos(level.dimension(), pos));
		event.setCanceled(true);
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

		var quickFill = WorldClientData.getInstance().quickFill;
		if (quickFill != null) {
			if (quickFill.getApplyKey().isActiveAndMatches(key)) {
				if (event != null) event.setCanceled(true);
				if (!quickFill.isWithinLimits()) return;
				quickFill.apply();
				return;
			}

			if (quickFill.getCancelKey().isActiveAndMatches(key)) {
				if (event != null) event.setCanceled(true);
				quickFill.cancel();
				WorldClientData.getInstance().quickFill = null;
				return;
			}
		}

		var activeOperation = WorldClientData.getInstance().activeOperation;
		if (!WandItem.inMainHand()) return;

		if (PickerDolliesClient.CONFIRM_OPERATION.get().isActiveAndMatches(key)) {
			if (event != null) event.setCanceled(true);

			if (activeOperation != null) {
				activeOperation.apply();
				return;
			}

			var target = LookingUtil.getTargetedBlock(player, false);
			if (target == null) return;
			var selection = WorldClientData.getInstance().selection;
			// Additional check to prevent expanding a selection between sublevels
			if (selection.isActive() && selection.getDimension().equals(player.level().dimension())) {
				var activeSublevel = SableCompanion.INSTANCE.getContaining(player.level(), selection.getPos());
				var newSublevel = SableCompanion.INSTANCE.getContaining(player.level(), target.pos());

				if (!Objects.equals(activeSublevel, newSublevel)) {
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

			var target = LookingUtil.getTargetedBlock(player, false);
			if (target == null) return;
			WorldClientData.getInstance().selection.clear();
		}

		if (PickerDolliesClient.OPERATION_PICK.get().isActiveAndMatches(key)) {
			if (event != null) event.setCanceled(true);

			var selection = WorldClientData.getInstance().selection;
			if (!selection.isActive()) return;

			var newDrag = DragState.tryStart(player);
			var target = LookingUtil.getTargetedBlock(player, true);

			if (activeOperation == null) {
				var selectedOperation = SharedClientData.selectedOperation.get();
				if (newDrag == null && !selectedOperation.supportsMoveTo()) return;
				if (target == null && !selectedOperation.supportsMove()) return;

				activeOperation = selectedOperation.activate();
				if (activeOperation == null) return;
			}

			if (newDrag != null && activeOperation.supportsMove()) {
				if (newDrag.target != activeOperation) newDrag = newDrag.withTarget(activeOperation);
				WorldClientData.getInstance().dragState = newDrag;
				return;
			}

			if (target == null) return;
			activeOperation.moveTo(target);
			return;
		}

		if (PickerDolliesClient.ROTATE.get().isActiveAndMatches(key)) {
			if (activeOperation == null) {
				activeOperation = SharedClientData.selectedOperation.get().activate();
				if (activeOperation == null) return;
			}

			if (event != null) event.setCanceled(true);

			activeOperation.applyRotation(Rotation.CLOCKWISE_90);
		}

		if (PickerDolliesClient.MIRROR.get().isActiveAndMatches(key)) {
			if (activeOperation == null) {
				activeOperation = SharedClientData.selectedOperation.get().activate();
				if (activeOperation == null) return;
			}

			if (event != null) event.setCanceled(true);

			var anchor = activeOperation.getAnchor();
			var direction = LookingUtil.getPlayerDirection(anchor);
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

			var target = LookingUtil.getTargetedBlock(player, true);
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

		var data = WorldClientData.getInstance();

		var shouldAdjustSelection = PickerDolliesClient.ADJUST_SELECTION.get().isDown();
		if (shouldAdjustSelection && WandItem.inMainHand() && data.selection.isActive() && data.activeOperation == null) {
			data.activeOperation = new AdjustSelectionOperation();
		} else if (!shouldAdjustSelection && data.activeOperation instanceof AdjustSelectionOperation adjustSelectionOperation) {
			adjustSelectionOperation.cancel();
		}

		if (data.quickFill != null) {
			if (!WandItem.isOffHand() || data.quickFill.start.dimension() != player.level().dimension()) {
				data.quickFill.cancel();
			} else {
				SharedClientData.selectedQuickFillShape.get().update(data.quickFill);
				return;
			}
		}

		var partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		var dir = player.getViewVector(partialTicks);
		var pos = player.getEyePosition(partialTicks);

		if (data.dragState == null) return;

		var dragState = data.dragState;
		// Stop the drag if an operation starts or ends
		if (dragState.target != data.activeOperation && (dragState.target == null || !dragState.target.ignoreInvalidation())) {
			data.dragState = null;
			return;
		}

		DebugRenderer.submitShape(new DebugRenderer.PointDebugShape(DebugRenderer.getPoseWorldToView(), dragState.origin, 0xffffff00));
		var hit = Intersectiond.intersectRayPlane(VectorUtil.vector3d(pos), VectorUtil.vector3d(dir), dragState.origin, dragState.normal, partialTicks);

		if (hit == -1) return;

		var newPosition = VectorUtil.vector3d(dir).mul(hit).add(pos.x, pos.y, pos.z);

		var delta = newPosition.sub(dragState.lastPosition, new Vector3d());
		var pose = new Matrix4d();
		var direction = LookingUtil.getDirectionFromVector(delta, pose, player.level(), dragState.target.getAnchor());
		var deltaFloored = delta.get(RoundingMode.HALF_DOWN, new Vector3i());
		if (deltaFloored.lengthSquared() == 0) return;

		var worldDeltaFloored = pose.invert().transformDirection(VectorUtil.vector3d(deltaFloored));
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
			dragState.target.move(VectorUtil.vec3i(deltaFloored), actualDirection, amount);
		}
	}

	public static void register() {
		// Nothing
	}
}
