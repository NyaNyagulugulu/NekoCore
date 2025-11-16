package org.bukkit.anticheating;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.command.defaults.NacCommand;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class NekoAntiCheating {
    private static NekoAntiCheating instance;
    private YamlConfiguration config;
    private File configFile;
    private Logger logger;
    private final Map<String, Integer> playerVLs = new HashMap<>();
    private FlyDetectionListener flyDetectionListener;

    public NekoAntiCheating() {}

    public static void init() {
        instance = new NekoAntiCheating();
        instance.enable();
    }

    public void enable() {
        logger = Bukkit.getLogger();

        configFile = new File("NAC.yml");
        if (!configFile.exists()) {
            createDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        if (config.getBoolean("enable-NAC", true)) {
            loadFlyDetection();

            Plugin paperPlugin = Bukkit.getPluginManager().getPlugin("Paper");
            if (paperPlugin != null) {
                Bukkit.getPluginManager().registerEvents(flyDetectionListener, paperPlugin);
            } else {
                Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
                if (plugins.length > 0) {
                    Bukkit.getPluginManager().registerEvents(flyDetectionListener, plugins[0]);
                }
            }

            registerCommands();
            logger.info("\n§\n§c§l◆ NekoCoreCiallo～(∠・ω< )⌒★私のおなにー见てください \n§6◆ NekoAntiCheating 已启用喵～ \n§\n");
        } else {
            logger.info("\n§\n§7 NekoCoreCiallo～(∠・ω< )⌒★私のおなにー见てください \n§b NekoAntiCheating 已在配置中禁用喵～ \n§\n");
        }
    }

    private void loadFlyDetection() {
        flyDetectionListener = new FlyDetectionListener(this);
        logger.info("[NAC] 飞行检测模块已加载");
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
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            commandMap.register("nac", new NacCommand("nac"));
        } catch (Exception e) {
            logger.severe("无法注册NAC命令: " + e.getMessage());
        }
    }

    public static NekoAntiCheating getInstance() { return instance; }
    public YamlConfiguration getNACConfig() { return config; }
    public void reloadNACConfig() {
        configFile = new File("NAC.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        logger.info("NAC配置已重载");
        if (flyDetectionListener != null) {
            flyDetectionListener.reloadConfig();
        }
    }
    public Map<String, Integer> getPlayerVLs() { return playerVLs; }
}
