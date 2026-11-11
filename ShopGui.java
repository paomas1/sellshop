package com.kyunsmp.shop;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** /shop menüleri ve /sell penceresi. */
public final class ShopGui implements Listener {

    private static final int ITEMS_PER_PAGE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_BALANCE = 50;
    private static final int SLOT_NEXT = 53;

    // ---------- Pencere sahipleri ----------

    private abstract static class Holder implements InventoryHolder {
        protected Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class MainHolder extends Holder {
        final Map<Integer, String> slots = new HashMap<>();
    }

    private static final class CategoryHolder extends Holder {
        final String category;
        final int page;
        boolean hasNext;
        final Map<Integer, Material> slots = new HashMap<>();

        CategoryHolder(String category, int page) {
            this.category = category;
            this.page = page;
        }
    }

    private static final class SellHolder extends Holder {
        boolean done;
    }

    private final KyunShop plugin;

    public ShopGui(KyunShop plugin) {
        this.plugin = plugin;
    }

    // ---------- Yardımcılar ----------

    private ItemStack namedItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Util.color(name));
        if (lore != null && !lore.isEmpty()) {
            List<Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(Util.color(line));
            }
            meta.lore(lines);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane() {
        return namedItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
    }

    private ItemStack balanceItem(Player p) {
        double bal = plugin.economy().get(p.getUniqueId());
        return namedItem(Material.GOLD_INGOT, "&6Bakiyen", List.of("&e" + Util.money(bal)));
    }

    // ---------- Ana menü ----------

    public void openMain(Player p) {
        ShopData data = plugin.shop();
        int count = Math.min(data.categories.size(), 28);
        int rows = Math.max(3, Math.min(6, (int) Math.ceil(count / 7.0) + 2));

        MainHolder holder = new MainHolder();
        holder.inventory = Bukkit.createInventory(holder, rows * 9, Util.color(data.title));
        for (int i = 0; i < rows * 9; i++) {
            holder.inventory.setItem(i, pane());
        }

        int index = 0;
        for (ShopData.Category cat : data.categories.values()) {
            if (index >= 28) {
                break;
            }
            int slot = (1 + index / 7) * 9 + 1 + index % 7;
            holder.inventory.setItem(slot, namedItem(cat.icon, cat.name,
                    List.of("&7" + cat.items.size() + " ürün", "", "&eAçmak için tıkla")));
            holder.slots.put(slot, cat.id);
            index++;
        }
        p.openInventory(holder.inventory);
    }

    // ---------- Kategori menüsü ----------

    public void openCategory(Player p, String id, int page) {
        ShopData.Category cat = plugin.shop().categories.get(id);
        if (cat == null) {
            openMain(p);
            return;
        }
        List<Map.Entry<Material, Double>> list = new ArrayList<>(cat.items.entrySet());
        int pages = Math.max(1, (list.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        int current = Math.max(0, Math.min(page, pages - 1));

        CategoryHolder holder = new CategoryHolder(id, current);
        holder.hasNext = current < pages - 1;
        holder.inventory = Bukkit.createInventory(holder, 54,
                Util.color(cat.name + " &8(" + (current + 1) + "/" + pages + ")"));

        int start = current * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < list.size(); i++) {
            Material m = list.get(start + i).getKey();
            double price = list.get(start + i).getValue();

            List<String> lore = new ArrayList<>();
            lore.add("&7Fiyat: &a" + Util.money(price));
            Double sellPrice = plugin.shop().sell.get(m);
            if (sellPrice != null) {
                lore.add("&7Satış: &e" + Util.money(sellPrice));
            }
            lore.add("");
            lore.add("&eSol tık: &f1 adet al");
            lore.add("&eSağ tık: &f16 adet al");
            lore.add("&eShift + tık: &f64 adet al");

            ItemStack icon = new ItemStack(m);
            ItemMeta meta = icon.getItemMeta();
            List<Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(Util.color(line));
            }
            meta.lore(lines);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            icon.setItemMeta(meta);

            holder.inventory.setItem(i, icon);
            holder.slots.put(i, m);
        }

        for (int i = ITEMS_PER_PAGE; i < 54; i++) {
            holder.inventory.setItem(i, pane());
        }
        if (current > 0) {
            holder.inventory.setItem(SLOT_PREV, namedItem(Material.ARROW, "&eÖnceki sayfa", null));
        }
        if (holder.hasNext) {
            holder.inventory.setItem(SLOT_NEXT, namedItem(Material.ARROW, "&eSonraki sayfa", null));
        }
        holder.inventory.setItem(SLOT_BACK, namedItem(Material.BARRIER, "&cAna menü", null));
        holder.inventory.setItem(SLOT_BALANCE, balanceItem(p));

        p.openInventory(holder.inventory);
    }

    // ---------- Satın alma ----------

