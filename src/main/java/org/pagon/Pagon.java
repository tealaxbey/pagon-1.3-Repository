package org.pagon;

import org.pagon.manager.*;
import org.pagon.util.BuildConfig;
import org.pagon.util.TextUtil;
import org.pagon.util.font.Fonts;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Pagon implements ModInitializer, ClientModInitializer {
    public static float TIMER = 1f;

    public static final Logger LOGGER = LogManager.getLogger("Pagon");
    public static ServerManager serverManager;
    public static ColorManager colorManager;
    public static RotationManager rotationManager;
    public static PositionManager positionManager;
    public static EventManager eventManager;
    public static SpeedManager speedManager;
    public static CommandManager commandManager;
    public static FriendManager friendManager;
    public static ModuleManager moduleManager;
    public static ConfigManager configManager;
    public static MiningManager miningManager;
    public static volatile boolean phasePause = false;
    public static WaypointManager waypointManager;
    public static DiscordRpcManager discordRpcManager;
    public static SwapManager swapManager;
    public static InventoryManager inventoryManager;
    public static TeleportManager teleportManager;

    @Override
    public void onInitialize() {
        LOGGER.info("Pre-initializing {} v{}",
                BuildConfig.NAME, BuildConfig.VERSION);
        configManager = new ConfigManager();
        eventManager = new EventManager();
        serverManager = new ServerManager();
        rotationManager = new RotationManager();
        positionManager = new PositionManager();
        friendManager = new FriendManager();
        colorManager = new ColorManager();
        commandManager = new CommandManager();
        moduleManager = new ModuleManager();
        speedManager = new SpeedManager();
        miningManager = new MiningManager();
        waypointManager = new WaypointManager();
        discordRpcManager = new DiscordRpcManager();
        swapManager = new SwapManager();
        inventoryManager = new InventoryManager();
        teleportManager = new TeleportManager();
        configManager.addConfig(discordRpcManager);

        TextUtil.init();
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {}", BuildConfig.NAME);

        long startTime = System.nanoTime();

        eventManager.init();
        commandManager.init();
        moduleManager.init();
        swapManager.init();
        inventoryManager.init();
        configManager.addConfig(waypointManager);
        friendManager.init();
        miningManager.init();
        Fonts.bootstrap();

        configManager.load();
        colorManager.init();
        Fonts.init();
        discordRpcManager.init();
        
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            for (var keyMapping : client.options.keyMappings) {
                if (keyMapping.getDefaultKey().getValue() == org.lwjgl.glfw.GLFW.GLFW_KEY_P) {
                    keyMapping.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
                    LOGGER.info("Unbound Minecraft keybind: {} (was P)", keyMapping.getName());
                }
            }
        });
        
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> configManager.shutdownAndSave());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (discordRpcManager != null) {
                discordRpcManager.shutdown();
            }
            Fonts.closeFonts();
            configManager.shutdownAndSave();
        }));

        long endTime = System.nanoTime();

        LOGGER.info("Initialized {} in {}ms",
                BuildConfig.NAME, (endTime - startTime) / 1000000.0);
    }
}


