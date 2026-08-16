package bt7s7k7.picker_dollies.interaction;

import java.util.stream.Stream;

import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;

public interface ActiveOperation {
	public Area getDestination();

	public int getColor();

	public Stream<Component> getHelpMessage();

	public void cancel();

	public void move(Vec3i offset);

	public void apply();

}
