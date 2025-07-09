# AutoOrganize

一个智能的 Minecraft Spigot 物品自动整理插件，支持可视化效果和插件集成。

## 功能特性

- 🎒 **智能整理** - 自动将物品分配到已有相同物品的容器中
- ✨ **视觉效果** - 物品飞行动画，增强用户体验
- 🔧 **插件集成** - 支持 CoreProtect 记录和 LockettePro 权限检查
- ⚡ **性能优化** - 分批处理，避免服务器卡顿
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
2. 将物品放入54格GUI界面
3. 关闭界面开始自动整理
4. 观看物品飞行动画

## 配置文件

```yaml
# 性能设置
performance:
  blocks_per_tick: 300  # 每tick扫描方块数
  items_per_tick: 5     # 每tick处理物品数

# 视觉效果
visual_effects:
  enabled: true         # 启用飞行动画
  flight_duration: 30   # 飞行时长(tick)

# 插件集成
integrations:
  coreprotect:
    enabled: true       # CoreProtect记录
  lockettepro:
    enabled: true       # LockettePro权限检查
```

## 支持的容器

- 箱子、陷阱箱
- 木桶
- 各色潜影盒

## 安装要求

- **Minecraft**: 1.21+
- **服务端**: Paper/Spigot
- **可选依赖**: CoreProtect, LockettePro

## 技术特点

- **同步分批处理** - 避免线程安全问题
- **ItemDisplay动画** - 使用1.19.4+新特性
- **软依赖设计** - 兼容性强
- **配置热重载** - 支持运行时配置修改

## 开发信息

- **版本**: 1.0
- **API**: Paper 1.21.1
- **语言**: Java 17+
- **构建**: Gradle

---

*简洁、高效、美观的物品整理解决方案*
