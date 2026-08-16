package bt7s7k7.picker_dollies.network;

import bt7s7k7.picker_dollies.PickerDollies;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class SelectionContentResponse implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SelectionContentResponse> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PickerDollies.MODID, "selection_content_response"));

    protected StructureTemplate template;
    protected CompoundTag tag;

    public SelectionContentResponse(StructureTemplate template) {
        this.template = template;
    }

    public SelectionContentResponse(CompoundTag tag) {
        this.tag = tag;
    }

    public CompoundTag tag() {
        if (this.tag != null) return this.tag;
        return this.tag = this.template.save(new CompoundTag());
    }

    public StructureTemplate template() {
        if (this.template != null) return this.template;
        var structure = new StructureTemplate();
        structure.load(BuiltInRegistries.BLOCK.asLookup(), this.tag);
        return this.template = structure;
    }

    public static final StreamCodec<ByteBuf, SelectionContentResponse> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG,
            SelectionContentResponse::tag,
            SelectionContentResponse::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
