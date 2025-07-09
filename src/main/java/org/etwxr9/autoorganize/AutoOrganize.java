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

    // 配置项
    private int blocksPerTick = 300;
    private int itemsPerTick = 5;
    private boolean visualEffectsEnabled = true;
    private boolean coreProtectEnabled = true;
    private boolean locketteProEnabled = true;

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

    public int getItemsPerTick() {
        return itemsPerTick;
    }

    public boolean isVisualEffectsEnabled() {
        return visualEffectsEnabled;
    }

    public boolean isCoreProtectConfigEnabled() {
        return coreProtectEnabled;
    }

    public boolean isLocketteProConfigEnabled() {
        return locketteProEnabled;
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
            ctx.getSource().getSender().sendMessage("此命令只能由玩家执行");
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
        itemsPerTick = getConfig().getInt("performance.items_per_tick", 5);
        visualEffectsEnabled = getConfig().getBoolean("visual_effects.enabled", true);
        coreProtectEnabled = getConfig().getBoolean("integrations.coreprotect.enabled", true);
        locketteProEnabled = getConfig().getBoolean("integrations.lockettepro.enabled", true);

        getLogger().info("配置已加载 - 每tick扫描方块数: " + blocksPerTick + ", 每tick处理物品数: " + itemsPerTick);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("AutoOrganize has been disabled!");
    }
}
