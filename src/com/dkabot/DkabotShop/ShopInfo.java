package com.dkabot.DkabotShop;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ShopInfo implements CommandExecutor {
	
	private DkabotShop plugin;
	
	public ShopInfo(DkabotShop plug) {
		plugin = plug;

	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		//Permission Check
		if(!sender.hasPermission("dkabotshop.shopinfo")) {
			sender.sendMessage(ChatColor.RED + "You lack permission to do this.");
			return true;
		}
		//More than one argument, tell player off
		if(args.length > 1) {
			sender.sendMessage(ChatColor.RED + "Too many arguments! Use " + ChatColor.GOLD + "/" + cmd.getName() + ChatColor.RED + " for more information.");
			return true;
		}
		//No arguments, main info screen
		if(args.length == 0) {
			mainHelpScreen(sender, cmd.getName());
			return true;
		}
		//Misc. Info screen
		if(equalsIgnoreSlash(args[0], "shopinfo") || equalsIgnoreSlash(args[0], "shop")) {
			shopInfoScreen(sender, cmd.getName());
			return true;
		}
		sender.sendMessage(ChatColor.RED + "Sorry, there is no command of that name. Use " + ChatColor.GOLD + "/" + cmd.getName() + ChatColor.RED + " for help.");
		return true;
	}
	
	//Checks for if something matches, ignoring case, with or without a preceding /
	private boolean equalsIgnoreSlash(String base, String test) {
		if(base.equalsIgnoreCase(test)) return true;
		if(("/" + base).equalsIgnoreCase(test)) return true;
		return false;
	}
	
	//Sends message if user has given permission, for convenience since the main info screen relies on it
	private void sendMessagePermission(CommandSender sender, String permission, String message) {
		if(sender.hasPermission(permission)) sender.sendMessage(message);
	}
	
	//Main info screen
	private void mainHelpScreen(CommandSender sender, String cmdName) {
		sender.sendMessage(ChatColor.GREEN + "DkabotShop Main Information");
		sendMessagePermission(sender, "dkabotshop.sell", ChatColor.BLUE + "To sell, use " + ChatColor.GOLD + "/sell <item> <amount> <price>");
		sendMessagePermission(sender, "dkabotshop.price", ChatColor.BLUE + "To change your selling price, use " + ChatColor.GOLD + "/price <item> <price>");
		sendMessagePermission(sender, "dkabotshop.cancel", ChatColor.BLUE + "To stop selling, use " + ChatColor.GOLD + "/cancel <item>");
		sendMessagePermission(sender, "dkabotshop.buy", ChatColor.BLUE + "To buy, use " + ChatColor.GOLD + "/buy <item> <amount>");
		sendMessagePermission(sender, "dkabotshop.stock", ChatColor.BLUE + "To see available items, use " + ChatColor.GOLD + "/stock");
		sendMessagePermission(sender, "dkabotshop.sales", ChatColor.BLUE + "To see previous sales, use " + ChatColor.GOLD + "/sales");
		sender.sendMessage(ChatColor.GREEN + "For more and misc. info, use " + ChatColor.GRAY + "/" + cmdName + " " + cmdName);
	}
	
	//Misc. info screen
	private void shopInfoScreen(CommandSender sender, String cmdName) {
		sender.sendMessage(ChatColor.BLUE + "For a list of commands, use " + ChatColor.GOLD + "/" + cmdName);
		sender.sendMessage(ChatColor.BLUE + "For elaborated info on a command, use " + ChatColor.GOLD + "/" + cmdName + " [commandname]");
		sender.sendMessage(ChatColor.BLUE + "* When specifying an item anywhere, you can use a name or item ID");
		sender.sendMessage(ChatColor.BLUE + "* Alternatively, you can use \"hand\" to use what you're holding as the item");
		sender.sendMessage(ChatColor.BLUE + "* You can specify a data value by separating the name and it with a " + ChatColor.GOLD + ":");
	}
	
	//Sell cmd info screen
	private void sellInfoScreen(CommandSender sender, String cmdName) {
		sender.sendMessage(ChatColor.GOLD + "/" + cmdName + ChatColor.BLUE + " - sell items on the market");
		sender.sendMessage(ChatColor.BLUE + "Usage: " + ChatColor.GOLD + "/" + cmdName + " ");
		
		sender.sendMessage(ChatColor.BLUE + "* When specifying amount, you can use \"all\" to specify all you have in your inventory.");		
	}
	
}
