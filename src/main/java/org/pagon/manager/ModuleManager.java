package org.pagon.manager;

import org.pagon.Pagon;
import org.pagon.event.impl.render.Render2DEvent;
import org.pagon.event.impl.render.Render3DEvent;
import org.pagon.features.Feature;
import org.pagon.features.gui.HudEditorScreen;
import org.pagon.features.commands.ModuleCommand;
import org.pagon.features.modules.Module;
import org.pagon.features.modules.client.ClickGuiModule;
import org.pagon.features.modules.client.ColorModule;
import org.pagon.features.modules.client.CapeModule;
import org.pagon.features.modules.client.DiscordRpcModule;
import org.pagon.features.modules.client.FontModule;
import org.pagon.features.modules.client.HudEditorModule;
import org.pagon.features.modules.client.NotificationsModule;
import org.pagon.features.modules.client.RotationsModule;
import org.pagon.features.modules.combat.AutoPotModule;
import org.pagon.features.modules.combat.AutoRegearModule;
import org.pagon.features.modules.combat.AutoMineModule;
import org.pagon.features.modules.combat.ElytraBlockerModule;
import org.pagon.features.modules.combat.MaceAuraModule;
import org.pagon.features.modules.combat.PearlBlockerModule;
import org.pagon.features.modules.movement.ElytraFlyModule;
import org.pagon.features.modules.movement.FakeFlyModule;
import org.pagon.features.modules.world.SpeedMineModule;
import org.pagon.features.modules.hud.CompassHudModule;
import org.pagon.features.modules.hud.FeatureListHudModule;
import org.pagon.features.modules.hud.InventoryHudModule;
import org.pagon.features.modules.hud.LagNotifierHudModule;
import org.pagon.features.modules.hud.WatermarkHudModule;
import org.pagon.features.modules.hud.WelcomerHudModule;
import org.pagon.util.traits.Jsonable;
import org.pagon.util.traits.Util;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class ModuleManager implements Util, Jsonable {
    private static final Logger LOGGER = LoggerFactory.getLogger("ModuleManager");

    private final Map<Class<? extends Module>, Module> fastRegistry = new HashMap<>();
    private final List<Module> modules = new ArrayList<>();

    public void init() {
        if (Pagon.configManager != null) {
            Pagon.configManager.addConfig(this);
        }

        register(new ColorModule());
        register(new FontModule());
        register(new HudEditorModule());
        register(new ClickGuiModule());
        register(new NotificationsModule());
        register(new RotationsModule());
        
        // Register and enable DiscordRPC module by default
        DiscordRpcModule discordRpcModule = new DiscordRpcModule();
        register(discordRpcModule);
        if (discordRpcModule.isEnabled()) {
            discordRpcModule.enable();
        }
        register(new AutoPotModule());
        register(new AutoRegearModule());
        register(new AutoMineModule());
        register(new MaceAuraModule());
        register(new PearlBlockerModule());
        register(new ElytraBlockerModule());
        register(new CapeModule());
        register(new ElytraFlyModule());
        register(new FakeFlyModule());
        register(new SpeedMineModule());
        register(new WatermarkHudModule());
        register(new CompassHudModule());
        register(new FeatureListHudModule());
        register(new InventoryHudModule());
        register(new LagNotifierHudModule());
        register(new WelcomerHudModule());

        LOGGER.info("Registered {} modules", modules.size());

        for (Module module : modules) {
            Pagon.commandManager.register(new ModuleCommand(module));
        }
    }

    public void register(Module module) {
        getModules().add(module);
        fastRegistry.put(module.getClass(), module);
        if (Pagon.configManager != null) {
            Pagon.configManager.addConfig(module);
        }
    }

    public List<Module> getModules() {
        return modules;
    }

    public Stream<Module> stream() {
        return getModules().stream();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModuleByClass(Class<T> clazz) {
        return (T) fastRegistry.get(clazz);
    }

    public Module getModuleByName(String name) {
        return stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public Module getModuleByDisplayName(String display) {
        return stream().filter(m -> m.getDisplayName().equalsIgnoreCase(display)).findFirst().orElse(null);
    }

    public List<Module> getModulesByCategory(Module.Category category) {
        return stream().filter(m -> m.getCategory() == category).toList();
    }

    public List<Module.Category> getCategories() {
        return List.of(Module.Category.CLIENT, Module.Category.COMBAT, Module.Category.MOVEMENT, 
                      Module.Category.RENDER, Module.Category.WORLD, Module.Category.HUD);
    }

    public void onLoad() {
        getModules().forEach(Module::onLoad);
    }

    public void onTick() {
        stream().filter(Feature::isEnabled).forEach(Module::onTick);
    }

    public void onRender2D(Render2DEvent event) {
        boolean hudEditor = mc.screen instanceof HudEditorScreen;
        stream().filter(module -> module.isEnabled()
                        && (module.getCategory() != Module.Category.HUD
                        || (!hudEditor && module.isDrawn())))
                .forEach(module -> module.onRender2D(event));
    }

    public void renderHudModules(Render2DEvent event) {
        stream().filter(module -> module.isEnabled() && module.getCategory() == Module.Category.HUD)
                .forEach(module -> module.onRender2D(event));
    }

    public void onRender3D(Render3DEvent event) {
        stream().filter(Feature::isEnabled).forEach(module -> module.onRender3D(event));
    }

    public void onUnload() {
        getModules().forEach(EVENT_BUS::unregister);
        getModules().forEach(Module::onUnload);
    }

    public void onKeyPressed(int key) {
        if (key <= 0 || mc.screen != null) return;
        stream().filter(module -> module.getBind().getKey() == key).forEach(Module::toggle);
    }

    public void onMouseClicked(int button) {
        if (mc.screen != null) return;
        int key = -button - 2;
        stream().filter(module -> module.getBind().getKey() == key).forEach(Module::toggle);
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        for (Module module : modules) {
            try {
                object.add(module.getName(), module.toJson());
            } catch (Throwable throwable) {
                LOGGER.error("Failed to serialize module {}", module.getName(), throwable);
            }
        }
        return object;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }

        JsonObject object = element.getAsJsonObject();
        for (Module module : modules) {
            JsonElement moduleElement = object.get(module.getName());
            if (moduleElement != null && !moduleElement.isJsonNull()) {
                try {
                    module.fromJson(moduleElement);
                } catch (Throwable throwable) {
                    LOGGER.error("Failed to deserialize module {}", module.getName(), throwable);
                }
            }
        }
    }

    @Override
    public String getFileName() {
        return "modules.json";
    }
}
