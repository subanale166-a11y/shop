package com.example.economysmp;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShardShop implements Listener {
    private final EconomySMP plugin;
    private final Map<Integer, Entry> entries = new HashMap<>();

    private record Entry(Material material, long cost) {}

    public ShardShop(EconomySMP plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        entries.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shard-shop.items");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            Material m = Material.matchMaterial(s.getString("material", ""));
            if (m == null) {
                plugin.getLogger().warning("Unknown material in shard-shop: " + key);
                continue;
            }
            entries.put(s.getInt("slot"), new Entry(m, s.getLong("cost")));
        }
    }

    public static class ShopHolder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
        void set(Inventory i) { this.inv = i; }
    }

    public void open(Player p) {
        ShopHolder holder = new ShopHolder();
        int size = plugin.getConfig().getInt("shard-shop.size", 27);
        Inventory inv = Bukkit.createInventory(holder, size,
                Util.color(plugin.getConfig().getString("shard-shop.title", "&5Shard Shop")));
        holder.set(inv);

        entries.forEach((slot, entry) -> {
            if (slot < 0 || slot >= size) return;
            ItemStack item = new ItemStack(entry.material());
            ItemMeta meta = item.getItemMeta();
            meta.lore(List.<Component>of(
                    Util.color("&7Cost: &d" + entry.cost() + " shards"),
                    Util.color("&eClick to buy")));
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        });
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof ShopHolder)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        Entry entry = entries.get(e.getSlot());
        if (entry == null) return;

        Player p = (Player) e.getWhoClicked();
        PlayerData d = plugin.data().get(p.getUniqueId());
        String prefix = plugin.getConfig().getString("messages.prefix", "");

        if (d.shards < entry.cost()) {
            p.sendMessage(Util.color(prefix + "&cYou need &d" + entry.cost()
                    + " shards&c, you only have &d" + d.shards + "&c."));
            return;
        }
        d.shards -= entry.cost();
        p.getInventory().addItem(new ItemStack(entry.material())).values()
                .forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(), rest));
        plugin.sidebar().update(p);
        p.sendMessage(Util.color(prefix + "Purchased for &d" + entry.cost() + " shards&7."));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof ShopHolder) e.setCancelled(true);
    }
}
