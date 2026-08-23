package bt7s7k7.picker_dollies.network;

import bt7s7k7.picker_dollies.PickerDollies;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

public class BlockPlacedNotification extends Event implements CustomPacketPayload {
	protected GlobalPos pos;

	public GlobalPos pos() {
		return this.pos;
	}

	public BlockPlacedNotification(GlobalPos pos) {
		this.pos = pos;
	}

	public static final CustomPacketPayload.Type<BlockPlacedNotification> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PickerDollies.MODID, "block_placed"));

	public static final StreamCodec<ByteBuf, BlockPlacedNotification> STREAM_CODEC = StreamCodec.composite(
			GlobalPos.STREAM_CODEC,
			BlockPlacedNotification::pos,
			BlockPlacedNotification::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
