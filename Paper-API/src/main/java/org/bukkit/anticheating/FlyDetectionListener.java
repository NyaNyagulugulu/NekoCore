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
 * 非插件型飞行检测器
 * 基于服务器物理状态、连续位置变化、玩家动作和方块状态来判定飞行行为
 * 每tick或定时主动调用 checkAllPlayers()
 */
public class FlyDetectionListener {

    // 玩家状态数据
    private final Map<String, Double> lastVelY = new HashMap<>(); // 上一tick的Y速度
    private final Map<String, Integer> airTicks = new HashMap<>(); // 空中持续tick数
    private final Map<String, Double> suspiciousValue = new HashMap<>(); // 可疑值
    private final Map<String, Location> lastLocation = new HashMap<>(); // 上一位置
    private final Map<String, Long> lastVLIncreaseTime = new HashMap<>(); // 最后VL增加时间
    
    private int maxVL = 10;
    private int vlDecayIntervalSeconds = 30; // VL衰减间隔秒数
    private List<String> punishCommands = Arrays.asList("kick {player} 禁止飞行作弊");
    private NekoAntiCheating plugin;

    public FlyDetectionListener(NekoAntiCheating plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    // 主动检测所有在线玩家（每tick或定时调用）
    public void checkAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 检查是否拥有豁免权限
            if (player.hasPermission("nac.bypass.fly") || player.isOp() || player.getAllowFlight() || player.isGliding()) continue;
            
            String playerName = player.getName();
            Location loc = player.getLocation();
            boolean onGround = player.isOnGround();
            Location lastLoc = lastLocation.get(playerName);
            
            // 保存当前位置
            lastLocation.put(playerName, loc.clone());
            
            // 如果没有上一位置，跳过本次检测
            if (lastLoc == null) continue;
            
            // 计算速度
            double velX = loc.getX() - lastLoc.getX();
            double velY = loc.getY() - lastLoc.getY();
            double velZ = loc.getZ() - lastLoc.getZ();
            double horizontalSpeed = Math.sqrt(velX * velX + velZ * velZ);
            
            // 获取上一tick的Y速度
            Double lastYVel = lastVelY.get(playerName);
            if (lastYVel == null) lastYVel = 0.0;
            lastVelY.put(playerName, velY);
            
            // 更新空中tick数
            int currentAirTicks = airTicks.getOrDefault(playerName, 0);
            if (!onGround && !isInLiquid(loc) && !isOnClimbable(loc) && !player.isInsideVehicle()) {
                currentAirTicks++;
            } else {
                currentAirTicks = 0;
            }
            airTicks.put(playerName, currentAirTicks);
            
            // 特殊状态排除检查
            if (isInLiquid(loc) || isOnClimbable(loc) || player.isInsideVehicle() || player.isGliding()) {
                // 特殊状态下重置可疑值
                suspiciousValue.put(playerName, 0.0);
                continue;
            }
            
            // 执行各项检测
            double currentSuspicious = suspiciousValue.getOrDefault(playerName, 0.0);
            
            // 1. 重力一致性检查
            currentSuspicious += checkGravityConsistency(player, velY, lastYVel, onGround);
            
            // 2. 空中单向上升检测
            currentSuspicious += checkUpwardMovement(player, velY, onGround);
            
            // 3. 横向速度异常检测
            currentSuspicious += checkHorizontalSpeed(player, horizontalSpeed, onGround);
            
            // 4. 停空检测
            currentSuspicious += checkHovering(player, velY, currentAirTicks, onGround);
            
            // 更新可疑值
            suspiciousValue.put(playerName, currentSuspicious);
            
            // 如果可疑值超过阈值，增加VL
            if (currentSuspicious >= 5.0) {
                int vl = playerVLs.getOrDefault(playerName, 0) + 1;
                playerVLs.put(playerName, vl);
                lastVLIncreaseTime.put(playerName, System.currentTimeMillis());
                
                sendDetectionAlert(player, "飞行(" + String.format("%.2f", currentSuspicious) + ")", vl, maxVL);
                
                if (vl >= maxVL) {
                    for (String cmd : punishCommands) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", playerName));
                    }
                    playerVLs.put(playerName, 0);
                    suspiciousValue.put(playerName, 0.0); // 重置可疑值
                }
                
                // 重置可疑值防止连续触发
                suspiciousValue.put(playerName, Math.max(0.0, currentSuspicious - 2.0));
            }
        }
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

    // 重力一致性检查
    private double checkGravityConsistency(Player player, double velY, double lastYVel, boolean onGround) {
        if (onGround) return 0.0;
        
        // Minecraft自然重力: velY_new = velY_last - 0.08
        double expectedVelY = lastYVel - 0.08;
        double diff = Math.abs(velY - expectedVelY);
        
        // 如果差异过大，可能是飞行
        if (diff > 0.05 && Math.abs(velY) < 0.01) {
            return 1.0; // 异常重力，增加可疑值
        }
        
        return 0.0;
    }

    // 空中单向上升检测
    private double checkUpwardMovement(Player player, double velY, boolean onGround) {
        if (onGround) return 0.0;
        
        // 空中向上移动
        if (velY > 0.01) {
            // 检查是否为合法跳跃或其他特殊情况
            if (!isLegalJump(player, velY)) {
                return 1.0; // 异常上升，增加可疑值
            }
        }
        
        return 0.0;
    }

    // 横向速度异常检测
    private double checkHorizontalSpeed(Player player, double horizontalSpeed, boolean onGround) {
        if (onGround) return 0.0;
        
        // 空中水平速度正常范围：0.3 ~ 0.42 blocks/tick
        if (horizontalSpeed > 0.45 || horizontalSpeed < 0.01) {
            return 0.5; // 横向速度异常，增加可疑值
        }
        
        return 0.0;
    }

    // 停空检测
    private double checkHovering(Player player, double velY, int airTicks, boolean onGround) {
        if (onGround) return 0.0;
        
        // 连续停空检测（推荐8~12 tick）
        if (airTicks > 10 && Math.abs(velY) < 0.01) {
            return 1.0; // 停空，增加可疑值
        }
        
        return 0.0;
    }

    // 判断是否为合法跳跃
    private boolean isLegalJump(Player player, double velY) {
        // 跳跃起跳速度约为0.42
        if (velY > 0.4 && velY < 0.5) {
            return true;
        }
        
        // 检查药水效果影响的跳跃
        if (player.hasPotionEffect(PotionEffectType.JUMP)) {
            int amplifier = player.getPotionEffect(PotionEffectType.JUMP).getAmplifier();
            double maxJumpVel = 0.42 + (amplifier + 1) * 0.1;
            if (velY > 0.4 && velY <= maxJumpVel + 0.1) {
                return true;
            }
        }
        
        return false;
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

    // -- 检查辅助 --
    private boolean isInLiquid(Location loc) {
        Material t = loc.getBlock().getType();
        return t == Material.WATER || t == Material.STATIONARY_WATER ||
            t == Material.LAVA || t == Material.STATIONARY_LAVA;
    }
    
    private boolean isOnClimbable(Location loc) {
        Material t = loc.getBlock().getType();
        return t == Material.LADDER || t == Material.VINE;
    }

    // 可暴露getVL接口用于外部调用
    public int getPlayerVL(String name) {
        return playerVLs.getOrDefault(name, 0);
    }
    
    // 重载配置
    public void reloadConfig() {
        YamlConfiguration config = plugin.getNACConfig();
        this.maxVL = config.getInt("fly.max_vl", 10);
        this.punishCommands = config.getStringList("fly.commands_on_max_vl");
        if (this.punishCommands.isEmpty()) {
            this.punishCommands = Arrays.asList("kick {player} 飞你妈呢");
        }
        // 配置文件中的单位是秒
        this.vlDecayIntervalSeconds = config.getInt("fly.vl_decay_interval_seconds", 30);
    }
    
    // VL存储（保持与原有接口兼容）
    private final Map<String, Integer> playerVLs = new HashMap<>();
}