    private void buy(Player p, Material m, double unitPrice, int amount) {
        if (!Util.fits(p, m, amount)) {
            Util.msg(p, "&cEnvanterinde yeterli yer yok.");
            return;
        }
        double total = Util.round(unitPrice * amount);
        if (!plugin.economy().take(p.getUniqueId(), total)) {
            Util.msg(p, "&cYeterli paran yok. Gerekli: &e" + Util.money(total));
            return;
        }
        int left = amount;
        int max = m.getMaxStackSize();
        while (left > 0) {
            int n = Math.min(max, left);
            Util.giveOrDrop(p, new ItemStack(m, n));
            left -= n;
        }
        Util.msg(p, "&a" + amount + "x " + Util.name(m) + " aldın: &c-" + Util.money(total));
    }

    // ---------- Sell penceresi ----------

    public void openSell(Player p) {
        SellHolder holder = new SellHolder();
        holder.inventory = Bukkit.createInventory(holder, 54, Util.color("&aSat &8- kapatınca satılır"));
        p.openInventory(holder.inventory);
        Util.msg(p, "Satmak istediğin eşyaları pencereye koy, sonra kapat.");
    }

    /** Elindeki eşyayı (stack) hemen satar. */
    public void sellHand(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Util.msg(p, "&cElinde eşya yok.");
            return;
        }
        Double price = plugin.shop().sellPrice(hand);
        if (price == null) {
            Util.msg(p, "&cBu eşya satılamıyor.");
            return;
        }
        double total = Util.round(price * hand.getAmount());
        int amount = hand.getAmount();
        p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        plugin.economy().add(p.getUniqueId(), total);
        Util.msg(p, "&a" + amount + " eşya sattın: &e+" + Util.money(total));
    }

    private void finishSell(Player p, SellHolder holder) {
        if (holder.done) {
            return;
        }
        holder.done = true;

        double total = 0;
        int sold = 0;
        List<ItemStack> giveBack = new ArrayList<>();
        for (ItemStack item : holder.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Double price = plugin.shop().sellPrice(item);
            if (price != null) {
                total += price * item.getAmount();
                sold += item.getAmount();
            } else {
                giveBack.add(item);
            }
        }
        holder.getInventory().clear();

        for (ItemStack item : giveBack) {
            Util.giveOrDrop(p, item);
        }
        if (sold > 0) {
            total = Util.round(total);
            plugin.economy().add(p.getUniqueId(), total);
            Util.msg(p, "&a" + sold + " eşya sattın: &e+" + Util.money(total));
        }
        if (!giveBack.isEmpty()) {
            Util.msg(p, "&cSatılamayan eşyalar geri verildi.");
        }
    }

    /** Sunucu kapanırken açık sell pencerelerini sat/iade et. */
    public void flushAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof SellHolder sh) {
                finishSell(p, sh);
                p.closeInventory();
            }
        }
    }

    // ---------- Olaylar ----------

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder top = e.getInventory().getHolder();
        if (!(top instanceof Holder)) {
            return;
        }
        if (top instanceof SellHolder) {
            return; // sell penceresinde normal kullanım serbest
        }
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getInventory()) {
            return;
        }
        int slot = e.getSlot();

        if (top instanceof MainHolder main) {
            String id = main.slots.get(slot);
            if (id != null) {
                openCategory(p, id, 0);
            }
            return;
        }

        if (top instanceof CategoryHolder ch) {
            if (slot == SLOT_PREV && ch.page > 0) {
                openCategory(p, ch.category, ch.page - 1);
                return;
            }
            if (slot == SLOT_NEXT && ch.hasNext) {
                openCategory(p, ch.category, ch.page + 1);
                return;
            }
            if (slot == SLOT_BACK) {
                openMain(p);
                return;
            }
            Material m = ch.slots.get(slot);
            if (m == null) {
                return;
            }
            ShopData.Category cat = plugin.shop().categories.get(ch.category);
            Double price = cat == null ? null : cat.items.get(m);
            if (price == null) {
                Util.msg(p, "&cBu ürün artık satışta değil.");
                p.closeInventory();
                return;
            }
            int amount = switch (e.getClick()) {
                case LEFT -> 1;
                case RIGHT -> 16;
                case SHIFT_LEFT, SHIFT_RIGHT -> 64;
                default -> 0;
            };
            if (amount == 0) {
                return;
            }
            buy(p, m, price, amount);
            ch.inventory.setItem(SLOT_BALANCE, balanceItem(p));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        InventoryHolder top = e.getInventory().getHolder();
        if (top instanceof Holder && !(top instanceof SellHolder)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof SellHolder sh)) {
            return;
        }
        if (!(e.getPlayer() instanceof Player p)) {
            return;
        }
        finishSell(p, sh);
    }
}
