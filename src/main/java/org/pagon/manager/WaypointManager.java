package org.pagon.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.pagon.Pagon;
import org.pagon.api.waypoint.UserWaypoint;
import org.pagon.api.waypoint.Waypoint;
import org.pagon.util.traits.Jsonable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WaypointManager implements Jsonable {
    private final List<Waypoint> waypoints = new CopyOnWriteArrayList<>();

    public void register(Waypoint waypoint) {
        waypoints.add(waypoint);
        scheduleSave();
    }

    public void register(Waypoint... waypoints) {
        for (Waypoint waypoint : waypoints) {
            register(waypoint);
        }
    }

    public boolean remove(Waypoint waypoint) {
        boolean removed = waypoints.remove(waypoint);
        if (removed) {
            scheduleSave();
        }
        return removed;
    }

    public boolean removeContains(String waypoint) {
        boolean removed = waypoints.removeIf(w -> w.getName().contains(waypoint));
        if (removed) {
            scheduleSave();
        }
        return removed;
    }

    public boolean remove(String waypoint) {
        boolean removed = waypoints.removeIf(w -> w.getName().equalsIgnoreCase(waypoint));
        if (removed) {
            scheduleSave();
        }
        return removed;
    }

    public boolean contains(String waypoint) {
        return waypoints.stream().anyMatch(w -> w.getName().contains(waypoint));
    }

    public void clear() {
        boolean removed = waypoints.removeIf(w -> !(w instanceof UserWaypoint));
        if (removed) {
            scheduleSave();
        }
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public List<String> getIps() {
        List<String> ips = new ArrayList<>();
        for (Waypoint waypoint : getWaypoints()) {
            ips.add(waypoint.getIp());
        }
        return ips;
    }

    @Override
    public JsonElement toJson() {
        JsonArray array = new JsonArray();
        for (Waypoint point : waypoints) {
            if (point instanceof UserWaypoint) {
                array.add(point.toJson());
            }
        }
        return array;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return;
        }
        waypoints.removeIf(UserWaypoint.class::isInstance);
        for (JsonElement e : element.getAsJsonArray()) {
            if (!e.isJsonObject()) {
                continue;
            }
            JsonObject object = e.getAsJsonObject();
            if (!object.has("tag") || !object.has("dimension") || !object.has("ip")
                    || !object.has("x") || !object.has("y") || !object.has("z")) {
                continue;
            }
            waypoints.add(new UserWaypoint(object.get("tag").getAsString(),
                    object.get("ip").getAsString(), object.get("dimension").getAsInt(),
                    object.get("x").getAsDouble(), object.get("y").getAsDouble(), object.get("z").getAsDouble()));
        }
    }

    @Override
    public String getFileName() {
        return "waypoints.json";
    }

    private void scheduleSave() {
        if (Pagon.configManager != null) {
            Pagon.configManager.scheduleSave();
        }
    }
}
