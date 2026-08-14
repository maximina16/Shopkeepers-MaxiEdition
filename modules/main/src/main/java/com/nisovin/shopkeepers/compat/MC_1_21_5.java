package com.nisovin.shopkeepers.compat;

import java.util.Optional;

import com.nisovin.shopkeepers.util.java.ClassUtils;
import com.nisovin.shopkeepers.util.logging.Log;

// TODO This can be removed once we only support Bukkit 1.21.5 upwards.
public final class MC_1_21_5 {

	private static Optional<Boolean> IS_AVAILABLE = Optional.empty();

	public static void init() {
		if (isAvailable()) {
			Log.debug("MC 1.21.5 exclusive features are enabled.");
		} else {
			Log.debug("MC 1.21.5 exclusive features are disabled.");
		}
	}

	/**
	 * Chicken/Cow/Pig biome variants (Spring to Life). Missing on 1.21.4 and below.
	 */
	public static boolean isAvailable() {
		if (!IS_AVAILABLE.isPresent()) {
			boolean isAvailable = ClassUtils.getClassOrNull("org.bukkit.entity.Chicken$Variant") != null;
			IS_AVAILABLE = Optional.of(isAvailable);
		}
		return IS_AVAILABLE.get();
	}

	private MC_1_21_5() {
	}
}
