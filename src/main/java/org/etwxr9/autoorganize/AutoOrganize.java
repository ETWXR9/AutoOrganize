package org.etwxr9.autoorganize;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public final class AutoOrganize extends JavaPlugin {

    private OrganizeGUIManager guiManager;

    // 配置项（从配置文件读取）
    private int blocksPerTick = 300;
    private boolean visualEffectsEnabled = true;
    private int flightDuration = 30;
    private double itemScale = 0.3;

    // 消息配置
    private String msgGuiOpen = "";
    private String msgGuiStartOrganizing = "";
    private String msgGuiNoItems = "";
    private String msgSearchContainers = "";
    private String msgContainersFound = "";
    private String msgNoContainers = "";
    private String msgOrganizingProgress = "";
    private String msgOrganizeComplete = "";
    private String msgItemsOrganized = "";
    private String msgItemsRemaining = "";
    private String msgAllItemsOrganized = "";
    private String msgErrorOccurred = "";
    private String msgPlayerOnly = "";

    private LocketteProHook locketteProHook;
    private boolean isLocketteProEnabled = false;

    public boolean isLocketteProEnabled() {
        return isLocketteProEnabled;
    }

    public LocketteProHook getLocketteProHook() {
        return locketteProHook;
    }

    private CoHook coHook;
    private boolean isCoEnabled = false;

    public boolean isCoEnabled() {
        return isCoEnabled;
    }

    public CoHook getCoHook() {
        return coHook;
    }

    // 配置项getter方法
    public int getBlocksPerTick() {
        return blocksPerTick;
    }

    public boolean isVisualEffectsEnabled() {
        return visualEffectsEnabled;
    }

    public int getFlightDuration() {
        return flightDuration;
    }

    public double getItemScale() {
        return itemScale;
    }

    // 消息配置getter方法
    public String getMsgGuiOpen() { return msgGuiOpen; }
    public String getMsgGuiStartOrganizing() { return msgGuiStartOrganizing; }
    public String getMsgGuiNoItems() { return msgGuiNoItems; }
    public String getMsgSearchContainers() { return msgSearchContainers; }
    public String getMsgContainersFound() { return msgContainersFound; }
    public String getMsgNoContainers() { return msgNoContainers; }
    public String getMsgOrganizingProgress() { return msgOrganizingProgress; }
    public String getMsgOrganizeComplete() { return msgOrganizeComplete; }
    public String getMsgItemsOrganized() { return msgItemsOrganized; }
    public String getMsgItemsRemaining() { return msgItemsRemaining; }
    public String getMsgAllItemsOrganized() { return msgAllItemsOrganized; }
    public String getMsgErrorOccurred() { return msgErrorOccurred; }
    public String getMsgPlayerOnly() { return msgPlayerOnly; }

    /**
     * 发送配置化消息给玩家
     * @param player 目标玩家
     * @param message 消息内容
     */
    public void sendMessage(Player player, String message) {
        if (message != null && !message.trim().isEmpty()) {
            player.sendMessage(message);
        }
    }

    /**
     * 发送带占位符的配置化消息给玩家
     * @param player 目标玩家
     * @param message 消息模板
     * @param placeholders 占位符键值对
     */
    public void sendMessage(Player player, String message, String... placeholders) {
        if (message != null && !message.trim().isEmpty()) {
            String finalMessage = message;
            for (int i = 0; i < placeholders.length; i += 2) {
                if (i + 1 < placeholders.length) {
                    finalMessage = finalMessage.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
                }
            }
            player.sendMessage(finalMessage);
        }
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("AutoOrganize has been enabled!");

        // 加载配置文件
        loadConfig();

        // soft depend
        // 检查服务器上是否存在并启用了 LockettePro 插件
        if (getServer().getPluginManager().isPluginEnabled("LockettePro")) {
            // 如果存在，我们就实例化我们的钩子类
            this.locketteProHook = new LocketteProHook();
            this.isLocketteProEnabled = true;
            getLogger().info("Successfully hooked into LockettePro!");
        } else {
            // 如果不存在，我们就不做任何事，并打印一条消息
            getLogger().info("LockettePro not found. Some features will be disabled.");
        }
        // 检查服务器上是否存在并启用了 CoreProtect 插件
        if (getServer().getPluginManager().isPluginEnabled("CoreProtect")) {
            // 如果存在，我们就实例化我们的钩子类
            this.coHook = new CoHook();
            this.isCoEnabled = coHook.isCoEnabled();
            if (isCoEnabled) {
                getLogger().info("Successfully hooked into CoreProtect!");
            } else {
                getLogger().info("CoreProtect found but API is not enabled. Some features will be disabled.");
            }

        } else {
            // 如果不存在，我们就不做任何事，并打印一条消息
            getLogger().info("CoreProtect not found. Some features will be disabled.");
        }

        // 初始化GUI管理器
        this.guiManager = new OrganizeGUIManager(this);

        var cmd = Commands.literal("organize")
                .then(Commands.argument("loc", ArgumentTypes.blockPosition())
                        .then(Commands.argument("range", IntegerArgumentType.integer())
                                .executes(ctx -> {
                                    runCmd(ctx);
                                    return Command.SINGLE_SUCCESS;
                                })));
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(cmd.build(), "autoorganize");
        });
    }

    private boolean runCmd(CommandContext<CommandSourceStack> ctx) {
        final BlockPositionResolver blockPositionResolver = ctx.getArgument("loc", BlockPositionResolver.class);
        BlockPosition blockPosition;
        try {
            blockPosition = blockPositionResolver.resolve(ctx.getSource());
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
            return false;
        }
        final int range = ctx.getArgument("range", int.class);
        // 检查命令发送者是否为玩家
        if (!(ctx.getSource().getSender() instanceof Player)) {
            getLogger().warning("Non-player tried to use /organize command");
            if (!msgPlayerOnly.trim().isEmpty()) {
                ctx.getSource().getSender().sendMessage(msgPlayerOnly);
            }
            return false;
        }

        Player player = (Player) ctx.getSource().getSender();

        // 打开整理GUI界面
        guiManager.openOrganizeGUI(player, blockPosition, range);

        return true;
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        // 保存默认配置文件（如果不存在）
        saveDefaultConfig();

        // 重新加载配置
        reloadConfig();

        // 读取配置项
        blocksPerTick = getConfig().getInt("performance.blocks_per_tick", 300);
        visualEffectsEnabled = getConfig().getBoolean("visual_effects.enabled", true);
        flightDuration = getConfig().getInt("visual_effects.flight_duration", 30);
        itemScale = getConfig().getDouble("visual_effects.item_scale", 0.3);

        // 读取消息配置
        msgGuiOpen = getConfig().getString("messages.gui_open", "");
        msgGuiStartOrganizing = getConfig().getString("messages.gui_start_organizing", "");
        msgGuiNoItems = getConfig().getString("messages.gui_no_items", "");
        msgSearchContainers = getConfig().getString("messages.search_containers", "");
        msgContainersFound = getConfig().getString("messages.containers_found", "");
        msgNoContainers = getConfig().getString("messages.no_containers", "");
        msgOrganizingProgress = getConfig().getString("messages.organizing_progress", "");
        msgOrganizeComplete = getConfig().getString("messages.organize_complete", "");
        msgItemsOrganized = getConfig().getString("messages.items_organized", "");
        msgItemsRemaining = getConfig().getString("messages.items_remaining", "");
        msgAllItemsOrganized = getConfig().getString("messages.all_items_organized", "");
        msgErrorOccurred = getConfig().getString("messages.error_occurred", "");
        msgPlayerOnly = getConfig().getString("messages.player_only", "");

        getLogger().info("配置已加载 - 每tick扫描方块数: " + blocksPerTick);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("AutoOrganize has been disabled!");
    }
}
