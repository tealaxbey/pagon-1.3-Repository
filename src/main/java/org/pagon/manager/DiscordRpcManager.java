package org.pagon.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import org.pagon.Pagon;
import org.pagon.event.impl.network.DisconnectEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.features.Feature;
import org.pagon.util.BuildConfig;
import org.pagon.util.traits.Jsonable;
import org.pagon.util.traits.Util;
import net.minecraft.client.multiplayer.ServerData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Discord RPC Manager - Displays game activity on Discord
 * 
 * Features:
 * - Shows "Playing Future with pagon" as the main title
 * - Displays current game state (Menu, Singleplayer, or Server IP)
 * - Shows large image (future.png) and small image (icon.png) from Discord application assets
 * - Works cross-platform: Windows, Linux, and macOS
 * 
 * Discord Application Setup:
 * Application ID: 1537760989557301279
 * Required assets in Discord Developer Portal:
 *   - "future" (large image) -> https://www.futureclient.net/
 *   - "icon" (small image) -> https://discord.gg/MMQ7yvGnAt
 * 
 * Note: The older Discord RPC SDK doesn't support clickable buttons. 
 * Image tooltips display URLs but aren't clickable in the Discord client.
 * To enable clickable buttons, you would need to migrate to Discord Game SDK.
 */
public class DiscordRpcManager implements Jsonable, Util {
    private static final String APPLICATION_ID = "1537760989557301279";
    private static final String BINARIES_VERSION = "3.4.0";
    private static final String LARGE_IMAGE_KEY = "future";
    private static final String LARGE_IMAGE_TEXT = "Future with pagon";
    private static final String SMALL_IMAGE_KEY = "icon";
    private static final String LARGE_IMAGE_URL = "https://www.futureclient.net/";
    private static final String SMALL_IMAGE_URL = "https://discord.gg/MMQ7yvGnAt";
    private static final String MAIN_MENU = "In Menu";
    private static final String SINGLEPLAYER = "In Singleplayer";
    private static final long RETRY_SLEEP_MS = 15000L;

    private final DiscordEventHandlers handlers = new DiscordEventHandlers();
    private final DiscordRichPresence presence = new DiscordRichPresence();
    private final long startTimestamp = System.currentTimeMillis() / 1000L;

    private DiscordRPC rpc;
    private volatile boolean nativePrepared;
    private volatile boolean enabled = true;
    private boolean running;
    private boolean connected;
    private boolean registered;
    private volatile boolean forceMenu = true;
    private volatile String lastDetails = "";
    private volatile String lastState = "";
    private long nextReconnectAttempt;

    public void init() {
        if (registered) {
            if (enabled && !running) {
                running = true;
                tryConnect();
            }
            return;
        }

        EVENT_BUS.register(this);
        registered = true;

        handlers.ready = user -> Pagon.LOGGER.info("Discord RPC connected");
        handlers.disconnected = (errorCode, message) -> {
            Pagon.LOGGER.debug("Discord RPC disconnected: {} {}", errorCode, message);
            disconnect();
        };
        handlers.errored = (errorCode, message) -> {
            Pagon.LOGGER.debug("Discord RPC error: {} {}", errorCode, message);
            disconnect();
        };

        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        Pagon.LOGGER.info("Discord RPC initializing");
        if (enabled) {
            running = true;
            tryConnect();
        } else {
            running = false;
            Pagon.LOGGER.info("Discord RPC disabled by config");
        }
    }

    public void shutdown() {
        if (!registered) {
            return;
        }
        running = false;

        disconnect();

        EVENT_BUS.unregister(this);
        registered = false;
    }

    private void tick() {
        if (!running) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!connected) {
            if (now >= nextReconnectAttempt) {
                tryConnect();
            }
            return;
        }

        try {
            if (rpc != null) {
                rpc.Discord_RunCallbacks();
            }
        } catch (Throwable throwable) {
            Pagon.LOGGER.debug("Discord RPC connection dropped", throwable);
            disconnect();
            nextReconnectAttempt = now + RETRY_SLEEP_MS;
            return;
        }

        if (forceMenu && hasGameContext()) {
            forceMenu = false;
        }

        refreshPresence(false);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (!running) {
            return;
        }

