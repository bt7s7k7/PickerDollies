package bt7s7k7.picker_dollies.network;

import bt7s7k7.picker_dollies.Config;
import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.data.DestinationArea;
import bt7s7k7.picker_dollies.data.Selection;
import bt7s7k7.picker_dollies.data.ServerPlayerData;
import bt7s7k7.picker_dollies.data.SharedClientData;
import bt7s7k7.picker_dollies.data.StructureData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber
public class CommandHandler {
	private static boolean canExecuteFreeOperation(Player player) {
		return !Config.DISABLE_FREE_OPERATIONS_IN_SURVIVAL.getAsBoolean() || player.isCreative();
	}

	@SubscribeEvent
	public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");

		registrar.commonToServer(CopyCommand.TYPE, CopyCommand.STREAM_CODEC, (payload, ctx) -> {
			var selection = payload.selection();

			if (!selection.isWithinLimits()) {
				PickerDollies.LOGGER.error("Client tried ot execute CopyCommand with a selection outside limits");
				return;
			}

			var structure = selection.getStructure();
			if (structure == null) return;

			ServerPlayerData.of(ctx.player()).structure = structure;

			ctx.reply(new SelectionContentResponse(new StructureData(structure)));
		});

		registrar.commonToClient(SelectionContentResponse.TYPE, SelectionContentResponse.STREAM_CODEC, (payload, ctx) -> {
			SharedClientData.setStructure(payload.data());
		});

		registrar.commonToServer(PasteCommand.TYPE, PasteCommand.STREAM_CODEC, (payload, ctx) -> {
			ServerPlayerData.of(ctx.player()).structure = payload.data().template();
		});

		registrar.commonToServer(MovementCommand.TYPE, MovementCommand.STREAM_CODEC, (payload, ctx) -> {
			var selection = payload.from();

			if (!selection.isWithinLimits()) {
				PickerDollies.LOGGER.error("Client tried ot execute MovementCommand with a selection outside limits");
				return;
			}

			var structure = selection.getStructure();
			if (structure == null) return;

			selection.fillBlocks(Blocks.AIR.defaultBlockState());

			var destination = payload.to();

			// If there are already blocks in the target are, we want to make them drop to prevent
			// material loss. This is only useful in survival.
			destination.applyStructure(structure, !ctx.player().isCreative());
		});

		registrar.commonToServer(StampCommand.TYPE, StampCommand.STREAM_CODEC, (payload, ctx) -> {
			var structure = ServerPlayerData.of(ctx.player()).structure;
			if (structure == null) {
				PickerDollies.LOGGER.error("Client tried ot execute StampCommand without a server-side structure");
				return;
			}

			var destination = payload.to();

			if (!canExecuteFreeOperation(ctx.player())) {
				PickerDollies.LOGGER.error("Client tried ot execute StampCommand in survival but DISABLE_FREE_OPERATIONS_IN_SURVIVAL is enabled");
				return;
			}

			// If there are already blocks in the target are, we want to make them drop to prevent
			// material loss. This is only useful in survival. I put it here because I copied this
			// code from MovementCommand, but StampCommand is not designed to be survival friendly,
			// so this might not serve a purpose.
			destination.applyStructure(structure, !ctx.player().isCreative());
		});

		registrar.commonToServer(StampManyCommand.TYPE, StampManyCommand.STREAM_CODEC, (payload, ctx) -> {
			var structure = ServerPlayerData.of(ctx.player()).structure;
			if (structure == null) {
				PickerDollies.LOGGER.error("Client tried ot execute StampManyCommand without a server-side structure");
				return;
			}

			var destination = payload.to();
			var positions = payload.positions();

			if (!canExecuteFreeOperation(ctx.player())) {
				PickerDollies.LOGGER.error("Client tried ot execute StampManyCommand in survival but DISABLE_FREE_OPERATIONS_IN_SURVIVAL is enabled");
				return;
			}

			for (var position : positions) {
				destination.setPos(position);
				destination.applyStructure(structure, !ctx.player().isCreative());
			}
		});

		registrar.commonToServer(FillCommand.TYPE, FillCommand.STREAM_CODEC, (payload, ctx) -> {
			var selection = payload.target();

			if (!selection.isWithinLimits()) {
				PickerDollies.LOGGER.error("Client tried ot execute FillCommand with a selection outside limits");
				return;
			}

			if (payload.source().isEmpty()) {
				if (!ctx.player().isCreative()) {
					selection.destroyBlocks();
				} else {
					selection.fillBlocks(Blocks.AIR.defaultBlockState());
				}

				return;
			}

			if (!canExecuteFreeOperation(ctx.player())) {
				PickerDollies.LOGGER.error("Client tried ot execute FillCommand in survival but DISABLE_FREE_OPERATIONS_IN_SURVIVAL is enabled");
				return;
			}

			var source = payload.source().get();
			var sourceSelection = new Selection(source.dimension(), new BoundingBox(source.pos()));
			var structure = sourceSelection.getStructure();

			if (payload.isGentle()) {
				// Do not replace existing blocks for quick fill
				var level = selection.getLevel();

				for (var pos : BlockPos.betweenClosed(selection.getPos(), selection.getPos().offset(selection.getSize()).offset(-1, -1, -1))) {
					var existing = level.getBlockState(pos);
					if (!existing.canBeReplaced()) continue;

					var destination = new DestinationArea(selection.getDimension(), new BoundingBox(pos));
					destination.applyStructure(structure, !ctx.player().isCreative());
				}

				return;
			}

			if (!ctx.player().isCreative()) {
				selection.destroyBlocks();
			}

			for (var pos : BlockPos.betweenClosed(selection.getPos(), selection.getPos().offset(selection.getSize()).offset(-1, -1, -1))) {
				var destination = new DestinationArea(selection.getDimension(), new BoundingBox(pos));
				destination.applyStructure(structure, false);
			}
		});

		registrar.commonToServer(CutCommand.TYPE, CutCommand.STREAM_CODEC, (payload, ctx) -> {
			var selection = payload.from();

			if (!selection.isWithinLimits()) {
				PickerDollies.LOGGER.error("Client tried ot execute CutCommand with a selection outside limits");
				return;
			}

			var structure = selection.getStructure();
			if (structure == null) return;

			if (!canExecuteFreeOperation(ctx.player())) {
				// When cheat mode is off, the survival player will not be able to past the cut
				// blocks back into the world -- we drop them to preserve resources.
				selection.destroyBlocks();
			} else {
				selection.fillBlocks(Blocks.AIR.defaultBlockState());
			}

			ServerPlayerData.of(ctx.player()).structure = structure;
			ctx.reply(new SelectionContentResponse(new StructureData(structure)));
		});

		registrar.commonToClient(BlockPlacedNotification.TYPE, BlockPlacedNotification.STREAM_CODEC, (payload, ctx) -> {
			NeoForge.EVENT_BUS.post(payload);
		});
	}

	@SubscribeEvent
	public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		PacketDistributor.sendToPlayer(player, new BlockPlacedNotification(new GlobalPos(player.level().dimension(), event.getPos())));
	}

	public static void register() {
		// Nothing
	}
}
