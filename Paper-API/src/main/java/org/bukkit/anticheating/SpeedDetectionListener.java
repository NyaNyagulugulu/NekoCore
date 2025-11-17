package org.bukkit.anticheating;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 非插件型速度检测器
 * 基于服务器物理状态、连续位置变化、玩家动作和方块状态来判定速度作弊行为
 * 每tick或定时主动调用 checkAllPlayers()
 */
public class SpeedDetectionListener {

    // 玩家状态数据
    private final Map<String, Location> lastLocation = new HashMap<>(); // 上一位置
    private final Map<String, Double> suspiciousValue = new HashMap<>(); // 可疑值
    private final Map<String, Long> lastVLIncreaseTime = new HashMap<>(); // 最后VL增加时间
    private final Map<String, Boolean> wasOnGroundLastTick = new HashMap<>(); // 上一tick是否在地面
    
    private int maxVL = 10;
    private int vlDecayIntervalSeconds = 30; // VL衰减间隔秒数
    private List<String> punishCommands = Arrays.asList("kick {player} 禁止速度作弊");
    private NekoAntiCheating plugin;

    public SpeedDetectionListener(NekoAntiCheating plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    // 主动检测所有在线玩家（每tick或定时调用）
    public void checkAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 检查是否拥有豁免权限
            if (player.hasPermission("nac.bypass.speed") || player.isOp() || player.getAllowFlight() || player.isGliding()) continue;
            
            String playerName = player.getName();
            Location loc = player.getLocation();
            boolean onGround = player.isOnGround();
            Location lastLoc = lastLocation.get(playerName);
            Boolean wasOnGround = wasOnGroundLastTick.get(playerName);
            
            // 更新地面状态
            wasOnGroundLastTick.put(playerName, onGround);
            
            // 保存当前位置
            lastLocation.put(playerName, loc.clone());
            
            // 如果没有上一位置，跳过本次检测
            if (lastLoc == null) continue;
            
            // 计算速度
            double velX = loc.getX() - lastLoc.getX();
            double velZ = loc.getZ() - lastLoc.getZ();
            double horizontalSpeed = Math.sqrt(velX * velX + velZ * velZ);
            
            // 更新可疑值
            double currentSuspicious = suspiciousValue.getOrDefault(playerName, 0.0);
            
            // 速度异常检测
            currentSuspicious += checkSpeed(player, horizontalSpeed, onGround);
            
            // 更新可疑值
            suspiciousValue.put(playerName, currentSuspicious);
            
            // 如果可疑值超过阈值，增加VL
            if (currentSuspicious >= 5.0) {
                int vl = playerVLs.getOrDefault(playerName, 0) + 1;
                playerVLs.put(playerName, vl);
                lastVLIncreaseTime.put(playerName, System.currentTimeMillis());
                
                sendDetectionAlert(player, "速度(" + String.format("%.2f", currentSuspicious) + ")", vl, maxVL);
                
                if (vl >= maxVL) {
                    for (String cmd : punishCommands) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", playerName));
                    }
                    playerVLs.put(playerName, 0);
                    suspiciousValue.put(playerName, 0.0); // 重置可疑值
                }
                
                // 降低可疑值防止连续触发
                suspiciousValue.put(playerName, Math.max(0.0, currentSuspicious - 3.0));
            }
        }
    }

    // 速度异常检测
    private double checkSpeed(Player player, double horizontalSpeed, boolean onGround) {
        if (onGround) {
            // 地面速度检测
            double maxExpectedSpeed = calculateMaxGroundSpeed(player);
            if (horizontalSpeed > maxExpectedSpeed * 1.3) { // 允许30%的误差
                return 1.0; // 地面速度异常，增加可疑值
            }
        } else {
            // 空中速度检测
            double maxExpectedSpeed = calculateMaxAirSpeed(player);
            if (horizontalSpeed > maxExpectedSpeed * 1.3) { // 允许30%的误差
                return 1.0; // 空中速度异常，增加可疑值
            }
        }
        
        return 0.0;
    }
    
    // 计算最大地面速度（考虑效果加成）
    private double calculateMaxGroundSpeed(Player player) {
        // 基础地面速度
        double baseSpeed = 0.28; // 基础行走速度
        
        // 检查速度效果
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier();
            baseSpeed *= (1 + (amplifier + 1) * 0.2); // 每级速度效果增加20%
        }
        
        // 检查缓慢效果
        if (player.hasPotionEffect(PotionEffectType.SLOW)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SLOW).getAmplifier();
            baseSpeed *= (1 - (amplifier + 1) * 0.15); // 每级缓慢效果减少15%
        }
        
        // 检查是否在水中（会减慢速度）
        if (player.getLocation().getBlock().getType() == Material.WATER || 
            player.getLocation().getBlock().getType() == Material.STATIONARY_WATER) {
            baseSpeed *= 0.7; // 水中速度减少30%
        }
        
        // 检查是否在岩浆上
        if (player.getLocation().getBlock().getType() == Material.LAVA || 
            player.getLocation().getBlock().getType() == Material.STATIONARY_LAVA) {
            baseSpeed *= 0.5; // 岩浆上速度减少50%
        }
        
        return baseSpeed;
    }
    
    // 计算最大空中速度（考虑效果加成）
    private double calculateMaxAirSpeed(Player player) {
        // 基础空中速度
        double baseSpeed = 0.28; // 基础空中移动速度
        
        // 检查速度效果
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier();
            baseSpeed *= (1 + (amplifier + 1) * 0.15); // 每级速度效果空中增加15%
        }
        
        // 检查缓慢效果
        if (player.hasPotionEffect(PotionEffectType.SLOW)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SLOW).getAmplifier();
            baseSpeed *= (1 - (amplifier + 1) * 0.15); // 每级缓慢效果减少15%
        }
        
        return baseSpeed;
    }

    // VL衰减（请主线程定时调用）
    public void decayVLs() {
        long currentTime = System.currentTimeMillis();
        long decayIntervalMs = vlDecayIntervalSeconds * 1000; // 秒转换为毫秒

        // 对所有有VL的玩家进行衰减检查
        for (String playerName : playerVLs.keySet()) {
            int vl = playerVLs.get(playerName);
            if (vl > 0) {
                // 检查最后VL增加的时间
                Long lastIncreaseTime = lastVLIncreaseTime.get(playerName);
                if (lastIncreaseTime != null) {
                    if (currentTime - lastIncreaseTime > decayIntervalMs) {
                        // 如果超过了衰减间隔时间，降低VL
                        playerVLs.put(playerName, vl - 1);
                        // 更新时间，这样会继续衰减直到VL为0
                        lastVLIncreaseTime.put(playerName, currentTime);
                    }
                } else {
                    // 如果没有记录最后增加时间，设置为当前时间
                    lastVLIncreaseTime.put(playerName, currentTime);
                }
            }
        }
    }

    // 管理员和控制台均收到检测告警
    private void sendDetectionAlert(Player player, String type, int currentVL, int maxVL) {
        String text = ChatColor.RED + "[NAC] " + ChatColor.WHITE +
            "玩家" + player.getName() + "触发" + type + "检测 " +
            ChatColor.AQUA + currentVL + "/" + maxVL + " VL";

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("nac.admin")) {
                online.sendMessage(text);
            }
        }
        Bukkit.getLogger().info(text.replace("§", "&"));
    }

    // 可暴露getVL接口用于外部调用
    public int getPlayerVL(String name) {
        return playerVLs.getOrDefault(name, 0);
    }
    
    // 重载配置
    public void reloadConfig() {
        YamlConfiguration config = plugin.getNACConfig();
        this.maxVL = config.getInt("speed.max_vl", 10);
        this.punishCommands = config.getStringList("speed.commands_on_max_vl");
        if (this.punishCommands.isEmpty()) {
            this.punishCommands = Arrays.asList("kick {player} 速度作弊");
        }
        // 配置文件中的单位是秒
        this.vlDecayIntervalSeconds = config.getInt("speed.vl_decay_interval_seconds", 30);
    }
    
    // VL存储（保持与原有接口兼容）
    private final Map<String, Integer> playerVLs = new HashMap<>();
}