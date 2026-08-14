package com.nisovin.shopkeepers.dependencies.customitems;

import java.lang.reflect.Method;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Rebuilds known custom items from their plugin definitions (MMOItems type+id, MaxiMinions
 * type+level, MaxiUpgrade lore rebuild). Soft-deps via reflection so Shopkeepers stays loadable
 * without those plugins.
 * <p>
 * Trade costs strip Nexo lore and MaxiItems rarity (lore + {@code mmoitems:vanilla_tier}) so
 * CustomCrops / farm produce still match Minecraft merchant ingredients.
 */
public final class CustomItemsRefresher {

	private static final String NBT_ITEM_CLASS = "io.lumine.mythic.lib.api.item.NBTItem";
	private static final String MMOITEMS_CLASS = "net.Indyuce.mmoitems.MMOItems";
	private static final String TAG_ITEM_ID = "MMOITEMS_ITEM_ID";
	private static final String TAG_ITEM_TYPE = "MMOITEMS_ITEM_TYPE";
	private static final String TAG_UPGRADE_LEVEL = "MMOITEMS_UPGRADE_LEVEL";
	private static final NamespacedKey NEXO_ID_KEY = new NamespacedKey("nexo", "id");
	private static final NamespacedKey VANILLA_TIER_KEY = new NamespacedKey("mmoitems", "vanilla_tier");

	private static volatile boolean mmoItemsWarned = false;
	private static volatile boolean minionsWarned = false;
	private static volatile boolean upgradeWarned = false;

	private CustomItemsRefresher() {
	}

	/**
	 * @return refreshed copy, or the same instance if unchanged / empty
	 */
	public static UnmodifiableItemStack refresh(UnmodifiableItemStack item) {
		if (ItemUtils.isEmpty(item)) return item;
		ItemStack copy = item.copy();
		ItemStack refreshed = refresh(copy);
		if (refreshed == copy) return item;
		return UnmodifiableItemStack.ofNonNull(refreshed);
	}

	/**
	 * Refresh identity, then strip volatile lore and MaxiItems rarity from merchant buy
	 * ingredients. Minecraft requires the offered item to contain the recipe's components;
	 * rarity NBT on the recipe side blocks farm produce that players actually hold.
	 */
	public static UnmodifiableItemStack prepareTradeCost(UnmodifiableItemStack item) {
		if (ItemUtils.isEmpty(item)) return item;
		ItemStack prepared = prepareTradeCost(item.copy());
		return UnmodifiableItemStack.ofNonNull(prepared);
	}

	/**
	 * Mutates or replaces the given stack. Returns the item to use (may be a new instance).
	 */
	public static ItemStack refresh(ItemStack item) {
		if (ItemUtils.isEmpty(item)) return item;

		ItemStack minion = tryRefreshMaxiMinion(item);
		if (minion != null) return minion;

		ItemStack mmo = tryRefreshMmoItem(item);
		if (mmo != null) return mmo;

		// Non-MMOItems: still stamp vanilla rarity lore if MaxiItems API is present.
		tryStampVanillaTier(item);
		return item;
	}

	public static ItemStack prepareTradeCost(ItemStack item) {
		ItemStack refreshed = refreshIdentity(item);
		return stripVolatileTradeCostData(refreshed);
	}

	/**
	 * Same as {@link #refresh(ItemStack)} but does not stamp MaxiItems rarity onto the cost.
	 */
	private static ItemStack refreshIdentity(ItemStack item) {
		if (ItemUtils.isEmpty(item)) return item;

		ItemStack minion = tryRefreshMaxiMinion(item);
		if (minion != null) return minion;

		ItemStack mmo = tryRefreshMmoItem(item);
		if (mmo != null) return mmo;

		return item;
	}

