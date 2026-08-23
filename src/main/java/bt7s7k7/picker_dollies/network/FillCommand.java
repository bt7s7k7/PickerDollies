package bt7s7k7.picker_dollies.network;

import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.data.Selection;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FillCommand(Selection target, GlobalPos source) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<FillCommand> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PickerDollies.MODID, "fill_command"));

	public static final StreamCodec<ByteBuf, FillCommand> STREAM_CODEC = StreamCodec.composite(
			Selection.STREAM_CODEC,
			FillCommand::target,
			GlobalPos.STREAM_CODEC,
			FillCommand::source,
			FillCommand::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
