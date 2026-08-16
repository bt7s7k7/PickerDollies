package bt7s7k7.picker_dollies.interaction;

import com.mojang.blaze3d.vertex.PoseStack;

import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.Support;
import bt7s7k7.picker_dollies.data.WorldClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = PickerDollies.MODID, value = Dist.CLIENT)
public class SelectionRenderer implements DebugRenderer.SimpleDebugRenderer {
	private static final SelectionRenderer instance = new SelectionRenderer();

	@SubscribeEvent
	public static void onRender(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
			return;
		}

		Vec3 cameraPosition = event.getCamera().getPosition();
		var renderBuffers = Support.<RenderBuffers>getField(event.getLevelRenderer(), "renderBuffers", LevelRenderer.class);

		instance.render(event.getPoseStack(), renderBuffers.bufferSource(), cameraPosition.x(), cameraPosition.y(), cameraPosition.z());
	}

	public void renderSelection(PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, Area area, int color, double inflate) {
		if (area == null) return;

		var mc = Minecraft.getInstance();
		var player = mc.player;
		if (player == null) return;
		if (area.getDimension() != player.level().dimension()) return;

		var outline = bufferSource.getBuffer(RenderType.debugLineStrip(1.0));
		var boundingBox = area.getBounds();
		var minX = (float) (boundingBox.minX() - inflate - camX);
		var minY = (float) (boundingBox.minY() - inflate - camY);
		var minZ = (float) (boundingBox.minZ() - inflate - camZ);
		var maxX = (float) (boundingBox.maxX() + 1.0 + inflate - camX);
		var maxY = (float) (boundingBox.maxY() + 1.0 + inflate - camY);
		var maxZ = (float) (boundingBox.maxZ() + 1.0 + inflate - camZ);

		var pose = poseStack.last().pose();

		outline.addVertex(pose, minX, minY, minZ).setColor(color);
		outline.addVertex(pose, maxX, minY, minZ).setColor(color);
		outline.addVertex(pose, maxX, maxY, minZ).setColor(color);
		outline.addVertex(pose, minX, maxY, minZ).setColor(color);
		outline.addVertex(pose, minX, minY, minZ).setColor(color);

		outline.addVertex(pose, minX, minY, maxZ).setColor(color);

		outline.addVertex(pose, maxX, minY, maxZ).setColor(color);
		outline.addVertex(pose, maxX, minY, minZ).setColor(color);
		outline.addVertex(pose, maxX, minY, maxZ).setColor(color);

		outline.addVertex(pose, maxX, maxY, maxZ).setColor(color);
		outline.addVertex(pose, maxX, maxY, minZ).setColor(color);
		outline.addVertex(pose, maxX, maxY, maxZ).setColor(color);

		outline.addVertex(pose, minX, maxY, maxZ).setColor(color);
		outline.addVertex(pose, minX, maxY, minZ).setColor(color);
		outline.addVertex(pose, minX, maxY, maxZ).setColor(color);

		outline.addVertex(pose, minX, minY, maxZ).setColor(color);

		var time = (double) (System.nanoTime() / 1000) / 1000.0;

		var r = FastColor.ARGB32.red(color) / 255.0f;
		var g = FastColor.ARGB32.green(color) / 255.0f;
		var b = FastColor.ARGB32.blue(color) / 255.0f;
		var a = 0.1f + 0.15f * (float) (Math.sin(time / 500) * 0.5 + 0.5);

		// Render both a box and an inside out version, so the faces are visible from the inside.
		// This allows the player to see block intersections with the outer walls and also makes the
		// box visible from the inside.
		DebugRenderer.renderFilledBox(poseStack, bufferSource,
				maxX, maxY, maxZ,
				minX, minY, minZ,
				r, g, b, a);
		DebugRenderer.renderFilledBox(poseStack, bufferSource,
				minX, minY, minZ,
				maxX, maxY, maxZ,
				r, g, b, a);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ) {
		var data = WorldClientData.getInstance();
		this.renderSelection(poseStack, bufferSource, camX, camY, camZ, data.selection.activeOrNull(), FastColor.ARGB32.color(0, 255, 255), 0.01);

		if (data.activeOperation != null) {
			// Larger inflation for this selection to make the operation area render in front of the selection
			this.renderSelection(poseStack, bufferSource, camX, camY, camZ, data.activeOperation.getDestination(), data.activeOperation.getColor(), 0.02);
		}
	}

	public static void register() {
		// Nothing
	}
}
