package org.pagon.features.commands.impl;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.pagon.Pagon;
import org.pagon.api.waypoint.UserWaypoint;
import org.pagon.features.commands.Command;
import org.pagon.manager.CommandManager;
import org.pagon.util.world.DimensionUtil;

public class WaypointCommand extends Command {
    public WaypointCommand() {
        super("waypoint");
        setDescription("Adds/Removes a waypoint");
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        var zArg = argument("z", DoubleArgumentType.doubleArg()).executes(ctx -> {
            String waypointName = StringArgumentType.getString(ctx, "waypoint");
            double x = DoubleArgumentType.getDouble(ctx, "x");
            double y = DoubleArgumentType.getDouble(ctx, "y");
            double z = DoubleArgumentType.getDouble(ctx, "z");
            String action = StringArgumentType.getString(ctx, "add/del");
            if (action.equalsIgnoreCase("add")) {
                if (Pagon.waypointManager.contains(waypointName)) {
                    return fail("Waypoint already exist!");
                }
                Pagon.waypointManager.register(new UserWaypoint(waypointName,
                        serverIp(), DimensionUtil.getDimension(), x, y, z));
                return success("Added waypoint with name {white}%s", waypointName);
            }
            return success();
        });
        var waypointArg = argument("waypoint", StringArgumentType.string())
                .then(argument("x", DoubleArgumentType.doubleArg())
                        .then(argument("y", DoubleArgumentType.doubleArg()).then(zArg)))
                .executes(ctx -> {
                    String waypointName = StringArgumentType.getString(ctx, "waypoint");
                    String action = StringArgumentType.getString(ctx, "add/del");
                    if (action.equalsIgnoreCase("remove") || action.equalsIgnoreCase("del") || action.equalsIgnoreCase("delete")) {
                        if (!Pagon.waypointManager.contains(waypointName)) {
                            return fail("Waypoint does not exist!");
                        }
                        Pagon.waypointManager.remove(waypointName);
                        return success("Removed waypoint with name {red}%s", waypointName);
                    }
                    return success();
                });
        builder.then(argument("add/del", StringArgumentType.word())
                .then(waypointArg)
                .executes(ctx -> fail("Must provide waypoint name!")));
        builder.executes(ctx -> fail("Invalid usage! Usage: waypoint <add/del> <name> [x] [y] [z]"));
    }

    private String serverIp() {
        if (mc.isSingleplayer()) {
            return "Singleplayer";
        }
        return mc.getCurrentServer() == null ? "Unknown" : mc.getCurrentServer().ip;
    }
}
