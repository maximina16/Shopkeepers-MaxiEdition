package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.StringArgument;
import com.nisovin.shopkeepers.commands.lib.commands.PlayerCommand;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.season.SeasonVisibility;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

class CommandSeasons extends PlayerCommand {

	private static final String ARGUMENT_NEW_SEASONS = "seasons";
	private static final String ARGUMENT_REMOVE = "-";
	private static final String ARGUMENT_QUERY = "?";

	CommandSeasons() {
		super("seasons");

		this.setPermission(ShopkeepersPlugin.ADMIN_PERMISSION);
		this.setDescription(Text.parse(
				"Tags the held shop item so it only appears in those seasons."
		));

		this.addArgument(new FirstOfArgument("seasonsArg", Arrays.asList(
				new LiteralArgument(ARGUMENT_QUERY)
						.orDefaultValue(ARGUMENT_QUERY),
				new LiteralArgument(ARGUMENT_REMOVE),
				new StringArgument(ARGUMENT_NEW_SEASONS, true)
		), true, true));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		Player player = (Player) input.getSender();
		ItemStack item = player.getInventory().getItemInMainHand();
		if (ItemUtils.isEmpty(item)) {
			TextUtils.sendMessage(player, Text.parse(
					"Hold the shop result item, then run /shopkeeper seasons WINTER"
			));
			return;
		}

		String newSeasons = context.getOrNull(ARGUMENT_NEW_SEASONS);
		boolean remove = context.has(ARGUMENT_REMOVE);
		String current = SeasonVisibility.readSeasons(item);
		if (current == null || current.isEmpty()) {
			current = "all";
		}

		if (remove) {
			SeasonVisibility.writeSeasons(item, "");
			player.getInventory().setItemInMainHand(item);
			TextUtils.sendMessage(player, Text.parse(
					"Item season tag cleared. This trade shows in every season."
			));
		} else if (newSeasons != null) {
			SeasonVisibility.writeSeasons(item, newSeasons);
			player.getInventory().setItemInMainHand(item);
			String stored = SeasonVisibility.readSeasons(item);
			if (stored == null || stored.isEmpty()) {
				stored = "all";
			}
			TextUtils.sendMessage(player, Text.parse(
					"Item only appears in shop during: {seasons}. Put it in the villager as the result."
			), "seasons", stored);
		} else {
			TextUtils.sendMessage(player, Text.parse("Item seasons: {seasons}."),
					"seasons", current
			);
		}
	}
}
