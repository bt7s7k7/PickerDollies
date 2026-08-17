package bt7s7k7.picker_dollies.network;

import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.data.StructureData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectionContentResponse(StructureData data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SelectionContentResponse> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PickerDollies.MODID, "selection_content"));

	public static final StreamCodec<ByteBuf, SelectionContentResponse> STREAM_CODEC = StreamCodec.composite(
			StructureData.STREAM_CODEC,
			SelectionContentResponse::data,
			SelectionContentResponse::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
