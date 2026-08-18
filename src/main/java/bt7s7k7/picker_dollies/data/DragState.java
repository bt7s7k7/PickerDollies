package bt7s7k7.picker_dollies.data;

import org.joml.Vector3d;

import bt7s7k7.picker_dollies.interaction.ActiveOperation;
import net.minecraft.core.Direction;

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
}
