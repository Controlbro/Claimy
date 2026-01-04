package com.controlbro.claimy;

import com.controlbro.claimy.commands.MallCommand;
import com.controlbro.claimy.commands.MallTabCompleter;
import com.controlbro.claimy.commands.StuckCommand;
import com.controlbro.claimy.commands.StuckTabCompleter;
import com.controlbro.claimy.commands.TownAdminCommand;
import com.controlbro.claimy.commands.TownAdminTabCompleter;
import com.controlbro.claimy.commands.TownCommand;
import com.controlbro.claimy.commands.TownTabCompleter;
import com.controlbro.claimy.gui.MallGui;
import com.controlbro.claimy.gui.TownGui;
import com.controlbro.claimy.listeners.ProtectionListener;
import com.controlbro.claimy.map.MapIntegration;
import com.controlbro.claimy.map.NoopMapIntegration;
import com.controlbro.claimy.map.SquaremapIntegration;
import com.controlbro.claimy.managers.MallManager;
import com.controlbro.claimy.managers.TownManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ClaimyPlugin extends JavaPlugin {
    private TownManager townManager;
    private MallManager mallManager;
    private TownGui townGui;
    private MallGui mallGui;
    private MapIntegration mapIntegration = new NoopMapIntegration();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("gui.yml", false);

        this.townManager = new TownManager(this);
        this.mallManager = new MallManager(this);
        this.townGui = new TownGui(this);
        this.mallGui = new MallGui(this);

        if (getConfig().getBoolean("settings.squaremap.enabled")
                && Bukkit.getPluginManager().getPlugin("squaremap") != null) {
            this.mapIntegration = new SquaremapIntegration(this);
        }

        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(townGui, this);
        Bukkit.getPluginManager().registerEvents(mallGui, this);

        getCommand("town").setExecutor(new TownCommand(this));
        getCommand("townadmin").setExecutor(new TownAdminCommand(this));
        getCommand("mall").setExecutor(new MallCommand(this));
        getCommand("stuck").setExecutor(new StuckCommand(this));

        getCommand("town").setTabCompleter(new TownTabCompleter(this));
        getCommand("townadmin").setTabCompleter(new TownAdminTabCompleter(this));
        getCommand("mall").setTabCompleter(new MallTabCompleter(this));
        getCommand("stuck").setTabCompleter(new StuckTabCompleter());

        mapIntegration.refreshAll();
    }

    @Override
    public void onDisable() {
        townManager.save();
        mallManager.save();
    }

    public TownManager getTownManager() {
        return townManager;
    }

    public MallManager getMallManager() {
        return mallManager;
    }

    public TownGui getTownGui() {
        return townGui;
    }

    public MallGui getMallGui() {
        return mallGui;
    }

    public MapIntegration getMapIntegration() {
        return mapIntegration;
    }

}
