
package com.blisssmp;

import com.blisssmp.cmd.BlissCommand;
import com.blisssmp.core.GemManager;
import com.blisssmp.core.ProgManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BlissSMP extends JavaPlugin {

    private static BlissSMP instance;
    private GemManager gemManager;
    private ProgManager progManager;

    public static BlissSMP get() { return instance; }
    public GemManager gems() { return gemManager; }
    public ProgManager progs() { return progManager; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        gemManager = new GemManager(this);
        progManager = new ProgManager(this);

        getServer().getPluginManager().registerEvents(gemManager, this);
        getServer().getPluginManager().registerEvents(progManager, this);

        getCommand("bliss").setExecutor(new BlissCommand(this));
        getCommand("bliss").setTabCompleter(new BlissCommand(this));

        getLogger().info("BlissSMP enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("BlissSMP disabled.");
    }
}
