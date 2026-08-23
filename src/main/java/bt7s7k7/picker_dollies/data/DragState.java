package bt7s7k7.picker_dollies.data;

import org.joml.Vector3d;

import bt7s7k7.picker_dollies.operation.ActiveOperation;
import bt7s7k7.picker_dollies.rendering.RenderedArea;
import bt7s7k7.picker_dollies.rendering.SelectionRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class DragState {
	public final ActiveOperation target;
	public final Vector3d origin;
	public final Vector3d normal;
	public final Vector3d lastPosition;
	public final Direction primaryDirection;
	public final Direction secondaryDirection;

	public DragState(ActiveOperation target, Vector3d origin, Vector3d normal, Direction primaryDirection, Direction secondaryDirection) {
		this.target = target;
		this.origin = origin;
		this.normal = normal;
		this.lastPosition = new Vector3d(origin);
		this.primaryDirection = primaryDirection;
		this.secondaryDirection = secondaryDirection;
	}

	public DragState withTarget(ActiveOperation target) {
		return new DragState(target, this.origin, this.normal, this.primaryDirection, this.secondaryDirection);
	}

	public static DragState tryStart(Player player) {
		var dir = player.getViewVector(0.0f);
		var playerPosition = player.getEyePosition(0.0f);

		var hitDistance = 10.0;
		// Rendered areas are positioned relative to the camera
		var start = Vec3.ZERO;

		var bestHit = (RenderedArea.HitResult) null;
		var bestHitDistance = Double.POSITIVE_INFINITY;
		var hitSelection = false;

		if (WorldClientData.getInstance().activeOperation == null) {
			if (SelectionRenderer.renderedSelection != null) {
				var hit = SelectionRenderer.renderedSelection.clip(start, dir, hitDistance);
				if (hit != null) {
					var distance = hit.position().distanceSquared(start.x, start.y, start.z);
					if (distance < bestHitDistance) {
						bestHitDistance = distance;
						bestHit = hit;
						hitSelection = true;
					}
				}
			}
		} else {
			for (var area : SelectionRenderer.renderedActiveAreas) {
				var hit = area.clip(start, dir, hitDistance);
				if (hit == null) continue;

				var distance = hit.position().distanceSquared(start.x, start.y, start.z);
				if (distance >= bestHitDistance) continue;

				bestHitDistance = distance;
				bestHit = hit;
				hitSelection = false;
			}
		}

		if (bestHit == null) return null;

		var worldHitPosition = new Vector3d(bestHit.position()).add(playerPosition.x, playerPosition.y, playerPosition.z);

		if (!hitSelection && WorldClientData.getInstance().activeOperation == null) throw new NullPointerException();

		return new DragState(hitSelection ? null : WorldClientData.getInstance().activeOperation, worldHitPosition, bestHit.normal(), bestHit.primaryDirection(), bestHit.secondaryDirection());
	}
}
