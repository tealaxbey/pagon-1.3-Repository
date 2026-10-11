package org.pagon.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.pagon.Pagon;
import org.pagon.features.commands.Command;
import org.pagon.manager.CommandManager;

import java.util.List;
import java.util.StringJoiner;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config", "cfg");
        setDescription("Saves and loads named configs");
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        builder.then(literal("save")
                        .then(argument("name", word())
                                .executes(ctx -> {
                                    String name = getString(ctx, "name");
                                    try {
                                        Pagon.configManager.saveConfig(name);
                                        return success("Saved config {green}%s", name);
                                    } catch (Throwable throwable) {
                                        return fail("Failed to save config: %s", throwable.getMessage());
                                    }
                                })))
                .then(literal("load")
                        .then(argument("name", word())
                                .executes(ctx -> {
                                    String name = getString(ctx, "name");
                                    try {
                                        Pagon.configManager.loadConfig(name);
                                        return success("Loaded config {green}%s", name);
                                    } catch (Throwable throwable) {
                                        return fail("Failed to load config: %s", throwable.getMessage());
                                    }
                                })))
                .then(literal("delete")
                        .then(argument("name", word())
                                .executes(ctx -> {
                                    String name = getString(ctx, "name");
                                    try {
                                        Pagon.configManager.deleteConfig(name);
                                        return success("Deleted config {green}%s", name);
                                    } catch (Throwable throwable) {
                                        return fail("Failed to delete config: %s", throwable.getMessage());
                                    }
                                })))
                .then(literal("list")
                        .executes(ctx -> {
                            List<String> configs = Pagon.configManager.listConfigs();
                            if (configs.isEmpty()) {
                                return success("No saved configs");
                            }
                            StringJoiner joiner = new StringJoiner(", ");
                            configs.forEach(joiner::add);
                            return success("Configs (%s): %s", configs.size(), joiner);
                        }));
    }
}
