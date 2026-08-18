package bt7s7k7.picker_dollies.interaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
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

		var mc = Minecraft.getInstance();
		var player = mc.player;
		if (player == null) return;
		var level = player.level();
		var structure = SharedClientData.getStructure();
		if (structure == null) return;

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

			var pose = getPose(level, rawPos, cameraPosition);

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

	public static record RenderedArea(Matrix4d pose, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		public static record HitResult(Vector3d position, Vector3d normal, Direction primaryDirection, Direction secondaryDirection) {};

		public HitResult clip(Vec3 start, Vec3 dir, double hitDistance) {
			var aabb = new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
			var inversePose = this.pose.invert(new Matrix4d());

			var localStart = inversePose.transformPosition(new Vector3d(start.x, start.y, start.z));
			var localDir = inversePose.transformDirection(new Vector3d(dir.x, dir.y, dir.z));
			var localEnd = new Vector3d(localDir).mul(hitDistance).add(localStart);
			var hit = AABB.clip(List.of(aabb), new Vec3(localStart.x, localStart.y, localStart.z), new Vec3(localEnd.x, localEnd.y, localEnd.z), BlockPos.ZERO);

			// debugShapes.add(new PointDebugShape(this.pose, localStart, 0xff00ff00));
			// debugShapes.add(new PointDebugShape(this.pose, localEnd, 0xffff0000));

			if (hit == null) return null;

			var localHitPosition = hit.getLocation();
			var localHitNormal = hit.getDirection().getNormal();

			var normalDirection = hit.getDirection();
			var anchor = new Vector3d(this.minX, this.minY, this.minZ)
					.add(this.maxX, this.maxY, this.maxZ)
					.mul(0.5)
					.add(localHitNormal.getX(), localHitNormal.getY(), localHitNormal.getZ());

			var approxPrimary = new Vector3d(localHitPosition.x, localHitPosition.y, localHitPosition.z).sub(anchor).normalize();

			var directions = Arrays.stream(Direction.values())
					.filter(v -> v != normalDirection && v != normalDirection.getOpposite())
					.collect(Collectors.toList());

			var dotProducts = directions.stream()
					.map(v -> new Vector3d(v.getNormal().getX(), v.getNormal().getY(), v.getNormal().getZ()).dot(approxPrimary))
					.collect(Collectors.toList());

			var primaryDirectionIdx = dotProducts.indexOf(Collections.max(dotProducts));
			var primaryDirection = directions.get(primaryDirectionIdx);

			dotProducts.remove(primaryDirectionIdx);
			directions.remove(primaryDirectionIdx);

			var primaryDirectionOpposite = primaryDirection.getOpposite();
			var primaryDirectionOppositeIdx = directions.indexOf(primaryDirectionOpposite);

			dotProducts.remove(primaryDirectionOppositeIdx);
			directions.remove(primaryDirectionOppositeIdx);

			var secondaryDirectionIdx = dotProducts.indexOf(Collections.max(dotProducts));
			var secondaryDirection = directions.get(secondaryDirectionIdx);

			// debugShapes.add(new PointDebugShape(this.pose, new Vector3d(localHitPosition.x, localHitPosition.y, localHitPosition.z), 0xffff00ff));

			var hitPosition = this.pose.transformPosition(new Vector3d(localHitPosition.x, localHitPosition.y, localHitPosition.z));
			var hitNormal = this.pose.transformDirection(new Vector3d(localHitNormal.getX(), localHitNormal.getY(), localHitNormal.getZ()));

			return new HitResult(hitPosition, hitNormal, primaryDirection, secondaryDirection);
		}
	};

	public static final List<RenderedArea> renderedActiveAreas = new ArrayList<>();
	public static RenderedArea renderedSelection = null;

	public interface DebugShape {
		public void render(MultiBufferSource bufferSource);

		public static Matrix4d getPoseWorldToView() {
			var mc = Minecraft.getInstance();
			var cameraPosition = mc.gameRenderer.getMainCamera().getPosition();
			var pose = new Matrix4d().translate(new Vector3d(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z));
			return pose;
		}
	}

	public static record LineDebugShape(Matrix4d pose, Vector3d from, Vector3d to, int color) implements DebugShape {
		@Override
		public void render(MultiBufferSource bufferSource) {
			var consumer = bufferSource.getBuffer(RenderType.debugLineStrip(1.0));
			var v3 = new Vector3f();
			var v3d = new Vector3d();
			consumer.addVertex(v3.set(this.pose.transformPosition(v3d.set(this.from)))).setColor(this.color);
			consumer.addVertex(v3.set(this.pose.transformPosition(v3d.set(this.to)))).setColor(this.color);
		}
	};

	public static record PointDebugShape(Matrix4d pose, Vector3d point, int color) implements DebugShape {
		@Override
		public void render(MultiBufferSource bufferSource) {
			var consumer = bufferSource.getBuffer(RenderType.debugLineStrip(1.0));
			renderOutlineBox(this.pose, consumer, this.point.x - 0.1, this.point.y - 0.1, this.point.z - 0.1, this.point.x + 0.1, this.point.y + 0.1, this.point.z + 0.1, this.color);
		}
	}

	public static final List<DebugShape> debugShapes = new ArrayList<>();

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
		renderedActiveAreas.add(new RenderedArea(pose, minX, minY, minZ, maxX, maxY, maxZ));

		var outline = bufferSource.getBuffer(RenderType.debugLineStrip(1.0));
		renderOutlineBox(pose, outline, minX, minY, minZ, maxX, maxY, maxZ, color);

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

	private static void renderOutlineBox(
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

		for (var debugShape : debugShapes) {
			debugShape.render(bufferSource);
		}
		debugShapes.clear();

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
