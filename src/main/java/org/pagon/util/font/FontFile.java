package org.pagon.util.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.pagon.util.traits.Jsonable;

public class FontFile implements Jsonable {
    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("fontpath", Fonts.FONT_FILE_PATH);
        return object;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("fontpath")) {
            Fonts.FONT_FILE_PATH = object.get("fontpath").getAsString();
        }
    }

    @Override
    public String getFileName() {
        return "font.json";
    }
}
