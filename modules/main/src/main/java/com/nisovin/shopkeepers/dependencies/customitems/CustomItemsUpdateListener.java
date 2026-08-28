package com.nisovin.shopkeepers.dependencies.customitems;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.events.UpdateItemEvent;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

/**
 * Official Shopkeepers extension point: refresh custom items when
 * {@code /shopkeeper updateItems} (or API) runs.
 */
public final class CustomItemsUpdateListener implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onUpdateItem(UpdateItemEvent event) {
		if (!CustomItemsDependency.isAnyEnabled()) return;

		UnmodifiableItemStack original = event.getItem();
		ItemStack refreshed = CustomItemsRefresher.rebuildFromDefinition(original.copy());
		if (ItemUtils.isEmpty(refreshed)) return;
		if (refreshed.isSimilar(original.copy())) return;

		event.setItem(UnmodifiableItemStack.ofNonNull(refreshed));
	}
}
