package bt7s7k7.picker_dollies.network;

import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.data.ServerPlayerData;
import bt7s7k7.picker_dollies.data.SharedClientData;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber
public class CommandHandler {
	@SubscribeEvent
	public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");

		registrar.commonToServer(SelectionContentRequest.TYPE, SelectionContentRequest.STREAM_CODEC, (payload, ctx) -> {
			var selection = payload.selection();

			if (!selection.isWithinLimits()) {
				PickerDollies.LOGGER.error("Client tried ot execute SelectionContentRequest with a selection outside limits");
				return;
			}

			var structure = selection.getStructure();
			if (structure == null) return;

			ServerPlayerData.of(ctx.player()).structure = structure;

			ctx.reply(new SelectionContentResponse(structure));
		});

		registrar.commonToClient(SelectionContentResponse.TYPE, SelectionContentResponse.STREAM_CODEC, (payload, ctx) -> {
			SharedClientData.setStructure(payload.template());
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

			// If there are already blocks in the target are, we want to make them drop to prevent
			// material loss. This is only useful in survival. I put it here because I copied this
			// code from MovementCommand, but StampCommand is not designed to be survival friendly,
			// so this might not serve a purpose.
			destination.applyStructure(structure, !ctx.player().isCreative());
		});
	}

	public static void register() {
		// Nothing
	}
}
