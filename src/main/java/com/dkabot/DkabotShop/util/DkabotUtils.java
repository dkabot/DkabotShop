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
import static com.dkabot.DkabotShop.DkabotShop.getMaterial;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
}
