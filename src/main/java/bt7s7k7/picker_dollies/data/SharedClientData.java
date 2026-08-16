package bt7s7k7.picker_dollies.data;

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
}
