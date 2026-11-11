package com.kyunsmp.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Util {

    private Util() {
    }

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final String PREFIX = "&6[Shop] &f";
    private static final String[] SUFFIX = {"", "K", "M", "B", "T"};

    /** "&a..." renk kodlu yazıyı Component'e çevirir (italik kapalı). */
    public static Component color(String text) {
        return LEGACY.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public static void msg(CommandSender to, String text) {
        to.sendMessage(color(PREFIX + text));
    }

    public static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** 1500 -> $1.5K, 2000000 -> $2M */
    public static String money(double value) {
        double x = Math.abs(value);
        int i = 0;
        while (x >= 1000 && i < SUFFIX.length - 1) {
            x /= 1000;
            i++;
        }
        DecimalFormat df = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return (value < 0 ? "-$" : "$") + df.format(x) + SUFFIX[i];
    }

    /** "1k", "2.5m", "1b", "150" -> sayı. Hatalıysa -1 döner. */
    public static double parse(String input) {
        String s = input.trim().toLowerCase(Locale.ROOT).replace(',', '.');
        if (s.isEmpty()) {
            return -1;
        }
        double mult = 1;
        char last = s.charAt(s.length() - 1);
        switch (last) {
            case 'k' -> mult = 1_000d;
            case 'm' -> mult = 1_000_000d;
            case 'b' -> mult = 1_000_000_000d;
            case 't' -> mult = 1_000_000_000_000d;
            default -> {
            }
        }
        if (mult != 1) {
            s = s.substring(0, s.length() - 1);
        }
        try {
            double v = Double.parseDouble(s) * mult;
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return -1;
            }
            return v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** DIAMOND_SWORD -> Diamond Sword */
    public static String name(Material m) {
        StringBuilder sb = new StringBuilder();
        for (String part : m.name().toLowerCase(Locale.ROOT).split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    /** İsim, lore, büyü gibi hiçbir özelliği olmayan düz eşya mı? */
    public static boolean plain(ItemStack item) {
        return item.isSimilar(new ItemStack(item.getType()));
    }

    /** Oyuncunun envanterine bu eşyadan 'amount' adet sığar mı? */
    public static boolean fits(Player p, Material m, int amount) {
        int max = m.getMaxStackSize();
        long space = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (s == null || s.getType().isAir()) {
                space += max;
            } else if (s.getType() == m && plain(s)) {
                space += Math.max(0, max - s.getAmount());
            }
        }
        return space >= amount;
    }

    /** Envantere ver, sığmayanı yere at. */
    public static void giveOrDrop(Player p, ItemStack item) {
        for (ItemStack left : p.getInventory().addItem(item).values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), left);
        }
    }
}
