package org.pagon.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.pagon.Pagon;
import org.pagon.features.commands.Command;
import org.pagon.manager.CommandManager;

public class DiscordRpcCommand extends Command {
    public DiscordRpcCommand() {
        super("discordrpc", "rpc", "rps");
        setDescription("Enables or disables Discord RPC");
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        builder.then(literal("on").executes(ctx -> {
                    Pagon.discordRpcManager.setEnabled(true);
                    return success("Discord RPC enabled");
                }))
                .then(literal("off").executes(ctx -> {
                    Pagon.discordRpcManager.setEnabled(false);
                    return success("Discord RPC disabled");
                }))
                .then(literal("toggle").executes(ctx -> {
                    boolean enabled = !Pagon.discordRpcManager.isEnabled();
                    Pagon.discordRpcManager.setEnabled(enabled);
                    return success("Discord RPC %s", enabled ? "enabled" : "disabled");
                }))
                .then(literal("status").executes(ctx -> {
                    return success("Discord RPC is currently %s", Pagon.discordRpcManager.isEnabled() ? "enabled" : "disabled");
                }))
                .executes(ctx -> {
                    boolean enabled = !Pagon.discordRpcManager.isEnabled();
                    Pagon.discordRpcManager.setEnabled(enabled);
                    return success("Discord RPC %s", enabled ? "enabled" : "disabled");
                });
    }
}
