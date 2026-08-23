package bt7s7k7.picker_dollies.support;

import org.joml.Matrix4d;
import org.joml.Vector3d;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record LocalSpaceContext(Matrix4d pose) {
	public Vector3d positionLocalToView(Vector3d pos) {
		return this.pose.transformPosition(pos);
	}

	public Vector3d positionLocalToWorld(Vector3d pos) {
		var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		return this.positionLocalToView(pos).add(cameraPos.x, cameraPos.y, cameraPos.z);
	}

	public void rayViewToLocal(Vector3d pos, Vector3d dir) {
		var inversePose = this.pose.invertAffine(new Matrix4d());
		inversePose.transformPosition(pos);
		inversePose.transformDirection(dir);
	}

	public void rayLocalToView(Vector3d pos, Vector3d dir) {
		this.pose.transformPosition(pos);
		this.pose.transformDirection(dir);
	}

	public static LocalSpaceContext from(Level level, BlockPos anchor, Vec3 cameraPosition) {
		var pose = new Matrix4d();
		pose.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

		var sublevel = SableCompanion.INSTANCE.getContaining(level, anchor);
		if (sublevel != null) {
			var posed = sublevel.logicalPose().bakeIntoMatrix(new Matrix4d());
			pose.mul(posed);
		}

		return new LocalSpaceContext(pose);
	}

	public static LocalSpaceContext from(Level level, BlockPos anchor) {
		return from(level, anchor, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
	}
}
