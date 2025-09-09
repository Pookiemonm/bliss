
package com.blisssmp.core;

import com.blisssmp.BlissSMP;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class GemManager implements Listener {

    private final BlissSMP plugin;
    private final NamespacedKey GEM_KEY;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public enum Gem { ASTRA, FIRE, FLUX, LIFE, PUFF, SPEED, STRENGTH, WEALTH }

    public GemManager(BlissSMP plugin) {
        this.plugin = plugin;
        this.GEM_KEY = new NamespacedKey(plugin, "gem-id");
    }

    public ItemStack createGem(Gem gem) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.AQUA + gem.name() + " Gem");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Right-click to activate", ChatColor.DARK_PURPLE + "Bliss S3"));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.getPersistentDataContainer().set(GEM_KEY, PersistentDataType.STRING, gem.name());
        item.setItemMeta(meta);
        return item;
    }

    private long baseCooldownTicks(Gem gem) {
        switch (gem) {
            case ASTRA: return 20 * 18; // 18s
            case FIRE: return 20 * 15;
            case FLUX: return 20 * 22;
            case LIFE: return 20 * 25;
            case PUFF: return 20 * 10;
            case SPEED: return 20 * 16;
            case STRENGTH: return 20 * 20;
            case WEALTH: return 20 * 30;
            default: return 20 * 20;
        }
    }

    private boolean hasDragonEgg(Player p) {
        return p.getInventory().contains(Material.DRAGON_EGG);
    }

    private boolean isOnCooldown(Player p, Gem gem) {
        Map<String, Long> map = cooldowns.getOrDefault(p.getUniqueId(), new HashMap<>());
        long now = System.currentTimeMillis();
        Long until = map.get(gem.name());
        return until != null && until > now;
    }

    private long remaining(Player p, Gem gem) {
        Map<String, Long> map = cooldowns.getOrDefault(p.getUniqueId(), new HashMap<>());
        long now = System.currentTimeMillis();
        Long until = map.get(gem.name());
        if (until == null) return 0;
        return Math.max(0, until - now);
    }

    private void startCooldown(Player p, Gem gem) {
        long ticks = baseCooldownTicks(gem);
        if (hasDragonEgg(p)) ticks /= 2; // half cooldown
        long ms = ticks * 50L;
        cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(gem.name(), System.currentTimeMillis() + ms);
    }

    private Optional<Gem> getGem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Optional.empty();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(GEM_KEY, PersistentDataType.STRING);
        if (id == null) return Optional.empty();
        try {
            return Optional.of(Gem.valueOf(id));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Ensure config exists
        plugin.getConfig().addDefault("messages.onCooldown", "&cGem is on cooldown: &f%seconds%s");
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        Optional<Gem> og = getGem(p.getInventory().getItemInMainHand());
        if (!og.isPresent()) return;

        Gem gem = og.get();
        if (isOnCooldown(p, gem)) {
            long secs = (long)Math.ceil(remaining(p, gem) / 1000.0);
            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.onCooldown").replace("%seconds%", String.valueOf(secs))));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }

        // Activate
        switch (gem) {
            case ASTRA:
                // Short blink + brief invis
                Location loc = p.getLocation().clone();
                Vector dir = loc.getDirection().normalize();
                loc.add(dir.multiply(6));
                loc.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 50, 0.5, 1, 0.5);
                p.teleport(loc);
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20*3, 0, false, false, true));
                p.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                break;
            case FIRE:
                // Heal burst + saturation tick
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20*4, 1));
                p.setFoodLevel(Math.min(20, p.getFoodLevel()+2));
                p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 60, 1, 0.5, 1, 0.01);
                p.playSound(p, Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
                break;
            case FLUX:
                // Damage the nearest target within 20 blocks in sight
                Player target = null;
                double best = 21;
                for (Player other : p.getWorld().getPlayers()) {
                    if (other.equals(p)) continue;
                    if (!other.getWorld().equals(p.getWorld())) continue;
                    double d = other.getLocation().distance(p.getLocation());
                    if (d <= 20 && d < best) {
                        best = d;
                        target = other;
                    }
                }
                if (target != null) {
                    target.damage(6.0, p); // 3 hearts
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20*5, 0));
                } else {
                    p.sendMessage(ChatColor.GRAY + "Flux found no target.");
                }
                break;
            case LIFE:
                // Regen pulse around player
                p.getWorld().spawnParticle(Particle.HEART, p.getLocation(), 20, 1, 1, 1);
                for (Player ally : p.getWorld().getPlayers()) {
                    if (ally.getLocation().distance(p.getLocation()) <= 8) {
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20*5, 1));
                    }
                }
                p.playSound(p, Sound.ITEM_HONEY_BOTTLE_DRINK, 1f, 1f);
                break;
            case PUFF:
                // Dash forward + slowfall landing
                Vector push = p.getLocation().getDirection().normalize().multiply(1.6).setY(0.4);
                p.setVelocity(push);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20*4, 0));
                p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 40, 0.4, 0.2, 0.4, 0.02);
                p.playSound(p, Sound.ENTITY_PHANTOM_FLAP, 1f, 1.8f);
                break;
            case SPEED:
                // Speed surge
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20*6, 2));
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.6f);
                break;
            case STRENGTH:
                // Cleanse bad effects + Strength II
                for (PotionEffect pe : new ArrayList<>(p.getActivePotionEffects())) {
                    PotionEffectType t = pe.getType();
                    if (t.equals(PotionEffectType.POISON) || t.equals(PotionEffectType.WEAKNESS) ||
                        t.equals(PotionEffectType.SLOWNESS) || t.equals(PotionEffectType.MINING_FATIGUE)) {
                        p.removePotionEffect(t);
                    }
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20*6, 1));
                p.getWorld().spawnParticle(Particle.CRIT, p.getLocation(), 50, 0.7, 1, 0.7, 0.1);
                p.playSound(p, Sound.ITEM_TOTEM_USE, 1f, 1.2f);
                break;
            case WEALTH:
                // Brief luck + haste + small absorption
                p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 20*10, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 20*10, 1));
                double newAbs = Math.min( (p.getAbsorptionAmount() + 2.0), 8.0);
                p.setAbsorptionAmount(newAbs);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                break;
        }

        startCooldown(p, gem);
        p.sendActionBar(ChatColor.LIGHT_PURPLE + gem.name() + " activated" + (hasDragonEgg(p) ? ChatColor.GRAY + " (egg: 1/2 cd)" : ""));
    }

    public void giveGem(Player target, Gem gem) {
        target.getInventory().addItem(createGem(gem));
        target.sendMessage(ChatColor.GREEN + "You received a " + gem.name() + " Gem!");
    }

    public List<String> gemNames() {
        List<String> list = new ArrayList<>();
        for (Gem g : Gem.values()) list.add(g.name());
        return list;
    }
}
