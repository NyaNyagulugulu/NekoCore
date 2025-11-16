package org.bukkit.anticheating;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.bukkit.command.defaults.NacCommand;

public class NekoAntiCheating {
    
    private static NekoAntiCheating instance;
    private YamlConfiguration config;
    private File configFile;
    private Logger logger;
    private final Map<String, Integer> playerVLs = new HashMap<>();
    private final FlyDetectionListener flyDetectionListener;
    
    public NekoAntiCheating() {
        flyDetectionListener = new FlyDetectionListener(this);
    }
    
    public static void init() {
        instance = new NekoAntiCheating();
        instance.enable();
    }
    
    public void enable() {
        logger = Bukkit.getLogger();
        
        // 初始化配置文件 - 直接在根目录创建
        configFile = new File("NAC.yml");
        
        // 如果配置文件不存在，则创建默认配置文件
        if (!configFile.exists()) {
            // 创建默认配置
            createDefaultConfig();
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 检查是否启用NAC
        if (config.getBoolean("enable-NAC", true)) {
            // 注册飞行检测监听器
            Plugin paperPlugin = Bukkit.getPluginManager().getPlugin("Paper");
            if (paperPlugin != null) {
                Bukkit.getPluginManager().registerEvents(flyDetectionListener, paperPlugin);
            } else {
                // 如果没有Paper插件，尝试获取第一个可用的插件
                org.bukkit.plugin.Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
                if (plugins.length > 0) {
                    Bukkit.getPluginManager().registerEvents(flyDetectionListener, plugins[0]);
                }
            }

            // 注册命令
            registerCommands();
            logger.info("\n" +
                "§4\n" +
                "§c NekoCoreCiallo～(∠・ω< )⌒★私のおなにー見てください \n" +
                "§6 NekoAntiCheating 已启用喵～ \n" +
                "§e\n"
            );
        } else {
            logger.info("\n" +
                "§8\n" +
                "§7 NekoCoreCiallo～(∠・ω< )⌒★私のおなにー見てください \n" +
                "§b NekoAntiCheating 已在配置中禁用喵～ \n" +
                "§9\n"
            );
        }
    }
    
    private void createDefaultConfig() {
        YamlConfiguration defaultConfig = new YamlConfiguration();
        defaultConfig.set("enable-NAC", true);
        defaultConfig.set("fly.max_vl", 10);
        defaultConfig.set("fly.commands_on_max_vl", new String[]{"kick {player} 飞你妈呢"});
        defaultConfig.set("fly.vl_decay_interval_seconds", 30);
        
        try {
            defaultConfig.save(configFile);
        } catch (IOException e) {
            logger.severe("无法创建默认配置文件: " + e.getMessage());
        }
    }
    
    private void registerCommands() {
        try {
            // 使用反射获取CommandMap
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            
            // 注册命令
            commandMap.register("nac", new NacCommand("nac"));
        } catch (Exception e) {
            logger.severe("无法注册NAC命令: " + e.getMessage());
        }
    }
    
    public static NekoAntiCheating getInstance() {
        return instance;
    }
    
    public YamlConfiguration getNACConfig() {
        return config;
    }
    
    public void reloadNACConfig() {
        configFile = new File("NAC.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        logger.info("NAC配置已重载");
    }
    
    public Map<String, Integer> getPlayerVLs() {
        return playerVLs;
    }
}
