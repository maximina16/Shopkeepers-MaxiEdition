package com.nisovin.shopkeepers.dependencies.customitems;

import org.bukkit.ChatColor;
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
 *
 * <p>
 * If the id was lost (broken item) but the material and visible name still match, the
 * trade is accepted. A different custom id never matches, even when the names match.
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

		if (customIdsConflict(provided, required)) return false;
		if (matchMaxiMinions(provided, required)) return true;
		if (matchMmoItems(provided, required)) return true;
		if (matchNexo(provided, required)) return true;
		if (matchVisibleName(provided, required)) return true;

		return Compat.getProvider().matches(provided, required);
	}

	private static boolean customIdsConflict(ItemStack provided, ItemStack required) {
		String reqType = CustomItemsRefresher.readNbtString(required, "MMOITEMS_ITEM_TYPE");
		String reqId = CustomItemsRefresher.readNbtString(required, "MMOITEMS_ITEM_ID");
		String provType = CustomItemsRefresher.readNbtString(provided, "MMOITEMS_ITEM_TYPE");
		String provId = CustomItemsRefresher.readNbtString(provided, "MMOITEMS_ITEM_ID");
		if (reqType != null && reqId != null && provType != null && provId != null) {
			if (!reqType.equalsIgnoreCase(provType) || !reqId.equalsIgnoreCase(provId)) {
				return true;
			}
		}

		String reqNexo = CustomItemsRefresher.readNexoId(required);
		String provNexo = CustomItemsRefresher.readNexoId(provided);
		if (reqNexo != null && provNexo != null && !reqNexo.equalsIgnoreCase(provNexo)) {
			return true;
		}

		if (!CustomItemsDependency.isMaxiMinionsEnabled()) return false;
		Plugin plugin = CustomItemsDependency.getMaxiMinions();
		if (plugin == null) return false;
		String reqMinion = readMinionType(required, plugin);
		String provMinion = readMinionType(provided, plugin);
		if (reqMinion == null || provMinion == null) return false;
		if (!reqMinion.equalsIgnoreCase(provMinion)) return true;
		return readMinionLevel(required, plugin) != readMinionLevel(provided, plugin);
	}

	private static boolean matchVisibleName(ItemStack provided, ItemStack required) {
		if (provided.getType() != required.getType()) return false;
		String requiredName = plainName(required);
		if (requiredName.isEmpty()) return false;
		return requiredName.equals(plainName(provided));
	}

	private static String plainName(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return "";
		String raw = "";
		if (meta.hasDisplayName()) {
			raw = meta.getDisplayName();
		}
		if (raw.isEmpty() && meta.hasItemName()) {
			raw = meta.getItemName();
		}
		String stripped = ChatColor.stripColor(raw);
		return stripped == null ? "" : stripped.trim();
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
