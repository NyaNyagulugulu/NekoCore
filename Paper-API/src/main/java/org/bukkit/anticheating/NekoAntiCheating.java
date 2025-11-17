package org.bukkit.anticheating;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
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
    private SpeedDetectionListener speedDetectionListener;
    private BukkitRunnable flyDetectionTask;
    private BukkitRunnable speedDetectionTask;
    private BukkitRunnable vlDecayTask;

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
            loadSpeedDetection();

            logger.info("\n§c§l◆ NekoCoreCiallo～(∠・ω< )⌒★私のおなにー见てください \n§6◆ NekoAntiCheating 已启用喵～ \n");
        } else {
            logger.info("\n§7 NekoCoreCiallo～(∠・ω< )⌒★私のおなにー见てください \n§b NekoAntiCheating 已在配置中禁用喵～ \n");
        }
    }

    private void loadFlyDetection() {
        flyDetectionListener = new FlyDetectionListener(this);
        logger.info("[NAC] 飞行检测模块已加载");
    }
    
    private void loadSpeedDetection() {
        speedDetectionListener = new SpeedDetectionListener(this);
        logger.info("[NAC] 速度检测模块已加载");
    }

    private void createDefaultConfig() {
        YamlConfiguration defaultConfig = new YamlConfiguration();
        defaultConfig.set("enable-NAC", true);
        defaultConfig.set("fly.max_vl", 10);
        defaultConfig.set("fly.commands_on_max_vl", new String[]{"kick {player} 飞你妈呢"});
        defaultConfig.set("fly.vl_decay_interval_seconds", 30);
        defaultConfig.set("speed.max_vl", 10);
        defaultConfig.set("speed.commands_on_max_vl", new String[]{"kick {player} 速度作弊"});
        defaultConfig.set("speed.vl_decay_interval_seconds", 30);

        try {
            defaultConfig.save(configFile);
        } catch (IOException e) {
            logger.severe("无法创建默认配置文件: " + e.getMessage());
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
        if (speedDetectionListener != null) {
            speedDetectionListener.reloadConfig();
        }
    }
    public Map<String, Integer> getPlayerVLs() { return playerVLs; }
    
    // 获取飞行检测监听器实例
    public FlyDetectionListener getFlyDetectionListener() {
        return flyDetectionListener;
    }
    
    // 获取速度检测监听器实例
    public SpeedDetectionListener getSpeedDetectionListener() {
        return speedDetectionListener;
    }
    
    // 供服务器内部调用以启动调度器
    public void startScheduler(Plugin plugin) {
        if (flyDetectionListener == null || speedDetectionListener == null) {
            return;
        }
        
        // 每tick执行一次飞行检测（约20次/秒）
        flyDetectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                flyDetectionListener.checkAllPlayers();
            }
        };
        flyDetectionTask.runTaskTimer(plugin, 1L, 1L); // 每1 tick执行一次（同步）
        
        // 每tick执行一次速度检测（约20次/秒）
        speedDetectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                speedDetectionListener.checkAllPlayers();
            }
        };
        speedDetectionTask.runTaskTimer(plugin, 1L, 1L); // 每1 tick执行一次（同步）
        
        // 每秒执行一次VL衰减
        vlDecayTask = new BukkitRunnable() {
            @Override
            public void run() {
                flyDetectionListener.decayVLs();
                speedDetectionListener.decayVLs();
            }
        };
        vlDecayTask.runTaskTimer(plugin, 20L, 20L); // 每秒执行一次（20 ticks）
        
        logger.info("[NAC] 调度器已启动");
    }
    
    // 作为内建功能启动调度器（不依赖插件）
    public void startSchedulerAsBuiltin(org.bukkit.Server server) {
        if (flyDetectionListener == null || speedDetectionListener == null) {
            return;
        }
        
        // 由于Bukkit的调度器需要Plugin实例，我们尝试获取一个可用的插件
        // 或者使用CraftServer的插件管理器获取一个插件
        org.bukkit.plugin.Plugin[] plugins;
        try {
            plugins = server.getPluginManager().getPlugins();
        } catch (Exception e) {
            logger.severe("无法获取插件列表: " + e.getMessage());
            return;
        }
        
        org.bukkit.plugin.Plugin mainPlugin = null;
        if (plugins.length > 0) {
            // 通常Plugin[0]会是主插件，但我们可以尝试找到一个合适的插件
            mainPlugin = plugins[0];
        } else {
            // 如果没有插件，创建一个虚拟的插件或使用其他方式
            // 但在这个API层，我们无法创建插件，所以我们需要在服务器端处理
            logger.warning("没有可用插件来启动NAC调度器");
            return;
        }
        
        // 使用找到的插件启动调度器
        startScheduler(mainPlugin);
    }
    
    // 供服务器内部调用以关闭调度器
    public void shutdown() {
        if (flyDetectionTask != null) {
            flyDetectionTask.cancel();
        }
        if (speedDetectionTask != null) {
            speedDetectionTask.cancel();
        }
        if (vlDecayTask != null) {
            vlDecayTask.cancel();
        }
    }
}
