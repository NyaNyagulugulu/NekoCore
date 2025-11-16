# NekoCore 1.12.2

NekoCore 是一个高性能的 Minecraft 1.12.2 服务器核心，基于 Paper 和 Spigot 进行优化。

## 特性

- 高性能优化，提升 TPS 和运行速度
- 内存占用优化，降低 RAM 使用
- 减少卡顿，提升服务器流畅度
- 回退到 1.8.8 战斗机制
- 支持广泛的插件生态
- 内嵌入式 NAC 反作弊系统

## NAC 反作弊系统

NekoCore 内嵌入了先进的 NAC (NekoAntiCheat) 反作弊系统，提供全面的飞行检测和其他反作弊功能。

### 功能特点

- **高性能飞行检测**：基于服务器物理状态、连续位置变化、玩家动作和方块状态进行检测
- **智能误判防护**：自动识别正常跳跃、跳砍、移动跳跃等行为，避免误判
- **多维度检测**：重力一致性、空中上升、横向速度、停空等多维度检测
- **可配置性**：支持通过配置文件调整检测参数和惩罚方式
- **实时监控**：管理员可通过权限实时接收检测告警

### 检测机制

1. **重力一致性检查**：验证空中玩家的纵向速度是否符合Minecraft物理规律
2. **空中上升检测**：识别异常上升行为，排除正常跳跃
3. **横向速度检测**：检测空中不合理的水平速度
4. **停空检测**：识别悬停行为，允许正常跳跃和移动
5. **连续跳跃检测**：区分正常跳砍和飞行作弊

### 指令

```
# 重载 NAC 配置
/nac reload
```

### 配置文件 (NAC.yml)

```yaml
# NAC反作弊配置
enable-NAC: true

# 飞行检测配置
fly:
  # VL最大值，达到后执行指令
  max_vl: 10
  # 达到最大VL后执行的指令（最多4条）
  commands_on_max_vl:
    - "kick {player} 飞你妈呢"
  # VL值降低间隔（秒），每过这个时间VL值减少1点
  vl_decay_interval_seconds: 30
  # 权限说明:
  # nac.bypass.fly - 跳过飞行检测
  # nac.admin - 接收飞行检测警报
```

### 权限节点

- `nac.command` - 使用 NAC 命令
- `nac.bypass.fly` - 跳过飞行检测
- `nac.admin` - 接收飞行检测警报

### 性能优化

NAC 系统经过优化，采用服务器端物理检测，不依赖网络延迟或客户端报文，确保准确性和性能。（因为延迟数据不可信可悲伪造）

## 性能优化特性

### 服务器配置优化
- 优化了实体处理机制，减少不必要的计算
- 改进了区块加载/卸载策略
- 优化了红石和TNT性能
- 减少了垃圾回收频率

### 内存管理
- 优化了对象池机制
- 减少了内存泄漏风险
- 改进了缓存机制

### TPS 提升
- 优化了实体AI计算
- 减少了同步操作
- 改进了任务调度机制

## 安装说明

1. 下载编译好的 JAR 文件
2. 将其放在服务器目录中
3. 使用以下命令启动服务器：
   ```
   java -Xms4G -Xmx4G -XX:+UseG1GC -jar nekocore.jar
   ```
   根据您的服务器配置调整内存参数

## 性能调优建议

### JVM 参数
```
-Xms4G -Xmx4G
-XX:+UseG1GC
-XX:+ParallelRefProcEnabled
-XX:MaxGCPauseMillis=200
-XX:+UnlockExperimentalVMOptions
-XX:+DisableExplicitGC
-XX:G1HeapWastePercent=5
-XX:G1MixedGCCountTarget=4
-XX:G1MixedGCLiveThresholdPercent=90
-XX:G1RSetUpdatingPauseTimePercent=5
-XX:SurvivorRatio=32
-XX:+PerfDisableSharedMem
-XX:MaxTenuringThreshold=1
```

### 服务器配置优化
在 `spigot.yml` 中：
```yaml
settings:
  player-shuffle: 100
  timeout-time: 60
  restart-on-crash: false
world-settings:
  default:
    entity-tracking-range:
      players: 48
      animals: 32
      monsters: 32
      misc: 16
      other: 64
    ticks-per:
      hopper-transfer: 8
      hopper-check: 1
```

## 插件兼容性

NekoCore 完全兼容 Bukkit、Spigot 和 Paper 插件。

## 贡献

欢迎提交 Issue 和 Pull Request 来改进 NekoCore。

## 许可证

请参阅项目中的许可证文件。