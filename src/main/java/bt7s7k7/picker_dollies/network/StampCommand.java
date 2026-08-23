package bt7s7k7.picker_dollies.network;

import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.data.DestinationArea;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StampCommand(DestinationArea to) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<StampCommand> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PickerDollies.MODID, "stamp_command"));

	public static final StreamCodec<ByteBuf, StampCommand> STREAM_CODEC = StreamCodec.composite(
			DestinationArea.STREAM_CODEC,
			StampCommand::to,
			StampCommand::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
