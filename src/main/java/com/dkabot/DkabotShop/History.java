package com.dkabot.DkabotShop;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.PagingList;
import com.avaje.ebean.Query;

public class History implements CommandExecutor {
	private DkabotShop plugin;
	public History(DkabotShop plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Sorry, console command usage is not yet supported!");
			return true;
		}
		if(cmd.getName().equalsIgnoreCase("sales")) {
			if(!sender.hasPermission("dkabotshop.sales")) {
				sender.sendMessage(ChatColor.RED + "You lack permission to do this.");
				return true;
			}
			if(args.length > 3) {
				sender.sendMessage(ChatColor.RED + "Too many arguments.");
				return true;
			}
			//Declare Variables
			Player player = (Player) sender;
			ItemStack material = null;
			Integer page = 0;
			List<DB_History> DBClass = null;
			PagingList<DB_History> DBPageList = null;
			Integer hyphenCount;
			String hyphens = "";
			Query<DB_History> query = plugin.getDatabase().find(DB_History.class).orderBy().desc("id");
			ExpressionList<?> eList = query.where();
			for(String arg : args) {
				if((arg.contains("p") || arg.contains("P")) && plugin.isInt(arg.replaceFirst("(?i)p", ""))) page = Integer.parseInt(arg.replaceFirst("(?i)p", "")) - 1;
				else if(plugin.getMaterial(arg, true, player) != null) material = plugin.getMaterial(arg, true, player);
			}
			if(page < 0) {
				sender.sendMessage(ChatColor.RED + "Invalid page number!");
				return true;
			}		
			if(material != null) {
				if(plugin.illegalItem(material)) {
					sender.sendMessage(ChatColor.RED + "Disallowed Item!");
					return true;
				}
				Integer itemID = material.getTypeId();
				Short durability = material.getDurability();
				eList = eList.eq("item", itemID.toString() + ":" + durability.toString());
			}
			DBPageList = query.findPagingList(8);
			DBClass = DBPageList.getPage(page).getList();
			if(DBClass.isEmpty()) {
				String message = "";
				if(page > 0) message = "Page " + (page + 1) + " contains no results. Try page 1";
				else if (material != null) message = "Nobody has sold any " + plugin.itemDB.rget(material.getTypeId(), material.getDurability()).toUpperCase() + " yet.";
				else message = "Nobody has sold anything yet.";
				sender.sendMessage(ChatColor.RED + message);
				return true;
			}
			//hyphen builder
			hyphenCount = ((40 - (" Page " + (page + 1) + " ").length()) / 2);
			for(Integer i = 0; i < hyphenCount;) {
				hyphens = hyphens + "-";
				i++;
			}
			//send that confirmation message of what's being looked for!
			if(material != null) sender.sendMessage(ChatColor.GREEN + "Items Sold, " + plugin.itemDB.rget(material.getTypeId(), material.getDurability()).toUpperCase() + " Only:");
			else sender.sendMessage(ChatColor.GREEN + "Items Sold, No Filer:");
			//send that info!
			sender.sendMessage(ChatColor.RED + hyphens + ChatColor.GRAY + " Page " + ChatColor.RED + (page + 1) + " " + hyphens);
			for(Integer i = 0; i < DBClass.size();) {
				DB_History DB = DBClass.get(i);
				String currencyName = "Error Getting Currency";
				if(DB.getCost() == 1) currencyName = plugin.economy.currencyNameSingular();
				else currencyName = plugin.economy.currencyNamePlural();
				sender.sendMessage(ChatColor.GOLD + DB.getBuyer() + ChatColor.BLUE + " bought " + ChatColor.GOLD +  DB.getAmount() + " " + plugin.itemDB.rget(DB.getItem()).toUpperCase() + ChatColor.BLUE + " for about " + ChatColor.GOLD + DB.getCost() + " " + currencyName + ChatColor.BLUE + " each.");
				i++;
			}
			if(DBPageList.getPage(page).hasNext()) sender.sendMessage(ChatColor.GREEN + "There is a next page in this list!");
			return true;
		}
		//If you made it to here, show me the Herobrine that helped you do it!
		return false;
	}

}
