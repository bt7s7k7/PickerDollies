package bt7s7k7.picker_dollies.selection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public record SelectionPoint(Level level, BlockPos position) {
	public GlobalPos toGlobalPos() {
		return GlobalPos.of(this.level.dimension(), this.position);
	}

	public static SelectionPoint fromGlobalPos(MinecraftServer server, GlobalPos globalPos) {
		return new SelectionPoint(server.getLevel(globalPos.dimension()), globalPos.pos());
	}
}
