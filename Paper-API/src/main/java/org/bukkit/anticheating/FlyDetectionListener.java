package org.bukkit.anticheating;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlyDetectionListener implements Listener {
    
    private final Map<String, Location> lastValidLocations = new HashMap<>();
    private final Map<String, Location> previousLocations = new HashMap<>();
    private final Map<String, Integer> consecutiveAirTicks = new HashMap<>();
    private final NekoAntiCheating plugin;
    
    public FlyDetectionListener(NekoAntiCheating plugin) {
        this.plugin = plugin;
        startVLDecayTask();
        startContinuousCheckTask();
    }
    
    public void reloadConfig() {
        // 重新加载配置时可以执行的操作
        Bukkit.getLogger().info("[NAC] 飞行检测配置已重新加载");
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        
        // 跳过有nac.bypass.fly权限的玩家（包括管理员）
        if (player.hasPermission("nac.bypass.fly")) {
            return;
        }
        
        // 获取当前配置
        YamlConfiguration config = plugin.getNACConfig();
        int maxVL = config.getInt("fly.max_vl", 10);
        
        // 获取玩家位置信息
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // 更新位置记录
        previousLocations.put(playerName, from);
        lastValidLocations.put(playerName, to);
        
        // 检查玩家是否在飞行
        if (isFlying(player, from, to)) {
            // 增加VL值
            int currentVL = plugin.getPlayerVLs().getOrDefault(playerName, 0);
            currentVL++;
            plugin.getPlayerVLs().put(playerName, currentVL);
            
            // 向有权限的管理员发送警报并在控制台同步输出
            sendAlertToAdmins(player, currentVL, maxVL);
            
            // 检查是否超过最大VL值
            if (currentVL >= maxVL) {
                // 执行指令
                executeCommandsOnMaxVL(player, config.getStringList("fly.commands_on_max_vl"));
                // 重置VL
                plugin.getPlayerVLs().put(playerName, 0);
            }
            
            // 取消玩家的移动，阻止非法飞行
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        
        // 如果玩家没有nac.bypass.fly权限，但试图启用飞行，则取消
        if (!player.hasPermission("nac.bypass.fly") && event.isFlying()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "飞行已被NAC反作弊系统禁用！");
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        
        // 重置玩家的计数器
        consecutiveAirTicks.put(playerName, 0);
    }
    
    private boolean isFlying(Player player, Location from, Location to) {
        // 检查玩家是否在地面
        boolean onGround = player.isOnGround();
        
        // 获取位置变化
        double deltaX = to.getX() - from.getX();
        double deltaY = to.getY() - from.getY();
        double deltaZ = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double totalDistance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        
        // 检查是否在合法的移动状态
        Location currentLoc = player.getLocation();
        
        // 检查是否在液体或特殊方块上
        if (isInLiquid(currentLoc) || isOnClimbable(currentLoc) || 
            currentLoc.getBlock().getType() == Material.WEB) {
            // 重置连续空中计数器
            consecutiveAirTicks.put(player.getName(), 0);
            return false; // 这些是合法的移动
        }
        
        // 检查是否在方块内部
        Material blockType = currentLoc.getBlock().getType();
        if (blockType != Material.AIR && blockType != Material.LONG_GRASS && 
            blockType != Material.RED_ROSE && blockType != Material.YELLOW_FLOWER) {
            // 重置连续空中计数器
            consecutiveAirTicks.put(player.getName(), 0);
            return false; // 可能是穿模或传送
        }
        
        // 检查是否在骑乘实体
        if (player.isInsideVehicle()) {
            // 重置连续空中计数器
            consecutiveAirTicks.put(player.getName(), 0);
            return false;
        }
        
        // 检查是否在创造模式
        if (player.getAllowFlight()) {
            // 重置连续空中计数器
            consecutiveAirTicks.put(player.getName(), 0);
            return false;
        }
        
        // 更新连续空中计数器
        String playerName = player.getName();
        int airTicks = consecutiveAirTicks.getOrDefault(playerName, 0);
        if (!onGround) {
            airTicks++;
            consecutiveAirTicks.put(playerName, airTicks);
        } else {
            airTicks = 0;
            consecutiveAirTicks.put(playerName, airTicks);
        }
        
        // 检查垂直速度异常（快速上升）
        if (deltaY > 0.15 && !onGround && !player.isInsideVehicle() && 
            !player.isGliding() && !hasJumpPotion(player) && 
            !player.hasPotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION) &&
            !player.hasPotionEffect(org.bukkit.potion.PotionEffectType.SPEED)) {
            
            Bukkit.getLogger().info("[NAC] 检测到垂直速度异常: " + player.getName() + 
                " DeltaY: " + deltaY + " 总距离: " + totalDistance + 
                " 水平距离: " + horizontalDistance + " 连续空中ticks: " + airTicks);
            return true;
        }
        
        // 检查位置突变（瞬移/飞行）
        if (totalDistance > 0.5 && !onGround && horizontalDistance > 0.5 && 
            !player.isInsideVehicle() && !player.isGliding()) {
            
            Bukkit.getLogger().info("[NAC] 检测到位置突变: " + player.getName() + 
                " 总距离: " + totalDistance + " DeltaY: " + deltaY + " 连续空中ticks: " + airTicks);
            return true;
        }
        
        // 检查长时间离地（可能是飞行）
        if (airTicks > 20 && !player.isGliding() && !player.isSneaking() && 
            !hasJumpPotion(player) && !player.hasPotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION) &&
            !isInLiquid(currentLoc) && !isOnClimbable(currentLoc) && 
            currentLoc.getBlock().getType() != Material.WEB) {
            
            Bukkit.getLogger().info("[NAC] 检测到长时间离地: " + player.getName() + 
                " 连续空中ticks: " + airTicks + " Y位置: " + currentLoc.getY());
            return true;
        }
        
        // 悬停检测：如果Y坐标几乎不变但长时间不接触地面
        if (Math.abs(deltaY) < 0.01 && !onGround && horizontalDistance < 0.1 && 
            !player.isSneaking() && airTicks > 10) {
            
            Bukkit.getLogger().info("[NAC] 检测到悬停行为: " + player.getName() + 
                " 连续空中ticks: " + airTicks);
            return true;
        }
        
        return false;
    }
    
    private void startContinuousCheckTask() {
        // 创建一个持续检查任务，每秒检查一次所有在线玩家
        new BukkitRunnable() {
            @Override
            public void run() {
                // 确保Bukkit已初始化且有世界
                if (Bukkit.getWorlds().isEmpty()) {
                    return;
                }
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // 跳过有nac.bypass.fly权限的玩家
                    if (player.hasPermission("nac.bypass.fly")) {
                        continue;
                    }
                    
                    // 检查玩家是否在创造模式
                    if (player.getAllowFlight()) {
                        continue;
                    }
                    
                    // 获取当前配置
                    YamlConfiguration config = plugin.getNACConfig();
                    int maxVL = config.getInt("fly.max_vl", 10);
                    
                    Location currentLoc = player.getLocation();
                    boolean onGround = player.isOnGround();
                    
                    String playerName = player.getName();
                    int airTicks = consecutiveAirTicks.getOrDefault(playerName, 0);
                    
                    // 更新连续空中计数器
                    if (!onGround) {
                        airTicks++;
                        consecutiveAirTicks.put(playerName, airTicks);
                    } else {
                        airTicks = 0;
                        consecutiveAirTicks.put(playerName, airTicks);
                    }
                    
                    // 检查长时间离地
                    if (airTicks > 20 && !player.isGliding() && !player.isSneaking() && 
                        !hasJumpPotion(player) && !player.hasPotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION) &&
                        !isInLiquid(currentLoc) && !isOnClimbable(currentLoc) && 
                        currentLoc.getBlock().getType() != Material.WEB) {
                        
                        // 增加VL值
                        int currentVL = plugin.getPlayerVLs().getOrDefault(playerName, 0);
                        currentVL++;
                        plugin.getPlayerVLs().put(playerName, currentVL);
                        
                        // 向有权限的管理员发送警报并在控制台同步输出
                        sendAlertToAdmins(player, currentVL, maxVL);
                        
                        // 检查是否超过最大VL值
                        if (currentVL >= maxVL) {
                            // 执行令
                            executeCommandsOnMaxVL(player, config.getStringList("fly.commands_on_max_vl"));
                            // 重置VL
                            plugin.getPlayerVLs().put(playerName, 0);
                        }
                        
                        // 发送警告给玩家
                        player.sendMessage(ChatColor.RED + "您被检测到飞行，请停止使用飞行作弊!");
                    }
                }
            }
        }.runTaskTimerAsynchronously(Bukkit.getWorlds().get(0), 20L, 20L); // 每秒运行一次
    }
    
    private double getGroundLevel(Location loc) {
        // 获取地面高度的简单方法
        Location groundLoc = loc.clone();
        int attempts = 0;
        while (groundLoc.getY() > 0 && !isGroundBlock(groundLoc.getBlock().getType()) && attempts < 256) {
            groundLoc = groundLoc.subtract(0, 1, 0);
            attempts++;
        }
        return groundLoc.getY();
    }
    
    private boolean isGroundBlock(Material type) {
        // 检查是否为地面方块
        return type.isSolid() && type != Material.FENCE && type != Material.FENCE_GATE &&
               type != Material.WALL_SIGN && type != Material.SIGN_POST;
    }
    
    private boolean isInLiquid(Location loc) {
        // 检查玩家是否在水中或岩浆中
        Material type = loc.getBlock().getType();
        return type == Material.WATER || type == Material.STATIONARY_WATER || 
               type == Material.LAVA || type == Material.STATIONARY_LAVA;
    }
    
    private boolean isOnClimbable(Location loc) {
        // 检查玩家是否在爬梯子或藤蔓上
        Material type = loc.getBlock().getType();
        return type == Material.LADDER || type == Material.VINE;
    }
    
    private boolean hasJumpPotion(Player player) {
        // 检查玩家是否有跳跃药水效果
        return player.hasPotionEffect(org.bukkit.potion.PotionEffectType.JUMP);
    }
    
    private void sendAlertToAdmins(Player player, int currentVL, int maxVL) {
        String alertMessage = ChatColor.RED + "[NAC] " + ChatColor.WHITE + 
                             player.getName() + " 触发飞行检测 " + 
                             ChatColor.AQUA + "VL " + currentVL + "/" + maxVL;
        
        // 向有nac.admin权限的玩家发送警报
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("nac.admin")) {
                onlinePlayer.sendMessage(alertMessage);
            }
        }
        
        // 在控制台同步输出警报
        Bukkit.getLogger().info(alertMessage.replace("§", "&"));
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
        }.runTaskLaterAsynchronously(Bukkit.getWorlds().get(0), 100); // 延迟5秒开始
        
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
        }.runTaskTimerAsynchronously(Bukkit.getWorlds().get(0), 
            20L * plugin.getNACConfig().getInt("fly.vl_decay_interval_seconds", 30), 
            20L * plugin.getNACConfig().getInt("fly.vl_decay_interval_seconds", 30));
    }
}