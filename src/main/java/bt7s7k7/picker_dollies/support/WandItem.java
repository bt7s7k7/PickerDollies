package bt7s7k7.picker_dollies.support;

import bt7s7k7.picker_dollies.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class WandItem {
	private WandItem() {}

	public static boolean inMainHand() {
		var wandItem = ResourceLocation.tryParse(Config.WAND_ITEM.get());
		if (wandItem == null) return false;

		var heldStack = Minecraft.getInstance().player.getMainHandItem();
		return heldStack != null && BuiltInRegistries.ITEM.getKey(heldStack.getItem()).equals(wandItem);
	}

	public static boolean isOffHand() {
		var wandItem = ResourceLocation.tryParse(Config.WAND_ITEM.get());
		if (wandItem == null) return false;

		var heldStack = Minecraft.getInstance().player.getOffhandItem();
		return heldStack != null && BuiltInRegistries.ITEM.getKey(heldStack.getItem()).equals(wandItem);
	}
}
