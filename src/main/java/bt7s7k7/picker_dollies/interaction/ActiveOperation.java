package bt7s7k7.picker_dollies.interaction;

import java.util.stream.Stream;

import bt7s7k7.picker_dollies.ClientInputEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public interface ActiveOperation {
	public DestinationArea getDestination();

	public int getColor();

	public default Stream<Component> getHelpMessage() {
		var destination = this.getDestination();
		return Stream.concat(Stream.of(Component.translatable("gui.picker_dollies.move_state",
				Component.literal("" + destination.offset.getX()).withStyle(ChatFormatting.GOLD),
				Component.literal("" + destination.offset.getY()).withStyle(ChatFormatting.GOLD),
				Component.literal("" + destination.offset.getZ()).withStyle(ChatFormatting.GOLD),
				Component.empty().append(destination.getMirror().symbol()).withStyle(ChatFormatting.GOLD),
				Component.literal(destination.getRotationAngle()).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.YELLOW)),
				ClientInputEvents.baseOperationHelp());
	}

	public void cancel();

	public void move(Vec3i offset);

	public void moveTo(GlobalPos globalPos);

	public void apply();

	public Stream<BlockPos> getPreviewRenderPositions();

	public void applyMirror(Mirror mirror);

	public void applyRotation(Rotation rotation);
}
