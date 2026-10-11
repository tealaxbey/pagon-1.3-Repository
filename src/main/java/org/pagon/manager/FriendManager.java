package org.pagon.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.pagon.Pagon;
import org.pagon.util.traits.Jsonable;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class FriendManager implements Jsonable {
    private final List<String> friends = new ArrayList<>();

    public void init() {
        Pagon.configManager.addConfig(this);
    }

    public boolean isFriend(String name) {
        return this.friends.stream().anyMatch(friend -> friend.equalsIgnoreCase(name));
    }

    public boolean isFriend(Player player) {
        return this.isFriend(player.getGameProfile().name());
    }

    public void addFriend(String name) {
        this.friends.add(name);
        Pagon.configManager.scheduleSave();
    }

    public void removeFriend(String name) {
        friends.removeIf(s -> s.equalsIgnoreCase(name));
        Pagon.configManager.scheduleSave();
    }

    public void clearFriends() {
        friends.clear();
        Pagon.configManager.scheduleSave();
    }

    public List<String> getFriends() {
        return this.friends;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        JsonArray array = new JsonArray();
        for (String friend : friends) {
            array.add(friend);
        }
        object.add("friends", array);
        return object;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject() || !element.getAsJsonObject().has("friends")) return;
        friends.clear();
        for (JsonElement e : element.getAsJsonObject().get("friends").getAsJsonArray()) {
            friends.add(e.getAsString());
        }
    }

    @Override
    public String getFileName() {
        return "friends.json";
    }
}

