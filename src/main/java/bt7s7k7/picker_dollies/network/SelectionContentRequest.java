package bt7s7k7.picker_dollies.network;

import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.interaction.Selection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectionContentRequest(Selection selection) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SelectionContentRequest> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PickerDollies.MODID, "selection_content_request"));

    public static final StreamCodec<ByteBuf, SelectionContentRequest> STREAM_CODEC = StreamCodec.composite(
            Selection.STREAM_CODEC,
            SelectionContentRequest::selection,
            SelectionContentRequest::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
