# NekoCore 1.12.2

NekoCore 是一个高性能的 Minecraft 1.12.2 服务器核心，基于 Paper 和 Spigot 进行优化。

## 特性

- 高性能优化，提升 TPS 和运行速度
- 内存占用优化，降低 RAM 使用
- 减少卡顿，提升服务器流畅度
- 兼容 1.8.8 战斗机制
- 支持广泛的插件生态

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