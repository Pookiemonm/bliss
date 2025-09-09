
package com.blisssmp.core;

import com.blisssmp.BlissSMP;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class ProgManager implements Listener {
    private final BlissSMP plugin;
    // 1: overworld only, 2: nether enabled, 3+: end enabled
    private int stage;

    public ProgManager(BlissSMP plugin) {
        this.plugin = plugin;
        this.stage = plugin.getConfig().getInt("progs.stage", 1);
    }

    public int getStage() { return stage; }
    public void setStage(int s) {
        this.stage = Math.max(1, Math.min(4, s));
        plugin.getConfig().set("progs.stage", this.stage);
        plugin.saveConfig();
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent e) {
        if (e.getTo() == null) return;
        World w = e.getTo().getWorld();
        if (w == null) return;
        Environment env = w.getEnvironment();

        if (env == Environment.NETHER && stage < 2) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Nether is locked until Prog 2.");
        } else if (env == Environment.THE_END && stage < 3) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "The End is locked until Prog 3.");
        }
    }
}
