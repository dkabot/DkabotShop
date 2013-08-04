//Code taken and modified from Essentials, as are ItemDb.java and items.csv, though items.csv is not marked as such.
package com.dkabot.DkabotShop;

import java.io.*;
import java.math.BigInteger;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import org.bukkit.Bukkit;


public class ManagedFile
{
	private final static int BUFFERSIZE = 1024 * 8;
	private final transient File file;

	public ManagedFile(final String filename, final DkabotShop plugin)
	{
		file = new File(plugin.getDataFolder(), filename);

		if (file.exists())
		{
			try
			{
				Scanner scan = new Scanner(file);
				String line1 = null;
				if(scan.hasNextLine()) line1 = scan.nextLine();
				scan.close();
				Boolean fileOK = false;
				String itemVersionString = null;
				String pluginVersionString = plugin.getDescription().getVersion().trim();
				Double itemVersion = null;
				Double pluginVersion = null;
				if(line1 != null) itemVersionString = line1.split("#version: ")[1].trim();
				if(itemVersionString != null && plugin.isDouble(itemVersionString)) itemVersion = Double.parseDouble(itemVersionString);
				if(itemVersion != null && itemVersion == -1) {
					fileOK = true;
					plugin.log.info("[DkabotShop] items.csv version set to -1, version check SKIPPED");
				}
				else {
					if(itemVersion != null && plugin.isDouble(pluginVersionString)) pluginVersion = Double.parseDouble(pluginVersionString);
					if(pluginVersion != null) {
						if(pluginVersion.equals(itemVersion)) {
							fileOK = true;
							plugin.log.info("[DkabotShop] items.csv version matches plugin version OK");
						}
						else if (pluginVersion > itemVersion) {
							//fileOK is already false
							plugin.log.info("[DkabotShop] items.csv version below plugin version UPDATING AND REPLACING");
						}
						else {
							fileOK = true;
							plugin.log.info("[DkabotShop] items.csv version above plugin version. NOT OK BUT NOT REPLACING");
							plugin.log.info("[DkabotShop] If you wish to disable overwrite set version to -1");
						}
					}
				}
				if(!fileOK) plugin.log.info("[DkabotShop] items.csv being rewritten - either updating or the version check failed");
				if (!fileOK && !file.delete())
				{
					throw new IOException("Could not delete file " + file.toString());
				}
			}
			catch (IOException ex)
			{
				Bukkit.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
			}
		}

		if (!file.exists())
		{
			try
			{
				copyResourceAscii("/" + filename, file);
			}
			catch (IOException ex)
			{
				Bukkit.getLogger().log(Level.SEVERE, "File " + file.getName() + " is not loaded!", ex);
			}
		}
	}

	public static void copyResourceAscii(final String resourceName, final File file) throws IOException
	{
		final InputStreamReader reader = new InputStreamReader(ManagedFile.class.getResourceAsStream(resourceName));
		try
		{
			final MessageDigest digest = getDigest();
			final DigestOutputStream digestStream = new DigestOutputStream(new FileOutputStream(file), digest);
			try
			{
				final OutputStreamWriter writer = new OutputStreamWriter(digestStream);
				try
				{
					final char[] buffer = new char[BUFFERSIZE];
					do
					{
						final int length = reader.read(buffer);
						if (length >= 0)
						{
							writer.write(buffer, 0, length);
						}
						else
						{
							break;
						}
					}
					while (true);
					writer.write("\n");
					writer.flush();
					final BigInteger hashInt = new BigInteger(1, digest.digest());
					digestStream.on(false);
					digestStream.write('#');
					digestStream.write(hashInt.toString(16).getBytes());
				}
				finally
				{
					writer.close();
				}
			}
			finally
			{
				digestStream.close();
			}
		}
		finally
		{
			reader.close();
		}
	}

	public static MessageDigest getDigest() throws IOException
	{
		try
		{
			return MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException ex)
		{
			throw new IOException(ex);
		}
	}

	public List<String> getLines()
	{
		try
		{
			final BufferedReader reader = new BufferedReader(new FileReader(file));
			try
			{
				final List<String> lines = new ArrayList<String>();
				do
				{
					final String line = reader.readLine();
					if (line == null)
					{
						break;
					}
					else
					{
						lines.add(line);
					}
				}
				while (true);
				return lines;
			}
			finally
			{
				reader.close();
			}
		}
		catch (IOException ex)
		{
			Bukkit.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
			return Collections.emptyList();
		}
	}
}