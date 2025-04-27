package dev.xhue.geyserCommandsYML;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.api.GeyserApi;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final GeyserCommandsYML plugin;
    private static final String PREFIX = ChatColor.DARK_AQUA + "GeyserCommandsYML " + ChatColor.DARK_GRAY + "| " + ChatColor.RESET;

    public CommandHandler(GeyserCommandsYML plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Usage: /geysercommands <add|list|reload> [args]");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "add":
                if (!sender.hasPermission(plugin.getConfig().getString("add-permission", "geysercommands.add"))) {
                    sender.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', "&cYou don't have permission to use this command."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Usage: /geysercommands add <command>");
                    return true;
                }
                String newCommand = args[1].startsWith("/") ? args[1].substring(1) : args[1];
                File commandsFile = new File(plugin.getDataFolder(), "commands.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(commandsFile);
                List<String> commands = config.getStringList("bedrock-commands");
                if (commands.contains(newCommand)) {
                    sender.sendMessage(PREFIX + ChatColor.YELLOW + "Command '" + newCommand + "' is already in the list.");
                    return true;
                }
                commands.add(newCommand);
                config.set("bedrock-commands", commands);
                try {
                    config.save(commandsFile);
                    plugin.loadCommandsConfig();
                    sender.sendMessage(PREFIX + ChatColor.GREEN + "Command '" + newCommand + "' has been added successfully!");
                } catch (IOException e) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "An error occurred while saving the command. Try deleting the commands.yml file.");
                    e.printStackTrace();
                }
                return true;

            case "list":
                if (!sender.hasPermission(plugin.getConfig().getString("list-permission", "geysercommands.list"))) {
                    sender.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', "&cYou don't have permission to use this command."));
                    return true;
                }
                List<String> listCommands = plugin.getConfig().getStringList("bedrock-commands");
                if (listCommands == null || listCommands.isEmpty()) {
                    listCommands = plugin.getCommandsConfig().getStringList("bedrock-commands");
                }
                if (listCommands == null || listCommands.isEmpty()) {
                    sender.sendMessage(PREFIX + ChatColor.YELLOW + "No Bedrock commands have been added yet.");
                    return true;
                }
                sender.sendMessage(PREFIX + "\n" + ChatColor.GOLD + "Bedrock Commands List" + ChatColor.DARK_GRAY + " [" + ChatColor.GRAY + listCommands.size() + ChatColor.DARK_GRAY + "]:");
                for (String cmd : listCommands) {
                    sender.sendMessage(ChatColor.DARK_GRAY + "| " + ChatColor.GRAY + "- /" + ChatColor.WHITE + cmd);
                }
                return true;

            case "remove":
                if (!sender.hasPermission(plugin.getConfig().getString("remove-permission", "geysercommands.remove"))) {
                    sender.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', "&cYou don't have permission to use this command."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Usage: /geysercommands remove <command>");
                    return true;
                }
                String commandToRemove = args[1].startsWith("/") ? args[1].substring(1) : args[1];
                File commandsFileRemove = new File(plugin.getDataFolder(), "commands.yml");
                FileConfiguration configRemove = YamlConfiguration.loadConfiguration(commandsFileRemove);
                List<String> commandsRemove = configRemove.getStringList("bedrock-commands");
                if (!commandsRemove.contains(commandToRemove)) {
                    sender.sendMessage(PREFIX + ChatColor.YELLOW + "Command '" + commandToRemove + "' is not in the list.");
                    return true;
                }
                commandsRemove.remove(commandToRemove);
                configRemove.set("bedrock-commands", commandsRemove);
                try {
                    configRemove.save(commandsFileRemove);
                    plugin.loadCommandsConfig();
                    sender.sendMessage(PREFIX + ChatColor.GREEN + "Command '" + commandToRemove + "' has been removed successfully!");
                } catch (IOException e) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "An error occurred while removing the command. Try deleting the commands.yml file.");
                    e.printStackTrace();
                }
                return true;

            case "reload":
                if (!sender.hasPermission(plugin.getConfig().getString("reload-permission", "geysercommands.reload"))) {
                    sender.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', "&cYou don't have permission to use this command."));
                    return true;
                }
                plugin.reloadConfig();
                plugin.loadCommandsConfig();
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()) || GeyserApi.api().isBedrockPlayer(player.getUniqueId())) {
                        player.kickPlayer(PREFIX + "Commands have been reloaded.\n Please rejoin to see the changes.");
                    }
                }
                sender.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', "&aConfiguration reloaded successfully!"));
                return true;

            default:
                sender.sendMessage(PREFIX + ChatColor.RED + "Unknown subcommand. Usage: /geysercommands <add|list|reload> [args]");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Suggest subcommands
            List<String> subcommands = new ArrayList<>();
            if (sender.hasPermission("geysercommands.add")) subcommands.add("add");
            if (sender.hasPermission("geysercommands.list")) subcommands.add("list");
            if (sender.hasPermission("geysercommands.remove")) subcommands.add("remove");
            if (sender.hasPermission("geysercommands.reload")) subcommands.add("reload");
            return subcommands;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            // Suggest commands for the "remove" subcommand
            return plugin.getCommandsConfig().getStringList("bedrock-commands");
        }
        return Collections.emptyList();
    }
    
}
