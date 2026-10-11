package org.pagon.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.pagon.event.impl.input.KeyInputEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.features.commands.Command;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Bind;
import org.pagon.manager.CommandManager;
import org.pagon.util.KeyboardUtil;

import static org.pagon.features.commands.argument.ModuleArgumentType.getModule;
import static org.pagon.features.commands.argument.ModuleArgumentType.module;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;

public class BindCommand extends Command {
    private Module module;

    public BindCommand() {
        super("bind", "setbind");
        setDescription("Sets a key bind for a module");
        EVENT_BUS.register(this);
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        builder.then(argument("module", module(true))
                .executes((ctx) -> {
                    module = getModule(ctx, "module");
                    return success("Press any key...");
                }));
    }

    @Subscribe
    public void onKey(final KeyInputEvent event) {
        if (nullCheck() || module == null || event.getKey() == GLFW_KEY_UNKNOWN) {
            return;
        }

        if (event.getKey() == GLFW_KEY_ESCAPE) {
            module = null;
            sendMessage("Operation canceled.", "fail");
            return;
        }

        sendMessage("Bind for {green} %s {} set to {green} %s",
                module.getName(),
                KeyboardUtil.getKeyName(event.getKey()));
        module.bind.setValue(new Bind(event.getKey()));
        module = null;
    }
}


