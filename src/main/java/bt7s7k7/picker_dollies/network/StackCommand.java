package bt7s7k7.picker_dollies.network;

import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.data.StackParameters;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StackCommand(StackParameters stack) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<StackCommand> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PickerDollies.MODID, "stack_command"));

	public static final StreamCodec<ByteBuf, StackCommand> STREAM_CODEC = StreamCodec.composite(
			StackParameters.STREAM_CODEC,
			StackCommand::stack,
			StackCommand::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
