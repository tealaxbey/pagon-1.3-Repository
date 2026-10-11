package org.pagon.features.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.pagon.Pagon;
import org.pagon.event.impl.ClientEvent;
import org.pagon.event.impl.render.Render2DEvent;
import org.pagon.event.impl.render.Render3DEvent;
import org.pagon.features.Feature;
import org.pagon.features.commands.Command;
import org.pagon.features.settings.Bind;
import org.pagon.features.settings.Setting;
import org.pagon.manager.ConfigManager;
import org.pagon.util.traits.Jsonable;
import org.pagon.util.traits.Toggleable;
import net.minecraft.ChatFormatting;
import org.joml.Vector2f;

import java.util.Locale;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;

public class Module extends Feature implements Jsonable, Toggleable {
    private final String description;
    private final Category category;
    private final boolean testBuild;

    public final Setting<Boolean> enabled = bool("Enabled", false);
    public final Setting<Boolean> drawn = bool("Drawn", true);
    public final Setting<Bind> bind = key("Keybind", new Bind(GLFW_KEY_UNKNOWN));
    public final Setting<String> displayName;

    public boolean hidden;

    public Module(String name, String description, Category category) {
        this(name, description, category, false);
    }

    public Module(String name, String description, Category category, boolean testBuild) {
        super(name);
        this.displayName = str("DisplayName", name);
        this.description = description;
        this.category = category;
        this.testBuild = testBuild;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onToggle() {
    }

    public void onLoad() {
    }

    public void onTick() {
    }

    public void onRender2D(Render2DEvent event) {
    }

    public void onRender3D(Render3DEvent event) {
    }

    public void onUnload() {
    }

    public String getDisplayInfo() {
        return null;
    }

    public void enable() {
        if (this.isEnabled()) {
            return;
        }
        this.enabled.setValue(true);
        EVENT_BUS.register(this);
        EVENT_BUS.post(new ClientEvent(ClientEvent.Type.TOGGLE_MODULE, this));
        this.onToggle();
        this.onEnable();
    }

    public void disable() {
        if (this.isAlwaysEnabled() || !this.isEnabled()) {
            return;
        }
        this.enabled.setValue(false);
        EVENT_BUS.unregister(this);
        EVENT_BUS.post(new ClientEvent(ClientEvent.Type.TOGGLE_MODULE, this));
        this.onToggle();
        this.onDisable();
    }

    public String getDisplayName() {
        return this.displayName.getValue();
    }

    public void setDisplayName(String name) {
        Module module = Pagon.moduleManager.getModuleByDisplayName(name);
        Module originalModule = Pagon.moduleManager.getModuleByName(name);
        if (module == null && originalModule == null) {
            Command.sendMessage(this.getDisplayName() + ", name: " + this.getName() + ", has been renamed to: " + name, "general");
            this.displayName.setValue(name);
            return;
        }
        Command.sendMessage("{red} A module of this name already exists.", "general");
    }

    @Override
    public boolean isEnabled() {
        return enabled.getValue();
    }

    @Override
    public boolean isToggled() {
        return isEnabled();
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isDrawn() {
        return this.drawn.getValue();
    }

    public void setDrawn(boolean drawn) {
        this.drawn.setValue(drawn);
    }

    public Category getCategory() {
        return this.category;
    }

    public boolean isTestBuild() {
        return this.testBuild;
    }

    public String getInfo() {
        return null;
    }

    public boolean isAlwaysEnabled() {
        return false;
    }

    public Bind getBind() {
        return this.bind.getValue();
    }

    public void setBind(int key) {
        this.bind.setValue(new Bind(key));
    }

    public String getFullArrayString() {
        return this.getDisplayName() + ChatFormatting.GRAY + (this.getDisplayInfo() != null ? " [" + ChatFormatting.WHITE + this.getDisplayInfo() + ChatFormatting.GRAY + "]" : "");
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        for (Setting<?> setting : getSettings()) {
            try {
                if (setting.getValue() instanceof Bind keyBind) {
                    object.addProperty(setting.getName(), keyBind.getKey());
                } else if (setting.getValue() instanceof java.awt.Color color) {
                    object.addProperty(setting.getName(), color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getAlpha());
                } else if (setting.getValue() instanceof Vector2f pos) {
                    object.addProperty(setting.getName(), pos.x() + "," + pos.y());
                } else {
                    object.addProperty(setting.getName(), setting.getValueAsString());
                }
            } catch (Throwable e) {
                Pagon.LOGGER.error("Failed to create JSON field", e);
            }
        }
        return object;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || element.isJsonNull()) return;
        JsonObject object = element.getAsJsonObject();
        if (object.has("Enabled")) {
            String enabled = object.get("Enabled").getAsString();
            setToggled(Boolean.parseBoolean(enabled));
        }
        for (Setting<?> setting : getSettings()) {
            try {
                JsonElement settingElement = object.get(setting.getName());
                if (settingElement != null && !settingElement.isJsonNull()) {
                    ConfigManager.setValueFromJson(this, setting, settingElement);
                }
            } catch (Throwable throwable) {
                Pagon.LOGGER.error("Failed to load from JSON", throwable);
            }
        }
    }

    @Override
    public String getFileName() {
        return "modules/" + sanitizeFileName(getName()) + ".json";
    }

    private static String sanitizeFileName(String input) {
        String value = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        value = value.replaceAll("[^a-z0-9._-]", "_");
        return value.isEmpty() ? "module" : value;
    }

    public enum Category {
        CLIENT("Client"),
        COMBAT("Combat"),
        MOVEMENT("Movement"),
        RENDER("Render"),
        WORLD("World"),
        HUD("Hud");

        private final String name;

        Category(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}


