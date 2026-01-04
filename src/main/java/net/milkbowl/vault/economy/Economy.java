package net.milkbowl.vault.economy;

import org.bukkit.OfflinePlayer;

import java.util.List;

public interface Economy {
    boolean isEnabled();

    String getName();

    boolean hasBankSupport();

    int fractionalDigits();

    String format(double amount);

    String currencyNamePlural();

    String currencyNameSingular();

    boolean hasAccount(OfflinePlayer player);

    boolean hasAccount(OfflinePlayer player, String worldName);

    boolean hasAccount(String playerName);

    boolean hasAccount(String playerName, String worldName);

    double getBalance(OfflinePlayer player);

    double getBalance(OfflinePlayer player, String world);

    double getBalance(String playerName);

    double getBalance(String playerName, String world);

    boolean has(OfflinePlayer player, double amount);

    boolean has(OfflinePlayer player, String worldName, double amount);

    boolean has(String playerName, double amount);

    boolean has(String playerName, String worldName, double amount);

    EconomyResponse withdrawPlayer(OfflinePlayer player, double amount);

    EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount);

    EconomyResponse withdrawPlayer(String playerName, double amount);

    EconomyResponse withdrawPlayer(String playerName, String worldName, double amount);

    EconomyResponse depositPlayer(OfflinePlayer player, double amount);

    EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount);

    EconomyResponse depositPlayer(String playerName, double amount);

    EconomyResponse depositPlayer(String playerName, String worldName, double amount);

    EconomyResponse createBank(String name, String player);

    EconomyResponse createBank(String name, OfflinePlayer player);

    EconomyResponse deleteBank(String name);

    EconomyResponse bankBalance(String name);

    EconomyResponse bankHas(String name, double amount);

    EconomyResponse bankWithdraw(String name, double amount);

    EconomyResponse bankDeposit(String name, double amount);

    EconomyResponse isBankOwner(String name, String player);

    EconomyResponse isBankOwner(String name, OfflinePlayer player);

    EconomyResponse isBankMember(String name, String player);

    EconomyResponse isBankMember(String name, OfflinePlayer player);

    List<String> getBanks();

    boolean createPlayerAccount(OfflinePlayer player);

    boolean createPlayerAccount(OfflinePlayer player, String worldName);

    boolean createPlayerAccount(String playerName);

    boolean createPlayerAccount(String playerName, String worldName);
}
