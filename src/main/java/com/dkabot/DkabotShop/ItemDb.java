//Code taken and modified from Essentials, as are ManagedFile.java and items.csv, though items.csv is not marked as such.
package com.dkabot.DkabotShop;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class ItemDb
{
	private final transient DkabotShop plugin;

	public ItemDb(final DkabotShop plugin)
	{
		this.plugin = plugin;
		file = new ManagedFile("items.csv", this.plugin);
	}
	private final transient Map<String, Long> items = new HashMap<String, Long>();
	private final transient Map<Long, String> rItems = new HashMap<Long, String>();
	private final transient ManagedFile file;
	private static final Pattern SPLIT = Pattern.compile("[^a-zA-Z0-9]");

	public void onReload()
	{
		final List<String> lines = file.getLines();

		if (lines.isEmpty())
		{
			return;
		}

		items.clear();

		for (String line : lines)
		{
			line = line.trim();
			if (line.length() > 0 && line.charAt(0) == '#')
			{
				continue;
			}

			final String[] parts = SPLIT.split(line);
			if (parts.length < 2)
			{
				continue;
			}

			final long numeric = Integer.parseInt(parts[1]);

			final long durability = parts.length > 2 && !(parts[2].length() == 1 && parts[2].charAt(0) == '0') ? Short.parseShort(parts[2]) : 0;
			items.put(parts[0].toLowerCase(Locale.ENGLISH), numeric | (durability << 32));
			if(!rItems.containsKey(numeric | (durability << 32)))rItems.put(numeric | (durability << 32), parts[0].toLowerCase(Locale.ENGLISH));
		}
	}

	public ItemStack get(final String id, final Player user)
	{
		final ItemStack stack = get(id.toLowerCase(Locale.ENGLISH));
		if(stack == null) return stack;
		int maxSize = stack.getMaxStackSize();
		if(maxSize < 0) stack.setAmount(1);
		else stack.setAmount(maxSize);
		return stack;
	}

	public ItemStack get(final String id, final int quantity)
	{
		final ItemStack retval = get(id.toLowerCase(Locale.ENGLISH));
		if(retval == null) return retval;
		retval.setAmount(quantity);
		return retval;
	}

	public ItemStack get(final String id)
	{
		int itemid = 0;
		String itemname = null;
		short metaData = 0;
		if (id.matches("^\\d+[:+',;.]\\d+$"))
		{
			itemid = Integer.parseInt(id.split("[:+',;.]")[0]);
			metaData = Short.parseShort(id.split("[:+',;.]")[1]);
		}
		else if (id.matches("^\\d+$"))
		{
			itemid = Integer.parseInt(id);
		}
		else if (id.matches("^[^:+',;.]+[:+',;.]\\d+$"))
		{
			itemname = id.split("[:+',;.]")[0].toLowerCase(Locale.ENGLISH);
			metaData = Short.parseShort(id.split("[:+',;.]")[1]);
		}
		else
		{
			itemname = id.toLowerCase(Locale.ENGLISH);
		}

		if (itemname != null)
		{
			if (items.containsKey(itemname))
			{
				long item = items.get(itemname);
				itemid = (int)(item & 0xffffffffL);
				if (metaData == 0)
				{
					metaData = (short)((item >> 32) & 0xffffL);
				}
			}
			else if (Material.matchMaterial(itemname) != null)
			{
				itemid = Material.matchMaterial(itemname).getId();
				metaData = 0;
			}
			else
			{
				return null;
			}
		}

		final Material mat = Material.getMaterial(itemid);
		if (mat == null)
		{
			return null;
		}
		int stackSize;
		if(mat.getMaxStackSize() == -1) stackSize = 1;
		else stackSize = mat.getMaxStackSize();
		final ItemStack retval = new ItemStack(mat, stackSize, metaData);
		return retval;
	}
	
	public String rget(final Integer id, final Short durability)
	{
		long dura = durability;
		String res = rItems.get(id | (dura << 32));
		if(res != null) return res;
		Material mat = Material.getMaterial(id);
		if(mat != null) return mat.toString() + ":" + durability.toString();
		return id.toString() + ":" + durability.toString();
	}
	
	public String rget(final String str) {
		if(str.split(":").length < 2) return null;
		try {
			Integer id = Integer.parseInt(str.split(":")[0]);
			Short dura = Short.parseShort(str.split(":")[1]);
			return rget(id, dura);
		}
		catch(Exception e) {
			return null;
		}
	}
}