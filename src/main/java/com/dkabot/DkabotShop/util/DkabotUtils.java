/*
 * Copyright (C) 2013 Joshua Michael Hertlein <jmhertlein@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dkabot.DkabotShop.util;

import com.dkabot.DkabotShop.DkabotShop;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Joshua Michael Hertlein <jmhertlein@gmail.com>
 */
public class DkabotUtils {
    
    //checks if an item is on a blacklist. Boolean for now, but will become something else once a datavalue item blacklist is added
    public static boolean illegalItem(ItemStack material) {
        for (String materialString : DkabotShop.getInstance().getConfig().getStringList("Blacklist.Always")) {
            ItemStack blackMaterial = getMaterial(materialString, false, null, false);
            if (blackMaterial.getTypeId() == material.getTypeId() && blackMaterial.getDurability() == material.getDurability()) {
                return true;
            }
        }
        return false;
    }

    //function to give items, split into itemstacks based on item.getMaxStackSize()
    public static Integer giveItem(ItemStack item, Player player) {
        Integer fullItemStacks = item.getAmount() / item.getMaxStackSize();
        Integer fullItemStacksRemaining = fullItemStacks;
        Integer nonFullItemStack = item.getAmount() % item.getMaxStackSize();
        Integer amountNotReturned = 0;
        Integer notReturnedAsInt = 0;
        for (int i = 0; i < fullItemStacks;) {
            HashMap<Integer, ItemStack> notReturned = player.getInventory().addItem(new ItemStack(item.getType(), item.getMaxStackSize(), item.getDurability()));
            fullItemStacksRemaining--;
            if (notReturned.isEmpty()) {
                i++;
            } else {
                for (int j = 0; j < notReturned.size();) {
                    notReturnedAsInt = notReturnedAsInt + notReturned.get(j).getAmount();
                    j++;
                }
                break;
            }
        }
        if (notReturnedAsInt != 0) {
            notReturnedAsInt = notReturnedAsInt + nonFullItemStack;
        } else if (nonFullItemStack != 0) {
            HashMap<Integer, ItemStack> notReturned = player.getInventory().addItem(new ItemStack(item.getType(), nonFullItemStack, item.getDurability()));
            for (int i = 0; i < notReturned.size();) {
                notReturnedAsInt = notReturnedAsInt + notReturned.get(i).getAmount();
                i++;
            }
        }
        amountNotReturned = amountNotReturned + (fullItemStacksRemaining * item.getMaxStackSize()) + notReturnedAsInt;
        return amountNotReturned;
    }

