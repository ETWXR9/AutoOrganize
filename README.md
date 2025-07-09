# AutoOrganize

一个智能的 Minecraft Spigot 物品自动整理命令插件。

## 功能特性

- 🎒 **智能整理** - 自动将物品分配到已有相同物品的容器中
- ✨ **视觉效果** - 物品飞行动画
- 🔧 **软依赖插件** - 支持 CoreProtect 记录和 LockettePro 权限检查
- ⚡ **性能优化** - 分散到每tick检索容器，避免服务器卡顿
- 🎛️ **可配置** - 支持配置文件自定义各项参数

## 使用方法

```bash
/organize <坐标> <范围>
```

**示例：**
```bash
/organize ~ ~ ~ 10  # 以当前位置为中心，10格范围内整理
```

**操作流程：**
1. 执行命令打开整理界面
2. 将物品放入GUI界面
3. 关闭界面开始自动整理
4. 观看物品飞行动画
5. 如期间玩家离线，物品会掉落在原地

## 配置文件

```yaml
# AutoOrganize 插件配置文件

# 性能设置
performance:
  # 每tick扫描的方块数量
  blocks_per_tick: 500

# 视觉效果设置
visual_effects:
  # 是否启用物品飞行动画
  enabled: true
  # 飞行持续时间（tick）
  flight_duration: 30
  # 物品缩放大小
  item_scale: 0.3

messages:
  # GUI Title
  gui_title: "§6自动整理 - 放入要整理的物品"
  # GUI相关消息（设为空则不发送）
  gui_open: "§a请将需要整理的物品放入界面中，关闭界面后将自动开始整理"
  gui_start_organizing: "§e开始自动整理物品..."
  gui_no_items: "§c没有检测到需要整理的物品"

  # 整理过程消息（设为空则不发送）
  search_containers: "§e开始搜索容器..."
  containers_found: "§a找到 {count} 个容器，开始整理物品..."
  no_containers: "§c未找到任何容器，物品已返回背包"
  organizing_progress: "§e整理进度: {progress}% ({current}/{total})"

  # 完成消息（设为空则不发送）
  organize_complete: "§a整理完成！"
  items_organized: "§a已整理物品: {count} 种"
  items_remaining: "§e剩余物品: {count} 种（已返回背包）"
  all_items_organized: "§a所有物品都已成功整理！"

  # 错误消息（设为空则不发送）
  error_occurred: "§c整理过程中发生错误，请稍后重试"
  player_only: "此命令只能由玩家执行"

```

## 支持的容器

- 箱子、陷阱箱
- 木桶
- 各色潜影盒

## 安装要求

- **Minecraft**: 1.21+
- **服务端**: Paper
- **软依赖**: CoreProtect, LockettePro

## 技术特点

- **同步分批处理** - 缓解性能压力
- **ItemDisplay动画** - 视觉友好
- **软依赖设计** - 兼容性强