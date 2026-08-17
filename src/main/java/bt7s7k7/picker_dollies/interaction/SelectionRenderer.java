package bt7s7k7.picker_dollies.interaction;

import java.util.List;

import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import bt7s7k7.picker_dollies.Config;
import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.Support;
import bt7s7k7.picker_dollies.data.SharedClientData;
import bt7s7k7.picker_dollies.data.WorldClientData;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

@EventBusSubscriber(modid = PickerDollies.MODID, value = Dist.CLIENT)
public class SelectionRenderer {
	public static RenderBuffers getRenderBuffers(LevelRenderer renderer) {
		return (RenderBuffers) Support.getField(renderer, "renderBuffers", LevelRenderer.class);
	}

	@SubscribeEvent
	public static void onRender(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
			if (!Config.SHOULD_RENDER_PREVIEW.getAsBoolean()) return;

			var activeOperation = WorldClientData.getInstance().activeOperation;
			if (activeOperation == null) return;

			var renderBuffers = getRenderBuffers(event.getLevelRenderer());
			renderOperationPreview(event.getPoseStack(), renderBuffers.bufferSource(), event.getCamera().getPosition());
		} else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
			var renderBuffers = getRenderBuffers(event.getLevelRenderer());
			renderSelectionOutlines(event.getPoseStack(), renderBuffers.bufferSource(), event.getCamera().getPosition());
		}
	}

	public static void renderOperationPreview(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 cameraPosition) {
		var activeOperation = WorldClientData.getInstance().activeOperation;
		if (activeOperation == null) return;

		var area = activeOperation.getDestination();
		var mc = Minecraft.getInstance();
		var player = mc.player;
		if (player == null) return;
		if (area.getDimension() != player.level().dimension()) return;

		var structure = SharedClientData.getStructure();
		if (structure == null) return;

		@SuppressWarnings("unchecked")
		var palettes = (List<StructureTemplate.Palette>) Support.getField(structure, "palettes", StructureTemplate.class);
		if (palettes.size() != 1) throw new IllegalStateException("Structure for SelectionRenderer does not have 1 palette but " + palettes.size());
		var palette = palettes.getFirst();

		for (var renderPosition : Support.getIterable(activeOperation.getPreviewRenderPositions()::iterator)) {
			var pose = getPose(player.level(), area.getPos(), cameraPosition);

			for (var blockInfo : palette.blocks()) {
				var pos = blockInfo.pos().offset(renderPosition);
				var state = blockInfo.state();
				var blockPose = new Matrix4d(pose).translate(pos.getX(), pos.getY(), pos.getZ());

				poseStack.pushPose();
				poseStack.last().pose().mul(new Matrix4f(blockPose));
				mc.getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
				poseStack.popPose();
			}
		}
	}

	public static Matrix4d getPose(Level level, BlockPos anchor, Vec3 cameraPosition) {
		var pose = new Matrix4d();
		pose.translate(new Vector3d(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z));

		var sublevel = SableCompanion.INSTANCE.getContaining(level, anchor);
		if (sublevel != null) {
			var posed = sublevel.logicalPose().bakeIntoMatrix(new Matrix4d());
			pose.mul(posed);
		}

		return pose;
	}

	public static void renderSelectionOutline(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 cameraPosition, Area area, int color, double inflate) {
		if (area == null) return;

		var mc = Minecraft.getInstance();
		var player = mc.player;
		if (player == null) return;
		if (area.getDimension() != player.level().dimension()) return;

		var boundingBox = area.getBounds();
		var minX = boundingBox.minX() - inflate;
		var minY = boundingBox.minY() - inflate;
		var minZ = boundingBox.minZ() - inflate;
		var maxX = boundingBox.maxX() + 1.0 + inflate;
		var maxY = boundingBox.maxY() + 1.0 + inflate;
		var maxZ = boundingBox.maxZ() + 1.0 + inflate;

		var pose = getPose(player.level(), area.getPos(), cameraPosition);

		var outline = bufferSource.getBuffer(RenderType.debugLineStrip(1.0));

		var v3 = new Vector3f();
		var v3d = new Vector3d();

		outline.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, minZ)))).setColor(color);
		outline.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, minZ)))).setColor(color);
		outline.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, minZ)))).setColor(color);
		outline.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, minZ)))).setColor(color);
		outline.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, minZ)))).setColor(color);

		outline.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, maxZ)))).setColor(color);

		outline.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, maxZ)))).setColor(color);
		outline.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, minZ)))).setColor(color);
		outline.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, maxZ)))).setColor(color);

		outline.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, maxZ)))).setColor(color);
		outline.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, minZ)))).setColor(color);
		outline.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, maxZ)))).setColor(color);

		outline.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, maxZ)))).setColor(color);
		outline.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, minZ)))).setColor(color);
		outline.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, maxZ)))).setColor(color);

		outline.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, maxZ)))).setColor(color);

		var time = (double) (System.nanoTime() / 1000) / 1000.0;
		color = FastColor.ARGB32.color(Mth.floor((0.1f + 0.15f * (float) (Math.sin(time / 500) * 0.5 + 0.5)) * 255.0), color);

		// Render both a box and an inside out version, so the faces are visible from the inside.
		// This allows the player to see block intersections with the outer walls and also makes the
		// box visible from the inside.
		var filledBox = bufferSource.getBuffer(RenderType.debugFilledBox());
		renderFilledBox(pose, filledBox,
				maxX, maxY, maxZ,
				minX, minY, minZ,
				color);
		renderFilledBox(pose, filledBox,
				minX, minY, minZ,
				maxX, maxY, maxZ,
				color);
	}

	// Custom copy of DebugRenderer.renderFilledBox with support for double precision pose and points.
	private static void renderFilledBox(
			Matrix4d pose, VertexConsumer consumer,
			double minX, double minY, double minZ,
			double maxX, double maxY, double maxZ,
			int color) {

		var v3 = new Vector3f();
		var v3d = new Vector3d();
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, maxZ)))).setColor(color);
	}

	public static void renderSelectionOutlines(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 cameraPosition) {
		var data = WorldClientData.getInstance();

		var selectionColor = 0xff00ffff;
		var selection = data.selection.activeOrNull();

		// Indicate selection outside max limit with a red box
		if (selection != null && !selection.isWithinLimits()) {
			selectionColor = 0xffff0000;
		}

		renderSelectionOutline(poseStack, bufferSource, cameraPosition, selection, selectionColor, 0.01);

		if (data.activeOperation != null) {
			// Larger inflation for this selection to make the operation area render in // front of the selection
			renderSelectionOutline(poseStack, bufferSource, cameraPosition, data.activeOperation.getDestination(), data.activeOperation.getColor(), 0.02);
		}
	}

	public static void register() {
		// Nothing
	}
}
