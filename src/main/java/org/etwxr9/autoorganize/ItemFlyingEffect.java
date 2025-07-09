package org.etwxr9.autoorganize;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

/**
 * 物品飞行视觉效果类
 * 创建ItemDisplay实体从起始位置飞向目标容器位置
 */
public class ItemFlyingEffect extends BukkitRunnable {
    
    private final AutoOrganize plugin;
    private final ItemDisplay itemDisplay;
    private final Location startLocation;
    private final Location targetLocation;
    private final Vector direction;
    private final double totalDistance;
    
    private int tickCount = 0;
    private static final int FLIGHT_DURATION_TICKS = 30; // 飞行持续时间（ticks）
    private static final double DECELERATION_FACTOR = 0.8; // 减速因子
    
    /**
     * 创建物品飞行效果
     * @param plugin 插件实例
     * @param itemStack 要显示的物品
     * @param startLocation 起始位置
     * @param targetLocation 目标位置
     */
    public ItemFlyingEffect(AutoOrganize plugin, ItemStack itemStack, Location startLocation, Location targetLocation) {
        this.plugin = plugin;
        this.startLocation = startLocation.clone().add(0.5, 0.5, 0.5); // 居中显示
        this.targetLocation = targetLocation.clone().add(0.5, 0.5, 0.5); // 居中显示
        
        // 计算方向向量和总距离
        this.direction = this.targetLocation.toVector().subtract(this.startLocation.toVector());
        this.totalDistance = this.direction.length();
        this.direction.normalize();
        
        // 创建ItemDisplay实体
        this.itemDisplay = startLocation.getWorld().spawn(this.startLocation, ItemDisplay.class);
        this.itemDisplay.setItemStack(itemStack);
        
        // 设置显示属性
        setupDisplayProperties();
    }
    
    /**
     * 设置ItemDisplay的显示属性
     */
    private void setupDisplayProperties() {
        // 设置显示模式为固定大小
        itemDisplay.setBillboard(Display.Billboard.CENTER);
        
        // 设置缩放
        // itemDisplay.setDisplayWidth(0.3f);
        // itemDisplay.setDisplayHeight(0.3f);
        var t = itemDisplay.getTransformation();
        t.getScale().set(0.3f);
        itemDisplay.setTransformation(t);
        
        // 设置发光效果
        // itemDisplay.setGlowing(true);
        
        // 设置插值持续时间（平滑移动）
        itemDisplay.setTeleportDuration(1);
        itemDisplay.setInterpolationDelay(0);
    }
    
    /**
     * 开始飞行动画
     */
    public void startFlying() {
        // 每tick执行一次，持续FLIGHT_DURATION_TICKS个tick
        this.runTaskTimer(plugin, 0L, 1L);
    }
    
    @Override
    public void run() {
        tickCount++;
        
        // 检查是否到达目标或超时
        if (tickCount >= FLIGHT_DURATION_TICKS) {
            finishFlying();
            return;
        }
        
        // 计算当前进度（0.0 到 1.0）
        double progress = (double) tickCount / FLIGHT_DURATION_TICKS;
        
        // 应用减速曲线（缓出效果）
        double easedProgress = applyEaseOutCurve(progress);
        
        // 计算当前位置
        Location currentLocation = calculateCurrentPosition(easedProgress);
        
        // 移动ItemDisplay到新位置
        itemDisplay.teleport(currentLocation);
        
        // 添加旋转效果
        // addRotationEffect();
    }
    
    /**
     * 应用缓出曲线，实现减速效果
     * @param progress 原始进度 (0.0 - 1.0)
     * @return 应用缓出效果后的进度
     */
    private double applyEaseOutCurve(double progress) {
        // 使用二次缓出函数: f(t) = 1 - (1-t)²
        return 1 - Math.pow(1 - progress, 2);
    }
    
    /**
     * 根据进度计算当前位置
     * @param progress 进度 (0.0 - 1.0)
     * @return 当前位置
     */
    private Location calculateCurrentPosition(double progress) {
        // 线性插值计算位置
        Vector currentVector = startLocation.toVector()
            .add(direction.clone().multiply(totalDistance * progress));
        
        Location currentLocation = startLocation.clone();
        currentLocation.setX(currentVector.getX());
        currentLocation.setY(currentVector.getY());
        currentLocation.setZ(currentVector.getZ());
        
        // 添加轻微的弧形轨迹（可选）
        double arcHeight = 0.3 * Math.sin(progress * Math.PI);
        currentLocation.add(0, arcHeight, 0);
        
        return currentLocation;
    }
    
    /**
     * 添加旋转效果
     */
    private void addRotationEffect() {
        // 让物品在飞行过程中缓慢旋转
        float rotationSpeed = 5.0f; // 度/tick
        float currentRotation = tickCount * rotationSpeed;
        
        // 设置Y轴旋转
        itemDisplay.setRotation(currentRotation, 0);
    }
    
    /**
     * 完成飞行，清理资源
     */
    private void finishFlying() {
        // 移除ItemDisplay实体
        if (itemDisplay != null && !itemDisplay.isDead()) {
            itemDisplay.remove();
        }
        
        // 取消任务
        this.cancel();
    }
    
    /**
     * 静态方法：创建并启动物品飞行效果
     * @param plugin 插件实例
     * @param itemStack 要显示的物品
     * @param startLocation 起始位置
     * @param targetLocation 目标位置
     */
    public static void createAndStart(AutoOrganize plugin, ItemStack itemStack, Location startLocation, Location targetLocation) {
        // 检查是否启用了视觉效果
        if (!plugin.isVisualEffectsEnabled()) {
            return;
        }

        ItemFlyingEffect effect = new ItemFlyingEffect(plugin, itemStack, startLocation, targetLocation);
        effect.startFlying();
    }
}
