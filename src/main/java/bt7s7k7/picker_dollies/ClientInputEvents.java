package bt7s7k7.picker_dollies;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import bt7s7k7.picker_dollies.selection.SelectionPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = PickerDollies.MODID, value = Dist.CLIENT)
public class ClientInputEvents {
	private static SelectionPoint getTargetedBlock(Player player) {
		// 1. Get the level (dimension) directly from the player
		var level = player.level();

		// 2. Perform a raycast along the player's line of sight
		// Max reach distance (in blocks), includeFluids (boolean)
		var reachDistance = 5.0;
		var hitResult = player.pick(reachDistance, 0.0f, false);

		// 3. Verify the raycast hit a block (not air or an entity)
		if (hitResult.getType() != HitResult.Type.BLOCK) return null;

		var blockHitResult = (BlockHitResult) hitResult;

		// 4. Extract the BlockPos
		var targetPos = blockHitResult.getBlockPos();

		return new SelectionPoint(level, targetPos);
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

		player.sendSystemMessage(Component.literal("Scroll, " + scrollDelta));
	}

	@SubscribeEvent
	public static void onMouseButton(InputEvent.MouseButton.Pre event) {
		var mc = Minecraft.getInstance();
		if (mc.screen != null) return;
		var player = mc.player;

		// Ensure the player is actually in-game
		if (player == null) return;

		if (event.getAction() == InputConstants.KEY_DOWN) {
			if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
				var target = getTargetedBlock(player);
				player.sendSystemMessage(Component.literal("Left button, " + target.toString()));
			}

			if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
				var target = getTargetedBlock(player);
				player.sendSystemMessage(Component.literal("Right button, " + target.toString()));
			}

			if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_3) {
				var target = getTargetedBlock(player);
				player.sendSystemMessage(Component.literal("Middle button, " + target.toString()));
			}
		}
	}

	public static void register() {
		// Nothing
	}
}
