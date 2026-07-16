package com.nisovin.shopkeepers.dependencies.customitems;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Soft dependencies used to refresh custom items stored by Shopkeepers.
 */
public final class CustomItemsDependency {

	public static final String MMOITEMS = "MMOItems";
	public static final String MAXI_MINIONS = "MaxiMinions";
	/** Plugin.yml name of MaxiUpgrade. */
	public static final String MAXI_UPGRADE = "maxiUpgrade";

	public static boolean isMmoItemsEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled(MMOITEMS);
	}

	public static boolean isMaxiMinionsEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled(MAXI_MINIONS);
	}

	public static boolean isMaxiUpgradeEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled(MAXI_UPGRADE);
	}

	public static boolean isAnyEnabled() {
		return isMmoItemsEnabled() || isMaxiMinionsEnabled() || isMaxiUpgradeEnabled();
	}

	public static @Nullable Plugin getMmoItems() {
		return Bukkit.getPluginManager().getPlugin(MMOITEMS);
	}

	public static @Nullable Plugin getMaxiMinions() {
		return Bukkit.getPluginManager().getPlugin(MAXI_MINIONS);
	}

	public static @Nullable Plugin getMaxiUpgrade() {
		return Bukkit.getPluginManager().getPlugin(MAXI_UPGRADE);
	}

	private CustomItemsDependency() {
	}
}
