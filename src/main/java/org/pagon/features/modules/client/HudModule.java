package org.pagon.features.modules.client;

import org.pagon.Pagon;
import org.pagon.event.impl.input.MouseInputEvent;
import org.pagon.event.impl.render.Render2DEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.features.gui.HudEditorScreen;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Setting;
import net.minecraft.client.gui.screens.ChatScreen;
import org.joml.Vector2f;

public abstract class HudModule extends Module {
    public final Setting<Vector2f> pos = vec2f("Position", 0.5f, 0.5f);

    private float dragX, dragY, width, height;
    private boolean dragging, button;

    public HudModule(String name, String description, float width, float height) {
        super(name, description, Category.HUD);
        this.width = width;
        this.height = height;
    }

    public float getX() {
        return mc.getWindow().getGuiScaledWidth() * pos.getValue().x();
    }

    public float getY() {
        float heightWithChat = mc.getWindow().getGuiScaledHeight() - 14;
        float baseY = mc.getWindow().getGuiScaledHeight() * pos.getValue().y();
        float combined = baseY + getHeight();

        if (mc.screen instanceof ChatScreen) {
            baseY = Math.min(combined, heightWithChat) - getHeight();
        }
        return baseY;
    }

    public void onRender2DHud(Render2DEvent e) {
        render(e);
    }

    @Override
    public void onRender2D(Render2DEvent e) {
        render(e);
    }

    @Subscribe
    public void onMouse(MouseInputEvent e) {
        if (!(mc.screen instanceof HudEditorScreen) || nullCheck()) return;

        handleEditorMouse(e.getAction());
    }

    public void handleEditorMouse(int action) {
        if (!(mc.screen instanceof HudEditorScreen) || nullCheck()) return;

        if (action == 0) {
            button = false;
            dragging = false;
            HudEditorScreen.getInstance().currentDragging = null;
        }

        if (action == 1 && isHovering()) {
            button = true;
        }
    }

    protected void render(Render2DEvent e) {
        if (!(mc.screen instanceof HudEditorScreen) || nullCheck()) return;

        float x = getX();
        float y = getY();

        if (button) {
            if (!dragging && isHovering() && HudEditorScreen.getInstance().currentDragging == null) {
                dragX = getMouseX() - x;
                dragY = getMouseY() - y;
                dragging = true;
                HudEditorScreen.getInstance().currentDragging = this;
            }

            if (dragging) {
                float finalX = Math.min(Math.max(getMouseX() - dragX, 0),
                        mc.getWindow().getGuiScaledWidth() - width);
                float finalY = Math.min(Math.max(getMouseY() - dragY, 0),
                        mc.getWindow().getGuiScaledHeight() - height);

                pos.getValue().x = finalX / mc.getWindow().getGuiScaledWidth();
                pos.getValue().y = finalY / mc.getWindow().getGuiScaledHeight();
            }
        } else {
            dragging = false;
        }

        HudEditorScreen.getInstance().anyHover = HudEditorScreen.getInstance().anyHover || isHovering();
    }

    public int getMouseX() {
        return (int) (mc.mouseHandler.xpos() / mc.getWindow().getGuiScale());
    }

    public int getMouseY() {
        return (int) (mc.mouseHandler.ypos() / mc.getWindow().getGuiScale());
    }

    public void setBounds(float x, float y, float width, float height) {
        this.width = width;
        this.height = height;
        pos.getValue().x = x / mc.getWindow().getGuiScaledWidth();
        pos.getValue().y = y / mc.getWindow().getGuiScaledHeight();
    }

    public boolean isHovering() {
        float x = getX();
        float y = getY();
        int mouseX = getMouseX();
        int mouseY = getMouseY();

        return mouseX >= x - 1 && mouseX <= x + width + 1 &&
                mouseY >= y - 1 && mouseY <= y + height + 1;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}


