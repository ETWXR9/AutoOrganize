package org.etwxr9.autoorganize;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import io.papermc.paper.math.BlockPosition;

/**
 * 展示框交互监听器
 * 监听玩家与展示框的右键交互，检测方块组合体并打开整理GUI
 */
public class ItemFrameInteractionListener implements Listener {
    
    private final AutoOrganize plugin;
    
    public ItemFrameInteractionListener(AutoOrganize plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // 检查是否为右键交互
        if (!event.getHand().equals(org.bukkit.inventory.EquipmentSlot.HAND)) {
            return;
        }
        
        // 检查交互的实体是否为展示框
        if (!(event.getRightClicked() instanceof ItemFrame)) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemFrame itemFrame = (ItemFrame) event.getRightClicked();
        
        // 进行三重检测
        if (validateBlockCombination(itemFrame, player)) {
            // 取消原始交互事件，防止展示框内容被修改
            event.setCancelled(true);
            
            // 获取方块组合体信息并打开GUI
            openOrganizeGUI(itemFrame, player);
        }
    }
    
    /**
     * 验证方块组合体是否符合要求
     * 1. 展示框内容是否是普通箱子
     * 2. 展示框贴着的方块是否是配置的上方方块（默认磁石）
     * 3. 上方方块下方是否是配置中的下方方块
     */
    private boolean validateBlockCombination(ItemFrame itemFrame, Player player) {
        // 检测1：展示框内容是否是普通箱子
        ItemStack frameItem = itemFrame.getItem();
        if (frameItem == null || frameItem.getType() != Material.CHEST) {
            return false;
        }
        
        // 获取展示框贴着的方块
        Block attachedBlock = getAttachedBlock(itemFrame);
        if (attachedBlock == null) {
            return false;
        }
        
        // 检测2：贴着的方块是否是配置的上方方块
        BlockCombinationConfig config = plugin.getBlockCombinationConfig();
        if (attachedBlock.getType() != config.getTopBlock()) {
            return false;
        }
        
        // 检测3：上方方块下方是否是配置中的下方方块
        Block bottomBlock = attachedBlock.getRelative(BlockFace.DOWN);
        if (!config.containsBottomBlock(bottomBlock.getType())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取展示框贴着的方块
     */
    private Block getAttachedBlock(ItemFrame itemFrame) {
        BlockFace attachedFace = itemFrame.getAttachedFace();
        if (attachedFace == null) {
            return null;
        }
        
        return itemFrame.getLocation().getBlock().getRelative(attachedFace);
    }
    
    /**
     * 打开整理GUI
     */
    private void openOrganizeGUI(ItemFrame itemFrame, Player player) {
        // 获取上方方块（磁石）
        Block topBlock = getAttachedBlock(itemFrame);
        if (topBlock == null) {
            return;
        }
        
        // 获取下方方块
        Block bottomBlock = topBlock.getRelative(BlockFace.DOWN);
        
        // 获取对应的检索范围
        BlockCombinationConfig config = plugin.getBlockCombinationConfig();
        Integer range = config.getRange(bottomBlock.getType());
        if (range == null) {
            return;
        }
        
        // 以下方方块为中心位置打开GUI
        BlockPosition centerPos = bottomBlock.getLocation().toBlock();
        
        // 打开整理GUI
        plugin.getGuiManager().openOrganizeGUI(player, centerPos, range);
    }
}
