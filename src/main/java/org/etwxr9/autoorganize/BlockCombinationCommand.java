package org.etwxr9.autoorganize;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;

/**
 * 方块组合体管理指令类
 * 处理方块组合体配置的相关指令
 */
public class BlockCombinationCommand {
    
    private final AutoOrganize plugin;
    
    public BlockCombinationCommand(AutoOrganize plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 构建指令树
     */
    public static com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> buildCommand(AutoOrganize plugin) {
        BlockCombinationCommand handler = new BlockCombinationCommand(plugin);
        
        return Commands.literal("autoorganize")
                // 设置上方方块为主手方块
                .then(Commands.literal("settop")
                        .executes(handler::setTopBlock))
                // 添加/更新下方方块配置
                .then(Commands.literal("addblock")
                        .then(Commands.argument("range", IntegerArgumentType.integer(1, 200))
                                .executes(handler::addBottomBlock)))
                // 删除下方方块配置
                .then(Commands.literal("removeblock")
                        .executes(handler::removeBottomBlock))
                // 设置每tick检索方块数量
                .then(Commands.literal("setblockspeed")
                        .then(Commands.argument("blocks_per_tick", IntegerArgumentType.integer(1))
                                .executes(handler::setBlocksPerTick)))
                // 设置Y轴搜索半径
                .then(Commands.literal("setyradius")
                        .then(Commands.argument("y_radius", IntegerArgumentType.integer(1, 50))
                                .executes(handler::setYRadius)))
                // 设置飞行动画时间
                .then(Commands.literal("setflighttime")
                        .then(Commands.argument("flight_duration", IntegerArgumentType.integer(1, 200))
                                .executes(handler::setFlightDuration)))
                // 列出当前配置
                .then(Commands.literal("list")
                        .executes(handler::listConfig))
                .build();
    }
    
    /**
     * 设置上方方块为主手方块
     */
    private int setTopBlock(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player)) {
            ctx.getSource().getSender().sendMessage("§c此命令只能由玩家执行");
            return 0;
        }
        
        Player player = (Player) ctx.getSource().getSender();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        
        if (mainHandItem == null || mainHandItem.getType() == Material.AIR) {
            player.sendMessage("§c请在主手持有一个方块");
            return 0;
        }
        
        Material blockType = mainHandItem.getType();
        if (!blockType.isBlock()) {
            player.sendMessage("§c主手物品不是方块");
            return 0;
        }
        
        // 更新配置
        plugin.getBlockCombinationConfig().setTopBlock(blockType);
        
        // 保存到配置文件
        saveConfig();
        
