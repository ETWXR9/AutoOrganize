package org.etwxr9.autoorganize;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 整理算法类 - 处理物品分配逻辑
 */
public class OrganizeAlgorithm {

    /**
     * 在指定范围内查找所有容器
     * 
     * @deprecated
     */
    public static List<ContainerInfo> findContainers(Entity centerEntity, int range) {
        return findContainers(centerEntity.getLocation(), range);
    }

    /**
     * 在指定范围内查找所有容器（基于位置）
     * 
     * @deprecated
     */
    public static List<ContainerInfo> findContainers(Location center, int range) {
        List<ContainerInfo> containers = new ArrayList<>();
        World world = center.getWorld();

        if (world == null) {
            return containers;
        }

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // 遍历范围内的所有方块
        for (int x = centerX - range; x <= centerX + range; x++) {
            for (int y = centerY - range; y <= centerY + range; y++) {
                for (int z = centerZ - range; z <= centerZ + range; z++) {
                    Block block = world.getBlockAt(x, y, z);

                    // 检查是否为容器方块（箱子、木桶等）
                    if (isContainerBlock(block.getType())) {
                        BlockState state = block.getState();
                        if (state instanceof InventoryHolder) {
                            InventoryHolder holder = (InventoryHolder) state;
                            containers.add(new ContainerInfo(block.getLocation(), holder.getInventory()));
                        }
                    }
                }
            }
        }

        return containers;
    }

    /**
     * 检查方块类型是否为容器
     */
    public static boolean isContainerBlock(Material material) {
        return material == Material.CHEST ||
                material == Material.TRAPPED_CHEST ||
                material == Material.BARREL ||
                material == Material.SHULKER_BOX ||
                material == Material.WHITE_SHULKER_BOX ||
                material == Material.ORANGE_SHULKER_BOX ||
                material == Material.MAGENTA_SHULKER_BOX ||
                material == Material.LIGHT_BLUE_SHULKER_BOX ||
                material == Material.YELLOW_SHULKER_BOX ||
                material == Material.LIME_SHULKER_BOX ||
                material == Material.PINK_SHULKER_BOX ||
                material == Material.GRAY_SHULKER_BOX ||
                material == Material.LIGHT_GRAY_SHULKER_BOX ||
                material == Material.CYAN_SHULKER_BOX ||
                material == Material.PURPLE_SHULKER_BOX ||
                material == Material.BLUE_SHULKER_BOX ||
                material == Material.BROWN_SHULKER_BOX ||
                material == Material.GREEN_SHULKER_BOX ||
                material == Material.RED_SHULKER_BOX ||
                material == Material.BLACK_SHULKER_BOX;
    }

    /**
     * 尝试将物品放入匹配的容器中
     * 
     * @param itemStack  要放入的物品
     * @param containers 可用的容器列表
     * @return 剩余的物品（如果完全放入则返回null）
     */
    public static ItemStack tryPlaceItemInMatchingContainers(ItemStack itemStack, List<ContainerInfo> containers) {
        return tryPlaceItemInMatchingContainers(itemStack, containers, null, null);
    }

