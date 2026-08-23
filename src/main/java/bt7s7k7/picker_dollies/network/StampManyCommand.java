package bt7s7k7.picker_dollies.network;

import java.util.List;

import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.data.DestinationArea;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StampManyCommand(DestinationArea to, List<BlockPos> positions) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<StampManyCommand> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PickerDollies.MODID, "stamp_many_command"));

	public static final StreamCodec<ByteBuf, StampManyCommand> STREAM_CODEC = StreamCodec.composite(
			DestinationArea.STREAM_CODEC,
			StampManyCommand::to,
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(512)),
			StampManyCommand::positions,
			StampManyCommand::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
