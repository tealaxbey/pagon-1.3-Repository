package org.pagon.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.pagon.Pagon;
import org.pagon.features.Feature;
import org.pagon.features.settings.Bind;
import org.pagon.features.settings.EntityTypeListSetting;
import org.pagon.features.settings.EnumConverter;
import org.pagon.features.settings.Setting;
import org.pagon.util.BuildConfig;
import org.pagon.util.traits.Jsonable;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigManager");
    private static final Path LEGACY_PATH = FabricLoader.getInstance().getGameDir().resolve("pagon");
    private static final Path CONFIGS_PATH = LEGACY_PATH.resolve("configs");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<Jsonable> jsonables = new LinkedList<>();
    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "Legacy Config Save");
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> scheduledSave;
    private volatile boolean loading;

    public void addConfig(Jsonable jsonable) {
        if (!jsonables.contains(jsonable)) {
            jsonables.add(jsonable);
        }
    }

    public boolean isLoading() {
        return loading;
    }

    public void load() {
        mkdirs();
        loadFromPath(LEGACY_PATH);
    }

    public synchronized void save() {
        saveToPath(LEGACY_PATH);
    }

    public void saveJsonable(Jsonable jsonable) {
        if (jsonable == null || loading) {
            return;
        }
        try {
            writeJson(LEGACY_PATH.resolve(jsonable.getFileName()), jsonable.toJson());
        } catch (Throwable e) {
            LOGGER.error("Failed to write config entry", e);
        }
    }

    public synchronized void scheduleSave() {
        if (loading) return;
        if (scheduledSave != null) {
            scheduledSave.cancel(false);
        }
        scheduledSave = saveExecutor.schedule(this::save, 250L, TimeUnit.MILLISECONDS);
    }

    public synchronized void shutdownAndSave() {
        if (scheduledSave != null) {
            scheduledSave.cancel(false);
            scheduledSave = null;
        }
        save();
        saveExecutor.shutdown();
    }

    public void saveConfig(String name) {
        mkdirs();
        Path file = getConfigPath(name);
        JsonObject object = new JsonObject();
        object.addProperty("clientVersion", BuildConfig.VERSION);
        for (Jsonable jsonable : jsonables) {
            try {
                object.add(jsonable.getFileName(), jsonable.toJson());
            } catch (Throwable e) {
                LOGGER.error("Failed to create config field", e);
            }
        }
        try {
            writeJson(file, object);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to save config: " + sanitizeName(name), e);
        }
    }

    public void loadConfig(String name) {
        Path file = getConfigPath(name);
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Config does not exist: " + sanitizeName(name));
        }
        loadFromFile(file);
        save();
    }

    public void deleteConfig(String name) {
        Path file = getConfigPath(name);
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Config does not exist: " + sanitizeName(name));
        }
        try {
            Files.delete(file);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to delete config: " + sanitizeName(name), e);
        }
    }

    public List<String> listConfigs() {
        mkdirs();
        try (Stream<Path> stream = Files.list(CONFIGS_PATH)) {
            return stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (Throwable e) {
            LOGGER.error("Failed to list configs", e);
            return List.of();
        }
    }

    private void loadFromPath(Path path) {
        try {
            loading = true;
            for (Jsonable jsonable : jsonables) {
                try {
                    Path file = path.resolve(jsonable.getFileName());
                    if (!Files.exists(file)) continue;
                    String read = Files.readString(file);
                    jsonable.fromJson(JsonParser.parseString(read));
                } catch (Throwable e) {
                    LOGGER.error("Failed to load", e);
                }
            }
        } finally {
            loading = false;
        }
    }

    private void loadFromFile(Path file) {
        try {
            loading = true;
            JsonElement element = JsonParser.parseString(Files.readString(file));
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Invalid config file");
            }
            JsonObject object = element.getAsJsonObject();
            for (Jsonable jsonable : jsonables) {
                JsonElement json = object.get(jsonable.getFileName());
                if (json != null && !json.isJsonNull()) {
                    jsonable.fromJson(json);
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load config: " + file.getFileName(), e);
        } finally {
            loading = false;
        }
    }

    private void saveToPath(Path path) {
        mkdirs(path);
        for (Jsonable jsonable : jsonables) {
            try {
                writeJson(path.resolve(jsonable.getFileName()), withClientVersion(jsonable.toJson()));
            } catch (Throwable e) {
                LOGGER.error("Failed to write to file", e);
            }
        }
    }

    private void mkdirs() {
        mkdirs(LEGACY_PATH);
        mkdirs(CONFIGS_PATH);
    }

    private void mkdirs(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create needed directories!", e);
        }
    }

    private Path getConfigPath(String name) {
        return CONFIGS_PATH.resolve(sanitizeName(name) + ".json");
    }

    private JsonObject withClientVersion(JsonElement element) {
        JsonObject object = new JsonObject();
        object.addProperty("clientVersion", BuildConfig.VERSION);
        if (element != null && element.isJsonObject()) {
            for (String key : element.getAsJsonObject().keySet()) {
                if (!key.equals("clientVersion")) {
                    object.add(key, element.getAsJsonObject().get(key));
                }
            }
        }
        return object;
    }

    private void writeJson(Path file, JsonElement element) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path directory = parent != null ? parent : LEGACY_PATH;
        Path tempFile = Files.createTempFile(directory, file.getFileName().toString(), ".tmp");
        try {
            Files.writeString(tempFile, GSON.toJson(element));
            try {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
            }
        }
    }

    private String sanitizeName(String name) {
        String sanitized = name == null ? "" : name.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.toLowerCase().endsWith(".json")) {
            sanitized = sanitized.substring(0, sanitized.length() - ".json".length());
        }
        if (sanitized.isEmpty() || sanitized.equals(".") || sanitized.equals("..")) {
            throw new IllegalArgumentException("Invalid config name");
        }
        return sanitized;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void setValueFromJson(Feature feature, Setting setting, JsonElement element) {
        if (element == null || element.isJsonNull()) return;
        switch (setting.getType()) {
            case "Boolean" -> setting.setValue(element.getAsBoolean());
            case "Double" -> setting.setValue(element.getAsDouble());
            case "Float" -> setting.setValue(element.getAsFloat());
            case "Integer" -> setting.setValue(element.getAsInt());
            case "String" -> setting.setValue(element.getAsString().replace("_", " "));
            case "Bind" -> setting.setValue(new Bind(element.getAsInt()));
            case "Color" -> {
                try {
                    String colorStr = element.getAsString();
                    String[] parts = colorStr.split(",");
                    if (parts.length == 4) {
                        int r = Integer.parseInt(parts[0]);
                        int g = Integer.parseInt(parts[1]);
                        int b = Integer.parseInt(parts[2]);
                        int a = Integer.parseInt(parts[3]);
                        setting.setValue(new Color(r, g, b, a));
                    }
                } catch (Exception exception) {
                    LOGGER.error("Error parsing color for: {} : {}", feature.getName(), setting.getName());
                }
            }
            case "Pos" -> {
                try {
                    String posStr = element.getAsString();
                    String[] parts = posStr.split(",");
                    if (parts.length == 2) {
                        float x = Float.parseFloat(parts[0]);
                        float y = Float.parseFloat(parts[1]);
                        setting.setValue(new Vector2f(x, y));
                    }
                } catch (Exception exception) {
                    LOGGER.error("Error parsing position for: {} : {}", feature.getName(), setting.getName());
                }
            }
            case "Enum" -> {
                try {
                    Enum<?> current = (Enum<?>) setting.getValue();
                    Class<?> enumClass = current.getDeclaringClass();
                    EnumConverter converter = new EnumConverter(enumClass == null ? current.getClass() : enumClass);
                    Enum value = converter.doBackward(element);
                    if (value != null) {
                        setting.setValue(value);
                    }
                } catch (Exception exception) {
                    LOGGER.error("Error parsing enum for {}.{}: {}", feature.getName(), setting.getName(), exception);
                }
            }
            case "EntityTypeList" -> {
                try {
                        List<EntityType<?>> entities = new ArrayList<>();
                        String raw = element.getAsString();
                        if (!raw.isBlank()) {
                            for (String id : raw.split(",")) {
                                String value = id.trim();
                                String[] split = value.split(":", 2);
                                Identifier location = split.length == 2
                                        ? Identifier.fromNamespaceAndPath(split[0], split[1])
                                        : Identifier.fromNamespaceAndPath("minecraft", value);
                                if (BuiltInRegistries.ENTITY_TYPE.containsKey(location)) {
                                    entities.add(BuiltInRegistries.ENTITY_TYPE.getValue(location));
                                }
                            }
                        }
                    setting.setValue(entities);
                } catch (Exception exception) {
                    LOGGER.error("Error parsing entity list for {}.{}: {}", feature.getName(), setting.getName(), exception);
                }
            }
            default -> LOGGER.error("Unknown Setting type for: {} : {}", feature.getName(), setting.getName());
        }
    }
}


