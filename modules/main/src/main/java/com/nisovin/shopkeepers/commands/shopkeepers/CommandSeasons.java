package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperArgument;
import com.nisovin.shopkeepers.commands.arguments.ShopkeeperFilter;
import com.nisovin.shopkeepers.commands.arguments.TargetShopkeeperFallback;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.StringArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

class CommandSeasons extends Command {

	private static final String ARGUMENT_SHOPKEEPER = "shopkeeper";
	private static final String ARGUMENT_NEW_SEASONS = "seasons";
	private static final String ARGUMENT_REMOVE = "-";
	private static final String ARGUMENT_QUERY = "?";

	CommandSeasons() {
		super("seasons");

		this.setPermission(ShopkeepersPlugin.ADMIN_PERMISSION);
		this.setDescription(Text.parse("Sets which seasons this shop is visible in."));

		this.addArgument(new TargetShopkeeperFallback(
				new ShopkeeperArgument(ARGUMENT_SHOPKEEPER,
						ShopkeeperFilter.ADMIN
								.and(ShopkeeperFilter.withAccess(DefaultUITypes.EDITOR()))),
				TargetShopkeeperFilter.ADMIN
		));
		this.addArgument(new FirstOfArgument("seasonsArg", Arrays.asList(
				new LiteralArgument(ARGUMENT_QUERY)
						.orDefaultValue(ARGUMENT_QUERY),
				new LiteralArgument(ARGUMENT_REMOVE),
				new StringArgument(ARGUMENT_NEW_SEASONS)
		), true, true));
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		AbstractShopkeeper shopkeeper = context.get(ARGUMENT_SHOPKEEPER);
		if (!shopkeeper.canEdit(sender, false)) {
			return;
		}

		String newSeasons = context.getOrNull(ARGUMENT_NEW_SEASONS);
		boolean remove = context.has(ARGUMENT_REMOVE);
		String current = shopkeeper.getSeasons();
		if (current.isEmpty()) {
			current = "all";
		}

		if (remove) {
			shopkeeper.setSeasons("");
			TextUtils.sendMessage(sender, Text.parse("Shop seasons cleared (visible always)."));
		} else if (newSeasons != null) {
			shopkeeper.setSeasons(newSeasons);
			String stored = shopkeeper.getSeasons();
			if (stored.isEmpty()) {
				stored = "all";
			}
			TextUtils.sendMessage(sender, Text.parse("Shop seasons set to {seasons}."),
					"seasons", stored
			);
		} else {
			TextUtils.sendMessage(sender, Text.parse("Shop seasons: {seasons}."),
					"seasons", current
			);
			return;
		}

		shopkeeper.save();
		SKShopkeepersPlugin.getInstance().getShopkeeperRegistry()
				.getChunkActivator()
				.refreshSeasonVisibility();
	}
}
