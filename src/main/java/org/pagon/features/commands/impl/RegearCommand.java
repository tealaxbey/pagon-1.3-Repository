package org.pagon.features.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.pagon.Pagon;
import org.pagon.features.commands.Command;
import org.pagon.features.modules.combat.AutoRegearModule;
import org.pagon.manager.CommandManager;

public class RegearCommand extends Command {
    public RegearCommand() {
        super("regear");
        setDescription("Saves or clears the AutoRegear inventory template");
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        builder.then(argument("action", StringArgumentType.word())
                .executes(ctx -> {
                    String action = StringArgumentType.getString(ctx, "action");
                    AutoRegearModule module = Pagon.moduleManager.getModuleByClass(AutoRegearModule.class);
                    if (module == null) {
                        return fail("AutoRegear module is not available");
                    }
                    if ("save".equalsIgnoreCase(action)) {
                        module.savePlayerInventory();
                        Pagon.configManager.save();
                        return success("Saved current inventory as AutoRegear template");
                    }
                    if ("clear".equalsIgnoreCase(action)) {
                        module.clearPlayerInventory();
                        Pagon.configManager.save();
                        return success("Cleared AutoRegear template");
                    }
                    return fail("Usage: .regear save | clear");
                }))
                .executes(ctx -> fail("Usage: .regear save | clear"));
    }
}