    /**
     * 尝试将物品放入匹配的容器中（支持CoreProtect记录）
     * 
     * @param itemStack  要放入的物品
     * @param containers 可用的容器列表
     * @param player     执行操作的玩家（用于CoreProtect记录）
     * @param plugin     插件实例（用于获取Hook）
     * @return 剩余的物品（如果完全放入则返回null）
     */
    public static ItemStack tryPlaceItemInMatchingContainers(ItemStack itemStack, List<ContainerInfo> containers,
            Player player, AutoOrganize plugin) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }

        ItemStack remaining = itemStack.clone();

        // 遍历所有容器，寻找已经包含相同物品类型的容器
        for (ContainerInfo containerInfo : containers) {
            Inventory inventory = containerInfo.getInventory();

            // 检查容器是否已包含相同类型的物品
            if (containsSameItemType(inventory, remaining)) {
                // 尝试将物品放入此容器
                remaining = tryAddItemToInventory(inventory, remaining, player, plugin);

                // 如果物品已完全放入，返回null
                if (remaining == null || remaining.getAmount() <= 0) {
                    return null;
                }
            }
        }

        return remaining;
    }

    /**
     * 尝试将物品放入匹配的容器中，并返回放置结果信息（用于视觉效果）
     * 
     * @param itemStack      要放入的物品
     * @param containers     可用的容器列表
     * @param centerLocation 中心位置（用于视觉效果起始点）
     * @return 放置结果信息
     */
    public static PlacementResult tryPlaceItemWithVisualEffect(ItemStack itemStack, List<ContainerInfo> containers,
            Location centerLocation) {
        return tryPlaceItemWithVisualEffect(itemStack, containers, centerLocation, null, null);
    }

    /**
     * 尝试将物品放入匹配的容器中，并返回放置结果信息（用于视觉效果，支持CoreProtect记录）
     * 
     * @param itemStack      要放入的物品
     * @param containers     可用的容器列表
     * @param centerLocation 中心位置（用于视觉效果起始点）
     * @param player         执行操作的玩家（用于CoreProtect记录）
     * @param plugin         插件实例（用于获取Hook）
     * @return 放置结果信息
     */
    public static PlacementResult tryPlaceItemWithVisualEffect(ItemStack itemStack, List<ContainerInfo> containers,
            Location centerLocation, Player player, AutoOrganize plugin) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return new PlacementResult(null, null, null);
        }

        ItemStack remaining = itemStack.clone();

        // 遍历所有容器，寻找已经包含相同物品类型的容器
        for (ContainerInfo containerInfo : containers) {
            // 检查方块是否被破坏
            if (containerInfo.getLocation().getBlock().getType() == Material.AIR) {
                continue;
            }
            Inventory inventory = containerInfo.getInventory();

            // 检查容器是否已包含相同类型的物品
            if (containsSameItemType(inventory, remaining)) {
                ItemStack beforePlacement = remaining.clone();

                // 尝试将物品放入此容器
                remaining = tryAddItemToInventory(inventory, remaining, player, plugin);

                // 计算实际放入的数量
                int placedAmount = beforePlacement.getAmount() - (remaining != null ? remaining.getAmount() : 0);

                if (placedAmount > 0) {
                    // 创建表示已放入物品的ItemStack
                    ItemStack placedItem = itemStack.clone();
                    placedItem.setAmount(placedAmount);

                    return new PlacementResult(remaining, placedItem, containerInfo.getLocation());
                }
            }
        }

        return new PlacementResult(remaining, null, null);
    }

    /**
     * 放置结果信息类
     */
    public static class PlacementResult {
        private final ItemStack remainingItem;
        private final ItemStack placedItem;
        private final Location targetLocation;

        public PlacementResult(ItemStack remainingItem, ItemStack placedItem, Location targetLocation) {
            this.remainingItem = remainingItem;
            this.placedItem = placedItem;
            this.targetLocation = targetLocation;
        }

        public ItemStack getRemainingItem() {
            return remainingItem;
        }

        public ItemStack getPlacedItem() {
            return placedItem;
        }

        public Location getTargetLocation() {
            return targetLocation;
        }

        public boolean hasPlacedItem() {
            return placedItem != null && placedItem.getAmount() > 0;
        }
    }

    /**
     * 检查容器是否包含相同类型的物品
     */
    private static boolean containsSameItemType(Inventory inventory, ItemStack itemStack) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == itemStack.getType()) {
                // 进一步检查物品是否完全相同（包括元数据）
                if (item.isSimilar(itemStack)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 尝试将物品添加到指定的物品栏中
     * 
     * @param inventory 目标物品栏
     * @param itemStack 要添加的物品
     * @return 剩余的物品（如果完全添加则返回null）
     */
    private static ItemStack tryAddItemToInventory(Inventory inventory, ItemStack itemStack) {
        return tryAddItemToInventory(inventory, itemStack, null, null);
    }

    /**
     * 尝试将物品添加到指定的物品栏中（支持CoreProtect记录）
     * 
     * @param inventory 目标物品栏
     * @param itemStack 要添加的物品
     * @param player    执行操作的玩家（用于CoreProtect记录）
     * @param plugin    插件实例（用于获取Hook）
     * @return 剩余的物品（如果完全添加则返回null）
     */
    private static ItemStack tryAddItemToInventory(Inventory inventory, ItemStack itemStack, Player player,
            AutoOrganize plugin) {
        if (itemStack == null || itemStack.getAmount() <= 0) {
            return null;
        }

        ItemStack remaining = itemStack.clone();
        boolean useCorePro = (player != null && plugin != null && plugin.isCoEnabled());

        // 如果启用了CoreProtect，先记录容器访问
        if (useCorePro) {
            boolean logSuccess = plugin.getCoHook().logContainerAccess(inventory, player);
            if (!logSuccess) {
                // 如果记录失败，返回原物品（不执行放置操作）
                return remaining;
            }
        }

        // 首先尝试堆叠到现有的相同物品上
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack existing = inventory.getItem(i);
            if (existing != null && existing.isSimilar(remaining)) {
                int maxStackSize = existing.getMaxStackSize();
                int currentAmount = existing.getAmount();
                int canAdd = maxStackSize - currentAmount;

                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, remaining.getAmount());

                    // 执行物品堆叠
                    if (useCorePro) {
                        // 使用CoreProtect记录的方式
                        ItemStack newStack = existing.clone();
                        newStack.setAmount(currentAmount + toAdd);
                        inventory.setItem(i, newStack);
                    } else {
                        // 直接修改现有物品
                        existing.setAmount(currentAmount + toAdd);
                    }

                    remaining.setAmount(remaining.getAmount() - toAdd);

                    if (remaining.getAmount() <= 0) {
                        return null;
                    }
                }
            }
        }

        // 然后尝试放入空格子
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot == null || slot.getType() == Material.AIR) {
                int maxStackSize = remaining.getMaxStackSize();
                int toPlace = Math.min(maxStackSize, remaining.getAmount());

                ItemStack newStack = remaining.clone();
                newStack.setAmount(toPlace);

                // 执行物品放置
                if (useCorePro) {
                    // 使用CoreProtect记录的方式（已经在前面记录过了）
                    inventory.setItem(i, newStack);
                } else {
                    // 直接放置
                    inventory.setItem(i, newStack);
                }

                remaining.setAmount(remaining.getAmount() - toPlace);
                if (remaining.getAmount() <= 0) {
                    return null;
                }
            }
        }

        return remaining;
    }

    /**
     * 将剩余物品返回给玩家
     */
    public static void returnItemsToPlayer(Player player, List<ItemStack> remainingItems, AutoOrganize plugin) {
        boolean drop = false;
        for (ItemStack item : remainingItems) {
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                // 尝试放入玩家背包
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                // 背包放不下的物品掉落在玩家脚下
                for (ItemStack droppedItem : leftover.values()) {
                    drop = true;
                    player.getWorld().dropItemNaturally(player.getLocation(), droppedItem);
                }
            }
        }
        if (drop) {
            plugin.sendMessage(player, plugin.getMsgItemsDrop());
        }
    }

    /**
     * 容器信息类
     */
    public static class ContainerInfo {
        private final Location location;
        private final Inventory inventory;

        public ContainerInfo(Location location, Inventory inventory) {
            this.location = location;
            this.inventory = inventory;
        }

        public Location getLocation() {
            return location;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }
}
