package org.bukkit.anticheating;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 非插件型飞行检测器
 * 每tick或定时主动调用 checkAllPlayers()
 */
public class FlyDetectionListener {

    private final Map<String, Integer> playerVLs = new HashMap<>();
    private final Map<String, Integer> consecutiveAirTicks = new HashMap<>();
    private int maxVL = 10;
    private int vlDecayIntervalTicks = 30; // VL衰减间隔ticks
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

            // 检查是否在空气且没有特殊豁免
            if (!onGround && !isInLiquid(loc) && !isOnClimbable(loc) && !player.isInsideVehicle()) {
                // 获取玩家速度信息
                double yVelocity = player.getVelocity().getY();
                double horizontalSpeed = Math.sqrt(
                    Math.pow(player.getVelocity().getX(), 2) + 
                    Math.pow(player.getVelocity().getZ(), 2)
                );
                
                // 检查是否在正常掉落
                boolean isFalling = yVelocity < -0.08;
                
                // 检查是否有水平移动（飞行作弊通常有异常水平移动）
                boolean hasHorizontalMovement = horizontalSpeed > 0.1;
                
                // 检查是否在方块附近（正常跳跃）
                boolean isNearBlocks = isNearSolidBlocks(loc);
                
                // 检查异常飞行模式
                if (isSuspiciousFlight(player, loc, yVelocity, horizontalSpeed)) {
                    int vl = playerVLs.getOrDefault(playerName, 0) + 1;
                    playerVLs.put(playerName, vl);

                    sendDetectionAlert(player, "飞行", vl, maxVL);

                    if (vl >= maxVL) {
                        // 直接执行命令，因为我们现在在主线程中
                        for (String cmd : punishCommands) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", playerName));
                        }
                        playerVLs.put(playerName, 0);
                    }
                } else if (!(isFalling && isNearBlocks)) {
                    // 如果不是正常掉落且不在方块附近，重置VL
                    playerVLs.put(playerName, 0);
                }
            } else {
                // 在地面上或特殊环境中，重置VL
                playerVLs.put(playerName, 0);
            }
        }
    }

    // VL衰减（请主线程定时调用，如每vlDecayIntervalTicks/tick执行）
    public void decayVLs() {
        for (String playerName : playerVLs.keySet()) {
            int vl = playerVLs.get(playerName);
            if (vl > 0) playerVLs.put(playerName, vl - 1);
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
    
    // 检测玩家是否在方块边缘（可能的正常跳跃）
    private boolean isOnBlockEdge(Location loc) {
        // 获取玩家脚下的方块坐标
        int x = loc.getBlockX();
        int y = loc.getBlockY() - 1; // 检查下方方块
        int z = loc.getBlockZ();
        
        // 检查玩家是否在方块的边缘，这通常是正常跳跃的迹象
        double xFrac = loc.getX() - x;
        double zFrac = loc.getZ() - z;
        
        // 如果玩家在方块的边缘（x或z坐标接近0.0或1.0），则认为是正常情况
        return (xFrac < 0.3 || xFrac > 0.7) || (zFrac < 0.3 || zFrac > 0.7);
    }
    
    // 检测玩家是否在固体方块附近（用于区分正常掉落和飞行）
    private boolean isNearSolidBlocks(Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        
        // 检查周围几个方块
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 1; dy++) { // 向下检查更多
                    Material m = loc.getWorld().getBlockAt(x + dx, y + dy, z + dz).getType();
                    if (m.isSolid() && m != Material.LADDER && m != Material.VINE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    // 检测可疑的飞行行为
    private boolean isSuspiciousFlight(Player player, Location loc, double yVelocity, double horizontalSpeed) {
        // 检查是否在空中停留时间过长
        String playerName = player.getName();
        int airTicks = consecutiveAirTicks.getOrDefault(playerName, 0);
        consecutiveAirTicks.put(playerName, airTicks + 1);
        
        // 如果Y速度接近0（悬停）且有水平移动，则可能是飞行
        if (Math.abs(yVelocity) < 0.01 && horizontalSpeed > 0.05) {
            return true;
        }
        
        // 如果Y速度为正（向上飞）且不是在方块附近
        if (yVelocity > 0.01 && !isNearSolidBlocks(loc)) {
            return true;
        }
        
        // 检查异常的水平移动速度（在空中）
        if (horizontalSpeed > 0.25 && yVelocity < 0.01) { // 在空中移动速度过快
            return true;
        }
        
        // 检查是否悬停
        if (Math.abs(yVelocity) < 0.01 && horizontalSpeed < 0.05 && airTicks > 20) {
            return true;
        }
        
        // 如果在空中时间过长（超过一定ticks）且没有明显下落
        if (airTicks > 50 && yVelocity > -0.1) {
            return true;
        }
        
        return false;
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
        // 将秒转换为tick
        int decayIntervalSeconds = config.getInt("fly.vl_decay_interval_seconds", 30);
        this.vlDecayIntervalTicks = decayIntervalSeconds * 20; // 假设20 ticks per second
    }
}