package com.kyunsmp.shop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** shop.yml dosyasını okur; kategori/ürün/satış fiyatlarını tutar. */
public final class ShopData {

    public static final class Category {
        public final String id;
        public String name;
        public Material icon = Material.CHEST;
        public final LinkedHashMap<Material, Double> items = new LinkedHashMap<>();

        Category(String id) {
            this.id = id;
        }
    }

    private final KyunShop plugin;
    private final File file;
    private YamlConfiguration yml;

    public String title = "&8Shop";
    public final LinkedHashMap<String, Category> categories = new LinkedHashMap<>();
    public final Map<Material, Double> sell = new HashMap<>();

    public ShopData(KyunShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shop.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("shop.yml", false);
        }
        yml = YamlConfiguration.loadConfiguration(file);
        categories.clear();
        sell.clear();

        title = yml.getString("menu-title", "&8Shop");

        ConfigurationSection cats = yml.getConfigurationSection("categories");
        if (cats != null) {
            for (String id : cats.getKeys(false)) {
                ConfigurationSection c = cats.getConfigurationSection(id);
                if (c == null) {
                    continue;
                }
                Category cat = new Category(id);
                cat.name = c.getString("name", "&f" + id);
                Material icon = Material.matchMaterial(c.getString("icon", "CHEST"));
                if (icon != null && icon.isItem()) {
                    cat.icon = icon;
                }
                ConfigurationSection items = c.getConfigurationSection("items");
                if (items != null) {
                    for (String key : items.getKeys(false)) {
                        Material m = Material.matchMaterial(key);
                        if (m == null || !m.isItem()) {
                            plugin.getLogger().warning("shop.yml: geçersiz eşya atlandı: " + key);
                            continue;
                        }
                        double price = items.getDouble(key);
                        if (price > 0) {
                            cat.items.put(m, price);
                        }
                    }
                }
                categories.put(id, cat);
            }
        }

        ConfigurationSection sec = yml.getConfigurationSection("sell");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                Material m = Material.matchMaterial(key);
                if (m == null || !m.isItem()) {
                    plugin.getLogger().warning("shop.yml: geçersiz satış eşyası atlandı: " + key);
                    continue;
                }
                double price = sec.getDouble(key);
                if (price > 0) {
                    sell.put(m, price);
                }
            }
        }
    }

    /** Bu eşyanın satış fiyatı (tanesi). Düz eşya değilse veya fiyatı yoksa null. */
    public Double sellPrice(ItemStack item) {
        Double price = sell.get(item.getType());
        if (price == null || !Util.plain(item)) {
            return null;
        }
        return price;
    }

    // ---------- Yönetim komutları için düzenleme metotları ----------

    private void saveAndReload() {
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("shop.yml kaydedilemedi: " + e.getMessage());
        }
        load();
    }

    public boolean addCategory(String id, Material icon) {
        if (yml.contains("categories." + id)) {
            return false;
        }
        yml.set("categories." + id + ".name", "&f" + id);
        yml.set("categories." + id + ".icon", icon.name());
        yml.createSection("categories." + id + ".items");
        saveAndReload();
        return true;
    }

    public boolean removeCategory(String id) {
        if (!yml.contains("categories." + id)) {
            return false;
        }
        yml.set("categories." + id, null);
        saveAndReload();
        return true;
    }

    public boolean rename(String id, String name) {
        if (!yml.contains("categories." + id)) {
            return false;
        }
        yml.set("categories." + id + ".name", name);
        saveAndReload();
        return true;
    }

    public boolean setIcon(String id, Material icon) {
        if (!yml.contains("categories." + id)) {
            return false;
        }
        yml.set("categories." + id + ".icon", icon.name());
        saveAndReload();
        return true;
    }

    public boolean setItem(String id, Material m, double price) {
        if (!yml.contains("categories." + id)) {
            return false;
        }
        yml.set("categories." + id + ".items." + m.name(), price);
        saveAndReload();
        return true;
    }

    public boolean removeItem(String id, Material m) {
        if (!yml.contains("categories." + id + ".items." + m.name())) {
            return false;
        }
        yml.set("categories." + id + ".items." + m.name(), null);
        saveAndReload();
        return true;
    }

    /** price <= 0 ise satış fiyatını kaldırır. */
    public void setSell(Material m, double price) {
        yml.set("sell." + m.name(), price > 0 ? price : null);
        saveAndReload();
    }
}
