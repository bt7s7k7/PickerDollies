package bt7s7k7.picker_dollies.rendering;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3f;

import bt7s7k7.picker_dollies.support.VectorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class DebugRenderer {
	private DebugRenderer() {}

	public interface DebugShape {
		public void render(MultiBufferSource bufferSource);
	}

	private static boolean flushed = false;
	private static final List<DebugShape> debugShapes = new ArrayList<>();

	public static void submitShape(DebugShape shape) {
		if (!Minecraft.getInstance().gui.getDebugOverlay().showDebugScreen()) return;
		submitShapeAlways(shape);
	}

	public static void submitShapeAlways(DebugShape shape) {
		if (flushed) {
			flushed = false;
			debugShapes.clear();
		}
		debugShapes.add(shape);
	}

	public static List<DebugShape> flush() {
		if (flushed == true) {
			flushed = false;
			debugShapes.clear();
			return debugShapes;
		}

		flushed = true;
		return debugShapes;
	}

	public static Matrix4d getPoseWorldToView() {
		var mc = Minecraft.getInstance();
		var cameraPosition = mc.gameRenderer.getMainCamera().getPosition();
		var pose = new Matrix4d().translate(VectorUtil.vector3d(cameraPosition).mul(-1.0));
		return pose;
	}

	public static record PointDebugShape(Matrix4d pose, Vector3d point, int color) implements DebugRenderer.DebugShape {
		@Override
		public void render(MultiBufferSource bufferSource) {
			var consumer = bufferSource.getBuffer(RenderType.debugLineStrip(1.0));
			SelectionRenderer.renderOutlineBox(this.pose, consumer, this.point.x - 0.1, this.point.y - 0.1, this.point.z - 0.1, this.point.x + 0.1, this.point.y + 0.1, this.point.z + 0.1, this.color);
		}
	}

	public static record LineDebugShape(Matrix4d pose, Vector3d from, Vector3d to, int color) implements DebugRenderer.DebugShape {
		@Override
		public void render(MultiBufferSource bufferSource) {
			var consumer = bufferSource.getBuffer(RenderType.debugLineStrip(1.0));
			var v3 = new Vector3f();
			var v3d = new Vector3d();
			consumer.addVertex(v3.set(this.pose.transformPosition(v3d.set(this.from)))).setColor(this.color);
			consumer.addVertex(v3.set(this.pose.transformPosition(v3d.set(this.to)))).setColor(this.color);
		}
	}
}
