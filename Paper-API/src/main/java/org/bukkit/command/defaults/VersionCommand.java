package org.bukkit.command.defaults;

import com.google.common.base.Charsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

// Paper start
import java.net.HttpURLConnection;
import com.destroystokyo.paper.VersionHistoryManager;
// Paper end

public class VersionCommand extends BukkitCommand {
    public VersionCommand(String name) {
        super(name);

        this.description = "Gets the version of this server including any plugins in use";
        this.usageMessage = "/version [plugin name]";
        this.setPermission("bukkit.command.version");
        this.setAliases(Arrays.asList("ver"));
    }

    @Override
    public boolean execute(CommandSender sender, String currentAlias, String[] args) {
        if (!testPermission(sender)) return true;

        if (args.length == 0) {
            String fullVersion = Bukkit.getVersion();
            // 移除 "(MC: ...)" 部分
            String cleanVersion = fullVersion.replaceAll("\\(MC: [^)]+\\)", "").trim();
            sender.sendMessage("This Server run " + cleanVersion);
            tellHistory(sender); // Paper
            sendVersion(sender); // Paper - We'll say when, thanks
        } else {
            StringBuilder name = new StringBuilder();

            for (String arg : args) {
                if (name.length() > 0) {
                    name.append(' ');
                }

                name.append(arg);
            }

            String pluginName = name.toString();
            Plugin exactPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
            if (exactPlugin != null) {
                describeToSender(exactPlugin, sender);
                return true;
            }

            boolean found = false;
            pluginName = pluginName.toLowerCase(java.util.Locale.ENGLISH);
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                if (plugin.getName().toLowerCase(java.util.Locale.ENGLISH).contains(pluginName)) {
                    describeToSender(plugin, sender);
                    found = true;
                }
            }

            if (!found) {
                sender.sendMessage("This server is not running any plugin by that name.");
                sender.sendMessage("Use /plugins to get a list of plugins.");
            }
        }
        return true;
    }

    // Paper start - show version history
    private void tellHistory(final CommandSender sender) {
        final VersionHistoryManager.VersionData data = VersionHistoryManager.INSTANCE.getVersionData();
        if (data == null) {
            return;
        }

        final String oldVersion = data.getOldVersion();
        if (oldVersion == null) {
            return;
        }

//        sender.sendMessage("Previous version: " + oldVersion);
    }
    // Paper end

    private void describeToSender(Plugin plugin, CommandSender sender) {
        PluginDescriptionFile desc = plugin.getDescription();
        sender.sendMessage(ChatColor.GREEN + desc.getName() + ChatColor.WHITE + " version " + ChatColor.GREEN + desc.getVersion());

        if (desc.getDescription() != null) {
            sender.sendMessage(desc.getDescription());
        }

        if (desc.getWebsite() != null) {
            sender.sendMessage("Website: " + ChatColor.GREEN + desc.getWebsite());
        }

        if (!desc.getAuthors().isEmpty()) {
            if (desc.getAuthors().size() == 1) {
                sender.sendMessage("Author: " + getAuthors(desc));
            } else {
                sender.sendMessage("Authors: " + getAuthors(desc));
            }
        }
    }

    private String getAuthors(final PluginDescriptionFile desc) {
        StringBuilder result = new StringBuilder();
        List<String> authors = desc.getAuthors();

        for (int i = 0; i < authors.size(); i++) {
            if (result.length() > 0) {
                result.append(ChatColor.WHITE);

                if (i < authors.size() - 1) {
                    result.append(", ");
                } else {
                    result.append(" and ");
                }
            }

            result.append(ChatColor.GREEN);
            result.append(authors.get(i));
        }

        return result.toString();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(args, "Arguments cannot be null");
        Validate.notNull(alias, "Alias cannot be null");

        if (args.length == 1) {
            List<String> completions = new ArrayList<String>();
            String toComplete = args[0].toLowerCase(java.util.Locale.ENGLISH);
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                if (StringUtil.startsWithIgnoreCase(plugin.getName(), toComplete)) {
                    completions.add(plugin.getName());
                }
            }
            return completions;
        }
        return ImmutableList.of();
    }

    private final ReentrantLock versionLock = new ReentrantLock();
    private boolean hasVersion = false;
    private String versionMessage = null;
    private final Set<CommandSender> versionWaiters = new HashSet<CommandSender>();
    private boolean versionTaskStarted = false;
    private long lastCheck = 0;

    private void sendVersion(CommandSender sender) {
        if (hasVersion) {
            if (System.currentTimeMillis() - lastCheck > 7200000) { // Paper - Lower to 2 hours
                lastCheck = System.currentTimeMillis();
                hasVersion = false;
            } else {
                if (!versionMessage.isEmpty()) { // 只有在消息不为空时才发送
                    sender.sendMessage(versionMessage);
                }
                return;
            }
        }
        versionLock.lock();
        try {
            if (hasVersion) {
                if (!versionMessage.isEmpty()) { // 只有在消息不为空时才发送
                    sender.sendMessage(versionMessage);
                }
                return;
            }
            versionWaiters.add(sender);
            // 移除"Checking version, please wait..."消息
            if (!versionTaskStarted) {
                versionTaskStarted = true;
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        obtainVersion();
                    }
                }).start();
            }
        } finally {
            versionLock.unlock();
        }
    }

    // Paper start
    private void obtainVersion() {
        String version = Bukkit.getVersion();
        if (version == null) version = "Custom";
        if (version.startsWith("NekoCoreCiallo～(∠・ω< )⌒★私のおなにー見てください-")) {
            // 直接设置为空消息，不再检查版本
            setVersionMessage("");
        } else if (version.startsWith("git-Bukkit-")) {
            // Paper end
            version = version.substring("git-Bukkit-".length());
            // 直接设置为空消息，不再检查版本
            setVersionMessage("");
        } else {
            // 直接设置为空消息，不再检查版本
            setVersionMessage("");
        }
    }

    private void setVersionMessage(String msg) {
        lastCheck = System.currentTimeMillis();
        versionMessage = msg;
        versionLock.lock();
        try {
            hasVersion = true;
            versionTaskStarted = false;
            for (CommandSender sender : versionWaiters) {
                if (!versionMessage.isEmpty()) { // 只有在消息不为空时才发送
                    sender.sendMessage(versionMessage);
                }
            }
            versionWaiters.clear();
        } finally {
            versionLock.unlock();
        }
    }

    // Paper start
    private static int getDistance(String repo, String verInfo) {
        // 直接返回0，表示版本是最新的，避免访问外部服务器
        return 0;
    }

    private static int getFromJenkins(int currentVer) {
        // 直接返回0，表示版本是最新的，避免访问外部服务器
        return 0;
    }

    private static final String BRANCH = "1.12.2";

    private static int getFromRepo(String repo, String hash) {
        // 直接返回0，表示版本是最新的，避免访问外部服务器
        return 0;
    }
}
