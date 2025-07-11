package org.etwxr9.autoorganize;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * 方块组合体配置管理类
 * 管理上方方块类型和下方方块类型及其对应的检索范围
 */
public class BlockCombinationConfig {
    
    private Material topBlock;
    private Map<Material, Integer> bottomBlockRanges;
    private int yRadius;

    public BlockCombinationConfig() {
        this.topBlock = Material.LODESTONE; // 默认上方方块为磁石
        this.bottomBlockRanges = new HashMap<>();
        this.yRadius = 5; // 默认Y轴半径为5
        
        // 设置默认的下方方块配置
        this.bottomBlockRanges.put(Material.DIAMOND_BLOCK, 50);
        this.bottomBlockRanges.put(Material.EMERALD_BLOCK, 30);
        this.bottomBlockRanges.put(Material.GOLD_BLOCK, 20);
        this.bottomBlockRanges.put(Material.IRON_BLOCK, 15);
    }
    
    /**
     * 从配置文件加载设置
     */
    public void loadFromConfig(ConfigurationSection config) {
        if (config == null) {
            return;
        }
        
        // 加载上方方块类型
        String topBlockName = config.getString("top_block", "LODESTONE");
        try {
            this.topBlock = Material.valueOf(topBlockName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 如果配置的方块类型无效，使用默认值
            this.topBlock = Material.LODESTONE;
        }

        // 加载Y轴半径
        this.yRadius = config.getInt("y_radius", 5);
        
        // 加载下方方块类型及其范围
        ConfigurationSection bottomBlocksSection = config.getConfigurationSection("bottom_blocks");
        if (bottomBlocksSection != null) {
            this.bottomBlockRanges.clear();
            for (String blockName : bottomBlocksSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(blockName.toUpperCase());
                    int range = bottomBlocksSection.getInt(blockName, 15);
                    this.bottomBlockRanges.put(material, range);
                } catch (IllegalArgumentException e) {
                    // 忽略无效的方块类型
                }
            }
        }
    }
    
    /**
     * 保存配置到配置文件
     */
    public void saveToConfig(ConfigurationSection config) {
        if (config == null) {
            return;
        }
        
        // 保存上方方块类型
        config.set("top_block", topBlock.name());

        // 保存Y轴半径
        config.set("y_radius", yRadius);

        // 保存下方方块类型及其范围
        ConfigurationSection bottomBlocksSection = config.createSection("bottom_blocks");
        for (Map.Entry<Material, Integer> entry : bottomBlockRanges.entrySet()) {
            bottomBlocksSection.set(entry.getKey().name(), entry.getValue());
        }
    }
    
    /**
     * 获取上方方块类型
     */
    public Material getTopBlock() {
        return topBlock;
    }
    
    /**
     * 设置上方方块类型
     */
    public void setTopBlock(Material topBlock) {
        this.topBlock = topBlock;
    }

    /**
     * 获取Y轴搜索半径
     */
    public int getYRadius() {
        return yRadius;
    }

    /**
     * 设置Y轴搜索半径
     */
    public void setYRadius(int yRadius) {
        this.yRadius = Math.max(1, yRadius); // 确保至少为1
    }
    
    /**
     * 获取下方方块类型及其范围的映射
     */
    public Map<Material, Integer> getBottomBlockRanges() {
        return new HashMap<>(bottomBlockRanges);
    }
    
    /**
     * 获取指定下方方块的检索范围
     */
    public Integer getRange(Material bottomBlock) {
        return bottomBlockRanges.get(bottomBlock);
    }
    
    /**
     * 添加或更新下方方块配置
     */
    public void setBottomBlockRange(Material bottomBlock, int range) {
        bottomBlockRanges.put(bottomBlock, range);
    }
    
    /**
     * 移除下方方块配置
     */
    public boolean removeBottomBlock(Material bottomBlock) {
        return bottomBlockRanges.remove(bottomBlock) != null;
    }
    
    /**
     * 检查是否包含指定的下方方块
     */
    public boolean containsBottomBlock(Material bottomBlock) {
        return bottomBlockRanges.containsKey(bottomBlock);
    }
    
    /**
     * 获取所有配置的下方方块类型
     */
    public Material[] getBottomBlocks() {
        return bottomBlockRanges.keySet().toArray(new Material[0]);
    }
    
    /**
     * 清空所有下方方块配置
     */
    public void clearBottomBlocks() {
        bottomBlockRanges.clear();
    }
    
    /**
     * 获取下方方块配置数量
     */
    public int getBottomBlockCount() {
        return bottomBlockRanges.size();
    }
}
