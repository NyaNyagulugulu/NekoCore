package org.bukkit.command.defaults;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.anticheating.NekoAntiCheating;

public class NacCommand extends Command {
    public NacCommand(String name) {
        super(name);
        this.description = "NekoAntiCheating command";
        this.usageMessage = "/nac <reload>";
        this.setPermission("nac.command");
    }
    
    @Override
    public boolean execute(CommandSender sender, String currentAlias, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage("§c用法: /nac <reload>");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            // 重载配置文件
            NekoAntiCheating.getInstance().reloadNACConfig();
            sender.sendMessage("§aNAC配置已重载！");
            return true;
        }
        
        sender.sendMessage("§c未知的子命令。用法: /nac <reload>");
        return true;
    }
}