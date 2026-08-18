package bt7s7k7.picker_dollies.interaction;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public interface OperationActivator {
	public ActiveOperation activate();

	public Component getName();

	public boolean supportsMoveTo();

	public default boolean canActivate() {
		return this.canActivate(Minecraft.getInstance().player);
	}

	public default boolean canActivate(Player player) {
		return true;
	}
}