	/**
	 * Drops MaxiItems rarity and Nexo/CustomCrops lore from the recipe ingredient.
	 */
	private static ItemStack stripVolatileTradeCostData(ItemStack item) {
		if (ItemUtils.isEmpty(item)) return item;

		ItemStack copy = item.clone();
		boolean changed = tryMaxiItemsStrip(copy);
		boolean nexo = readNexoId(copy) != null;
		boolean hadVanillaTier = hasVanillaTier(copy);
		if (!nexo && !hadVanillaTier && !changed) return item;

		ItemMeta meta = copy.getItemMeta();
		if (meta == null) return changed ? copy : item;
		if (hadVanillaTier) {
			meta.getPersistentDataContainer().remove(VANILLA_TIER_KEY);
			changed = true;
		}
		if (nexo || hadVanillaTier) {
			meta.setLore(null);
			changed = true;
		}
		if (changed) {
			copy.setItemMeta(meta);
			return copy;
		}
		return item;
	}

	private static boolean hasVanillaTier(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return false;
		return meta.getPersistentDataContainer().has(VANILLA_TIER_KEY, PersistentDataType.STRING);
	}

	private static boolean tryMaxiItemsStrip(ItemStack item) {
		try {
			Class<?> api = Class.forName("net.Indyuce.mmoitems.api.VanillaTierAPI");
			Object result = api.getMethod("strip", ItemStack.class).invoke(null, item);
			return result instanceof Boolean && (Boolean) result;
		} catch (ClassNotFoundException ignored) {
			return false;
		} catch (NoSuchMethodException ignored) {
			return false;
		} catch (Throwable ignored) {
			return false;
		}
	}

	static @Nullable String readNexoId(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return null;
		String id = meta.getPersistentDataContainer().get(NEXO_ID_KEY, PersistentDataType.STRING);
		return (id == null || id.isEmpty()) ? null : id;
	}

	private static @Nullable ItemStack tryRefreshMaxiMinion(ItemStack item) {
		if (!CustomItemsDependency.isMaxiMinionsEnabled()) return null;
		Plugin plugin = CustomItemsDependency.getMaxiMinions();
		if (plugin == null) return null;

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return null;

		NamespacedKey typeKey = new NamespacedKey(plugin, "minion_type");
		if (!meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
			return null;
		}

		String typeId = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
		if (typeId == null || typeId.isEmpty()) return null;

		NamespacedKey levelKey = new NamespacedKey(plugin, "minion_level");
		NamespacedKey collectedKey = new NamespacedKey(plugin, "minion_collected");
		int level = meta.getPersistentDataContainer().getOrDefault(
				levelKey,
				PersistentDataType.INTEGER,
				1
		);
		long collected = meta.getPersistentDataContainer().getOrDefault(
				collectedKey,
				PersistentDataType.LONG,
				0L
		);
		int amount = item.getAmount();

		try {
			Object placement = plugin.getClass().getMethod("getPlacementManager").invoke(plugin);
			if (placement == null) return null;
			Method create = placement.getClass().getMethod(
					"createMinionItem",
					String.class,
					int.class,
					long.class
			);
			ItemStack fresh = (ItemStack) create.invoke(placement, typeId, level, collected);
			if (ItemUtils.isEmpty(fresh)) return null;
			fresh.setAmount(Math.max(1, amount));
			return fresh;
		} catch (Throwable t) {
			if (!minionsWarned) {
				minionsWarned = true;
				Log.warning("MaxiMinions shop item refresh failed (further errors suppressed): "
						+ t.getMessage());
			}
			return null;
		}
	}

