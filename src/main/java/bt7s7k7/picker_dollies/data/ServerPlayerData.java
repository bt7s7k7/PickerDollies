package bt7s7k7.picker_dollies.data;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class ServerPlayerData {
	public StructureTemplate structure = null;

	public static ServerPlayerData of(Player player) {
		return ServerData.getInstance().serverPlayerData.computeIfAbsent(player.getUUID(), __ -> new ServerPlayerData());
	}
}
