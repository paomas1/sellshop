package com.kyunsmp.shop;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class KyunShop extends JavaPlugin {

    private Economy economy;
    private ShopData shop;
    private ShopGui gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        economy = new Economy(this);
        economy.load();

        shop = new ShopData(this);
        shop.load();

        gui = new ShopGui(this);
        getServer().getPluginManager().registerEvents(gui, this);

        ShopCommands commands = new ShopCommands(this);
        for (String name : new String[] {"shop", "sell", "balance", "pay", "eco", "shopadmin"}) {
            PluginCommand cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(commands);
                cmd.setTabCompleter(commands);
            }
        }

        // Her 30 saniyede bir değişen bakiyeleri kaydet
        getServer().getScheduler().runTaskTimer(this, economy::saveIfDirty, 600L, 600L);
        getLogger().info("KyunShop aktif: " + shop.categories.size() + " kategori, "
                + shop.sell.size() + " satış fiyatı yüklendi.");
    }

    @Override
    public void onDisable() {
        if (gui != null) {
            gui.flushAll();
        }
        if (economy != null) {
            economy.save();
        }
    }

    public Economy economy() {
        return economy;
    }

    public ShopData shop() {
        return shop;
    }

    public ShopGui gui() {
        return gui;
    }
}
