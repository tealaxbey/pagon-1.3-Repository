package org.pagon.api.waypoint;

import com.google.gson.JsonObject;
import org.pagon.util.traits.Jsonable;
import org.pagon.util.traits.Util;
import net.minecraft.world.phys.Vec3;

public class Waypoint implements Jsonable, Util {
    private final String name;
    private final String ip;
    private final int dimension;
    private final double x;
    private final double y;
    private final double z;

    public Waypoint(String name, String ip, int dimension, double x, double y, double z) {
        this.name = name;
        this.ip = ip;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public int getDimension() {
        return dimension;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Vec3 getPos() {
        return new Vec3(x, y, z);
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("tag", getName());
        object.addProperty("ip", getIp());
        object.addProperty("dimension", getDimension());
        object.addProperty("x", getX());
        object.addProperty("y", getY());
        object.addProperty("z", getZ());
        return object;
    }

    @Override
    public void fromJson(com.google.gson.JsonElement element) {
    }

    @Override
    public String getFileName() {
        return "waypoints.json";
    }
}
