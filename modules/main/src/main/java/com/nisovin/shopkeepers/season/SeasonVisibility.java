package com.nisovin.shopkeepers.season;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

/**
 * Item-based season filter: a trade is hidden unless its result item is tagged for the current
 * global season. Untagged items (all existing shops) are always visible. Villagers themselves are
 * never despawned.
 */
public final class SeasonVisibility {

	private static final String PDC_KEY = "trade_seasons";

	private SeasonVisibility() {
	}

	public static NamespacedKey key() {
		return new NamespacedKey(SKShopkeepersPlugin.getInstance(), PDC_KEY);
	}

	public static boolean isRecipeVisible(TradingRecipe recipe) {
		if (recipe == null) {
			return true;
		}
		return isItemVisible(recipe.getResultItem());
	}

	public static boolean isItemVisible(@Nullable UnmodifiableItemStack item) {
		if (item == null || ItemUtils.isEmpty(item)) {
			return true;
		}
		return isItemVisible(item.copy());
	}

	public static boolean isItemVisible(@Nullable ItemStack item) {
		String seasons = readSeasons(item);
		if (seasons == null || seasons.isEmpty()) {
			return true; // No tag: always shown (existing villagers)
		}
		String current = currentSeasonConfigName();
		if (current == null) {
			return true; // MaxiSeasons missing
		}
		for (String part : seasons.split("[,]+")) {
			String token = part.trim();
			if (token.isEmpty()) {
				continue;
			}
			if (token.equalsIgnoreCase(current) || token.equalsIgnoreCase("ALL")) {
				return true;
			}
		}
		return false;
	}

	public static List<? extends TradingRecipe> filterRecipes(
			List<? extends TradingRecipe> recipes
	) {
		if (recipes == null || recipes.isEmpty()) {
			return recipes;
		}
		List<TradingRecipe> visible = new ArrayList<>(recipes.size());
		for (TradingRecipe recipe : recipes) {
			if (isRecipeVisible(recipe)) {
				visible.add(recipe);
			}
		}
		return visible;
	}

	public static @Nullable String readSeasons(@Nullable ItemStack item) {
		if (item == null || ItemUtils.isEmpty(item) || !item.hasItemMeta()) {
			return null;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return null;
		}
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		String raw = pdc.get(key(), PersistentDataType.STRING);
		if (raw == null || raw.isBlank()) {
			return null;
		}
		return raw;
	}

	public static void writeSeasons(ItemStack item, @Nullable String seasons) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return;
		}
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		String normalized = normalize(seasons);
		if (normalized.isEmpty()) {
			pdc.remove(key());
		} else {
			pdc.set(key(), PersistentDataType.STRING, normalized);
		}
		item.setItemMeta(meta);
	}

	public static String normalize(@Nullable String raw) {
		if (raw == null) {
			return "";
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty() || trimmed.equals("-") || trimmed.equalsIgnoreCase("all")) {
			return "";
		}
		return trimmed.replace(';', ',').replace(' ', ',').toUpperCase(Locale.ROOT);
	}

	private static @Nullable String currentSeasonConfigName() {
		if (Bukkit.getPluginManager().getPlugin("MaxiSeasons") == null) {
			return null;
		}
		try {
			Class<?> apiClass = Class.forName("me.casperge.realisticseasons.api.SeasonsAPI");
			Object api = apiClass.getMethod("getInstance").invoke(null);
			if (api == null) {
				return null;
			}
			Object season = apiClass.getMethod("getGlobalSeason").invoke(api);
			if (season == null) {
				return null;
			}
			Object name = season.getClass().getMethod("getConfigName").invoke(season);
			return name instanceof String string ? string : null;
		} catch (Throwable ignored) {
			return null;
		}
	}
}
