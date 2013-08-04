package com.dkabot.DkabotShop.command;

import com.dkabot.DkabotShop.persistence.SaleEntity;
import com.dkabot.DkabotShop.DkabotShop;
import com.dkabot.DkabotShop.util.DkabotUtils;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SellerCommandExecutor implements CommandExecutor {

    private DkabotShop plugin;

    public SellerCommandExecutor(DkabotShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Sorry, console command usage is not yet supported!");
            return true;
        }
        //Player check, no console for these commands!
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can add or remove items from the market!");
            return true;
        }
        //Initializing it with an error... gets set for real below.
        String currencyName = "Error Getting Currency!";
        //Code for /sell command
        if (cmd.getName().equalsIgnoreCase("sell")) {
            //Permission Check
            if (!sender.hasPermission("dkabotshop.sell")) {
                sender.sendMessage(ChatColor.RED + "You lack permission to do this.");
                return true;
            }
            //Define Variables
            ItemStack material;
            Integer itemID;
            Short durability;
            Player player = (Player) sender;
            Double cost;
            Integer amount = 0;
            HashMap<Integer, ? extends ItemStack> instancesOfItem = null;
            //Check for input length
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Not enough arguments.");
                return true;
            }
            if (args.length > 3) {
                sender.sendMessage(ChatColor.RED + "Too many arguments.");
                return true;
            }
            if (!args[1].equalsIgnoreCase("all") && !plugin.isInt(args[1])) {
                sender.sendMessage(ChatColor.RED + "Amount to sell must be a number.");
                return true;
            }
            //Get and parse input for validity
            material = plugin.getMaterial(args[0], true, player);
            if (material == null) {
                sender.sendMessage(ChatColor.RED + "Invalid Item!");
                return true;
            }
            if (DkabotUtils.illegalItem(material)) {
                sender.sendMessage(ChatColor.RED + "Disallowed Item!");
                return true;
            }
            //Set ID and durability since the item IS valid
            itemID = material.getTypeId();
            durability = material.getDurability();
            //More logic here, to allow for a case of "all" argument
            instancesOfItem = DkabotUtils.all(player.getInventory(), material);
            if (args[1].equalsIgnoreCase("all")) {
                if (instancesOfItem.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "You must have the item you wish to sell");
                    sender.sendMessage(ChatColor.RED + "in your inventory!");
                    return true;
                }
                for (Integer key : instancesOfItem.keySet()) {
                    amount += instancesOfItem.get(key).getAmount();
                }
            } else {
                amount = Integer.parseInt(args[1]);
            }
            //Back to validating
            if (amount < 1) {
                sender.sendMessage(ChatColor.RED + "You can't sell none or negative of an item!");
                return true;
            }
            if (!DkabotUtils.contains(player.getInventory(), material, amount)) {
                sender.sendMessage(ChatColor.RED + "You must have the item you wish to sell");
                sender.sendMessage(ChatColor.RED + "in your inventory!");
                return true;
            }
            //Get info from DB
            SaleEntity DBClass = plugin.getDatabase().find(SaleEntity.class).where().eq("seller", sender.getName()).eq("item", itemID.toString() + ":" + durability.toString()).findUnique();
            //Checking that the player can choose not to manually set a cost or not.
            if (DBClass == null && args.length == 2) {
                sender.sendMessage(ChatColor.RED + "You do not have this item on the market.");
                sender.sendMessage(ChatColor.RED + "You must define a cost for this item.");
                return true;
            }
            //Manually set cost if the user chooses to do so
            if (args.length == 3) {
                cost = plugin.getMoneyPlayer(args[2], (Player) sender);
                //Check validity of price
                if (cost == null) {
                    return true;
                }
                if (DBClass != null && cost.equals(DBClass.getCost())) {
                    sender.sendMessage(ChatColor.RED + "The cost entered matches the current cost!");
                    cost = null;
                }
            } //So Eclipse won't say cost wasn't initialized
            else {
                cost = null;
            }
            //Make sure it's not too much to sell
            if (!sender.hasPermission("dkabotshopadmin.bypassMaxStock")) {
                Integer maxStock = plugin.getConfig().getInt("MaxStock");
                if (maxStock != -1) {
                    Boolean check = false;
                    if (DBClass != null) {
                        check = (DBClass.getAmount() + amount > maxStock);
                    } else {
                        check = (amount > maxStock);
                    }
                    if (check) {
                        sender.sendMessage(ChatColor.RED + "You can't stock your shop with more than " + maxStock + " of an item!");
                        return true;
                    }
                }
            }
            //Setting currency name, gotta get plurals right
            if (cost != null) {
                if (cost == 1) {
                    currencyName = plugin.getEconomy().currencyNameSingular();
                } else {
                    currencyName = plugin.getEconomy().currencyNamePlural();
                }
            }
            //Remove the item from the player's inventory
            HashMap<Integer, ItemStack> itemsNotRemoved = player.getInventory().removeItem(new ItemStack(material.getType(), amount, durability));
            if (!itemsNotRemoved.isEmpty()) {
                Integer amountNotRemoved = 0;
                Integer amountRemoved = 0;
                for (Integer key : itemsNotRemoved.keySet()) {
                    amountNotRemoved += itemsNotRemoved.get(key).getAmount();
                }
                amountRemoved = amount - amountNotRemoved;
                //Figure out how many there were in the inventory, and set the amount to that
                if (args[1].equalsIgnoreCase("all")) {
                    if (amountRemoved <= 0) {
                        sender.sendMessage(ChatColor.RED + "You must have the item you wish to sell");
                        sender.sendMessage(ChatColor.RED + "in your inventory!");
                        return true;
                    }
                    amount = amountRemoved;
                } //You must be doing something wrong~
                else {
                    if (amountRemoved > 0) {
                        player.getInventory().addItem(new ItemStack(material.getType(), amountRemoved, durability));
                    }
                    sender.sendMessage(ChatColor.RED + "You must have the item you wish to sell");
                    sender.sendMessage(ChatColor.RED + "in your inventory!");
                    return true;
                }
            }

            //Item isn't in shop, add it
            if (DBClass == null) {
                DBClass = new SaleEntity();
                DBClass.setSeller(sender.getName());
                DBClass.setItem(itemID.toString() + ":" + durability.toString());
                DBClass.setAmount(amount);
                DBClass.setCost(cost);
                plugin.getDatabase().save(DBClass);
                DkabotUtils.broadcastMessage(DkabotUtils.formatMessage("NewlySelling", sender.getName(), plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase(), amount, cost, currencyName));
            } //Item is in shop, modify the entry
            else {
                //Set amount in the DB
                DBClass.setAmount(DBClass.getAmount() + amount);
                //Cost not changed, just broadcast
                if (cost == null) {
                    DkabotUtils.broadcastMessage(DkabotUtils.formatMessage("Added", sender.getName(), plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase(), amount, null, null));
                } else {
                    //Cost changed, set cost and broadcast varied message.
                    DBClass.setCost(cost);
                    DkabotUtils.broadcastMessage(DkabotUtils.formatMessage("AddedPriceChange", sender.getName(), plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase(), amount, cost, currencyName));
                }
                //Save new info to the DB
                plugin.getDatabase().save(DBClass);
            }
            //If this is reached, we have success!
            return true;
        }

        //Code for /cancel command
        if (cmd.getName().equalsIgnoreCase("cancel")) {
            //Permission Check
            if (!sender.hasPermission("dkabotshop.cancel")) {
                sender.sendMessage(ChatColor.RED + "You lack permission to do this.");
                return true;
            }
            //Check for input length
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Too few arguments.");
                return true;
            }
            if (args.length == 3) {
                sender.sendMessage(ChatColor.RED + "Too many arguments.");
                return true;
            }
            //Define variables, surprisingly more than /sell
            Player player = (Player) sender;
            ItemStack material;
            ItemStack materialToReturn;
            Integer itemID;
            Short durability;
            HashMap<Integer, ItemStack> materialNotReturned;
            Integer amountNotReturned = 0;
            //Get and check validity of input
            material = plugin.getMaterial(args[0], true, player);
            if (material == null) {
                sender.sendMessage(ChatColor.RED + "Invalid Item!");
                return true;
            }
            if (DkabotUtils.illegalItem(material)) {
                sender.sendMessage(ChatColor.RED + "Disallowed Item!");
                return true;
            }
            //Set ID and durability
            itemID = material.getTypeId();
            durability = material.getDurability();
            //Get info from DB
            SaleEntity DBClass = plugin.getDatabase().find(SaleEntity.class).where().eq("seller", sender.getName()).eq("item", itemID.toString() + ":" + durability.toString()).findUnique();
            //MOAR validity check!
            if (DBClass == null) {
                sender.sendMessage(ChatColor.RED + "You aren't currently selling this!");
                return true;
            }
            //In case of /cancel item amount
            if (args.length == 2 && !args[1].equalsIgnoreCase("all")) {
                //Set the amount to return, check if it's a valid Integer
                Integer amountToReturn;
                if (!plugin.isInt(args[1])) {
                    sender.sendMessage(ChatColor.RED + "Amount to return must be a number.");
                    return true;
                } else {
                    amountToReturn = Integer.parseInt(args[1]);
                }
                //Check if the amount to return is not 0 or negative
                if (amountToReturn <= 0) {
                    sender.sendMessage(ChatColor.RED + "Amount to return cannot be 0 or negative!");
                    return true;
                }
                //Sets amountNotReturned in case the player has enough space
                amountNotReturned = DBClass.getAmount() - amountToReturn;
                if (DBClass.getAmount() < amountToReturn) {
                    sender.sendMessage(ChatColor.RED + "The shop does not have enough stock for that!");
                    return true;
                }
                materialToReturn = new ItemStack(material.getType(), amountToReturn, durability);
            } //In case of just /cancel item... much simpler.
            else {
                materialToReturn = new ItemStack(material.getType(), DBClass.getAmount(), durability);
            }
            //Give the player the items and capture amount that could not be given
            materialNotReturned = player.getInventory().addItem(materialToReturn);
            //Calculate amount to put back in the shop
            if (!materialNotReturned.isEmpty()) {
                //Declare variables
                Integer amountInShop = DBClass.getAmount();
                Integer amountReturned;
                //Reset amountNotReturned, as it will obviously be different.
                amountNotReturned = 0;
                //Calculate amount not returned from a hashmap of ItemStacks
                for (Integer i = 0; i < materialNotReturned.size();) {
                    amountNotReturned = amountNotReturned + materialNotReturned.get(i).getAmount();
                    i++;
                }
                //In case NONE of it was returned.
                if (amountInShop == amountNotReturned) {
                    sender.sendMessage(ChatColor.RED + "You lack enough space for any of this item!");
                    return true;
                }
                amountReturned = amountInShop - amountNotReturned;
                //Set info in DB and save
                DBClass.setAmount(amountNotReturned);
                plugin.getDatabase().save(DBClass);
                //Inform the player of their lack of space
                sender.sendMessage(ChatColor.GREEN + "You can only hold " + amountReturned + " of this, so you got that much back.");
                //Tell the whole server what just happened
                DkabotUtils.broadcastMessage(DkabotUtils.formatMessage("RemovedSome", sender.getName(), plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase(), amountNotReturned, null, null));

            } else {
                if (args.length == 2 && amountNotReturned != 0) {
                    //Again, in case of /cancel item amount. Saves amount in DB and tells the server what happened.
                    DBClass.setAmount(amountNotReturned);
                    plugin.getDatabase().save(DBClass);
                    DkabotUtils.broadcastMessage(DkabotUtils.formatMessage("RemovedSome", sender.getName(), plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase(), amountNotReturned, null, null));
                } else {
                    //In case the supply was emptied, tell the whole server and delete the DB entry.
                    //If /cancel item amount depletes the supply, this is called instead.
                    DkabotUtils.broadcastMessage(DkabotUtils.formatMessage("RemovedAll", sender.getName(), plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase(), null, null, null));
                    plugin.getDatabase().delete(DBClass);
                }
            }
            //If you reach here, success!
            return true;
        }

        //Code for /price command
        if (cmd.getName().equalsIgnoreCase("price")) {
            //Permission Check
            if (!sender.hasPermission("dkabotshop.price")) {
                sender.sendMessage(ChatColor.RED + "You lack permission to do this.");
                return true;
            }
            //Check input length
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Too few arguments.");
                return true;
            }
            if (args.length > 2) {
                sender.sendMessage(ChatColor.RED + "Too many arguments.");
                return true;
            }
            //Declare and initialize variables
            ItemStack material = plugin.getMaterial(args[0], true, (Player) sender);
            Double cost = plugin.getMoneyPlayer(args[1], (Player) sender);
            Integer itemID;
            Short durability;
            //Check for an invalid price
            if (cost == null) {
                return true;
            }
            //Check for a valid material;
            if (material == null) {
                sender.sendMessage(ChatColor.RED + "Invalid Item!");
                return true;
            }
            if (DkabotUtils.illegalItem(material)) {
                sender.sendMessage(ChatColor.RED + "Disallowed Item!");
                return true;
            }
            //Set item ID and durability
            itemID = material.getTypeId();
            durability = material.getDurability();
            //Get info from DB
            SaleEntity DBClass = plugin.getDatabase().find(SaleEntity.class).where().eq("seller", sender.getName()).eq("item", itemID.toString() + ":" + durability.toString()).findUnique();
            //Check if they are actually selling the item
            if (DBClass == null) {
                sender.sendMessage(ChatColor.RED + "You aren't currently selling this!");
                return true;
            }
            //Check if the two cost values match
            if (cost.equals(DBClass.getCost())) {
                sender.sendMessage(ChatColor.RED + "The current cost is already set to that!");
                return true;
            }
            //Set the new cost and save the database
            DBClass.setCost(cost);
            plugin.getDatabase().save(DBClass);
            //Set currencyName
            if (cost == 1) {
                currencyName = plugin.getEconomy().currencyNameSingular();
            } else {
                currencyName = plugin.getEconomy().currencyNamePlural();
            }
            //Tell the whole server what just happened
            DkabotUtils.broadcastMessage(DkabotUtils.formatMessage("PriceChange", sender.getName(), plugin.getItemDB().rget(material.getTypeId(), material.getDurability()).toUpperCase(), null, cost, currencyName));
            //If you reach here, success!
            return true;
        }
        //If you reach here, who are you, what is your black magic, and can I use it?
        return false;
    }
}
