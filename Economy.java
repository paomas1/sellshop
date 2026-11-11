package com.kyunsmp.shop;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Basit, dosya tabanlı bakiye sistemi (plugins/KyunShop/balances.yml). */
public final class Economy {

    private final KyunShop plugin;
    private final File file;
    private final Map<UUID, Double> balances = new HashMap<>();
    private boolean dirty;

    public Economy(KyunShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "balances.yml");
    }

    public void load() {
        balances.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yml.getConfigurationSection("balances");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                balances.put(UUID.fromString(key), section.getDouble(key));
            } catch (IllegalArgumentException ignored) {
                // bozuk satırı atla
            }
        }
    }

    public double get(UUID id) {
        Double value = balances.get(id);
        if (value != null) {
            return value;
        }
        return plugin.getConfig().getDouble("starting-balance", 1000.0);
    }

    public void set(UUID id, double value) {
        balances.put(id, Util.round(Math.max(0, value)));
        dirty = true;
    }

    public void add(UUID id, double value) {
        set(id, get(id) + value);
    }

    /** Yeterli para varsa düşer ve true döner. */
    public boolean take(UUID id, double value) {
        double current = get(id);
        if (current + 0.000001 < value) {
            return false;
        }
        set(id, current - value);
        return true;
    }

    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Double> e : balances.entrySet()) {
            yml.set("balances." + e.getKey(), e.getValue());
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().severe("balances.yml kaydedilemedi: " + ex.getMessage());
        }
    }
}