    //broadcasts messages
    public static void broadcastMessage(String message) {
        FileConfiguration c = DkabotShop.getInstance().getConfig();
        //In case broadcasting is disabled, just exit now
        if (c.getBoolean("DisableBroadcasting")) {
            return;
        }
        //In case alternate broadcasting is enabled, send the message to every player
        if (c.getBoolean("AlternateBroadcasting")) {
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                player.sendMessage(message);
            }
        } //In case alternate broadcasting is disabled (default), make the server send the message
        else {
            Bukkit.getServer().broadcastMessage(message);
        }
    }

    //formats messages for broadcast
    public static String formatMessage(String messagePointer, String player, String item, Integer amount, Double cost, String currencyName) {
        if (messagePointer == null) {
            return "Failure to Generate Message";
        }
        String message = DkabotShop.getInstance().getConfig().getString("Messages." + messagePointer, null);
        if (message == null) {
            return "Failure to Generate Message";
        }

        Map<String, Object> replacements = new HashMap<String, Object>();
        replacements.put("[player]", player);
        replacements.put("[item]", item);
        replacements.put("[amount]", amount);
        replacements.put("[cost]", cost);
        replacements.put("[currency]", currencyName);

        for (String replace : replacements.keySet()) {
            String replacement;
            if (replacements.get(replace) == null) {
                replacement = "NULL";
            } else if (replacements.get(replace) instanceof String) {
                replacement = (String) replacements.get(replace);
            } else {
                replacement = replacements.get(replace).toString();
            }
            message = message.replace(replace, replacement);
        }

        for (ChatColor color : ChatColor.values()) {
            message = message.replace("&" + color.getChar(), color.toString());
        }

        return message;
    }
    //Same as bukkit's all, but needs an inventory argument and ignores the amount in the stack

    public static HashMap<Integer, ItemStack> all(Inventory inv, ItemStack stack) {
        HashMap<Integer, ItemStack> slots = new HashMap<Integer, ItemStack>();

        ItemStack[] inventory = inv.getContents();
        for (int i = 0; i < inventory.length; i++) {
            ItemStack item = inventory[i];
            if (item != null && item.getTypeId() == stack.getTypeId() && item.getDurability() == stack.getDurability()) {
                slots.put(i, item);
            }
        }
        return slots;
    }

    //Same as bukkit's contains, but needs an inventory argument and ignores the amount in the stack in favor of the amount argument
    public static boolean contains(Inventory inv, ItemStack stack, int amount) {
        int amt = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getTypeId() == stack.getTypeId() && item.getDurability() == stack.getDurability()) {
                amt += item.getAmount();
            }
        }
        return amt >= amount;
    }
    
        public static ItemStack getMaterial(String itemString, boolean allowHand, Player player) {
        return getMaterial(itemString, allowHand, player, true);
    }

    public static ItemStack getMaterial(String itemString, boolean allowHand, Player player, boolean useAlias) {
        Material material;
        String materialString = itemString.split(":")[0];
        Short dataValue = null;
        if (itemString.split(":").length > 1) {
            try {
                dataValue = Short.parseShort(itemString.split(":")[1]);
            } catch (NumberFormatException nfe) {
                dataValue = 0;
            }
        }
        if (useAlias) {
            //Aliases, always first
            for (String alias : DkabotShop.getInstance().getConfig().getStringList("ItemAlias")) {
                if (!materialString.equalsIgnoreCase(alias.split(",")[0])) {
                    continue;
                }
                String actualMaterial = alias.split(",")[1];
                //In case of an item ID
                if (isInt(actualMaterial)) {
                    material = Material.getMaterial(Integer.parseInt(actualMaterial));
                } //Must be a material name
                else {
                    material = Material.getMaterial(actualMaterial.toUpperCase());
                    if (material == null) {
                        ItemStack stack = DkabotShop.getInstance().getItemDB().get(actualMaterial);
                        if (stack == null) {
                            return stack;
                        }
                        material = stack.getType();
                        if (dataValue == null) {
                            dataValue = stack.getDurability();
                        }
                    }
                }
                //Should be an actual material
                if (dataValue == null) {
                    dataValue = 0;
                }
                if (material == Material.AIR) {
                    return null;
                }
                return new ItemStack(material, 1, dataValue);
            }
        }
        //"hand" as an item, can be overridden by an alias
        if (materialString.equalsIgnoreCase("hand")) {
            if (allowHand) {
                material = player.getItemInHand().getType();
                dataValue = player.getItemInHand().getDurability();
            } else {
                return null; //if hand is not allowed and it's not an alias, not bothering
            }
        } //if it's an item ID, that's all we need
        else if (isInt(materialString)) {
            material = Material.getMaterial(Integer.parseInt(materialString));
        } //if it's not, more effort.
        else {
            //try as an items.csv name
            ItemStack stack = DkabotShop.getInstance().getItemDB().get(materialString);
            if (stack != null) {
                material = stack.getType();
                if (dataValue == null) {
                    dataValue = stack.getDurability();
                }
            } //items.csv didn't work... try material name?
            else {
                material = Material.getMaterial(materialString.toUpperCase());
                if (material == null) {
                    return null;
                }
            }
        }
        if (dataValue == null) {
            dataValue = 0;
        }
        if (material == Material.AIR) {
            return null;
        }
        //could return null or not
        return new ItemStack(material, 1, dataValue);
    }

    public static Double parseMoneyAmount(String s) {
        try {
            Double d = Double.parseDouble(s);
            DecimalFormat twoDForm = new DecimalFormat("#.00");
            return Double.parseDouble(twoDForm.format(d));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double getMoneyPlayer(String s, Player player) {
        Double price = parseMoneyAmount(s);
        if (price == null) {
            player.sendMessage(ChatColor.RED + "Invalid cost amount!");
            return null;
        }
        if (price <= 0) {
            player.sendMessage(ChatColor.RED + "The cost cannot be 0 or negative!");
            return null;
        }
        if (!player.hasPermission("dkabotshopadmin.bypassMaxPrice")) {
            Double maxPrice = DkabotShop.getInstance().getConfig().getDouble("MaxPrice");
            if (maxPrice != -1 && price > maxPrice) {
                player.sendMessage(ChatColor.RED + "The cost cannot be above " + maxPrice.toString() + "!");
                return null;
            }
        }
        if (!player.hasPermission("dkabotshopadmin.bypassMinPrice")) {
            Double minPrice = DkabotShop.getInstance().getConfig().getDouble("MinPrice");
            if (minPrice != -1 && price < minPrice) {
                player.sendMessage(ChatColor.RED + "The cost cannot be below " + minPrice.toString() + "!");
                return null;
            }
        }
        return price;
    }
    
        public static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    public static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
    
    public static boolean isDecimal(double v) {
        return (Math.floor(v) != v);
        //If true, decimal, else whole number.
    }
}
