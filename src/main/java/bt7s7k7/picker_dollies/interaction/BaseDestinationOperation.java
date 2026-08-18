package bt7s7k7.picker_dollies.interaction;

import java.util.stream.Stream;

import bt7s7k7.picker_dollies.ClientInputEvents;
import bt7s7k7.picker_dollies.data.WorldClientData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public abstract class BaseDestinationOperation implements ActiveOperation {
	public DestinationArea destination;

	public BaseDestinationOperation(DestinationArea destination) {
		this.destination = destination;
	}

	public BaseDestinationOperation(Selection source) {
		this.destination = DestinationArea.from(source);
	}

	public abstract int getColor();

	@Override
	public boolean supportsMoveTo() {
		return true;
	}

	@Override
	public Stream<Component> getHelpMessage() {
		return Stream.concat(Stream.of(Component.translatable("gui.picker_dollies.move_state",
				Component.literal("" + this.destination.offset.getX()).withStyle(ChatFormatting.GOLD),
				Component.literal("" + this.destination.offset.getY()).withStyle(ChatFormatting.GOLD),
				Component.literal("" + this.destination.offset.getZ()).withStyle(ChatFormatting.GOLD),
				Component.empty().append(this.destination.getMirror().symbol()).withStyle(ChatFormatting.GOLD),
				Component.literal(this.destination.getRotationAngle()).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.YELLOW)),
				ClientInputEvents.baseOperationHelp());
	}

	@Override
	public GlobalPos getAnchor() {
		return new GlobalPos(this.destination.dimension, this.destination.getPos());
	}

	@Override
	public Rotation getRotation() {
		return this.destination.rotation;
	}

	@Override
	public Stream<PreviewBox> getPreviewRenderPositions() {
		return Stream.of(new PreviewBox(this.destination, this.destination.getUntransformedArea().getPos()));
	}

	@Override
	public Stream<DestinationBox> getDestinationBoxes() {
		return Stream.of(new DestinationBox(this.destination, this.getColor()));
	}

	@Override
	public void cancel() {
		WorldClientData.getInstance().activeOperation = null;
	}

	@Override
	public void move(Vec3i offset, Direction direction, int amount) {
		this.destination.applyOffset(offset);
	}

	@Override
	public void moveTo(GlobalPos globalPos) {
		this.destination.moveTo(globalPos);
	}

	@Override
	public void applyMirror(Mirror mirror) {
		this.destination.applyMirror(mirror);
	}

	@Override
	public void applyRotation(Rotation rotation) {
		this.destination.applyRotation(rotation);
	}
}
