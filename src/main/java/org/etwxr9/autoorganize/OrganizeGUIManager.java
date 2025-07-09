package org.etwxr9.autoorganize;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import io.papermc.paper.math.BlockPosition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GUI管理类 - 处理自动整理的物品栏界面
 */
public class OrganizeGUIManager implements Listener {

    private final AutoOrganize plugin;
    private final Map<UUID, OrganizeSession> activeSessions;

    public OrganizeGUIManager(AutoOrganize plugin) {
        this.plugin = plugin;
        this.activeSessions = new HashMap<>();

        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 为玩家打开整理GUI界面
     */
    public void openOrganizeGUI(Player player, BlockPosition pos, int range) {
        // 创建6行54格的物品栏
        Inventory gui = Bukkit.createInventory(null, InventoryType.CHEST,
                Component.text(plugin.getMsgGuiTitle()).color(NamedTextColor.GOLD));

        // 创建整理会话
        OrganizeSession session = new OrganizeSession(player, pos, range, gui);
        activeSessions.put(player.getUniqueId(), session);

        // 打开GUI
        player.openInventory(gui);
        plugin.sendMessage(player, plugin.getMsgGuiOpen());
    }

    /**
     * 处理物品栏点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        // 检查是否为整理GUI
        if (!activeSessions.containsKey(playerId)) {
            return;
        }

        OrganizeSession session = activeSessions.get(playerId);
        if (!event.getInventory().equals(session.getGui())) {
            return;
        }

        // 允许玩家在GUI中放置和取出物品
        // 不需要特殊处理，让默认行为处理物品移动
    }

    /**
     * 处理物品栏关闭事件
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 检查是否为整理GUI
        if (!activeSessions.containsKey(playerId)) {
            return;
        }

        OrganizeSession session = activeSessions.get(playerId);
        if (!event.getInventory().equals(session.getGui())) {
            return;
        }

        // 检查GUI中是否有物品
        boolean hasItems = false;
        for (ItemStack item : event.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                hasItems = true;
                break;
            }
        }

        if (hasItems) {
            // 启动自动整理
            plugin.sendMessage(player, plugin.getMsgGuiStartOrganizing());

            // 创建整理任务
            OrganizeTask organizeTask = new OrganizeTask(
                    plugin,
                    player,
                    session.getPos(),
                    session.getRange(),
                    event.getInventory().getContents());

            // 同步执行整理任务（分批处理避免卡顿）
            organizeTask.runTaskTimer(plugin, 1L, 1L); // 延迟1tick开始，每1tick执行一次
        } else {
            plugin.sendMessage(player, plugin.getMsgGuiNoItems());
        }

        // 移除会话
        activeSessions.remove(playerId);
    }

    /**
     * 整理会话类 - 存储单次整理操作的信息
     */
    private static class OrganizeSession {
        private final Player player;
        private final BlockPosition pos;
        private final int range;
        private final Inventory gui;

        public OrganizeSession(Player player, BlockPosition pos, int range, Inventory gui) {
            this.player = player;
            this.pos = pos;
            this.range = range;
            this.gui = gui;
        }

        public Player getPlayer() {
            return player;
        }

        public BlockPosition getPos() {
            return pos;
        }

        public int getRange() {
            return range;
        }

        public Inventory getGui() {
            return gui;
        }
    }
}
