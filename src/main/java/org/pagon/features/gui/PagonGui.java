package org.pagon.features.gui;

import org.pagon.Pagon;
import org.pagon.features.Feature;
import org.pagon.features.gui.items.Item;
import org.pagon.features.gui.items.buttons.ModuleButton;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;
import org.pagon.util.render.ClientFont;
import org.pagon.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PagonGui extends Screen {
    private static PagonGui INSTANCE;
    private static Color colorClipboard = null;

    static {
        INSTANCE = new PagonGui();
    }

    private final ArrayList<Widget> widgets = new ArrayList<>();
    private boolean searchOpen;
    private String searchQuery = "";

    public PagonGui() {
        super(Component.literal("Legacy"));
        setInstance();
        load();
    }

    public static PagonGui getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PagonGui();
        }
        return INSTANCE;
    }

    public static PagonGui getClickGui() {
        return PagonGui.getInstance();
    }

    private void setInstance() {
        INSTANCE = this;
    }

    private void load() {
        int x = -GuiStyle.PANEL_WIDTH - GuiStyle.PANEL_GAP + 1;
        for (Module.Category category : Pagon.moduleManager.getCategories()) {
            if (category == Module.Category.HUD) continue;
            Widget panel = new Widget(category.getName(), x += GuiStyle.PANEL_WIDTH + GuiStyle.PANEL_GAP, 8, true);
            Pagon.moduleManager.stream()
                    .filter(m -> m.getCategory() == category && !m.hidden)
                    .map(ModuleButton::new)
                    .forEach(panel::addButton);
            this.widgets.add(panel);
        }
        this.widgets.forEach(components -> components.getItems().sort(Comparator.comparing(Feature::getName)));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        Item.context = context;
        context.fill(0, 0, context.guiWidth(), context.guiHeight(), GuiStyle.background());
        this.widgets.forEach(components -> components.drawScreen(context, mouseX, mouseY, delta));
        if (searchOpen) {
            drawSearch(context);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        this.widgets.forEach(components -> components.mouseClicked((int) click.x(), (int) click.y(), click.button()));
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        this.widgets.forEach(components -> components.mouseReleased((int) click.x(), (int) click.y(), click.button()));
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount < 0) {
            this.widgets.forEach(component -> component.setY(component.getY() - 10));
        } else if (verticalAmount > 0) {
            this.widgets.forEach(component -> component.setY(component.getY() + 10));
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.input();
        if (key == GLFW.GLFW_KEY_F && isCtrlDown()) {
            searchOpen = true;
            searchQuery = "";
            return true;
        }

        if (searchOpen) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                searchOpen = false;
                searchQuery = "";
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                removeLastSearchChar();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) {
                toggleFirstSearchResult();
                return true;
            }
            return true;
        }

        this.widgets.forEach(component -> component.onKeyPressed(input.input()));
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (searchOpen) {
            String typed = input.codepointAsString();
            if (typed != null && !typed.isBlank() && typed.codePoints().noneMatch(Character::isISOControl)) {
                searchQuery += typed;
            }
            return true;
        }

        this.widgets.forEach(component -> component.onKeyTyped(input.codepointAsString(), input.modifiers()));
        return super.charTyped(input);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
    }

    public final ArrayList<Widget> getComponents() {
        return this.widgets;
    }

    public int getTextOffset() {
        return 0;
    }

    public static Color getColorClipboard() {
        return colorClipboard;
    }

    public static void setColorClipboard(Color color) {
        colorClipboard = color;
    }

    private void drawSearch(GuiGraphics context) {
        List<SearchResult> results = getSearchResults();
        int width = 308;
        int rowHeight = 13;
        int inputHeight = 24;
        int visibleRows = Math.min(results.size(), 11);
        int x = Math.max(4, context.guiWidth() / 2 - width / 2);
        int y = 17;
        int height = inputHeight + visibleRows * rowHeight + 2;

        RenderUtil.rect(context, x, y, x + width, y + height, GuiStyle.PANEL_DARK);
        RenderUtil.rect(context, x, y, x + width, y + height, GuiStyle.accentBorder(), 1.0f);
        RenderUtil.rect(context, x, y, x + width, y + inputHeight, 0xC0101010);
        RenderUtil.rect(context, x, y + inputHeight - 1, x + width, y + inputHeight, GuiStyle.accent());

        String caret = (System.currentTimeMillis() / 450L) % 2L == 0L ? "|" : "";
        ClientFont.drawGuiString(context, ClientFont.trimToWidth("> " + searchQuery + caret, width - 14), x + 7, y + 6, GuiStyle.TEXT_BRIGHT);

        for (int i = 0; i < visibleRows; i++) {
            SearchResult result = results.get(i);
            int rowY = y + inputHeight + i * rowHeight;
            if (i == 0) {
                RenderUtil.rect(context, x + 1, rowY, x + width - 1, rowY + rowHeight, GuiStyle.accent(95));
            } else if (i % 2 == 1) {
                RenderUtil.rect(context, x + 1, rowY, x + width - 1, rowY + rowHeight, 0x60151010);
            }

            int nameColor = result.module().isEnabled() ? GuiStyle.accent() : GuiStyle.TEXT_BRIGHT;
            String category = "[" + result.module().getCategory().getName() + "]";
            int categoryWidth = ClientFont.guiWidth(category);
            int labelMaxWidth = Math.max(1, width - categoryWidth - 24);
            ClientFont.drawGuiString(context, ClientFont.trimToWidth(result.label(), labelMaxWidth), x + 7, rowY + 3, nameColor);
            ClientFont.drawGuiString(context, category, x + width - categoryWidth - 8, rowY + 3, GuiStyle.TEXT);
        }
    }

    private List<SearchResult> getSearchResults() {
        String query = searchQuery.toLowerCase(Locale.ROOT).trim();
        List<SearchResult> results = new ArrayList<>();
        if (query.isEmpty()) {
            return results;
        }

        for (Module module : Pagon.moduleManager.getModules()) {
            if (module.hidden || module.getCategory() == Module.Category.HUD) continue;
            String moduleName = module.getName();
            if (moduleName.toLowerCase(Locale.ROOT).contains(query)) {
                results.add(new SearchResult(moduleName, module));
            }
            for (Setting<?> setting : module.getSettings()) {
                String settingName = setting.getName();
                if (settingName.equalsIgnoreCase("Enabled") || settingName.equalsIgnoreCase("DisplayName")) continue;
                if (settingName.toLowerCase(Locale.ROOT).contains(query)) {
                    results.add(new SearchResult(moduleName + " > " + settingName, module));
                }
            }
        }

        results.sort(Comparator.comparing(SearchResult::label, String.CASE_INSENSITIVE_ORDER));
        return results;
    }

    private boolean isCtrlDown() {
        long handle = minecraft.getWindow().handle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private void removeLastSearchChar() {
        if (searchQuery.isEmpty()) return;
        int last = searchQuery.offsetByCodePoints(searchQuery.length(), -1);
        searchQuery = searchQuery.substring(0, last);
    }

    private void toggleFirstSearchResult() {
        List<SearchResult> results = getSearchResults();
        if (!results.isEmpty()) {
            results.get(0).module().toggle();
        }
    }

    private record SearchResult(String label, Module module) {
    }
}


