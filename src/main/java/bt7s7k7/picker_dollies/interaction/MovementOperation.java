package bt7s7k7.picker_dollies.interaction;

import java.util.stream.Stream;

import bt7s7k7.picker_dollies.ClientInputEvents;
import bt7s7k7.picker_dollies.data.WorldClientData;
import bt7s7k7.picker_dollies.network.MovementCommand;
import bt7s7k7.picker_dollies.network.SelectionContentRequest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class MovementOperation implements ActiveOperation {
	public Selection source;
	public DestinationArea destination;

	public MovementOperation(Selection source, DestinationArea destination) {
		this.source = source;
		this.destination = destination;
	}

	public MovementOperation(Selection source) {
		this.source = source;
		this.destination = DestinationArea.from(source);
	}

	@Override
	public Area getDestination() {
		return this.destination;
	}

	@Override
	public int getColor() {
		return 0xffffff00;
	}

	@Override
	public Stream<Component> getHelpMessage() {
		return Stream.concat(Stream.<Component>of(Component.literal("Offset: [").withStyle(ChatFormatting.YELLOW)
				.append(Component.literal("" + this.destination.offset.getX()).withStyle(ChatFormatting.GOLD)).append(Component.literal(", "))
				.append(Component.literal("" + this.destination.offset.getY()).withStyle(ChatFormatting.GOLD)).append(Component.literal(", "))
				.append(Component.literal("" + this.destination.offset.getZ()).withStyle(ChatFormatting.GOLD)).append(Component.literal("]"))),
				ClientInputEvents.BASE_OPERATION_HELP.stream());
	}

	@Override
	public void cancel() {
		WorldClientData.getInstance().activeOperation = null;
	}

	@Override
	public void move(Vec3i offset) {
		this.destination.applyOffset(offset);
	}

	@Override
	public void apply() {
		PacketDistributor.sendToServer(new MovementCommand(this.source, this.destination));
		var data = WorldClientData.getInstance();
		data.activeOperation = null;
		data.selection.clear();
	}

	public static MovementOperation activate() {
		var data = WorldClientData.getInstance();
		var operation = new MovementOperation(data.selection);
		data.activeOperation = operation;
		PacketDistributor.sendToServer(new SelectionContentRequest(data.selection));
		return operation;
	}
}
