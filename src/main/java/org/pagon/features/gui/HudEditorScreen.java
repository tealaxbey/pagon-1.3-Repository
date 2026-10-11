package org.pagon.features.gui;

import org.pagon.Pagon;
import org.pagon.event.impl.render.Render2DEvent;
import org.pagon.features.Feature;
import org.pagon.features.gui.items.buttons.ModuleButton;
import org.pagon.features.modules.Module;
import org.pagon.features.modules.client.HudModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;

public class HudEditorScreen extends Screen {
    private static HudEditorScreen INSTANCE;

    private final ArrayList<Widget> components = new ArrayList<>();
    public HudModule currentDragging;
    public boolean anyHover;

    private HudEditorScreen() {
        super(Component.literal("Legacy-hudeditor"));
        load();
    }

    private void load() {
        this.components.clear();
        Widget hud = new Widget("Hud", 50, 50, true);
        Pagon.moduleManager.stream()
                .filter(m -> m.getCategory() == Module.Category.HUD && !m.hidden)
                .map(ModuleButton::new)
                .forEach(hud::addButton);
        this.components.add(hud);
        this.components.forEach(component -> component.getItems().sort(Comparator.comparing(Feature::getName)));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        anyHover = false;
        context.fill(0, 0, context.guiWidth(), context.guiHeight(), GuiStyle.background());
        this.components.forEach(component -> component.drawScreen(context, mouseX, mouseY, delta));
        Pagon.moduleManager.renderHudModules(new Render2DEvent(context, delta));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        this.components.forEach(component -> component.mouseClicked((int) click.x(), (int) click.y(), click.button()));
        Pagon.moduleManager.stream()
                .filter(module -> module instanceof HudModule && module.isEnabled())
                .map(module -> (HudModule) module)
                .forEach(module -> module.handleEditorMouse(1));
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        this.components.forEach(component -> component.mouseReleased((int) click.x(), (int) click.y(), click.button()));
        Pagon.moduleManager.stream()
                .filter(module -> module instanceof HudModule && module.isEnabled())
                .map(module -> (HudModule) module)
                .forEach(module -> module.handleEditorMouse(0));
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount < 0) {
            this.components.forEach(component -> component.setY(component.getY() - 10));
        } else if (verticalAmount > 0) {
            this.components.forEach(component -> component.setY(component.getY() + 10));
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        this.components.forEach(component -> component.onKeyPressed(input.input()));
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        this.components.forEach(component -> component.onKeyTyped(input.codepointAsString(), input.modifiers()));
        return super.charTyped(input);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
    }

    public ArrayList<Widget> getComponents() {
        return components;
    }

    public static HudEditorScreen getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new HudEditorScreen();
        }
        return INSTANCE;
    }
}


