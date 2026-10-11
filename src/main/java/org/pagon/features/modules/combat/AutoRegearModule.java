package org.pagon.features.modules.combat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.pagon.Pagon;
import org.pagon.features.commands.Command;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;
import org.pagon.manager.ConfigManager;
import org.pagon.util.inventory.InventoryUtil;
import org.pagon.util.models.Timer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AutoRegearModule extends Module {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path REGEAR_FILE = FabricLoader.getInstance().getGameDir()
            .resolve("Legacy")
            .resolve("autoregear.json");

    private final Setting<Float> delay = num("Delay", 0.05f, 0.0f, 2.0f);
    private final Setting<Integer> actionsPerTick = num("ActionsPerTick", 3, 1, 8);
    private final Setting<Boolean> debug = bool("Debug", false);

    private final Timer clickTimer = new Timer();
    private final Map<Integer, ItemStack> regearInventory = new HashMap<>();

    public AutoRegearModule() {
        super("AutoRegear", "Automatically refills your inventory with gear", Category.COMBAT);
        clickTimer.reset();
    }

    @Override
    public void onEnable() {
        if (regearInventory.isEmpty()) {
            Command.sendMessage("{red} No regear configuration set. Use .regear save", "general");
            disable();
            return;
        }
        clickTimer.reset();
    }

    @Override
    public void onDisable() {
        if (nullCheck()) {
            return;
        }
        returnCursor(mc.player.containerMenu);
    }

    @Override
    public void onTick() {
        if (nullCheck() || mc.gameMode == null || regearInventory.isEmpty()) {
            return;
        }

        AbstractContainerMenu menu = mc.player.containerMenu;
        if (!isSupportedContainerScreen()) {
            return;
        }

        if (!clickTimer.passedMs((long) (delay.getValue() * 1000.0f))) {
            return;
        }

        int containerSlots = menu.slots.size() - 36;
        if (containerSlots <= 0) {
            return;
        }

        int maxActions = Math.max(1, actionsPerTick.getValue());
        for (int i = 0; i < maxActions; i++) {
            if (!InventoryUtil.cursor().isEmpty()) {
                if (returnCursor(menu)) {
                    clickTimer.reset();
                }
                return;
            }

            if (!performMove(menu, containerSlots)) {
                return;
            }

            if (delay.getValue() > 0.0f) {
                clickTimer.reset();
                return;
            }
        }
    }

    private boolean performMove(AbstractContainerMenu menu, int containerSlots) {
        for (int targetSlot = 0; targetSlot < 36; targetSlot++) {
            ItemStack desiredStack = regearInventory.get(targetSlot);
            if (desiredStack == null || desiredStack.isEmpty() || desiredStack.is(Items.AIR)) {
                continue;
            }

            ItemStack currentStack = mc.player.getInventory().getItem(targetSlot);
            if (isSatisfied(currentStack, desiredStack)) {
                continue;
            }

            int playerSource = findBestPlayerSource(targetSlot, desiredStack);
            if (playerSource != -1) {
                debug("fill target %s from player %s with %s", targetSlot, playerSource, itemName(desiredStack));
                moveStack(toMenuSlot(playerSource, containerSlots), toMenuSlot(targetSlot, containerSlots));
                return true;
            }

            int containerSource = findContainerSource(menu, containerSlots, desiredStack);
            if (containerSource != -1) {
                if (!currentStack.isEmpty() && !canMerge(currentStack, desiredStack)) {
                    int relocationSlot = findPlayerRelocationTarget(targetSlot, currentStack);
                    if (relocationSlot != -1) {
                        debug("clear target %s: move %s x%s to %s", targetSlot, itemName(currentStack), currentStack.getCount(), relocationSlot);
                        moveStack(toMenuSlot(targetSlot, containerSlots), toMenuSlot(relocationSlot, containerSlots));
                        return true;
                    }

                    int containerBuffer = findEmptyContainerSlot(menu, containerSlots);
                    if (containerBuffer == -1 || containerBuffer == containerSource) {
                        debug("blocked target %s: no container buffer for %s", targetSlot, itemName(currentStack));
                        continue;
                    }

                    debug("clear target %s: stash %s x%s to container %s", targetSlot, itemName(currentStack), currentStack.getCount(), containerBuffer);
                    moveStack(toMenuSlot(targetSlot, containerSlots), containerBuffer);
                    return true;
                }

                debug("fill target %s from container %s with %s", targetSlot, containerSource, itemName(desiredStack));
                moveStack(containerSource, toMenuSlot(targetSlot, containerSlots));
                return true;
            }

            debug("missing target %s for %s", targetSlot, itemName(desiredStack));
        }
        return false;
    }

    private boolean isSupportedContainerScreen() {
        if (mc.screen instanceof InventoryScreen) {
            return false;
        }
        return mc.screen instanceof ContainerScreen || mc.screen instanceof ShulkerBoxScreen;
    }

    public void clearPlayerInventory() {
        regearInventory.clear();
        saveRegearFile();
    }

    public void savePlayerInventory() {
        regearInventory.clear();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                regearInventory.put(slot, stack.copy());
            }
        }
        saveRegearFile();
    }

    private boolean isSatisfied(ItemStack currentStack, ItemStack desiredStack) {
        if (currentStack.isEmpty()) {
            return false;
        }
        if (!isSameType(currentStack, desiredStack)) {
            return false;
        }
        return !currentStack.isStackable() || currentStack.getCount() >= Math.min(desiredStack.getCount(), currentStack.getMaxStackSize());
    }

    private int findBestPlayerSource(int targetSlot, ItemStack desiredStack) {
        int bestSlot = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int slot = 0; slot < 36; slot++) {
            if (slot == targetSlot) {
                continue;
            }
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (stack.isEmpty() || !isSameType(stack, desiredStack)) {
                continue;
            }
            ItemStack slotDesired = regearInventory.get(slot);
            if (slotDesired != null && isSatisfied(stack, slotDesired)) {
                continue;
            }
            int score = scorePlayerSource(slot, targetSlot, stack);
            if (score > bestScore) {
                bestSlot = slot;
                bestScore = score;
            }
        }
        return bestSlot;
    }

    private int scorePlayerSource(int sourceSlot, int targetSlot, ItemStack sourceStack) {
        int score = sourceStack.getCount();
        ItemStack targetStack = mc.player.getInventory().getItem(targetSlot);
        ItemStack sourceDesired = regearInventory.get(sourceSlot);

        if (targetStack.isEmpty()) {
            score += 2000;
        } else if (canMerge(targetStack, sourceStack)) {
            score += 1500;
        } else {
            score += 1000;
        }

        if (sourceDesired == null || sourceDesired.isEmpty()) {
            score += 400;
        } else if (targetStack.isEmpty()) {
            score += 100;
        } else if (isSameType(targetStack, sourceDesired)) {
            score += 3000;
        }

        return score;
    }

    private int findContainerSource(AbstractContainerMenu menu, int containerSlots, ItemStack desiredStack) {
        int bestSlot = -1;
        int bestCount = -1;
        for (int slot = 0; slot < containerSlots; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty() || !isSameType(stack, desiredStack)) {
                continue;
            }
            if (stack.getCount() > bestCount) {
                bestSlot = slot;
                bestCount = stack.getCount();
            }
        }
        return bestSlot;
    }

    private int findPlayerRelocationTarget(int sourceSlot, ItemStack movingStack) {
        ItemStack sourceDesired = regearInventory.get(sourceSlot);
        int emptyWrongSlot = -1;
        int emptyNoTemplateSlot = -1;
        for (int slot = 0; slot < 36; slot++) {
            if (slot == sourceSlot) {
                continue;
            }

            ItemStack desiredStack = regearInventory.get(slot);
            ItemStack slotStack = mc.player.getInventory().getItem(slot);
            if (desiredStack != null && !desiredStack.isEmpty() && isSameType(movingStack, desiredStack) && !isSatisfied(slotStack, desiredStack)) {
                if (slotStack.isEmpty() || canMerge(slotStack, movingStack)) {
                    return slot;
                }
            }

            if (sourceDesired != null && !sourceDesired.isEmpty() && slotStack.isEmpty() && isSameType(movingStack, sourceDesired)) {
                continue;
            }

            if (slotStack.isEmpty()) {
                if (desiredStack == null || desiredStack.isEmpty()) {
                    if (emptyNoTemplateSlot == -1) {
                        emptyNoTemplateSlot = slot;
                    }
                } else if (emptyWrongSlot == -1) {
                    emptyWrongSlot = slot;
                }
            }
        }
        return emptyNoTemplateSlot != -1 ? emptyNoTemplateSlot : emptyWrongSlot;
    }

    private int findEmptyContainerSlot(AbstractContainerMenu menu, int containerSlots) {
        for (int slot = 0; slot < containerSlots; slot++) {
            if (menu.getSlot(slot).getItem().isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private void moveStack(int sourceSlot, int targetSlot) {
        InventoryUtil.click(sourceSlot, 0, ClickType.PICKUP);
        InventoryUtil.click(targetSlot, 0, ClickType.PICKUP);
        if (!InventoryUtil.cursor().isEmpty()) {
            InventoryUtil.click(sourceSlot, 0, ClickType.PICKUP);
        }
    }

    private boolean returnCursor(AbstractContainerMenu menu) {
        ItemStack cursor = InventoryUtil.cursor();
        if (cursor.isEmpty()) {
            return true;
        }

        int emptySlot = -1;
        for (int slot = 0; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty()) {
                if (emptySlot == -1) {
                    emptySlot = slot;
                }
                continue;
            }
            if (isSameType(stack, cursor) && stack.isStackable() && stack.getCount() < stack.getMaxStackSize()) {
                InventoryUtil.click(slot, 0, ClickType.PICKUP);
                return InventoryUtil.cursor().isEmpty();
            }
        }

        if (emptySlot != -1) {
            InventoryUtil.click(emptySlot, 0, ClickType.PICKUP);
            return InventoryUtil.cursor().isEmpty();
        }
        return false;
    }

    private boolean isSameType(ItemStack first, ItemStack second) {
        if (first.getItem() != second.getItem()) {
            return false;
        }
        if (first.isDamageableItem() || second.isDamageableItem()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(first, second);
    }

    private boolean canMerge(ItemStack target, ItemStack incoming) {
        return isSameType(target, incoming) && target.isStackable() && target.getCount() < target.getMaxStackSize();
    }

    private void debug(String message, Object... args) {
        if (!debug.getValue()) {
            return;
        }
        Command.sendMessage("{gray}[AutoRegear] " + message, "autoregear_debug", args);
    }

    private String itemName(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private Identifier parseIdentifier(String id) {
        String[] split = id.split(":", 2);
        if (split.length == 2) {
            return Identifier.fromNamespaceAndPath(split[0], split[1]);
        }
        return Identifier.fromNamespaceAndPath("minecraft", id);
    }

    private int toMenuSlot(int inventorySlot, int containerSlots) {
        return inventorySlot < 9 ? containerSlots + 27 + inventorySlot : containerSlots + inventorySlot - 9;
    }

    @Override
    public JsonElement toJson() {
        return super.toJson();
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        boolean enable = object.has("Enabled") && object.get("Enabled").getAsBoolean();
        for (Setting<?> setting : getSettings()) {
            if ("Enabled".equals(setting.getName())) {
                continue;
            }
            try {
                JsonElement settingElement = object.get(setting.getName());
                if (settingElement != null && !settingElement.isJsonNull()) {
                    ConfigManager.setValueFromJson(this, setting, settingElement);
                }
            } catch (Throwable throwable) {
                Pagon.LOGGER.error("Failed to load AutoRegear setting", throwable);
            }
        }
        if (!loadRegearFile()) {
            loadLegacyInventory(object.get("RegearInventory"));
        }
        setToggled(enable);
    }

    private JsonArray createRegearArray() {
        JsonArray regearArray = new JsonArray();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = regearInventory.get(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            JsonObject stackObject = new JsonObject();
            stackObject.addProperty("slot", slot);
            stackObject.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            stackObject.addProperty("count", stack.getCount());
            regearArray.add(stackObject);
        }
        return regearArray;
    }

    private void loadLegacyInventory(JsonElement inventoryElement) {
        regearInventory.clear();
        if (inventoryElement == null || !inventoryElement.isJsonArray()) {
            return;
        }
        readRegearArray(inventoryElement.getAsJsonArray());
        saveRegearFile();
    }

    private void readRegearArray(JsonArray regearArray) {
        regearInventory.clear();
        for (JsonElement jsonElement : regearArray) {
            if (!jsonElement.isJsonObject()) {
                continue;
            }
            JsonObject stackObject = jsonElement.getAsJsonObject();
            if (!stackObject.has("slot") || !stackObject.has("item")) {
                continue;
            }
            int slot = stackObject.get("slot").getAsInt();
            if (slot < 0 || slot >= 36) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getValue(parseIdentifier(stackObject.get("item").getAsString()));
            if (item == null || item == Items.AIR) {
                continue;
            }
            int count = stackObject.has("count") ? stackObject.get("count").getAsInt() : 1;
            regearInventory.put(slot, new ItemStack(item, Math.max(1, count)));
        }
    }

    private boolean loadRegearFile() {
        try {
            if (!Files.exists(REGEAR_FILE)) {
                return false;
            }
            JsonElement json = GSON.fromJson(Files.readString(REGEAR_FILE), JsonElement.class);
            if (json == null || !json.isJsonObject()) {
                return false;
            }
            JsonElement inventoryElement = json.getAsJsonObject().get("items");
            if (inventoryElement == null || !inventoryElement.isJsonArray()) {
                return false;
            }
            readRegearArray(inventoryElement.getAsJsonArray());
            return true;
        } catch (Throwable throwable) {
            Pagon.LOGGER.error("Failed to load AutoRegear file", throwable);
            return false;
        }
    }

    private void saveRegearFile() {
        try {
            Files.createDirectories(REGEAR_FILE.getParent());
            JsonObject object = new JsonObject();
            object.add("items", createRegearArray());
            Files.writeString(REGEAR_FILE, GSON.toJson(object));
        } catch (Throwable throwable) {
            Pagon.LOGGER.error("Failed to save AutoRegear file", throwable);
        }
    }
}


