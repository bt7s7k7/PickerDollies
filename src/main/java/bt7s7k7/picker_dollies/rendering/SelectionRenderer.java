package bt7s7k7.picker_dollies.rendering;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import bt7s7k7.picker_dollies.Config;
import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.data.Area;
import bt7s7k7.picker_dollies.data.DestinationArea;
import bt7s7k7.picker_dollies.data.SharedClientData;
import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.operation.ActiveOperation;
import bt7s7k7.picker_dollies.support.LocalSpaceContext;
import bt7s7k7.picker_dollies.support.Support;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

@EventBusSubscriber(modid = PickerDollies.MODID, value = Dist.CLIENT)
public class SelectionRenderer {
	private static RenderBuffers getRenderBuffers(LevelRenderer renderer) {
		return (RenderBuffers) Support.getField(renderer, "renderBuffers", LevelRenderer.class);
	}

	@SubscribeEvent
	public static void onRender(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
			if (!Config.SHOULD_RENDER_PREVIEW.getAsBoolean()) return;

			var renderBuffers = getRenderBuffers(event.getLevelRenderer());
			renderOperationPreviews(event.getPoseStack(), renderBuffers.bufferSource(), event.getCamera().getPosition());
		} else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
			var renderBuffers = getRenderBuffers(event.getLevelRenderer());
			renderSelectionOutlines(event.getPoseStack(), renderBuffers.bufferSource(), event.getCamera().getPosition());
		}
	}

	public static void renderOperationPreviews(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 cameraPosition) {
		var activeOperation = WorldClientData.getInstance().activeOperation;

		var quickFill = WorldClientData.getInstance().quickFill;
		if (quickFill != null && quickFill.structure != null && quickFill.isWithinLimits()) {
			renderPreview(quickFill, quickFill.structure, poseStack, bufferSource, cameraPosition);
		}

		if (activeOperation == null) return;

		var structure = SharedClientData.getStructure();
		if (structure == null) return;

		renderPreview(activeOperation, structure, poseStack, bufferSource, cameraPosition);
	}

	public static void renderPreview(ActiveOperation activeOperation, StructureTemplate structure, PoseStack poseStack, MultiBufferSource bufferSource, Vec3 cameraPosition) {
		var mc = Minecraft.getInstance();
		var player = mc.player;
		if (player == null) return;
		var level = player.level();

		for (var previewBox : Support.getIterable(activeOperation.getPreviewRenderPositions()::iterator)) {
			var area = previewBox.area();
			if (area.getDimension() != level.dimension()) return;

			var renderPosition = previewBox.position();

			var mirror = area.getMirror();
			var rotation = area.getRotation();
			var rawArea = area.getUntransformedArea();
			var rawSize = rawArea.getSize();
			var rawPos = rawArea.getPos();

			@SuppressWarnings("unchecked")
			var palettes = (List<StructureTemplate.Palette>) Support.getField(structure, "palettes", StructureTemplate.class);
			if (palettes.size() != 1) throw new IllegalStateException("Structure for SelectionRenderer does not have 1 palette but " + palettes.size());
			var palette = palettes.getFirst();

			var pose = LocalSpaceContext.from(level, rawPos, cameraPosition).pose();

			for (var blockInfo : palette.blocks()) {
				var pos = blockInfo.pos();

				pos = DestinationArea.transformPositionAccordingToMirrorAndRotate(pos, BlockPos.ZERO, rawSize, rotation, mirror);

				pos = pos.offset(renderPosition);
				var state = blockInfo.state().mirror(mirror).rotate(level, pos, rotation);

				var blockPose = new Matrix4d(pose).translate(pos.getX(), pos.getY(), pos.getZ());

				poseStack.pushPose();
				poseStack.last().pose().mul(new Matrix4f(blockPose));
				mc.getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
				poseStack.popPose();
			}
		}
	}

	public static final List<RenderedArea> renderedActiveAreas = new ArrayList<>();
	public static RenderedArea renderedSelection = null;

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

		var localSpace = LocalSpaceContext.from(player.level(), area.getPos(), cameraPosition);
		renderedActiveAreas.add(new RenderedArea(localSpace, minX, minY, minZ, maxX, maxY, maxZ));

		var outline = bufferSource.getBuffer(RenderType.debugLineStrip(1.0));
		renderOutlineBox(localSpace.pose(), outline, minX, minY, minZ, maxX, maxY, maxZ, color);

		var time = (double) (System.nanoTime() / 1000) / 1000.0;
		color = FastColor.ARGB32.color(Mth.floor((0.1f + 0.15f * (float) (Math.sin(time / 500) * 0.5 + 0.5)) * 255.0), color);

		// Render both a box and an inside out version, so the faces are visible from the inside.
		// This allows the player to see block intersections with the outer walls and also makes the
		// box visible from the inside.
		var filledBox = bufferSource.getBuffer(RenderType.debugFilledBox());
		renderFilledBox(localSpace.pose(), filledBox,
				maxX, maxY, maxZ,
				minX, minY, minZ,
				color);
		renderFilledBox(localSpace.pose(), filledBox,
				minX, minY, minZ,
				maxX, maxY, maxZ,
				color);
	}

	static void renderOutlineBox(
			Matrix4d pose, VertexConsumer consumer,
			double minX, double minY, double minZ,
			double maxX, double maxY, double maxZ,
			int color) {
		var v3 = new Vector3f();
		var v3d = new Vector3d();

		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, minZ)))).setColor(color);

		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, maxZ)))).setColor(color);

		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, minY, maxZ)))).setColor(color);

		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(maxX, maxY, maxZ)))).setColor(color);

		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, maxZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, minZ)))).setColor(color);
		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, maxY, maxZ)))).setColor(color);

		consumer.addVertex(v3.set(pose.transformPosition(v3d.set(minX, minY, maxZ)))).setColor(color);
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

		for (var debugShape : DebugRenderer.flush()) {
			debugShape.render(bufferSource);
		}

		if (data.quickFill != null) {
			renderSelectionOutline(poseStack, bufferSource, cameraPosition, data.quickFill, data.quickFill.isDestruction() || !data.quickFill.isWithinLimits() ? 0xffff0000 : 0xff00ff00, 0.01);
		}

		renderedActiveAreas.clear();
		renderedSelection = null;

		if (data.activeOperation == null || Config.SHOW_SELECTION_DURING_OPERATION.getAsBoolean()) {
			renderSelectionOutline(poseStack, bufferSource, cameraPosition, selection, selectionColor, 0.01);

			if (!renderedActiveAreas.isEmpty()) {
				renderedSelection = renderedActiveAreas.getFirst();
				renderedActiveAreas.clear();
			}
		}

		if (data.activeOperation == null) return;

		for (var destination : Support.getIterable(data.activeOperation.getDestinationBoxes()::iterator)) {
			// Larger inflation for this selection to make the operation area render in front of the selection
			renderSelectionOutline(poseStack, bufferSource, cameraPosition, destination.area(), destination.color(), 0.02);
		}
	}

	public static void register() {
		// Nothing
	}
}
