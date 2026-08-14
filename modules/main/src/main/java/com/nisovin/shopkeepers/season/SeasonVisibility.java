package com.nisovin.shopkeepers.season;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Soft-hook to MaxiSeasons: shops with a {@code seasons} list only spawn in those global seasons.
 */
public final class SeasonVisibility {

	private SeasonVisibility() {
	}

	public static boolean isVisible(AbstractShopkeeper shopkeeper) {
		String seasons = shopkeeper.getSeasons();
		if (seasons == null || seasons.isEmpty()) {
			return true;
		}
		String current = currentSeasonConfigName();
		if (current == null) {
			return true; // MaxiSeasons missing: show all shops
		}
		String[] parts = seasons.split("[,]+");
		for (String part : parts) {
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

	public static void register(SKShopkeepersPlugin plugin) {
		if (Bukkit.getPluginManager().getPlugin("MaxiSeasons") == null) {
			return;
		}
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(
					"me.casperge.realisticseasons.api.GlobalSeasonChangeEvent"
			);
			EventExecutor executor = (listener, event) -> plugin.getShopkeeperRegistry()
					.getChunkActivator()
					.refreshSeasonVisibility();
			Bukkit.getPluginManager().registerEvent(
					eventClass,
					new Listener() {
					},
					EventPriority.MONITOR,
					executor,
					plugin,
					false
			);
			Log.debug("Registered MaxiSeasons shop visibility listener.");
		} catch (Throwable e) {
			Log.debug("MaxiSeasons season event not found: " + e.getMessage());
		}
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
