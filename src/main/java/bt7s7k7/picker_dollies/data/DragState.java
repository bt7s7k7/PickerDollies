package bt7s7k7.picker_dollies.data;

import org.joml.Vector3d;

import bt7s7k7.picker_dollies.interaction.ActiveOperation;

public class DragState {
	public final ActiveOperation target;
	public final Vector3d origin;
	public final Vector3d normal;
	public final Vector3d lastPosition;

	public DragState(ActiveOperation target, Vector3d origin, Vector3d normal) {
		this.target = target;
		this.origin = origin;
		this.normal = normal;
		this.lastPosition = new Vector3d(origin);
	}
}
