/*
 * CozyWarps - Used to create player warps.
 * Copyright (C) 2024 CozyPlugins
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.cozyplugins.cozywarps.inventory;

import com.github.cozyplugins.cozylibrary.command.datatype.CommandArguments;
import com.github.cozyplugins.cozylibrary.inventory.InventoryInterface;
import com.github.cozyplugins.cozylibrary.inventory.InventoryItem;
import com.github.cozyplugins.cozylibrary.inventory.action.action.AnvilValueAction;
import com.github.cozyplugins.cozylibrary.inventory.action.action.ClickAction;
import com.github.cozyplugins.cozylibrary.inventory.action.action.ConfirmAction;
import com.github.cozyplugins.cozylibrary.inventory.inventory.AnvilInputInventory;
import com.github.cozyplugins.cozylibrary.inventory.inventory.ConfirmationInventory;
import com.github.cozyplugins.cozylibrary.user.PlayerUser;
import com.github.cozyplugins.cozywarps.CozyWarps;
import com.github.cozyplugins.cozywarps.Warp;
import com.github.cozyplugins.cozywarps.WarpVisit;
import com.github.cozyplugins.cozywarps.command.WarpsCreateCommand;
import com.github.smuddgge.squishyconfiguration.memory.MemoryConfigurationSection;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

/**
 * Represents the warp's inventory.
 * Contains a list of all the possible warps.
 */
public class WarpsInventory extends InventoryInterface {

    private int page;

    /**
     * Used to create an instance of the warp's
     * inventory.
     */
    public WarpsInventory() {
        super(54, "&f₴₴₴₴₴₴₴₴⁌");
        this.page = 0;
    }

    @Override
    protected void onGenerate(PlayerUser player) {

        final int amountOfWarpsOwned = CozyWarps.getInstance().getAmountOwned(player.getUuid());
        final int cost = CozyWarps.getInstance().getPrice(amountOfWarpsOwned + 1);

        // Create button.
        this.setItem(new InventoryItem()
                .setMaterial(Material.PINK_STAINED_GLASS_PANE)
                .setCustomModelData(1)
                .setName("&a&lCreate")
                .setLore("&7Click to create a warp",
                        "&7where you are standing.",
                        "&7",
                        "&aCost &f{0} coins"
                                .replace("{0}", Integer.toString(cost))
                )
                .addAction(new AnvilValueAction()
                        .setAnvilTitle("&8&lWarp Name")
                        .setAction((value, user) -> {
                            if (value == null) {
                                user.sendMessage("&7&l> &7Aborted warp creation.");
                                return;
                            }
                            CozyWarps.getInstance().createWarp(user, value, user.getPlayer().getLocation());
                        })
                )
                .addSlot(45, 46, 47)
        );

        // Help button.
        this.setItem(new InventoryItem()
                .setMaterial(Material.PINK_STAINED_GLASS_PANE)
                .setCustomModelData(1)
                .setName("&b&lHelp")
                .setLore("&7Player warps let other players teleport",
                        "&7to locations you have chosen.",
                        "&7",
                        "&a/warps &fTo list all warps.",
                        "&a/warps create <name> &fTo create a warp.",
                        "&a/warps delete <name> &fTo delete a warp.",
                        "&a/warps edit <name> &fTo edit a warp.",
                        "&a/warps ban <player> &fTo ban a player from your warps.",
                        "&a/warps unban <player> &fTo unban a player from your warps.",
                        "&7",
                        "&7Visits are counted as every unique player",
                        "&7visiting your warp every hour.")
                .addSlot(49)
        );

        // My warp's button.
        this.setItem(new InventoryItem()
                .setMaterial(Material.PINK_STAINED_GLASS_PANE)
                .setCustomModelData(1)
                .setName("&d&lMy Warps")
                .setLore("&7Click to view your warps!")
                .addSlot(51, 52, 53)
                .addAction((ClickAction) (user, type, inventory) -> {
                    new MyWarpsInventory(user.getUuid()).open(user.getPlayer());
                })
        );

        // Back arrow.
        this.setItem(new InventoryItem()
                .setMaterial(Material.PINK_STAINED_GLASS_PANE)
                .setCustomModelData(1)
                .setName("&a&lLast Page")
                .setLore("&7Click to go to the page after.",
                        "&7",
                        "&fCurrent Page &a" + this.page)
                .addSlot(48)
                .addAction((ClickAction) (user, type, inventory) -> {
                    if (this.page == 0) return;
                    this.page += 1;
                    this.onGenerate(player);
                })
        );

        // Next arrow.
        this.setItem(new InventoryItem()
                .setMaterial(Material.PINK_STAINED_GLASS_PANE)
                .setCustomModelData(1)
                .setName("&a&lNext Page")
                .setLore("&7Click to go to the page after.",
                        "&7",
                        "&fCurrent Page &a" + this.page)
                .addSlot(50)
                .addAction((ClickAction) (user, type, inventory) -> {
                    if (this.getMaxPages() == this.page) return;
                    this.page -= 1;
                    this.onGenerate(player);
                })
        );

        // Add all the warps.
        this.addAllWarps(player);
    }

    /**
     * Used to add all the warps to the inventory given
     * the player. If the player is banned from a warp it
     * will show the icon but with a banned lore message.
     *
     * @param player The instance of the player
     * @return This instance.
     */
    public @NotNull WarpsInventory addAllWarps(PlayerUser player) {

        int warpNumber = 0;
        int startingSlot = this.page * 45;
        int endingSlot = (this.page + 1) * 45;

        List<Warp> list = CozyWarps.getInstance().getAllWarps();
        list.sort(Warp::compareTo);

        // Loop though all the warps.
        for (Warp warp : list) {

            // Check if the warp number is smaller than the
            // starting slot.
            if (warpNumber < startingSlot) {
                warpNumber++;
                continue;
            }

            // Check if the warp number is equal to the ending
            // slot, then finish creating the warps.
            if (warpNumber == endingSlot) {
                return this;
            }

            // Create the base item.
            InventoryItem item = warp.createInventoryItem()
                    .addSlot(warpNumber % 45);

            // Check if the player is banned.
            if (CozyWarps.getInstance().isBanned(player.getUuid(), warp.getOwnerUuid())) {
                item.setLore("&7You are banned from this warp.");
            } else {
                item.addAction((ClickAction) (user, type, inventory) -> {
                    player.sendMessage("&7&l> &7Teleporting to " + warp.getName() + "...");
                    warp.teleport(player);

                    if (!CozyWarps.getInstance().hasVisited(warp.getIdentifier(), user.getUuid())) {

                        // Add a warp visit to the recent list.
                        CozyWarps.getInstance().addWarpVisit(
                                new WarpVisit(warp.getIdentifier(), user.getUuid())
                        );

                        // Increment the number of times visited.
                        warp.incrementVisits();
                        warp.save();
                    }
                });
            }

            // Otherwise, add the warp item.
            this.setItem(item);
            warpNumber++;
        }

        return this;
    }

    /**
     * Used to get the maximum number of pages.
     *
     * @return The maximum number of pages.
     */
    public int getMaxPages() {
        return CozyWarps.getInstance().getAllWarps().size() / 45;
    }
}
