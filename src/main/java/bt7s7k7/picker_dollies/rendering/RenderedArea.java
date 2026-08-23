package bt7s7k7.picker_dollies.rendering;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.joml.Matrix4d;
import org.joml.Vector3d;

import bt7s7k7.picker_dollies.support.LocalSpaceContext;
import bt7s7k7.picker_dollies.support.VectorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record RenderedArea(LocalSpaceContext localSpace, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
	public static record HitResult(Vector3d position, Vector3d normal, Direction primaryDirection, Direction secondaryDirection) {};

	public HitResult clip(Vec3 start, Vec3 dir, double hitDistance) {
		var aabb = new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);

		var localStart = VectorUtil.vector3d(start);
		var localDir = VectorUtil.vector3d(dir);
		this.localSpace.rayViewToLocal(localStart, localDir);

		var actualInverse = new Matrix4d(this.localSpace.pose()).invertAffine();
		var actualZero = this.localSpace.pose().transformPosition(actualInverse.transformPosition(new Vector3d()));
		localStart.add(actualZero);

		var localEnd = new Vector3d(localDir).mul(hitDistance).add(localStart);
		var hit = AABB.clip(List.of(aabb), VectorUtil.vec3(localStart), VectorUtil.vec3(localEnd), BlockPos.ZERO);

		DebugRenderer.submitShape(new DebugRenderer.PointDebugShape(this.localSpace.pose(), localEnd, 0xffff0000));
		DebugRenderer.submitShape(new DebugRenderer.LineDebugShape(this.localSpace.pose(), localStart, localEnd, 0xffff0000));
		DebugRenderer.submitShape(new DebugRenderer.PointDebugShape(this.localSpace.pose(), localStart, 0xff00ff00));

		if (hit == null) return null;

		var localHitPosition = hit.getLocation();
		var localHitNormal = hit.getDirection().getNormal();

		var normalDirection = hit.getDirection();
		var anchor = new Vector3d(this.minX, this.minY, this.minZ)
				.add(this.maxX, this.maxY, this.maxZ)
				.mul(0.5)
				.add(localHitNormal.getX(), localHitNormal.getY(), localHitNormal.getZ());

		var approxPrimary = VectorUtil.vector3d(localHitPosition).sub(anchor).normalize();

		var directions = Arrays.stream(Direction.values())
				.filter(v -> v != normalDirection && v != normalDirection.getOpposite())
				.collect(Collectors.toList());

		var dotProducts = directions.stream()
				.map(v -> VectorUtil.vector3d(v.getNormal()).dot(approxPrimary))
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

		DebugRenderer.submitShape(new DebugRenderer.PointDebugShape(this.localSpace.pose(), VectorUtil.vector3d(localHitPosition), 0xffff00ff));

		var hitPosition = VectorUtil.vector3d(localHitPosition);
		var hitNormal = VectorUtil.vector3d(localHitNormal);
		this.localSpace.rayLocalToView(hitPosition, hitNormal);

		return new HitResult(hitPosition, hitNormal, primaryDirection, secondaryDirection);
	}
}
