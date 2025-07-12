package org.etwxr9.autoorganize;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import io.papermc.paper.math.BlockPosition;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步整理任务类 - 处理分批执行的整理逻辑
 * 使用同步任务避免异步访问世界数据的问题，通过分批处理实现性能负载均衡
 */
public class OrganizeTask extends BukkitRunnable {

    private final AutoOrganize plugin;
    private final Player player;
    private final Location loc;
    private final int range;
    private final int yRadius;
    private final List<ItemStack> itemsToOrganize;

    private List<OrganizeAlgorithm.ContainerInfo> containers;
    private int currentItemIndex = 0;
    private final List<ItemStack> remainingItems;

    // 任务执行阶段
    private TaskPhase currentPhase = TaskPhase.FIND_CONTAINERS;

    // 从配置文件读取的值
    private final int BLOCKS_SCAN_PER_TICK; // 每tick扫描的方块数量

    // 容器搜索相关变量
    private int scanX, scanY, scanZ;
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private boolean scanInitialized = false;

    /**
     * 任务执行阶段枚举
     */
    private enum TaskPhase {
        FIND_CONTAINERS, // 查找容器阶段
        ORGANIZE_ITEMS, // 整理物品阶段
        FINISH // 完成阶段
    }

    public OrganizeTask(AutoOrganize plugin, Player player, BlockPosition pos, int range, ItemStack[] items) {
        this.plugin = plugin;
        this.player = player;
        this.loc = pos.toLocation(player.getWorld());
        this.range = range;
        this.yRadius = plugin.getBlockCombinationConfig().getYRadius();
        this.itemsToOrganize = new ArrayList<>();
        this.remainingItems = new ArrayList<>();

        // 从配置文件读取性能参数
        this.BLOCKS_SCAN_PER_TICK = plugin.getBlocksPerTick();

        // 过滤掉空物品
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                this.itemsToOrganize.add(item.clone());
            }
        }
    }

    @Override
    public void run() {
        try {
            // 检查玩家是否仍然有效
            if (!player.isOnline()) {
                this.cancel();
                plugin.getLogger().info("玩家 " + player.getName() + " 在整理过程中离线，物品将掉落在原地");
                for (ItemStack item : itemsToOrganize) {
                    if (item != null && item.getType() != Material.AIR) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
                return;
            }

            // 根据当前阶段执行相应的处理
            switch (currentPhase) {
                case FIND_CONTAINERS:
                    findContainersPhase();
                    break;
                case ORGANIZE_ITEMS:
                    organizeItemsPhase();
                    break;
                case FINISH:
                    finishOrganizing();
                    break;
            }

        } catch (Exception e) {
            // 错误处理
            plugin.getLogger().severe("整理任务执行出错: " + e.getMessage());
            e.printStackTrace();

            if (player.isOnline()) {
                plugin.sendMessage(player, plugin.getMsgErrorOccurred());
                // 返回所有物品给玩家
                List<ItemStack> allItems = new ArrayList<>(itemsToOrganize);
                allItems.addAll(remainingItems);
                OrganizeAlgorithm.returnItemsToPlayer(player, allItems, plugin);
            } else {
                for (ItemStack item : itemsToOrganize) {
                    if (item != null && item.getType() != Material.AIR) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
            }

            this.cancel();
        }
    }

    /**
     * 第一阶段：分批查找容器
     */
    private void findContainersPhase() {
        // 初始化扫描范围（只在第一次执行时）
        if (!scanInitialized) {
            initializeScan();
        }

        // 分批扫描方块
        int scannedBlocks = 0;
        while (scannedBlocks < BLOCKS_SCAN_PER_TICK && hasMoreBlocksToScan()) {
            scanCurrentBlock();
            moveToNextBlock();
            scannedBlocks++;
        }

        // 如果扫描完成，进入下一阶段
        if (!hasMoreBlocksToScan()) {
            if (containers.isEmpty()) {
                plugin.sendMessage(player, plugin.getMsgNoContainers());
                OrganizeAlgorithm.returnItemsToPlayer(player, itemsToOrganize, plugin);
                this.cancel();
                return;
            }

            plugin.sendMessage(player, plugin.getMsgContainersFound(), "count", String.valueOf(containers.size()));
            currentPhase = TaskPhase.ORGANIZE_ITEMS;
        }
    }

    /**
     * 初始化扫描参数
     */
    private void initializeScan() {
        Location center = loc;
        minX = center.getBlockX() - range;
        minY = Math.max(center.getBlockY() - yRadius, center.getWorld().getMinHeight());
        minZ = center.getBlockZ() - range;
        maxX = center.getBlockX() + range;
        maxY = Math.min(center.getBlockY() + yRadius, center.getWorld().getMaxHeight());
        maxZ = center.getBlockZ() + range;

        scanX = minX;
        scanY = minY;
        scanZ = minZ;

        containers = new ArrayList<>();
        scanInitialized = true;

        // 计算预计tick数并发送消息
        long totalBlocks = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        long estimatedTicks = (totalBlocks + BLOCKS_SCAN_PER_TICK - 1) / BLOCKS_SCAN_PER_TICK; // 向上取整

        plugin.sendMessage(player,
                plugin.getMsgSearchContainers().replace("{estimated_ticks}", String.valueOf(estimatedTicks)));
    }

    /**
     * 检查是否还有更多方块需要扫描
     */
    private boolean hasMoreBlocksToScan() {
        return scanX <= maxX;
    }

    /**
     * 扫描当前方块
     */
    private void scanCurrentBlock() {
        Block block = loc.getWorld().getBlockAt(scanX, scanY, scanZ);

        // 检查是否为容器方块
        if (OrganizeAlgorithm.isContainerBlock(block.getType())) {
            BlockState state = block.getState();
            if (state instanceof InventoryHolder) {
                InventoryHolder holder = (InventoryHolder) state;
                // 执行lockette pro牌子锁检测
                if (plugin.isLocketteProEnabled()) {
                    if (!plugin.getLocketteProHook().canAccess(player, block)) {
                        return;
                    }
                }
                containers.add(new OrganizeAlgorithm.ContainerInfo(block.getLocation(), holder.getInventory()));
            }
        }
    }

    /**
     * 移动到下一个方块
     */
    private void moveToNextBlock() {
        scanZ++;
        if (scanZ > maxZ) {
            scanZ = minZ;
            scanY++;
            if (scanY > maxY) {
                scanY = minY;
                scanX++;
            }
        }
    }

    /**
     * 第二阶段：处理物品整理
     */
    private void organizeItemsPhase() {
        // int processedItems = 0;

        // 处理当前批次的物品
        while (currentItemIndex < itemsToOrganize.size()) {
            ItemStack currentItem = itemsToOrganize.get(currentItemIndex);

            if (currentItem != null && currentItem.getType() != Material.AIR) {
                // 使用新的方法尝试放置物品并获取结果信息
                OrganizeAlgorithm.PlacementResult result = OrganizeAlgorithm.tryPlaceItemWithVisualEffect(
                        currentItem, containers, loc, player, plugin);

                // 如果成功放入了物品，创建视觉效果
                if (result.hasPlacedItem()) {
                    ItemFlyingEffect.createAndStart(plugin, result.getPlacedItem(), loc, result.getTargetLocation());
                }

                // 如果有剩余物品，添加到剩余列表
                if (result.getRemainingItem() != null && result.getRemainingItem().getAmount() > 0) {
                    remainingItems.add(result.getRemainingItem());
                }
            }

            currentItemIndex++;
            // processedItems++;
        }

        // 更新进度
        final int finalCurrentIndex = currentItemIndex;
        final int totalItems = itemsToOrganize.size();
        if (player.isOnline()) {
            int progress = (finalCurrentIndex * 100) / totalItems;
            plugin.sendMessage(player, plugin.getMsgOrganizingProgress(),
                    "progress", String.valueOf(progress),
                    "current", String.valueOf(finalCurrentIndex),
                    "total", String.valueOf(totalItems));
        }

        // 如果所有物品都处理完了，进入完成阶段
        if (currentItemIndex >= itemsToOrganize.size()) {
            currentPhase = TaskPhase.FINISH;
            finishOrganizing();
        }
    }

    /**
     * 第三阶段：完成整理
     */
    private void finishOrganizing() {

        int organizedCount = itemsToOrganize.size() - remainingItems.size();
        int remainingCount = remainingItems.size();
        if (player.isOnline()) {
            plugin.sendMessage(player, plugin.getMsgOrganizeComplete());
        }
        if (remainingCount > 0) {
            if (player.isOnline()) {
                plugin.sendMessage(player, plugin.getMsgItemsOrganized(), "count", String.valueOf(organizedCount));
                plugin.sendMessage(player, plugin.getMsgItemsRemaining(), "count", String.valueOf(remainingCount));
                // 返回剩余物品给玩家
                OrganizeAlgorithm.returnItemsToPlayer(player, remainingItems, plugin);
            } else {
                plugin.getLogger().info("玩家 " + player.getName() + " 在整理过程中离线，剩余物品将掉落在原地");
                for (ItemStack item : remainingItems) {
                    if (item != null && item.getType() != Material.AIR) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
            }

        } else {
            plugin.sendMessage(player, plugin.getMsgAllItemsOrganized());
        }

        // 取消任务
        this.cancel();
    }
}
