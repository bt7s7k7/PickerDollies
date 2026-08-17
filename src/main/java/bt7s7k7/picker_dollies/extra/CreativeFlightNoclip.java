package bt7s7k7.picker_dollies.extra;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import bt7s7k7.picker_dollies.Config;
import bt7s7k7.picker_dollies.Support;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class CreativeFlightNoclip {
	private CreativeFlightNoclip() {}

	private static PlayerInfo getPlayerInfo(Player player) {
		return Minecraft.getInstance().getConnection().getPlayerInfo(player.getUUID());
	}

	protected static final Set<UUID> markedPlayersServer = new HashSet<>();
	protected static final Set<UUID> markedPlayersLocal = new HashSet<>();

	@SubscribeEvent
	public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
		if (!Config.CREATIVE_FLIGHT_NOCLIP.getAsBoolean()) return;

		var player = event.getEntity();
		if (!player.getAbilities().flying || !player.isCreative()) return;

		if (player instanceof ServerPlayer serverPlayer) {
			Support.setField(serverPlayer.gameMode, "gameModeForPlayer", ServerPlayerGameMode.class, GameType.SPECTATOR);
			markedPlayersServer.add(player.getUUID());
		} else if (player instanceof LocalPlayer localPlayer) {
			var playerInfo = getPlayerInfo(localPlayer);
			Support.setField(playerInfo, "gameMode", PlayerInfo.class, GameType.SPECTATOR);
			markedPlayersLocal.add(player.getUUID());
		} else {
			return;
		}
	}

	@SubscribeEvent
	public static void onPlayerTickPost(PlayerTickEvent.Post event) {
		var player = event.getEntity();

		if (player instanceof ServerPlayer serverPlayer) {
			if (!markedPlayersServer.remove(serverPlayer.getUUID())) return;
			Support.setField(serverPlayer.gameMode, "gameModeForPlayer", ServerPlayerGameMode.class, GameType.CREATIVE);
		} else if (player instanceof LocalPlayer localPlayer) {
			if (!markedPlayersLocal.remove(localPlayer.getUUID())) return;
			var playerInfo = getPlayerInfo(localPlayer);
			Support.setField(playerInfo, "gameMode", PlayerInfo.class, GameType.CREATIVE);
		}
	}

	public static void register() {
		// Nothing
	}
}
