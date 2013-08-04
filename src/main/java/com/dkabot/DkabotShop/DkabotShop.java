package com.dkabot.DkabotShop;

import com.dkabot.DkabotShop.util.ItemDb;
import com.dkabot.DkabotShop.command.ShopInfoCommandExecutor;
import com.dkabot.DkabotShop.persistence.SaleEntity;
import com.dkabot.DkabotShop.persistence.HistoryEntity;
import com.dkabot.DkabotShop.command.HistoryCommandExecutor;
import com.dkabot.DkabotShop.command.SellerCommandExecutor;
import com.dkabot.DkabotShop.command.BuyerCommandExecutor;
import com.dkabot.DkabotShop.util.DkabotUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import net.milkbowl.vault.Vault;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

public class DkabotShop extends JavaPlugin {

    private static DkabotShop instance;
    private static Logger log;
    private ItemDb itemDB = null;
    private static Vault vault = null;
    private Economy economy = null;

    @Override
    public void onEnable() {
        log = this.getLogger();

        hookDependencies();
        if (!setupEconomy()) {
            log.severe("No economy system found. You need one to use this!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        //Sets up ItemDb
        setupItemDB();

        checkConfig();
        validateConfig();
        setupDatabase();
        setupCommandExecutors();
        startMetrics();


        log.info(getDescription().getName() + " version " + getDescription().getVersion() + " is now enabled,");
        reloadConfig();
    }

    @Override
    public void onDisable() {
        log.info(getDescription().getName() + " is now disabled.");
    }

    public void setupDatabase() {
        try {
            getDatabase().find(SaleEntity.class).findRowCount();
            getDatabase().find(HistoryEntity.class).findRowCount();
        } catch (PersistenceException ex) {
            log.info("Installing database due to first time usage");
            installDDL();
        }
    }

    @Override
    public ArrayList<Class<?>> getDatabaseClasses() {
        ArrayList<Class<?>> list = new ArrayList<>();
        list.add(SaleEntity.class);
        list.add(HistoryEntity.class);
        return list;
    }

    private Boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }

    /**
     * Ensures the configuration is not missing any needed fields.
     *
     * If no config exists, will create a new one.
     */
    private void checkConfig() {
        //Create and set string lists
        List<String> blacklistAlways = new ArrayList<>();
        List<String> itemAlias = new ArrayList<>();
        blacklistAlways.add("137");
        itemAlias.add("wood,17:0");
        //Add default config and save
        getConfig().addDefault("Blacklist.Always", blacklistAlways);
        getConfig().addDefault("ItemAlias", itemAlias);
        getConfig().addDefault("DisableBroadcasting", false);
        getConfig().addDefault("AlternateBroadcasting", false);
        getConfig().addDefault("AlwaysBuyAvailable", false);
        getConfig().addDefault("MaxStock", -1);
        getConfig().addDefault("MaxPrice", (double) -1);
        getConfig().addDefault("MinPrice", (double) -1);
        //default messages
        getConfig().addDefault("Messages.NewlySelling", "&6[player]&9 is now selling&6 [amount] [item]&9 for &6 [cost] [currency]&9 each.");
        getConfig().addDefault("Messages.Added", "&6[player]&9 has added&6 [amount]&9 more&6 [item]&9 to their shop.");
        getConfig().addDefault("Messages.AddedPriceChange", "&6[player]&9 has added&6 [amount]&9 more&6 [item]&9 to their shop and changed it's price to&6 [cost] [currency]&9.");
        getConfig().addDefault("Messages.RemovedSome", "&6[player]&9 has reduced their shop's supply of&6 [item]&9 to&6 [amount]&9.");
        getConfig().addDefault("Messages.RemovedAll", "&6[player]&9 has removed their&6 [item]&9 from their shop.");
        getConfig().addDefault("Messages.PriceChange", "&6[player]&9 has changed their shop's price of&6 [item]&9 to&6 [cost] [currency]&9.");
        getConfig().addDefault("Messages.ShopBoughtSome", "&6[player]&9 bought &6[amount]&9 of your shop's &6[item]&9!");
        getConfig().addDefault("Messages.ShopBoughtAll", "&6[player]&9 bought &6all&9 your shop's &6[item]&9!");
        //save that info
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    //Validates the config, as the function name suggests
    private void validateConfig() {
        List<String> errors = new ArrayList<>();
        try {
            for (String str : getConfig().getStringList("ItemAlias")) {
                if (str.split(",").length != 2) {
                    errors.add("formatting,ItemAlias");
                    continue;
                }
                String materialString = str.split(",")[1];
                if (!materialString.equalsIgnoreCase("hand") && DkabotUtils.getMaterial(materialString, false, null, false) == null) {
                    errors.add(materialString + ",ItemAlias");
                }
            }
            for (String materialString : getConfig().getStringList("Blacklist.Always")) {
                if (DkabotUtils.getMaterial(materialString, false, null, false) == null) {
                    errors.add(materialString + ",Blacklist Always");
                }
            }
            if (getConfig().getDouble("MaxPrice") <= 0 && getConfig().getDouble("MaxPrice") != -1) {
                errors.add(getConfig().getDouble("MaxPrice") + ",Maximum Price");
            }
            if (getConfig().getDouble("MinPrice") <= 0 && getConfig().getDouble("MinPrice") != -1) {
                errors.add(getConfig().getDouble("MinPrice") + ",Minimum Price");
            }
            if (getConfig().getInt("MaxStock") <= 0 && getConfig().getInt("MaxStock") != -1) {
                errors.add(getConfig().getInt("MaxStock") + ",Max Stock");
            }
        } catch (Exception e) {
            log.severe("Exception occurred while processing the configuration! Printing stacktrace and disabling...");
            e.printStackTrace();
            errors = null;
        }

        if (errors == null) {
            getServer().getPluginManager().disablePlugin(this);
        }

        if (!errors.isEmpty()) {
            log.severe("Error(s) in configuration!");
            for (String s : errors) {
                String[] split = s.split(",");
                log.severe("Error on " + split[0] + " in the " + split[1] + " section!");
            }
            log.severe("Disabling due to above errors...");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Hooks into all dependent plugins.
     *
     * @return true if all hooks were successful, false if any were not found
     */
    private boolean hookDependencies() {
        //Vault dependency checker
        Plugin vaultPlugin = this.getServer().getPluginManager().getPlugin("Vault");
        if (vaultPlugin != null & vaultPlugin instanceof Vault) {
            vault = (Vault) vaultPlugin;
            log.info(String.format("[%s] Hooked %s %s", getDescription().getName(), vault.getDescription().getName(), vault.getDescription().getVersion()));
        } else {
            log.severe(String.format("Vault dependency not found! Disabling..."));
            getPluginLoader().disablePlugin(this);
            return false;
        }

        return true;
    }

    public ItemDb getItemDB() {
        return itemDB;
    }

    public static DkabotShop getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    private void setupCommandExecutors() {
        SellerCommandExecutor sellExecutor = new SellerCommandExecutor(this);
        BuyerCommandExecutor buyExecutor = new BuyerCommandExecutor(this);
        HistoryCommandExecutor histExecutor = new HistoryCommandExecutor(this);
        ShopInfoCommandExecutor infoExecutor = new ShopInfoCommandExecutor(this);
        getCommand("buy").setExecutor(buyExecutor);
        getCommand("stock").setExecutor(buyExecutor);
        getCommand("sell").setExecutor(sellExecutor);
        getCommand("cancel").setExecutor(sellExecutor);
        getCommand("price").setExecutor(sellExecutor);
        getCommand("sales").setExecutor(histExecutor);
        getCommand("shopinfo").setExecutor(infoExecutor);
    }

    private void startMetrics() {
        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException ex) {
            log.warning("Failed to start plugin metrics.");
        }
    }

    private void setupItemDB() {
        itemDB = new ItemDb(this);
        itemDB.onReload();
    }
}