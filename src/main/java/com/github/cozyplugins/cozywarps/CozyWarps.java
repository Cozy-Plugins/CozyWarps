package com.github.cozyplugins.cozywarps;

import com.github.cozyplugins.cozylibrary.CozyPlugin;
import com.github.cozyplugins.cozywarps.command.WarpsCommand;
import com.github.smuddgge.squishyconfiguration.ConfigurationFactory;
import com.github.smuddgge.squishyconfiguration.interfaces.Configuration;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents the main class.
 */
public final class CozyWarps extends CozyPlugin {

    private static @NotNull CozyWarps instance;

    private @NotNull Configuration storage;
    private @NotNull Configuration config;
    private @NotNull List<WarpVisit> warpVisitList = new ArrayList<>();

    @Override
    public boolean enableCommandDirectory() {
        return true;
    }

    @Override
    public void onCozyEnable() {

        // Initialise the warp visit list.
        this.warpVisitList = new ArrayList<>();

        // Create the instance of the storage configuration.
        this.storage = ConfigurationFactory.YAML.create(this.getDataFolder(), "storage");
        this.storage.load();

        // Create the instance of the config file.
        this.config = ConfigurationFactory.YAML.create(this.getDataFolder(), "config");
        this.config.setDefaultPath("src/main/resources/config.yml");
        this.config.load();

        // Initialise the instance getter.
        CozyWarps.instance = this;

        // Add all the command types.
        this.addCommandType(new WarpsCommand());
    }

    /**
     * Used to get the configuration file.
     *
     * @return The configuration file.
     */
    public @NotNull Configuration getCozyWarpsConfig() {
        return this.config;
    }

    /**
     * Used to get the price a warp would cost.
     *
     * @param warpNumber The number warp they are buying.
     * @return The price of the warp.
     */
    public int getPrice(int warpNumber) {
        return this.config.getInteger("warp." + warpNumber, -1);
    }

    /**
     * Used to get a warp from storage given its
     * unique identifier.
     *
     * @param identifier The warps unique identifier.
     * @return The optional warp.
     */
    public @NotNull Optional<Warp> getWarp(@NotNull UUID identifier) {
        if (this.storage.getKeys().contains(identifier.toString())) {
            return Optional.of(new Warp(identifier).convert(this.storage.getSection(identifier.toString())));
        }

        return Optional.empty();
    }

    /**
     * Used to get a warp a player owns.
     * A player cannot have two warps named the same.
     *
     * @param playerUuid The player's uuid.
     * @param warpName The warp's name.
     * @return The optional warp.
     */
    public @NotNull Optional<Warp> getWarp(@NotNull UUID playerUuid, @NotNull String warpName) {
        for (Warp warp : this.getAllWarps()) {
            if (warp.getOwnerUuid() != playerUuid) continue;
            if (!warp.getName().equals(warpName)) continue;
            return Optional.of(warp);
        }
        return Optional.empty();
    }

    /**
     * Used to get the list of all warps.
     *
     * @return The list of all warps.
     */
    public @NotNull List<Warp> getAllWarps() {
        List<Warp> list = new ArrayList<>();

        // Loop though all keys.
        for (String key : this.storage.getKeys()) {
            list.add(new Warp(UUID.fromString(key)).convert(this.storage.getSection(key)));
        }

        return list;
    }

    /**
     * Used to get all warps a player owns.
     *
     * @param playerUuid The player uuid.
     * @return The list of their warps.
     */
    public @NotNull List<Warp> getAllWarps(@NotNull UUID playerUuid) {
        List<Warp> list = new ArrayList<>();
        for (Warp warp : this.getAllWarps()) {
            if (warp.getOwnerUuid() == playerUuid) list.add(warp);
        }
        return list;
    }

    /**
     * Used to get the number of warps owned by a specific player.
     *
     * @param uuid The players uuid.
     * @return The number of warps they own.
     */
    public int getAmountOwned(@NotNull UUID uuid) {
        int amountOwned = 0;
        for (Warp warp : this.getAllWarps()) {
            if (warp.getOwnerUuid() == uuid) amountOwned++;
        }
        return amountOwned;
    }

    /**
     * Used to get the warp owners names.
     *
     * @return The names of the warp owners.
     */
    public @NotNull List<String> getOwnerNames() {
        List<String> list = new ArrayList<>();
        for (Warp warp : this.getAllWarps()) {
            if (list.contains(warp.getOwnerName())) continue;
            list.add(warp.getOwnerName());
        }
        return list;
    }

    /**
     * Used to add a warp visit to the visit list.
     *
     * @param warpVisit The instance of the warp visit.
     * @return This instance.
     */
    public @NotNull CozyWarps addWarpVisit(@NotNull WarpVisit warpVisit) {
        this.warpVisitList.add(warpVisit);
        return this;
    }

    /**
     * Used to remove all the warp visits registered.
     *
     * @return This instance.
     */
    public @NotNull CozyWarps removeWarpVisits() {
        this.warpVisitList = new ArrayList<>();
        return this;
    }

    /**
     * Used to check if the list of visits contains a certain
     * warp uuid and player uuid.
     *
     * @param warpUuid The warp uuid to check.
     * @param playerUuid The player uuid to check.
     * @return True if they have visited recently.
     */
    public boolean hasVisited(@NotNull UUID warpUuid, @NotNull UUID playerUuid) {
        for (WarpVisit warpVisit : this.warpVisitList) {
            if (warpVisit.isWarpUuid(warpUuid)
                    && warpVisit.isPlayerUuid(playerUuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Used to update or insert a warp in storage.
     *
     * @param warp The instance of the warp to update or insert.
     * @return This instance.
     */
    public @NotNull CozyWarps updateWarp(@NotNull Warp warp) {
        this.storage.set(warp.getIdentifier().toString(), warp.convert().getMap());
        this.storage.save();
        return this;
    }

    /**
     * Used to remove a warp from storage.
     *
     * @param playerName The player's name.
     * @param warpName The name of the warp.
     * @return This instance.
     */
    public @NotNull CozyWarps removeWarp(String playerName, String warpName) {
        for (Warp warp : this.getAllWarps()) {
            if (Bukkit.getOfflinePlayer(playerName).getUniqueId() != warp.getOwnerUuid()) continue;
            if (!warpName.equals(warp.getName())) continue;
            this.storage.set(warp.getIdentifier().toString(), null);
            this.storage.save();
            return this;
        }
        return this;
    }

    /**
     * Used to start the bukkit task of removing
     * visits every hour.
     */
    public void startVisitRemovingTask() {

        // Create the task.
        Bukkit.getScheduler().runTaskTimer(
                this,
                this::removeWarpVisits,
                72000,
                72000
        );
    }

    /**
     * Used to get the instance of the plugin.
     *
     * @return The instance of the plugin.
     */
    public static @NotNull CozyWarps getInstance() {
        return CozyWarps.instance;
    }
}