        player.sendMessage("§a已将上方方块设置为: §e" + blockType.name());
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 添加/更新下方方块配置
     */
    private int addBottomBlock(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player)) {
            ctx.getSource().getSender().sendMessage("§c此命令只能由玩家执行");
            return 0;
        }
        
        Player player = (Player) ctx.getSource().getSender();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        int range = ctx.getArgument("range", Integer.class);
        
        if (mainHandItem == null || mainHandItem.getType() == Material.AIR) {
            player.sendMessage("§c请在主手持有一个方块");
            return 0;
        }
        
        Material blockType = mainHandItem.getType();
        if (!blockType.isBlock()) {
            player.sendMessage("§c主手物品不是方块");
            return 0;
        }
        
        // 更新配置
        boolean isUpdate = plugin.getBlockCombinationConfig().containsBottomBlock(blockType);
        plugin.getBlockCombinationConfig().setBottomBlockRange(blockType, range);
        
        // 保存到配置文件
        saveConfig();
        
        if (isUpdate) {
            player.sendMessage("§a已更新下方方块配置: §e" + blockType.name() + " §a-> 范围: §e" + range);
        } else {
            player.sendMessage("§a已添加下方方块配置: §e" + blockType.name() + " §a-> 范围: §e" + range);
        }
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 删除下方方块配置
     */
    private int removeBottomBlock(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player)) {
            ctx.getSource().getSender().sendMessage("§c此命令只能由玩家执行");
            return 0;
        }
        
        Player player = (Player) ctx.getSource().getSender();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        
        if (mainHandItem == null || mainHandItem.getType() == Material.AIR) {
            player.sendMessage("§c请在主手持有一个方块");
            return 0;
        }
        
        Material blockType = mainHandItem.getType();
        if (!blockType.isBlock()) {
            player.sendMessage("§c主手物品不是方块");
            return 0;
        }
        
        // 删除配置
        boolean removed = plugin.getBlockCombinationConfig().removeBottomBlock(blockType);
        
        if (removed) {
            // 保存到配置文件
            saveConfig();
            player.sendMessage("§a已删除下方方块配置: §e" + blockType.name());
        } else {
            player.sendMessage("§c该方块未在配置中: §e" + blockType.name());
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 设置每tick检索方块数量
     */
    private int setBlocksPerTick(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player)) {
            ctx.getSource().getSender().sendMessage("§c此命令只能由玩家执行");
            return 0;
        }

        Player player = (Player) ctx.getSource().getSender();
        int blocksPerTick = ctx.getArgument("blocks_per_tick", Integer.class);

        // 更新配置文件
        plugin.getConfig().set("performance.blocks_per_tick", blocksPerTick);
        plugin.saveConfig();
        plugin.reloadConfig();

        // 更新内存中的配置
        plugin.setBlocksPerTick(blocksPerTick);

        player.sendMessage("§a已设置每tick检索方块数量为: §e" + blocksPerTick);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 设置Y轴搜索半径
     */
    private int setYRadius(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player)) {
            ctx.getSource().getSender().sendMessage("§c此命令只能由玩家执行");
            return 0;
        }

        Player player = (Player) ctx.getSource().getSender();
        int yRadius = ctx.getArgument("y_radius", Integer.class);

        // 更新配置
        plugin.getBlockCombinationConfig().setYRadius(yRadius);

        // 保存到配置文件
        saveConfig();

        player.sendMessage("§a已设置Y轴搜索半径为: §e" + yRadius);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 设置飞行动画时间
     */
    private int setFlightDuration(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player)) {
            ctx.getSource().getSender().sendMessage("§c此命令只能由玩家执行");
            return 0;
        }

        Player player = (Player) ctx.getSource().getSender();
        int flightDuration = ctx.getArgument("flight_duration", Integer.class);

        // 更新配置文件
        plugin.getConfig().set("visual_effects.flight_duration", flightDuration);
        plugin.saveConfig();
        plugin.reloadConfig();

        // 更新内存中的配置
        plugin.setFlightDuration(flightDuration);

        player.sendMessage("§a已设置飞行动画时间为: §e" + flightDuration + " tick");
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 列出当前配置
     */
    private int listConfig(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player)) {
            ctx.getSource().getSender().sendMessage("§c此命令只能由玩家执行");
            return 0;
        }
        
        Player player = (Player) ctx.getSource().getSender();
        BlockCombinationConfig config = plugin.getBlockCombinationConfig();
        
        player.sendMessage("§6=== 方块组合体配置 ===");
        player.sendMessage("§a上方方块: §e" + config.getTopBlock().name());
        player.sendMessage("§aY轴搜索半径: §e" + config.getYRadius());
        player.sendMessage("§a每tick检索方块数: §e" + plugin.getBlocksPerTick());
        player.sendMessage("§a飞行动画时间: §e" + plugin.getFlightDuration() + " tick");
        player.sendMessage("§a下方方块配置:");
        
        if (config.getBottomBlockCount() == 0) {
            player.sendMessage("  §c无配置");
        } else {
            for (Material material : config.getBottomBlocks()) {
                Integer range = config.getRange(material);
                player.sendMessage("  §e" + material.name() + " §a-> 范围: §e" + range);
            }
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 保存配置到文件
     */
    private void saveConfig() {
        plugin.getBlockCombinationConfig().saveToConfig(
            plugin.getConfig().createSection("block_combination")
        );
        plugin.saveConfig();
    }
}
