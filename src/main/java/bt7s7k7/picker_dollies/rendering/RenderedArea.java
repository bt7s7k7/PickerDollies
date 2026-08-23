package bt7s7k7.picker_dollies.rendering;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.joml.Matrix4d;
import org.joml.Vector3d;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record RenderedArea(Matrix4d pose, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
	public static record HitResult(Vector3d position, Vector3d normal, Direction primaryDirection, Direction secondaryDirection) {};

	public HitResult clip(Vec3 start, Vec3 dir, double hitDistance) {
		var aabb = new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
		var inversePose = this.pose.invert(new Matrix4d());

		var localStart = inversePose.transformPosition(new Vector3d(start.x, start.y, start.z));
		var localDir = inversePose.transformDirection(new Vector3d(dir.x, dir.y, dir.z));
		var localEnd = new Vector3d(localDir).mul(hitDistance).add(localStart);
		var hit = AABB.clip(List.of(aabb), new Vec3(localStart.x, localStart.y, localStart.z), new Vec3(localEnd.x, localEnd.y, localEnd.z), BlockPos.ZERO);

		// DebugRenderer.submitShape(new DebugRenderer.PointDebugShape(this.pose, localStart, 0xff00ff00));
		// DebugRenderer.submitShape(new DebugRenderer.PointDebugShape(this.pose, localEnd, 0xffff0000));

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

		// DebugRenderer.submitShape(new DebugRenderer.PointDebugShape(this.pose, new Vector3d(localHitPosition.x, localHitPosition.y, localHitPosition.z), 0xffff00ff));

		var hitPosition = this.pose.transformPosition(new Vector3d(localHitPosition.x, localHitPosition.y, localHitPosition.z));
		var hitNormal = this.pose.transformDirection(new Vector3d(localHitNormal.getX(), localHitNormal.getY(), localHitNormal.getZ()));

		return new HitResult(hitPosition, hitNormal, primaryDirection, secondaryDirection);
	}
}
