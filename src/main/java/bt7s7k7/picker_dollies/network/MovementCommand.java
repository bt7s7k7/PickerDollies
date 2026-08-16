package bt7s7k7.picker_dollies.network;

import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.interaction.DestinationArea;
import bt7s7k7.picker_dollies.interaction.Selection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MovementCommand(Selection from, DestinationArea to) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MovementCommand> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PickerDollies.MODID, "movement_command"));

	public static final StreamCodec<ByteBuf, MovementCommand> STREAM_CODEC = StreamCodec.composite(
			Selection.STREAM_CODEC,
			MovementCommand::from,
			DestinationArea.STREAM_CODEC,
			MovementCommand::to,
			MovementCommand::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
