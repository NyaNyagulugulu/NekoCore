package org.bukkit.anticheating;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class FlyDetectionListener implements Listener {
    
    private final Map<String, Location> lastValidLocations = new HashMap<>();
    private final NekoAntiCheating plugin;
    
    public FlyDetectionListener(NekoAntiCheating plugin) {
        this.plugin = plugin;
        startVLDecayTask();
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        
        // 获取当前配置
        YamlConfiguration config = plugin.getNACConfig();
        int maxVL = config.getInt("fly.max_vl", 10);
        int decayInterval = config.getInt("fly.vl_decay_interval_seconds", 30);
        
        // 获取玩家位置信息
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // 检查玩家是否在飞行
        if (isFlying(player, from, to)) {
            // 增加VL值
            int currentVL = plugin.getPlayerVLs().getOrDefault(playerName, 0);
            currentVL++;
            plugin.getPlayerVLs().put(playerName, currentVL);
            
            // 检查是否超过最大VL值
            if (currentVL >= maxVL) {
                // 执行指令
                executeCommandsOnMaxVL(player, config.getStringList("fly.commands_on_max_vl"));
                // 重置VL
                plugin.getPlayerVLs().put(playerName, 0);
            }
        } else {
            // 如果玩家没有飞行，重置VL
            plugin.getPlayerVLs().put(playerName, 0);
        }
        
        // 更新最后有效位置
        lastValidLocations.put(playerName, to);
    }
    
    private boolean isFlying(Player player, Location from, Location to) {
        // 检查Y轴变化是否异常（可能是飞行）
        double deltaY = to.getY() - from.getY();
        
        // 如果玩家不在地面且没有合法的飞行原因（比如跳跃、使用鞘翅等）
        if (!player.isOnGround() && 
            deltaY > 0.1 && 
            !player.isInsideVehicle() && 
            !player.isGliding() && 
            !player.hasPermission("nac.bypass.fly")) {
            
            // 简单的飞行检测逻辑：如果Y轴增加且速度异常
            double distance = from.distance(to);
            double horizontalDistance = Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + Math.pow(to.getZ() - from.getZ(), 2));
            
            // 如果垂直移动距离相对于水平移动距离过大，可能是在飞行
            if (deltaY > 0.3 && horizontalDistance < 0.5) {
                return true;
            }
        }
        
        return false;
    }
    
    private void executeCommandsOnMaxVL(Player player, List<String> commands) {
        for (String command : commands) {
            if (command != null && !command.isEmpty()) {
                // 替换命令中的占位符
                command = command.replace("{player}", player.getName());
                
                // 执行命令
                org.bukkit.Bukkit.getServer().dispatchCommand(
                    org.bukkit.Bukkit.getServer().getConsoleSender(), 
                    command
                );
            }
        }
    }
    
    private void startVLDecayTask() {
        // 获取一个有效的插件实例
        Plugin schedulerPlugin = null;
        if (Bukkit.getPluginManager().getPlugin("Paper") != null) {
            schedulerPlugin = Bukkit.getPluginManager().getPlugin("Paper");
        } else if (Bukkit.getPluginManager().getPlugins().length > 0) {
            schedulerPlugin = Bukkit.getPluginManager().getPlugins()[0];
        }
        
        // 如果找不到插件实例，则不启动任务
        if (schedulerPlugin == null) {
            return;
        }
        
        // 等待服务器完全启动后再启动VL衰减任务
        new BukkitRunnable() {
            @Override
            public void run() {
                // 遍历所有玩家，减少VL值
                for (String playerName : plugin.getPlayerVLs().keySet()) {
                    int currentVL = plugin.getPlayerVLs().get(playerName);
                    if (currentVL > 0) {
                        plugin.getPlayerVLs().put(playerName, currentVL - 1);
                    }
                }
            }
        }.runTaskLaterAsynchronously(schedulerPlugin, 100); // 延迟5秒开始
        
        // 然后创建一个重复任务来定期执行VL衰减
        new BukkitRunnable() {
            @Override
            public void run() {
                // 遍历所有玩家，减少VL值
                for (String playerName : plugin.getPlayerVLs().keySet()) {
                    int currentVL = plugin.getPlayerVLs().get(playerName);
                    if (currentVL > 0) {
                        plugin.getPlayerVLs().put(playerName, currentVL - 1);
                    }
                }
            }
        }.runTaskTimerAsynchronously(schedulerPlugin, 
            20L * plugin.getNACConfig().getInt("fly.vl_decay_interval_seconds", 30), 
            20L * plugin.getNACConfig().getInt("fly.vl_decay_interval_seconds", 30));
    }
}