	private static @Nullable ItemStack tryRefreshMmoItem(ItemStack item) {
		if (!CustomItemsDependency.isMmoItemsEnabled()) return null;

		String typeId = readNbtString(item, TAG_ITEM_TYPE);
		String itemId = readNbtString(item, TAG_ITEM_ID);
		if (typeId == null || typeId.isEmpty() || itemId == null || itemId.isEmpty()) {
			return null;
		}

		int amount = item.getAmount();
		int upgradeLevel = readNbtInt(item, TAG_UPGRADE_LEVEL);

		try {
			Class<?> mmoItemsClass = Class.forName(MMOITEMS_CLASS);
			Object mmoPlugin = mmoItemsClass.getField("plugin").get(null);
			Method getItem = mmoItemsClass.getMethod("getItem", String.class, String.class);
			ItemStack fresh = (ItemStack) getItem.invoke(mmoPlugin, typeId, itemId);
			if (ItemUtils.isEmpty(fresh)) return null;
			fresh.setAmount(Math.max(1, amount));

			ItemStack upgraded = tryMaxiUpgradeRebuild(fresh, upgradeLevel);
			if (upgraded != null) {
				fresh = upgraded;
				fresh.setAmount(Math.max(1, amount));
			}

			tryStampVanillaTier(fresh);
			return fresh;
		} catch (Throwable t) {
			if (!mmoItemsWarned) {
				mmoItemsWarned = true;
				Log.warning("MMOItems shop item refresh failed (further errors suppressed): "
						+ t.getMessage());
			}
			return null;
		}
	}

	private static @Nullable ItemStack tryMaxiUpgradeRebuild(ItemStack item, int upgradeLevel) {
		if (!CustomItemsDependency.isMaxiUpgradeEnabled()) return null;
		Plugin plugin = CustomItemsDependency.getMaxiUpgrade();
		if (plugin == null) return null;
		try {
			Object rebuildService = plugin.getClass().getMethod("rebuildService").invoke(plugin);
			if (rebuildService == null) return null;
			ItemStack result;
			if (upgradeLevel > 0) {
				Method rebuildForced = rebuildService.getClass().getMethod(
						"rebuildWithForcedLevel",
						ItemStack.class,
						org.bukkit.entity.Player.class,
						int.class
				);
				result = (ItemStack) rebuildForced.invoke(
						rebuildService,
						item,
						null,
						upgradeLevel
				);
			} else {
				Method rebuild = rebuildService.getClass().getMethod(
						"rebuild",
						ItemStack.class,
						org.bukkit.entity.Player.class
				);
				result = (ItemStack) rebuild.invoke(rebuildService, item, null);
			}
			return ItemUtils.isEmpty(result) ? null : result;
		} catch (Throwable t) {
			if (!upgradeWarned) {
				upgradeWarned = true;
				Log.warning("MaxiUpgrade shop item rebuild failed (further errors suppressed): "
						+ t.getMessage());
			}
			return null;
		}
	}

	private static void tryStampVanillaTier(ItemStack item) {
		if (!CustomItemsDependency.isMmoItemsEnabled()) return;
		if (item.getType() == Material.AIR) return;
		try {
			Class<?> api = Class.forName("net.Indyuce.mmoitems.api.VanillaTierAPI");
			api.getMethod("stamp", ItemStack.class).invoke(null, item);
		} catch (ClassNotFoundException ignored) {
			// Older MaxiItems without VanillaTierAPI.
		} catch (Throwable ignored) {
			// Soft no-op.
		}
	}

	static @Nullable String readNbtString(ItemStack item, String key) {
		try {
			Class<?> nbtClass = Class.forName(NBT_ITEM_CLASS);
			Object nbt = nbtClass.getMethod("get", ItemStack.class).invoke(null, item);
			if (nbt == null) return null;
			boolean has = (boolean) nbtClass.getMethod("hasTag", String.class).invoke(nbt, key);
			if (!has) return null;
			String value = (String) nbtClass.getMethod("getString", String.class).invoke(nbt, key);
			return (value == null || value.isEmpty()) ? null : value;
		} catch (Throwable t) {
			return null;
		}
	}

	static int readNbtInt(ItemStack item, String key) {
		try {
			Class<?> nbtClass = Class.forName(NBT_ITEM_CLASS);
			Object nbt = nbtClass.getMethod("get", ItemStack.class).invoke(null, item);
			if (nbt == null) return 0;
			boolean has = (boolean) nbtClass.getMethod("hasTag", String.class).invoke(nbt, key);
			if (!has) return 0;
			return (int) nbtClass.getMethod("getInteger", String.class).invoke(nbt, key);
		} catch (Throwable t) {
			return 0;
		}
	}
}
