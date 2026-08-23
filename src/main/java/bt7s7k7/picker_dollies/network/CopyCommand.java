package bt7s7k7.picker_dollies.network;

import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.data.Selection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CopyCommand(Selection selection) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CopyCommand> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PickerDollies.MODID, "copy_command"));

	public static final StreamCodec<ByteBuf, CopyCommand> STREAM_CODEC = StreamCodec.composite(
			Selection.STREAM_CODEC,
			CopyCommand::selection,
			CopyCommand::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
