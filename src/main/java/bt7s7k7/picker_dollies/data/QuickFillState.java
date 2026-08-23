package bt7s7k7.picker_dollies.data;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.joml.Intersectiond;
import org.joml.Vector3d;

import bt7s7k7.picker_dollies.network.CutCommand;
import bt7s7k7.picker_dollies.network.FillCommand;
import bt7s7k7.picker_dollies.operation.ActiveOperation;
import bt7s7k7.picker_dollies.support.LocalSpaceContext;
import bt7s7k7.picker_dollies.support.LookingUtil;
import bt7s7k7.picker_dollies.support.VectorUtil;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.network.PacketDistributor;

public class QuickFillState extends ActiveOperation.Readonly implements Area {
	public static enum Shape {
		LEGACY("shape.picker_dollies.legacy", state -> {
			var player = Minecraft.getInstance().player;
			var target = LookingUtil.getTargetedBlock(player, !state.isDestruction());

			do {
				if (target == null) break;

				var activeSublevel = SableCompanion.INSTANCE.getContaining(player.level(), state.start.pos());
				var newSublevel = SableCompanion.INSTANCE.getContaining(player.level(), target.pos());

				if (!Objects.equals(activeSublevel, newSublevel)) {
					break;
				}

				state.end = target.pos();
				return;
			} while (false);

			useForwardOffset(state);
			return;
		}),

		LINE("shape.picker_dollies.line", state -> {
			planeCast(state, new Vector3d(1, 0, 0), new Vector3d(0, 0, 1), new Vector3d(0, 1, 0));
			var delta = state.end.subtract(state.start.pos());

			var x = delta.getX();
			var y = delta.getY();
			var z = delta.getZ();

			var absX = Math.abs(x);
			var absY = Math.abs(y);
			var absZ = Math.abs(z);

			BlockPos result;

			if (absX >= absY && absX >= absZ) {
				result = new BlockPos(x, 0, 0);
			} else if (absY >= absX && absY >= absZ) {
				result = new BlockPos(0, y, 0);
			} else {
				result = new BlockPos(0, 0, z);
			}

			state.end = state.start.pos().offset(result);
		}),

		WALL("shape.picker_dollies.wall", state -> planeCast(state, new Vector3d(1, 0, 0), new Vector3d(0, 0, 1))),

		FLOOR("shape.picker_dollies.floor", state -> planeCast(state, new Vector3d(0, 1, 0))),

		CUBE("shape.picker_dollies.cube", Shape::useForwardOffset);

		private static void useForwardOffset(QuickFillState state) {
			var player = Minecraft.getInstance().player;
			var localSpace = LocalSpaceContext.from(player.level(), state.start.pos());

			var pos = new Vector3d();
			var dir = VectorUtil.vector3d(player.getViewVector(0.0f));
			localSpace.rayViewToLocal(pos, dir);

			pos.add(dir.mul(5.0));
			state.end = VectorUtil.blockPos(pos);
		}

		private static void planeCast(QuickFillState state, Vector3d... normals) {
			var player = Minecraft.getInstance().player;
			var localSpace = LocalSpaceContext.from(player.level(), state.start.pos());

			var pos = new Vector3d();
			var dir = VectorUtil.vector3d(player.getViewVector(0.0f));
			localSpace.rayViewToLocal(pos, dir);

			var normal = (Vector3d) null;
			var maxDot = 0.0;
			var maxDotReal = 0.0;
			for (var candidate : normals) {
				var dot = candidate.dot(dir);
				var dotAbs = Math.abs(dot);
				if (dotAbs > maxDot) {
					maxDot = dotAbs;
					maxDotReal = dot;
					normal = candidate;
				}
			}

			if (maxDotReal > 0.0) {
				normal = normal.mul(-1.0);
			}

			var point = VectorUtil.vector3d(state.start.pos());

			var t = Intersectiond.intersectRayPlane(pos, dir, point, normal, 0);
			if (t < 0.0) {
				return;
			}

			var hit = pos.add(dir.mul(t));
			state.end = VectorUtil.blockPos(hit);
		}

		private final String translationKey;
		private final Consumer<QuickFillState> update;

		private Shape(String translationKey, Consumer<QuickFillState> update) {
			this.translationKey = translationKey;
			this.update = update;
		}

		public Component getName() {
			return Component.translatable(this.translationKey);
		}

		public void update(QuickFillState state) {
			this.update.accept(state);
		}
	}

	public final GlobalPos start;
	public final StructureTemplate structure;
	private BlockPos end = null;

	public QuickFillState(GlobalPos start, StructureTemplate structure) {
		this.start = start;
		this.structure = structure;
	}

	public static QuickFillState makeBuild(GlobalPos start) {
		return new QuickFillState(start, new Selection(new QuickFillState(start, null)).getStructure());
	}

	public static QuickFillState makeDestroy(GlobalPos start) {
		return new QuickFillState(start, null);
	}

	public boolean isDestruction() {
		return this.structure == null;
	}

	public KeyMapping getApplyKey() {
		return this.isDestruction()
				? Minecraft.getInstance().options.keyAttack
				: Minecraft.getInstance().options.keyUse;

	}

	public KeyMapping getCancelKey() {
		return this.isDestruction()
				? Minecraft.getInstance().options.keyUse
				: Minecraft.getInstance().options.keyAttack;
	}

	@Override
	public ResourceKey<Level> getDimension() {
		return this.start.dimension();
	}

	@Override
	public BoundingBox getBounds() {
		if (this.end == null) return new BoundingBox(this.start.pos());
		return BoundingBox.fromCorners(this.start.pos(), this.end);
	}

	@Override
	public GlobalPos getAnchor() {
		return this.start;
	}

	@Override
	public Stream<PreviewBox> getPreviewRenderPositions() {
		var bounds = this.getBounds();
		var level = this.getLevel();

		return BlockPos.betweenClosedStream(bounds)
				.filter(pos -> level.getBlockState(pos).canBeReplaced())
				.map(pos -> new PreviewBox(new DestinationArea(this.getDimension(), bounds), pos));
	}

	@Override
	public void apply() {
		if (this.isDestruction()) {
			PacketDistributor.sendToServer(new CutCommand(new Selection(this)));
		} else {
			PacketDistributor.sendToServer(new FillCommand(new Selection(this), this.start, true));
		}

		WorldClientData.getInstance().quickFill = null;
	}

	@Override
	public void cancel() {
		if (!this.isDestruction()) {
			PacketDistributor.sendToServer(new FillCommand(new Selection().reset(this.start), Optional.empty(), false));
		}

		WorldClientData.getInstance().quickFill = null;
	}
}
