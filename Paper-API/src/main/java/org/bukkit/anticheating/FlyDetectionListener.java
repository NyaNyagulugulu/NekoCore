package org.bukkit.anticheating;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

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
    private int vlDecayIntervalTicks = 30; // 建议由主线程驱动定时衰减
    private List<String> punishCommands = List.of("kick {player} 禁止飞行作弊");

    public FlyDetectionListener() {}

    // 主动检测所有在线玩家（每tick或定时调用）
    public void checkAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp() || player.getAllowFlight() || player.isGliding()) continue;
            String playerName = player.getName();
            Location loc = player.getLocation();
            boolean onGround = player.isOnGround();

            // 检查是否在空气且没有特殊豁免
            if (!onGround && !isInLiquid(loc) && !isOnClimbable(loc) && !player.isInsideVehicle()) {
                int airTicks = consecutiveAirTicks.getOrDefault(playerName, 0) + 1;
                consecutiveAirTicks.put(playerName, airTicks);

                if (airTicks > 20) {
                    int vl = playerVLs.getOrDefault(playerName, 0) + 1;
                    playerVLs.put(playerName, vl);

                    sendDetectionAlert(player, "飞行", vl, maxVL);

                    if (vl >= maxVL) {
                        for (String cmd : punishCommands) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", playerName));
                        }
                        playerVLs.put(playerName, 0);
                    }
                }
            } else {
                consecutiveAirTicks.put(playerName, 0);
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

    // 可选：按需设置参数
    public void setMaxVL(int maxVL) { this.maxVL = maxVL; }
    public void setVlDecayIntervalTicks(int ticks) { this.vlDecayIntervalTicks = ticks; }
    public void setPunishCommands(List<String> cmds) { this.punishCommands = cmds; }

    // 可暴露getVL接口用于外部调用
    public int getPlayerVL(String name) {
        return playerVLs.getOrDefault(name, 0);
    }
}
