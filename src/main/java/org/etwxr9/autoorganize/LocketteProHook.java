package org.etwxr9.autoorganize;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import me.crafter.mc.lockettepro.LocketteProAPI;

public class LocketteProHook {

    /**
     * 检查一个方块是否被 LockettePro 保护。
     * 
     * @param block 要检查的方块
     * @return 如果被保护，则返回 true
     */
    public boolean isProtected(Block block) {
        return LocketteProAPI.isProtected(block);
    }

    /**
     * 检查一个玩家是否有权限操作一个被保护的方块。
     * 
     * @param player 玩家
     * @param block  方块
     * @return 如果玩家是所有者或被允许，则返回 true
     */
    public boolean canAccess(Player player, Block block) {
        // 首先检查方块是否被保护
        if (!LocketteProAPI.isProtected(block)) {
            return true; // 如果没有被保护，任何人都可以访问
        }
        // 然后检查玩家是否是所有者或被添加了权限
        return (LocketteProAPI.isOwner(block, player) || LocketteProAPI.isUser(block, player));
    }

}
