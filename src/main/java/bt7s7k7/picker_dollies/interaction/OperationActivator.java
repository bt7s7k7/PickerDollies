package bt7s7k7.picker_dollies.interaction;

import net.minecraft.network.chat.Component;

public interface OperationActivator {
	public ActiveOperation activate();

	public Component getName();

	public default boolean canActivate() {
		return true;
	}
}
