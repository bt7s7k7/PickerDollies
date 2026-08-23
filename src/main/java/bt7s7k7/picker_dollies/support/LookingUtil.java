package bt7s7k7.picker_dollies.support;

import org.joml.Matrix4d;
import org.joml.Vector3d;

import bt7s7k7.picker_dollies.Config;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class LookingUtil {
	private LookingUtil() {}

	public static GlobalPos getTargetedBlock(Player player, boolean above) {
		var level = player.level();

		var reachDistance = Config.REACH_DISTANCE.getAsInt();
		var hitResult = player.pick(reachDistance, 0.0f, false);

		// Verify the raycast hit a block (not air or an entity)
		if (hitResult.getType() != HitResult.Type.BLOCK) return null;
		var blockHitResult = (BlockHitResult) hitResult;

		var targetPos = blockHitResult.getBlockPos();
		if (above) {
			targetPos = targetPos.offset(blockHitResult.getDirection().getNormal());
		}

		return new GlobalPos(level.dimension(), targetPos);
	}

	public static Direction getPlayerDirection(GlobalPos anchor) {
		var player = Minecraft.getInstance().player;
		return LookingUtil.getDirectionFromVector(player.getForward(), player.level(), anchor);
	}

	public static Direction getDirectionFromVector(Vec3 forward, Level level, GlobalPos anchor) {
		return LookingUtil.getDirectionFromVector(new Vector3d(forward.x, forward.y, forward.z), level, anchor);
	}

	public static Direction getDirectionFromVector(Vector3d forward, Level level, GlobalPos anchor) {
		return LookingUtil.getDirectionFromVector(forward, new Matrix4d(), level, anchor);
	}

	public static Direction getDirectionFromVector(Vector3d forward, Matrix4d pose, Level level, GlobalPos anchor) {
		if (anchor != null) {
			var position = anchor.pos();
			var sublevel = SableCompanion.INSTANCE.getContaining(level, position);

			if (sublevel != null) {
				sublevel.logicalPose().bakeIntoMatrix(pose).invert().transformDirection(forward);
			}
		}

		var direction = Direction.getNearest(forward.x, forward.y, forward.z);
		return direction;
	}
}
