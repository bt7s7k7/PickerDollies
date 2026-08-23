package bt7s7k7.picker_dollies.support;

import org.joml.Vector3d;
import org.joml.Vector3i;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

public final class VectorUtil {
	private VectorUtil() {}

	public static Vector3d vector3d(Vec3 a) {
		return new Vector3d(a.x, a.y, a.z);
	}

	public static Vector3d vector3d(Vec3i a) {
		return new Vector3d(a.getX(), a.getY(), a.getZ());
	}

	public static Vector3d vector3d(Vector3i a) {
		return new Vector3d(a.x, a.y, a.z);
	}

	public static Vector3i vector3i(Vec3i a) {
		return new Vector3i(a.getX(), a.getY(), a.getZ());
	}

	public static Vec3 vec3(Vector3d a) {
		return new Vec3(a.x, a.y, a.z);
	}

	public static Vec3i vec3i(Vector3i a) {
		return new Vec3i(a.x, a.y, a.z);
	}

	public static Vec3i vec3iSize(BoundingBox a) {
		return new Vec3i(a.getXSpan(), a.getYSpan(), a.getZSpan());
	}

	public static BlockPos blockPosMin(BoundingBox a) {
		return new BlockPos(a.minX(), a.minY(), a.minZ());
	}

	public static BlockPos blockPos(Vector3d a) {
		return BlockPos.containing(a.x, a.y, a.z);
	}
}
