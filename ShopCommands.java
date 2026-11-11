package com.kyunsmp.shop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShopCommands implements CommandExecutor, TabCompleter {

    private static final String ADMIN = "kyunshop.admin";

    private final KyunShop plugin;

    public ShopCommands(KyunShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);
        switch (name) {
            case "shop" -> shopCommand(sender, args);
            case "sell" -> sellCommand(sender, args);
            case "balance" -> balanceCommand(sender, args);
            case "pay" -> payCommand(sender, args);
            case "eco" -> ecoCommand(sender, args);
            case "shopadmin" -> adminCommand(sender, args);
            default -> {
                return false;
            }
        }
        return true;
    }

    // ---------- /shop ----------

    private void shopCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            Util.msg(sender, "&cBu komut sadece oyuncular için.");
            return;
        }
        if (args.length == 0) {
            plugin.gui().openMain(p);
            return;
        }
        String id = args[0].toLowerCase(Locale.ROOT);
        if (plugin.shop().categories.containsKey(id)) {
            plugin.gui().openCategory(p, id, 0);
        } else {
            Util.msg(p, "&cBöyle bir kategori yok.");
        }
    }

    // ---------- /sell ----------

    private void sellCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            Util.msg(sender, "&cBu komut sadece oyuncular için.");
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("hand")) {
            plugin.gui().sellHand(p);
        } else {
            plugin.gui().openSell(p);
        }
    }

    // ---------- /balance ----------

    private void balanceCommand(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 1) {
            target = findPlayer(args[0]);
            if (target == null) {
                Util.msg(sender, "&cOyuncu bulunamadı.");
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            Util.msg(sender, "&cKullanım: /balance <oyuncu>");
            return;
        }
        double bal = plugin.economy().get(target.getUniqueId());
        if (sender instanceof Player self && self.getUniqueId().equals(target.getUniqueId())) {
            Util.msg(sender, "Bakiyen: &e" + Util.money(bal));
        } else {
            String shown = target.getName() != null ? target.getName() : "Oyuncu";
            Util.msg(sender, shown + " bakiyesi: &e" + Util.money(bal));
        }
    }

    // ---------- /pay ----------

    private void payCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            Util.msg(sender, "&cBu komut sadece oyuncular için.");
            return;
        }
        if (args.length < 2) {
            Util.msg(p, "&cKullanım: /pay <oyuncu> <miktar>");
            return;
        }
        Player to = Bukkit.getPlayerExact(args[0]);
        if (to == null) {
            Util.msg(p, "&cOyuncu bulunamadı veya çevrimdışı.");
            return;
        }
        if (to.getUniqueId().equals(p.getUniqueId())) {
            Util.msg(p, "&cKendine para gönderemezsin.");
            return;
        }
        double amount = Util.round(Util.parse(args[1]));
        if (amount < 0.01) {
            Util.msg(p, "&cGeçerli bir miktar gir. (örn. 500, 1k, 2.5m)");
            return;
        }
        if (!plugin.economy().take(p.getUniqueId(), amount)) {
            Util.msg(p, "&cYeterli paran yok.");
            return;
        }
        plugin.economy().add(to.getUniqueId(), amount);
        Util.msg(p, "&e" + to.getName() + "&f oyuncusuna &a" + Util.money(amount) + "&f gönderdin.");
        Util.msg(to, "&e" + p.getName() + "&f sana &a" + Util.money(amount) + "&f gönderdi.");
    }

    // ---------- /eco ----------

    private void ecoCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            Util.msg(sender, "&cBunun için yetkin yok.");
            return;
        }
        if (args.length < 3) {
            Util.msg(sender, "&cKullanım: /eco <give|take|set> <oyuncu> <miktar>");
            return;
        }
        OfflinePlayer target = findPlayer(args[1]);
        if (target == null) {
            Util.msg(sender, "&cOyuncu bulunamadı.");
            return;
        }
        double amount = Util.round(Util.parse(args[2]));
        if (amount < 0) {
            Util.msg(sender, "&cGeçerli bir miktar gir.");
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give" -> plugin.economy().add(target.getUniqueId(), amount);
            case "take" -> plugin.economy().set(target.getUniqueId(),
                    plugin.economy().get(target.getUniqueId()) - amount);
            case "set" -> plugin.economy().set(target.getUniqueId(), amount);
            default -> {
                Util.msg(sender, "&cKullanım: /eco <give|take|set> <oyuncu> <miktar>");
                return;
            }
        }
        String shown = target.getName() != null ? target.getName() : args[1];
        Util.msg(sender, shown + " yeni bakiye: &e"
                + Util.money(plugin.economy().get(target.getUniqueId())));
    }

    // ---------- /shopadmin ----------

    private void adminCommand(CommandSender sender, String[] a) {
        if (!sender.hasPermission(ADMIN)) {
            Util.msg(sender, "&cBunun için yetkin yok.");
            return;
        }
        if (a.length == 0) {
            adminHelp(sender);
            return;
        }
        ShopData data = plugin.shop();
        String sub = a[0].toLowerCase(Locale.ROOT);

        if (sub.equals("reload")) {
            plugin.reloadConfig();
            data.load();
            Util.msg(sender, "&aShop yeniden yüklendi: " + data.categories.size() + " kategori, "
                    + data.sell.size() + " satış fiyatı.");
            return;
        }

        if (sub.equals("addcategory")) {
            if (a.length < 2) {
                Util.msg(sender, "&cKullanım: /shopadmin addcategory <id>  (elindeki eşya ikon olur)");
                return;
            }
            String id = a[1].toLowerCase(Locale.ROOT);
            if (!validId(id)) {
                Util.msg(sender, "&cId sadece küçük harf, rakam, - ve _ içerebilir (en fazla 24).");
                return;
            }
            Material icon = held(sender);
            if (icon == null) {
                return;
            }
            if (data.addCategory(id, icon)) {
                Util.msg(sender, "&a'" + id + "' kategorisi eklendi. Ürün eklemek için: /shopadmin additem " + id + " <fiyat>");
            } else {
                Util.msg(sender, "&cBu id'de bir kategori zaten var.");
            }
            return;
        }

        if (sub.equals("removecategory")) {
            if (a.length < 2) {
                Util.msg(sender, "&cKullanım: /shopadmin removecategory <id>");
                return;
            }
            if (data.removeCategory(a[1].toLowerCase(Locale.ROOT))) {
                Util.msg(sender, "&aKategori silindi.");
            } else {
                Util.msg(sender, "&cBöyle bir kategori yok.");
            }
            return;
        }

        if (sub.equals("rename")) {
            if (a.length < 3) {
                Util.msg(sender, "&cKullanım: /shopadmin rename <id> <yeni isim>  (renk için &a, &c ...)");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < a.length; i++) {
                if (i > 2) {
                    sb.append(' ');
                }
                sb.append(a[i]);
            }
            if (data.rename(a[1].toLowerCase(Locale.ROOT), sb.toString())) {
                Util.msg(sender, "&aKategori adı değişti.");
            } else {
                Util.msg(sender, "&cBöyle bir kategori yok.");
            }
            return;
        }

        if (sub.equals("seticon")) {
            if (a.length < 2) {
                Util.msg(sender, "&cKullanım: /shopadmin seticon <id>  (elindeki eşya ikon olur)");
                return;
            }
            Material icon = held(sender);
            if (icon == null) {
                return;
            }
            if (data.setIcon(a[1].toLowerCase(Locale.ROOT), icon)) {
                Util.msg(sender, "&aİkon değişti.");
            } else {
                Util.msg(sender, "&cBöyle bir kategori yok.");
            }
            return;
        }

        if (sub.equals("additem")) {
            if (a.length < 3) {
                Util.msg(sender, "&cKullanım: /shopadmin additem <id> <alış fiyatı> [satış fiyatı]");
                return;
            }
            Material m = held(sender);
            if (m == null) {
                return;
            }
            double buy = Util.parse(a[2]);
            if (buy <= 0) {
                Util.msg(sender, "&cAlış fiyatı geçersiz. (örn. 50, 1.5k)");
                return;
            }
            if (!data.setItem(a[1].toLowerCase(Locale.ROOT), m, buy)) {
                Util.msg(sender, "&cBöyle bir kategori yok.");
                return;
            }
            String extra = "";
            if (a.length >= 4) {
                double sellPrice = Util.parse(a[3]);
                if (sellPrice > 0) {
                    data.setSell(m, sellPrice);
                    extra = " &7(satış: " + Util.money(sellPrice) + ")";
                }
            }
            Util.msg(sender, "&a" + Util.name(m) + " eklendi: " + Util.money(buy) + extra);
            return;
        }

        if (sub.equals("removeitem")) {
            if (a.length < 2) {
                Util.msg(sender, "&cKullanım: /shopadmin removeitem <id>  (elindeki eşya silinir)");
                return;
            }
            Material m = held(sender);
            if (m == null) {
                return;
            }
            if (data.removeItem(a[1].toLowerCase(Locale.ROOT), m)) {
                Util.msg(sender, "&a" + Util.name(m) + " kategoriden çıkarıldı.");
            } else {
                Util.msg(sender, "&cBu eşya o kategoride yok.");
            }
            return;
        }

        if (sub.equals("setsell")) {
            if (a.length < 2) {
                Util.msg(sender, "&cKullanım: /shopadmin setsell <fiyat>  (0 = satışı kapat)");
                return;
            }
            Material m = held(sender);
            if (m == null) {
                return;
            }
            double price = Util.parse(a[1]);
            if (price < 0) {
                Util.msg(sender, "&cFiyat geçersiz.");
                return;
            }
            data.setSell(m, price);
            if (price > 0) {
                Util.msg(sender, "&a" + Util.name(m) + " satış fiyatı: " + Util.money(price));
            } else {
                Util.msg(sender, "&a" + Util.name(m) + " artık satılamaz.");
            }
            return;
        }

        adminHelp(sender);
    }

    private void adminHelp(CommandSender s) {
        Util.msg(s, "&e/shopadmin komutları:");
        s.sendMessage(Util.color("&7 reload"));
        s.sendMessage(Util.color("&7 addcategory <id>  &8(elindeki eşya ikon olur)"));
        s.sendMessage(Util.color("&7 removecategory <id>"));
        s.sendMessage(Util.color("&7 rename <id> <isim>  &8(renk: &a, &c ...)"));
        s.sendMessage(Util.color("&7 seticon <id>  &8(elindeki eşya)"));
        s.sendMessage(Util.color("&7 additem <id> <alış> [satış]  &8(elindeki eşya)"));
        s.sendMessage(Util.color("&7 removeitem <id>  &8(elindeki eşya)"));
        s.sendMessage(Util.color("&7 setsell <fiyat>  &8(elindeki eşya, 0 = kapat)"));
    }

    // ---------- Yardımcılar ----------

    private boolean validId(String id) {
        return id.matches("[a-z0-9_-]{1,24}");
    }

    private Material held(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            Util.msg(sender, "&cBunu oyunda, elinde eşya tutarak kullan.");
            return null;
        }
        Material m = p.getInventory().getItemInMainHand().getType();
        if (m.isAir()) {
            Util.msg(sender, "&cElinde bir eşya tutmalısın.");
            return null;
        }
        return m;
    }

    private OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    // ---------- Tab tamamlama ----------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);

        switch (name) {
            case "shop" -> {
                if (args.length == 1) {
                    options.addAll(plugin.shop().categories.keySet());
                }
            }
            case "sell" -> {
                if (args.length == 1) {
                    options.add("hand");
                }
            }
            case "balance" -> {
                if (args.length == 1) {
                    addPlayers(options);
                }
            }
            case "pay" -> {
                if (args.length == 1) {
                    addPlayers(options);
                }
            }
            case "eco" -> {
                if (args.length == 1) {
                    options.addAll(List.of("give", "take", "set"));
                } else if (args.length == 2) {
                    addPlayers(options);
                }
            }
            case "shopadmin" -> {
                if (args.length == 1) {
                    options.addAll(List.of("reload", "addcategory", "removecategory", "rename",
                            "seticon", "additem", "removeitem", "setsell"));
                } else if (args.length == 2) {
                    String sub = args[0].toLowerCase(Locale.ROOT);
                    if (List.of("removecategory", "rename", "seticon", "additem", "removeitem").contains(sub)) {
                        options.addAll(plugin.shop().categories.keySet());
                    }
                }
            }
            default -> {
            }
        }

        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(typed)) {
                result.add(option);
            }
        }
        return result;
    }

    private void addPlayers(List<String> options) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            options.add(p.getName());
        }
    }
}
