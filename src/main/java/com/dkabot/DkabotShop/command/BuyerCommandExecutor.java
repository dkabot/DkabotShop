package com.dkabot.DkabotShop.command;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.PagingList;
import com.avaje.ebean.Query;
import com.dkabot.DkabotShop.persistence.SaleEntity;
import com.dkabot.DkabotShop.persistence.HistoryEntity;
import com.dkabot.DkabotShop.DkabotShop;
import com.dkabot.DkabotShop.util.DkabotUtils;

public class BuyerCommandExecutor implements CommandExecutor {

    private DkabotShop plugin;

    public BuyerCommandExecutor(DkabotShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Sorry, console command usage is not yet supported!");
            return true;
        }
        //Code for /buy
        if (cmd.getName().equalsIgnoreCase("buy")) {
            return handleBuyCommand(sender, cmd, label, args);
        }

        //Code for /stock
        if (cmd.getName().equalsIgnoreCase("stock")) {
            return handleStockCommand(sender, cmd, label, args);
        }

        return false;
    }

    private boolean handleBuyCommand(CommandSender sender, Command cmd, String label, String[] args) {
        //Player check
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can buy items!");
            return true;
        }
        //Permission Check
        if (!sender.hasPermission("dkabotshop.buy")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to do this.");
            return true;
        }
        //Argument length validation
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Too few arguments.");
            return true;
        }
        if (args.length > 3) {
            sender.sendMessage(ChatColor.RED + "Too many arguments.");
            return true;
        }
        //Declare variables
        ItemStack material;
        Integer itemID;
        Short durability;
        Integer amount = null;
        Integer lastSellerAmount = null;
        Integer amountRemaining = null;
        Integer prevAmountRemaining = null;
        Double totalCost = 0.00;
        Double maxPrice = null;
        Integer amountGiven = 0;
        Integer sellers;
        Player player = (Player) sender;
        String currencyName = "Error Getting Currency";
        String messageType = null;
        
        //Item validation
        material = DkabotUtils.getMaterial(args[0], true, player);
        if (material == null) {
            sender.sendMessage(ChatColor.RED + "Invalid item!");
            return true;
        }
        if (DkabotUtils.illegalItem(material)) {
            sender.sendMessage(ChatColor.RED + "Disallowed item!");
            return true;
        }
        
        //Amount setting and validation
        if (!DkabotUtils.isInt(args[1])) {
            sender.sendMessage(ChatColor.RED + "Amount to buy must be a number.");
            return true;
        }
        amount = Integer.parseInt(args[1]);
        
        amountRemaining = amount;
        itemID = material.getTypeId();
        durability = material.getDurability();
        
        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "Amount to buy cannot be 0 or negative.");
            return true;
        }
        
        //Set max cost if specified
        if (args.length == 3) {
            maxPrice = DkabotUtils.parseMoneyAmount(args[2]);
            //Validate max cost
            if (maxPrice == null) {
                sender.sendMessage(ChatColor.RED + "Invalid maximum purchase price!");
                return true;
            }
            if (maxPrice <= 0) {
                sender.sendMessage(ChatColor.RED + "Max purchase price cannot be 0 or negative!");
                return true;
            }
        }
        //Get all instances of this item for sale, save for the ones sold by the buyer
        Query<SaleEntity> query = plugin.getDatabase().find(SaleEntity.class).orderBy().asc("cost");
        ExpressionList<?> eList = query.where().eq("item", itemID.toString() + ":" + durability.toString()).ne("seller", sender.getName());
        
        //Max price check, if applicable
        if (maxPrice != null) {
            eList = eList.le("cost", maxPrice);
        }
        
        List<SaleEntity> saleEntities = query.findList();
        //Loop through all the entries
        for (sellers = 0; sellers < saleEntities.size();) {
            //Get the specific entry
            SaleEntity tmpDB = saleEntities.get(sellers);
            //Previous amount remaining to be bought, for calculations
            prevAmountRemaining = amountRemaining;
            //Set amount remaining
            amountRemaining = amountRemaining - tmpDB.getAmount();
            //If the amount to buy has been reached
            if (amountRemaining <= 0) {
                //Set last seller amount
                lastSellerAmount = prevAmountRemaining;
                //Add to the total cost for the buyer
                totalCost = totalCost + (prevAmountRemaining * tmpDB.getCost());
                break;
            } else if (sellers + 1 == saleEntities.size()) {
                //Set last seller amount
                lastSellerAmount = tmpDB.getAmount();
                //Add to the total cost for the buyer
                totalCost = totalCost + (tmpDB.getAmount() * tmpDB.getCost());
                break;
            } else {
                //Add to the total cost for the buyer
                totalCost = totalCost + (tmpDB.getAmount() * tmpDB.getCost());
            }
            //Continue through the loop
            sellers++;
        }
        //Check if not even one item fit the criteria
        if (amountRemaining == amount) {
            sender.sendMessage(ChatColor.RED + "Nobody is selling this!");
            return true;
        }
        //Check if not enough items fit the criteria
        if (amountRemaining > 0) {
            if (plugin.getConfig().getBoolean("AlwaysBuyAvailable")) {
                sender.sendMessage(ChatColor.GOLD + "There isn't " + amount.toString() + " for sale, only " + (amount - amountRemaining) + ". Attempting to buy that much.");
                //The amount variable is used by some output messages
                amount = amount - amountRemaining;
            } else {
                sender.sendMessage(ChatColor.RED + "There isn't enough for sale!");
                return true;
            }
        }
        //Check if the buyer has enough funds
        if (plugin.getEconomy().getBalance(sender.getName()) < totalCost) {
            Double amountNeeded = totalCost - plugin.getEconomy().getBalance(sender.getName());
            if (amountNeeded == 1) {
                currencyName = plugin.getEconomy().currencyNameSingular();
            } else {
                currencyName = plugin.getEconomy().currencyNamePlural();
            }
            sender.sendMessage(ChatColor.RED + "You lack enough funds, you need " + ChatColor.YELLOW + amountNeeded + ChatColor.RED + " more " + currencyName + "!");
            return true;
        }
        //Finally, the transaction actually takes place!
        //Give the player items
        for (Integer i = 0; i <= sellers;) {
            if (i < sellers) {
                SaleEntity tmpDB = saleEntities.get(i);
                amountGiven = amountGiven + tmpDB.getAmount();
                Integer amountNotReturned = DkabotUtils.giveItem(new ItemStack(material.getType(), tmpDB.getAmount(), durability), player);
                if (amountNotReturned != 0) {
                    sender.sendMessage(ChatColor.RED + "You lack enough space for this!");
                    player.getInventory().removeItem(new ItemStack(material.getType(), amountGiven - amountNotReturned, durability));
                    return true;
                }
            } else {
                amountGiven = amountGiven + lastSellerAmount;
                Integer amountNotReturned = DkabotUtils.giveItem(new ItemStack(material.getType(), lastSellerAmount, durability), player);
                if (amountNotReturned != 0) {
                    sender.sendMessage(ChatColor.RED + "You lack enough space for this!");
                    player.getInventory().removeItem(new ItemStack(material.getType(), amountGiven - amountNotReturned, durability));
                    return true;
                }
            }
            i++;
        }
        //Take funds from the player
        plugin.getEconomy().withdrawPlayer(sender.getName(), totalCost);
        for (Integer i = 0; i < sellers;) {
            SaleEntity tmpDB = saleEntities.get(i);
            //For any and all sellers sold out, give them money and remove their DB entry
            plugin.getEconomy().depositPlayer(tmpDB.getSeller(), tmpDB.getAmount() * tmpDB.getCost());
            Player seller = Bukkit.getServer().getPlayer(tmpDB.getSeller());
            if (seller != null) {
                seller.sendMessage(DkabotUtils.formatMessage("ShopBoughtAll", sender.getName(), plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase(), null, null, null));
            }
            plugin.getDatabase().delete(tmpDB);
            i++;
        }
        //Set shop amount for final seller and give them money
        SaleEntity finalSellerDB = saleEntities.get(sellers);
        if (finalSellerDB.getAmount() - lastSellerAmount <= 0) {
            messageType = "ShopBoughtAll";
            plugin.getDatabase().delete(finalSellerDB);
        } else {
            messageType = "ShopBoughtSome";
            finalSellerDB.setAmount(finalSellerDB.getAmount() - lastSellerAmount);
        }
        plugin.getEconomy().depositPlayer(finalSellerDB.getSeller(), lastSellerAmount * finalSellerDB.getCost());
        Player finalSeller = Bukkit.getServer().getPlayer(finalSellerDB.getSeller());
        if (finalSeller != null) {
            finalSeller.sendMessage(DkabotUtils.formatMessage(messageType, sender.getName(), plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase(), lastSellerAmount, null, null));
        }
        plugin.getDatabase().save(saleEntities);

        logTransaction(amount, sender, itemID, durability, totalCost);
        
        if (totalCost == 1) {
            currencyName = plugin.getEconomy().currencyNameSingular();
        } else {
            currencyName = plugin.getEconomy().currencyNamePlural();
        }
        sender.sendMessage(ChatColor.GREEN + "Successfully bought " + amount + " " + plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase() + ". Total cost: " + totalCost + " " + currencyName);
        //If you get here, success!
        return true;
    }

    private boolean handleStockCommand(CommandSender sender, Command cmd, String label, String[] args) {
        //Permission Check
        if (!sender.hasPermission("dkabotshop.stock")) {
            sender.sendMessage(ChatColor.RED + "You lack permission to do this.");
            return true;
        }
        //Check input length
        if (args.length > 3) {
            sender.sendMessage(ChatColor.RED + "Too many arguments.");
            return true;
        }
        //Declare Variables
        Player player = (Player) sender;
        String seller = "";
        PagingList<SaleEntity> DBPageList = null;
        ItemStack material = null;
        List<SaleEntity> DBClass = null;
        Integer hyphenCount;
        Integer page = 0;
        String hyphens = "";
        Query<SaleEntity> query = plugin.getDatabase().find(SaleEntity.class).orderBy().asc("cost");
        ExpressionList<?> eList = query.where();
        for (String arg : args) {
            if ((arg.contains("p") || arg.contains("P")) && DkabotUtils.isInt(arg.replaceFirst("(?i)p", ""))) {
                page = Integer.parseInt(arg.replaceFirst("(?i)p", "")) - 1;
            } else if (DkabotUtils.getMaterial(arg, true, player) != null) {
                material = DkabotUtils.getMaterial(arg, true, player);
            } else {
                seller = arg;
            }
        }
        if (material != null) {
            if (DkabotUtils.illegalItem(material)) {
                sender.sendMessage(ChatColor.RED + "Disallowed Item!");
                return true;
            }
            Integer itemID = material.getTypeId();
            Short durability = material.getDurability();
            eList = eList.eq("item", itemID.toString() + ":" + durability.toString());
        }
        if (seller != "") {
            eList = eList.ieq("seller", seller);
        }
        DBPageList = query.findPagingList(8);
        //Page number validation
        if (page < 0) {
            sender.sendMessage(ChatColor.RED + "Invalid page number!");
            return true;
        }
        //Get the page worth of info
        DBClass = DBPageList.getPage(page).getList();

        if (DBClass.isEmpty()) {
            String message = "";
            if (page > 0) {
                message = "Page " + (page + 1) + " contains no results. Try page 1";
            } else if (material != null && seller != "") {
                message = seller + " is not selling any " + plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase();
            } else if (material != null) {
                message = "Nobody is selling any " + plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase();
            } else if (seller != "") {
                message = seller + " is not selling anything";
            } else {
                message = "Nobody is selling anything";
            }
            sender.sendMessage(ChatColor.RED + message);
            return true;
        }

        hyphenCount = ((40 - (" Page " + (page + 1) + " ").length()) / 2);
        for (Integer i = 0; i < hyphenCount;) {
            hyphens = hyphens + "-";
            i++;
        }
        if (seller != "" && material != null) {
            sender.sendMessage(ChatColor.GREEN + "Items For Sale, " + plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase() + " Sold By " + seller + " Only:");
        } else if (seller != "") {
            sender.sendMessage(ChatColor.GREEN + "Items For Sale, " + seller + " Only:");
        } else if (material != null) {
            sender.sendMessage(ChatColor.GREEN + "Items For Sale, " + plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase() + " Only:");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Items For Sale, No Filter:");
        }
        sender.sendMessage(ChatColor.RED + hyphens + ChatColor.GRAY + " Page " + ChatColor.RED + (page + 1) + " " + hyphens);
        for (Integer i = 0; i < DBClass.size();) {
            SaleEntity DB = DBClass.get(i);
            String currencyName;
            if (DB.getCost() == 1) {
                currencyName = plugin.getEconomy().currencyNameSingular();
            } else {
                currencyName = plugin.getEconomy().currencyNamePlural();
            }
            sender.sendMessage(ChatColor.GOLD + DB.getSeller() + ChatColor.BLUE + ": " + ChatColor.GOLD + DB.getAmount() + " " + plugin.getItemDB().rget(DB.getItem()).toUpperCase() + ChatColor.BLUE + " for " + ChatColor.GOLD + DB.getCost() + " " + currencyName + ChatColor.BLUE + " each.");
            i++;
        }
        if (DBPageList.getPage(page).hasNext()) {
            sender.sendMessage(ChatColor.GREEN + "There is a next page in this list!");
        }
        return true;
    }

    private void logTransaction(int amount, CommandSender sender, Integer itemID, Short durability, Double totalCost) {
        HistoryEntity transactionLog = new HistoryEntity();
        transactionLog.setAmount(amount);
        transactionLog.setBuyer(sender.getName());
        transactionLog.setItem(itemID.toString() + ":" + durability.toString());
        transactionLog.setTotalCost(totalCost);
        transactionLog.setCost(totalCost / amount);
        plugin.getDatabase().save(transactionLog);
    }
}