        forceMenu = true;
        refreshPresence(true);
    }

    private boolean tryConnect() {
        if (!enabled) {
            return false;
        }
        try {
            prepareNativeLibrary();
            DiscordRPC localRpc = DiscordRPC.INSTANCE;
            localRpc.Discord_Initialize(APPLICATION_ID, handlers, true, "");
            rpc = localRpc;
            connected = true;
            nextReconnectAttempt = 0L;
            forceMenu = forceMenu || !hasGameContext();
            lastDetails = "";
            lastState = "";
            refreshPresence(true);
            return true;
        } catch (Throwable throwable) {
            Pagon.LOGGER.debug("Discord RPC is unavailable", throwable);
            connected = false;
            nextReconnectAttempt = System.currentTimeMillis() + RETRY_SLEEP_MS;
            return false;
        }
    }

    private void disconnect() {
        DiscordRPC localRpc = rpc;
        connected = false;
        rpc = null;
        if (localRpc == null) {
            return;
        }

        try {
            localRpc.Discord_ClearPresence();
        } catch (Throwable ignored) {
        }

        try {
            localRpc.Discord_Shutdown();
        } catch (Throwable ignored) {
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;
        if (enabled) {
            running = true;
            if (registered) {
                tryConnect();
            }
        } else {
            running = false;
            forceMenu = true;
            nextReconnectAttempt = 0L;
            disconnect();
        }

        if (Pagon.configManager != null && !Pagon.configManager.isLoading()) {
            Pagon.configManager.saveJsonable(this);
        }
    }

    private void refreshPresence(boolean force) {
        DiscordRPC localRpc = rpc;
        if (!connected || localRpc == null) {
            return;
        }

        PresenceSnapshot snapshot = buildSnapshot();
        String state = snapshot.state.isBlank() ? null : snapshot.state;
        if (!force && snapshot.details.equals(lastDetails) && snapshot.state.equals(lastState)) {
            return;
        }

        presence.details = LARGE_IMAGE_TEXT;
        presence.state = snapshot.state;
        presence.startTimestamp = startTimestamp;
        presence.largeImageKey = LARGE_IMAGE_KEY;
        presence.largeImageText = LARGE_IMAGE_URL;
        presence.smallImageKey = SMALL_IMAGE_KEY;
        presence.smallImageText = SMALL_IMAGE_URL;
        
        try {
            localRpc.Discord_UpdatePresence(presence);
            lastDetails = snapshot.details;
            lastState = snapshot.state;
        } catch (Throwable throwable) {
            Pagon.LOGGER.debug("Failed to update Discord RPC presence", throwable);
            disconnect();
        }
    }

    private PresenceSnapshot buildSnapshot() {
        if (forceMenu || Feature.nullCheck()) {
            return new PresenceSnapshot(LARGE_IMAGE_TEXT, MAIN_MENU);
        }

        return new PresenceSnapshot(LARGE_IMAGE_TEXT, getGameState());
    }

    private String getGameState() {
        if (mc.isSingleplayer()) {
            return SINGLEPLAYER;
        }

        if (!shouldShowServerIp()) {
            return "In Multiplayer";
        }

        try {
            ServerData server = mc.getCurrentServer();
            if (server != null) {
                if (server.ip != null && !server.ip.isBlank()) {
                    return server.ip;
                }
            }
        } catch (Throwable ignored) {
        }

        return "In Multiplayer";
    }

    private boolean shouldShowServerIp() {
        try {
            var module = Pagon.moduleManager.getModuleByClass(org.pagon.features.modules.client.DiscordRpcModule.class);
            return module != null && module.shouldShowServerIp();
        } catch (Throwable ignored) {
            return true;
        }
    }

    private boolean hasGameContext() {
        return mc.player != null && mc.level != null;
    }

    private synchronized void prepareNativeLibrary() throws IOException {
        if (nativePrepared) {
            return;
        }

        String resource = getNativeResourcePath();
        String fileName = resource.substring(resource.lastIndexOf('/') + 1);
        String platform = resource.substring(0, resource.indexOf('/'));
        Path directory = Path.of(System.getProperty("java.io.tmpdir"), "legacy-discord-rpc",
                BINARIES_VERSION, platform, Long.toString(ProcessHandle.current().pid()));
        Path target = directory.resolve(fileName);

        Files.createDirectories(directory);
        try (InputStream input = DiscordRpcManager.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing Discord RPC native resource: " + resource);
            }
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }

        target.toFile().deleteOnExit();
        prependJnaPath(directory);
        nativePrepared = true;
    }

    private String getNativeResourcePath() throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean is64Bit = arch.contains("64");

        if (os.contains("win")) {
            return is64Bit ? "win-x64/discord-rpc.dll" : "win-x86/discord-rpc.dll";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return "darwin/libdiscord-rpc.dylib";
        }
        if (os.contains("linux")) {
            return "linux/libdiscord-rpc.so";
        }

        throw new IOException("Unsupported Discord RPC platform: " + os + " " + arch);
    }

    private void prependJnaPath(Path directory) {
        String path = directory.toAbsolutePath().toString();
        String current = System.getProperty("jna.library.path");
        if (current == null || current.isBlank()) {
            System.setProperty("jna.library.path", path);
            return;
        }

        if (!current.contains(path)) {
            System.setProperty("jna.library.path", path + File.pathSeparator + current);
        }
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("enabled", enabled);
        return object;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }

        JsonObject object = element.getAsJsonObject();
        if (object.has("enabled")) {
            setEnabled(object.get("enabled").getAsBoolean());
        } else if (object.has("Enabled")) {
            setEnabled(object.get("Enabled").getAsBoolean());
        }
    }

    @Override
    public String getFileName() {
        return "discord-rpc.json";
    }

    private record PresenceSnapshot(String details, String state) {
    }
}
