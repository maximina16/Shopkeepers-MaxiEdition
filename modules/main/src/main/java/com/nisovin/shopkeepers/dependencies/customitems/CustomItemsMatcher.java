package com.nisovin.shopkeepers.dependencies.customitems;



import org.bukkit.NamespacedKey;

import org.bukkit.inventory.ItemStack;

import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.persistence.PersistentDataType;

import org.bukkit.plugin.Plugin;

import org.checkerframework.checker.nullness.qual.Nullable;



import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

import com.nisovin.shopkeepers.compat.Compat;

import com.nisovin.shopkeepers.util.inventory.ItemUtils;



/**

 * Matches custom items by stable IDs so lore/stat refreshes do not break trades.

 * <p>

 * CustomCrops watering cans rewrite lore and add {@code CustomCrops.water} when filled;

 * Nexo id matching ignores that state.

 */

public final class CustomItemsMatcher {



	private CustomItemsMatcher() {

	}



	public static boolean matches(

			@Nullable ItemStack provided,

			@Nullable UnmodifiableItemStack required

	) {

		return matches(provided, ItemUtils.asItemStackOrNull(required));

	}



	public static boolean matches(@Nullable ItemStack provided, @Nullable ItemStack required) {

		if (ItemUtils.isEmpty(required)) return ItemUtils.isEmpty(provided);

		if (ItemUtils.isEmpty(provided)) return false;

		assert provided != null && required != null;



		if (matchMaxiMinions(provided, required)) return true;

		if (matchMmoItems(provided, required)) return true;

		if (matchNexo(provided, required)) return true;



		return Compat.getProvider().matches(provided, required);

	}



	private static boolean matchNexo(ItemStack provided, ItemStack required) {

		String reqId = CustomItemsRefresher.readNexoId(required);

		if (reqId == null) return false;

		String provId = CustomItemsRefresher.readNexoId(provided);

		if (provId == null) return false;

		return reqId.equalsIgnoreCase(provId);

	}



	private static boolean matchMmoItems(ItemStack provided, ItemStack required) {

		if (!CustomItemsDependency.isMmoItemsEnabled()) return false;

		String reqType = CustomItemsRefresher.readNbtString(required, "MMOITEMS_ITEM_TYPE");

		String reqId = CustomItemsRefresher.readNbtString(required, "MMOITEMS_ITEM_ID");

		if (reqType == null || reqId == null) return false;



		String provType = CustomItemsRefresher.readNbtString(provided, "MMOITEMS_ITEM_TYPE");

		String provId = CustomItemsRefresher.readNbtString(provided, "MMOITEMS_ITEM_ID");

		if (provType == null || provId == null) return false;

		if (provided.getType() != required.getType()) return false;

		return reqType.equalsIgnoreCase(provType) && reqId.equalsIgnoreCase(provId);

	}



	private static boolean matchMaxiMinions(ItemStack provided, ItemStack required) {

		if (!CustomItemsDependency.isMaxiMinionsEnabled()) return false;

		Plugin plugin = CustomItemsDependency.getMaxiMinions();

		if (plugin == null) return false;



		String reqType = readMinionType(required, plugin);

		if (reqType == null) return false;

		String provType = readMinionType(provided, plugin);

		if (provType == null) return false;

		if (!reqType.equalsIgnoreCase(provType)) return false;



		int reqLevel = readMinionLevel(required, plugin);

		int provLevel = readMinionLevel(provided, plugin);

		return reqLevel == provLevel;

	}



	private static @Nullable String readMinionType(ItemStack item, Plugin plugin) {

		ItemMeta meta = item.getItemMeta();

		if (meta == null) return null;

		NamespacedKey key = new NamespacedKey(plugin, "minion_type");

		return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

	}



	private static int readMinionLevel(ItemStack item, Plugin plugin) {

		ItemMeta meta = item.getItemMeta();

		if (meta == null) return 1;

		NamespacedKey key = new NamespacedKey(plugin, "minion_level");

		return meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 1);

	}

}


