package dev.xhue.geyserCommandsYML;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.geysermc.floodgate.api.FloodgateApi;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.geysermc.geyser.api.GeyserApi;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GeyserCommandsYML extends JavaPlugin implements Listener {
    private FileConfiguration commandsConfig;
    // Store the processed commands (without leading slash) for efficiency
    private List<String> processedBedrockCommands = Collections.emptyList();
    // Cache plugin presence and debug status
    private boolean floodgatePresent = false;
    private boolean geyserPresent = false;
    private boolean debugEnabled = false;


    @Override
    public void onEnable() {
        // Create default config if it doesn't exist
        saveDefaultConfig();

        // Check for Floodgate and Geyser *once*
        floodgatePresent = getServer().getPluginManager().isPluginEnabled("floodgate");
        geyserPresent = getServer().getPluginManager().isPluginEnabled("Geyser-Spigot");

        // Load commands configuration and cache settings
        loadCommandsConfig();

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Register command
        getCommand("geysercommands").setExecutor(new CommandHandler(this));
        getCommand("geysercommands").setTabCompleter(new CommandHandler(this));

        getLogger().info("GeyserCommandsYML has been enabled!");
        if (!floodgatePresent && !geyserPresent) {
            getLogger().warning("Neither Floodgate nor Geyser API found. Bedrock player detection might not work.");
        } else {
            if (floodgatePresent) getLogger().info("Floodgate API detected.");
            if (geyserPresent) getLogger().info("Geyser API detected.");
        }
    }

    public void loadCommandsConfig() {
        // Ensure config directory exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Create commands.yml file if it doesn't exist
        File commandsFile = new File(getDataFolder(), "commands.yml");
        if (!commandsFile.exists()) {
            saveResource("commands.yml", false);
        }

        // Load commands configuration
        commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);

        // Cache debug setting *before* using it for logging
        // Reload config first to get the latest value
        reloadConfig();
        debugEnabled = getConfig().getBoolean("debug", false);

        // Load raw bedrock commands from config
        List<String> rawBedrockCommands = commandsConfig.getStringList("bedrock-commands");

        // Pre-process commands: remove leading slash and store in the optimized list
        processedBedrockCommands = rawBedrockCommands.stream()
                .map(command -> command.startsWith("/") ? command.substring(1) : command)
                .collect(Collectors.toList());

        getLogger().info("Loaded and processed " + processedBedrockCommands.size() + " custom commands for Bedrock players.");
        if (debugEnabled) {
            getLogger().info("Debug mode enabled.");
        }
    }

    @EventHandler
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();

        // Check if player is from Bedrock using cached API presence
        if (isBedrockPlayer(player)) {
            // Clear default commands
            event.getCommands().clear();

            // Add our pre-processed custom commands efficiently
            event.getCommands().addAll(processedBedrockCommands);

            if (debugEnabled) {
                getLogger().info("Sent " + processedBedrockCommands.size() + " custom command suggestions to Bedrock player: " + player.getName());
            }
        }
    }

    private boolean isBedrockPlayer(Player player) {
        boolean isFloodgatePlayer = false;
        boolean isGeyserPlayer = false;

        // Check Floodgate API only if the plugin is present
        if (floodgatePresent) {
            try {
                isFloodgatePlayer = FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
            } catch (Exception | NoClassDefFoundError e) {
                // Log Floodgate check failure only if debug is enabled
                if (debugEnabled) {
                    getLogger().warning("Error checking Floodgate API for player " + player.getName() + ": " + e.getMessage());
                }
                // Potentially disable floodgate check for future calls if it consistently fails? (More advanced)
                // floodgatePresent = false;
            }
        }

        // Check Geyser API only if the plugin is present and Floodgate didn't identify the player
        // (Optimization: skip Geyser check if Floodgate already confirmed)
        if (!isFloodgatePlayer && geyserPresent) {
            try {
                isGeyserPlayer = GeyserApi.api().isBedrockPlayer(player.getUniqueId());
            } catch (Exception | NoClassDefFoundError e) {
                // Log Geyser check failure only if debug is enabled
                if (debugEnabled) {
                    getLogger().warning("Error checking Geyser API for player " + player.getName() + ": " + e.getMessage());
                }
                // Potentially disable geyser check for future calls? (More advanced)
                // geyserPresent = false;
            }
        }

        // Return true if either API identifies the player as Bedrock
        return isFloodgatePlayer || isGeyserPlayer;
    }

    // Add a getter for commandsConfig
    public FileConfiguration getCommandsConfig() {
        return commandsConfig;
    }
}
