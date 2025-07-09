package org.etwxr9.autoorganize;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;

public class CoHook {

    private CoreProtectAPI coreProtect;

    private boolean isCoEnabled = false;

    public boolean isCoEnabled() {
        return isCoEnabled;
    }

    CoHook() {
        this.coreProtect = getCoreProtect();
        if (coreProtect != null) {
            this.isCoEnabled = true;
        }
    }

    private CoreProtectAPI getCoreProtect() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (plugin == null || !(plugin instanceof CoreProtect)) {
            return null;
        }

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) plugin).getAPI();
        if (CoreProtect.isEnabled() == false) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 10) {
            return null;
        }

        return CoreProtect;
    }

    public boolean addItemCo(Inventory inventory, Player player, ItemStack itemStack) {
        boolean success = coreProtect.logContainerTransaction(player.getName(), inventory.getLocation());
        if (success) {
            inventory.addItem(itemStack);
        }
        return success; // 返回成功状态
    }

    /**
     * 记录容器事务并执行精确的物品放置操作
     * @param inventory 目标容器
     * @param player 执行操作的玩家
     * @param itemStack 要放置的物品
     * @param slot 目标槽位
     * @param amount 要放置的数量
     * @return 是否成功记录和放置
     */
    public boolean logAndSetItem(Inventory inventory, Player player, int slot, ItemStack itemStack) {
        // 记录容器事务
        boolean success = coreProtect.logContainerTransaction(player.getName(), inventory.getLocation());
        if (success) {
            // 如果记录成功，执行物品放置
            inventory.setItem(slot, itemStack);
        }
        return success;
    }

    /**
     * 记录容器事务但不执行物品操作（用于手动控制物品放置）
     * @param inventory 目标容器
     * @param player 执行操作的玩家
     * @return 是否成功记录
     */
    public boolean logContainerAccess(Inventory inventory, Player player) {
        return coreProtect.logContainerTransaction(player.getName(), inventory.getLocation());
    }

}
