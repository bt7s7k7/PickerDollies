package bt7s7k7.picker_dollies.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import bt7s7k7.picker_dollies.PickerDollies;
import bt7s7k7.picker_dollies.interaction.CloneOperation;
import bt7s7k7.picker_dollies.interaction.MovementOperation;
import bt7s7k7.picker_dollies.interaction.OperationActivator;
import bt7s7k7.picker_dollies.interaction.StackOperation;
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

	public static final List<OperationActivator> OPERATIONS = List.of(
			MovementOperation.ACTIVATOR,
			CloneOperation.ACTIVATOR,
			StackOperation.ACTIVATOR);
	private static int selectedOperationIdx = 0;

	public static void selectNextOperation() {
		selectedOperationIdx++;
		if (selectedOperationIdx >= OPERATIONS.size()) {
			selectedOperationIdx = 0;
		}
	}

	public static void selectPreviousOperation() {
		selectedOperationIdx--;
		if (selectedOperationIdx < 0) {
			selectedOperationIdx = OPERATIONS.size() - 1;
		}
	}

	public static OperationActivator getSelectedOperation() {
		while (true) {
			// There should probably be a guard here against infinite loops, but there shouldn't be
			// a case where there are no activatable operations, because MoveOperation is always
			// activatable.

			var operation = OPERATIONS.get(selectedOperationIdx);
			if (operation.canActivate()) return operation;
			selectNextOperation();
		}
	}

}
