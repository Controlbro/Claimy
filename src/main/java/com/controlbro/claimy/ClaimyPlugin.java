package com.controlbro.claimy;

import com.controlbro.claimy.commands.MallCommand;
import com.controlbro.claimy.commands.StuckCommand;
import com.controlbro.claimy.commands.TownAdminCommand;
import com.controlbro.claimy.commands.TownCommand;
import com.controlbro.claimy.gui.TownGui;
import com.controlbro.claimy.listeners.ProtectionListener;
import com.controlbro.claimy.managers.MallManager;
import com.controlbro.claimy.managers.TownManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ClaimyPlugin extends JavaPlugin {
    private TownManager townManager;
    private MallManager mallManager;
    private TownGui townGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("gui.yml", false);

        this.townManager = new TownManager(this);
        this.mallManager = new MallManager(this);
        this.townGui = new TownGui(this);

        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(townGui, this);

        getCommand("town").setExecutor(new TownCommand(this));
        getCommand("townadmin").setExecutor(new TownAdminCommand(this));
        getCommand("mall").setExecutor(new MallCommand(this));
        getCommand("stuck").setExecutor(new StuckCommand(this));
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

}
