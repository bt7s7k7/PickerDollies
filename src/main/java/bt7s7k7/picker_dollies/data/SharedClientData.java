package bt7s7k7.picker_dollies.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.data.QuickFillState.Shape;
import bt7s7k7.picker_dollies.operation.AdjustSelectionOperation;
import bt7s7k7.picker_dollies.operation.CloneOperation;
import bt7s7k7.picker_dollies.operation.FillOperation;
import bt7s7k7.picker_dollies.operation.MovementOperation;
import bt7s7k7.picker_dollies.operation.OperationActivator;
import bt7s7k7.picker_dollies.operation.StackOperation;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.fml.loading.FMLPaths;

public class SharedClientData {
	private SharedClientData() {}

	private static StructureData structure = null;
	private static final Path structurePersistencePath = FMLPaths.GAMEDIR.get().resolve("picker_dollies_clipboard.nbt");

	static {
		loadStructureNow();
	}

	public static StructureTemplate getStructure() {
		return structure == null ? null : structure.template();
	}

	public static StructureData getStructureData() {
		return structure;
	}

	public static boolean saveStructureNow() {
		if (structure == null) return false;

		var tag = structure.tag();

		try (var outputstream = new FileOutputStream(structurePersistencePath.toFile())) {
			NbtIo.writeCompressed(tag, outputstream);
		} catch (IOException e) {
			PickerDollies.LOGGER.error("Failed to save structure to persistent path: {}", e);
			return false;
		}

		return true;
	}

	public static boolean loadStructureNow() {
		try (var inputstream = new FileInputStream(structurePersistencePath.toFile())) {
			var tag = NbtIo.readCompressed(inputstream, NbtAccounter.unlimitedHeap());
			structure = new StructureData(tag);
		} catch (FileNotFoundException e) {
			PickerDollies.LOGGER.info("There is not persistent clipboard file");
			return false;
		} catch (IOException e) {
			PickerDollies.LOGGER.error("Failed to load structure from persistent path: {}", e);
			return false;
		}

		return true;
	}

	public static void setStructure(StructureData structure) {
		SharedClientData.structure = structure;
		saveStructureNow();
	}

	public static final ScrollSelectedValue<OperationActivator> selectedOperation = new ScrollSelectedValue<OperationActivator>() {
		private final List<OperationActivator> OPERATIONS = List.of(
				MovementOperation.ACTIVATOR,
				CloneOperation.ACTIVATOR,
				AdjustSelectionOperation.ACTIVATOR,
				FillOperation.ACTIVATOR,
				StackOperation.ACTIVATOR);

		@Override
		public List<OperationActivator> getOptions() {
			return this.OPERATIONS;
		}

		@Override
		public boolean canUse(OperationActivator value) {
			return value.canActivate();
		}
	};

	public static final ScrollSelectedValue<QuickFillState.Shape> selectedQuickFillShape = new ScrollSelectedValue<QuickFillState.Shape>() {
		@Override
		public List<Shape> getOptions() {
			return Arrays.asList(QuickFillState.Shape.values());
		}

		@Override
		public boolean canUse(Shape value) {
			return true;
		}
	};

	static {
		selectedQuickFillShape.selectIfPossible(Shape.LEGACY);
	}
}
