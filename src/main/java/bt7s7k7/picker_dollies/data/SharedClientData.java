package bt7s7k7.picker_dollies.data;

import java.util.List;

import bt7s7k7.picker_dollies.interaction.CloneOperation;
import bt7s7k7.picker_dollies.interaction.MovementOperation;
import bt7s7k7.picker_dollies.interaction.OperationActivator;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class SharedClientData {
	private SharedClientData() {}

	private static StructureTemplate structure = null;

	public static StructureTemplate getStructure() {
		return structure;
	}

	public static void setStructure(StructureTemplate structure) {
		SharedClientData.structure = structure;
	}

	public static final List<OperationActivator> OPERATIONS = List.of(
			MovementOperation.ACTIVATOR,
			CloneOperation.ACTIVATOR);
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
