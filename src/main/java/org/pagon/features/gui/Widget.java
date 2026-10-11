package org.pagon.features.gui;

import org.pagon.features.Feature;
import org.pagon.features.gui.items.Item;
import org.pagon.features.gui.items.buttons.Button;
import org.pagon.util.render.Animation;
import org.pagon.util.render.ClientFont;
import org.pagon.util.render.RenderUtil;
import org.pagon.util.render.ScissorUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Widget
        extends Feature {
    protected GuiGraphics context;
    private final List<Item> items = new ArrayList<>();
    public boolean drag;
    private int x;
    private int y;
    private int x2;
    private int y2;
    private int width;
    private int height;
    private boolean open;
    private boolean hidden = false;
    private final Animation openAnimation = new Animation(0.35f, Animation.Easing.EASE_OUT_CUBIC);

    public Widget(String name, int x, int y, boolean open) {
        super(name);
        this.x = x;
        this.y = y;
        this.width = GuiStyle.PANEL_WIDTH;
        this.height = GuiStyle.headerHeight();
        this.open = open;
    }

    private void drag(int mouseX, int mouseY) {
        if (!this.drag) {
            return;
        }
        this.x = this.x2 + mouseX;
        this.y = this.y2 + mouseY;
    }

    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        this.context = context;
        this.height = GuiStyle.headerHeight();
        this.drag(mouseX, mouseY);
        
        // Update animation
        openAnimation.setTarget(this.open ? 1.0f : 0.0f);
        openAnimation.update(partialTicks);
        
        float animProgress = openAnimation.getValue();
        float totalHeight = this.getTotalItemHeight();
        float animatedHeight = totalHeight * animProgress;
        int bodyBottom = Math.round(this.y + this.height + animatedHeight);

        RenderUtil.rect(context, this.x, this.y, this.x + this.width, bodyBottom, GuiStyle.PANEL);
        RenderUtil.rect(context, this.x, this.y, this.x + this.width, this.y + this.height, GuiStyle.accent());
        if (animProgress > 0.01f) {
            int alpha = (int) (animProgress * 255);
            RenderUtil.rect(context, this.x + 1, this.y + this.height, this.x + this.width - 1, bodyBottom - 1, GuiStyle.PANEL);
        }

        String count = String.valueOf(getVisibleItemCount());
        int countWidth = ClientFont.guiWidth(count);
        int bracketWidth = ClientFont.guiWidth("[");
        int rightBracketWidth = ClientFont.guiWidth("]");
        float countX = this.x + this.width - bracketWidth - countWidth - rightBracketWidth - 2.0f;
        int titleMaxWidth = Math.max(1, Math.round(countX - this.x - 4.0f));
        drawString(ClientFont.trimToWidth(this.getName(), titleMaxWidth), this.x + 2.0f, this.y + 1.0f, GuiStyle.TEXT_BRIGHT);
        drawString("[", countX, this.y + 1.0f, GuiStyle.TEXT_DIM);
        drawString(count, countX + bracketWidth, this.y + 1.0f, GuiStyle.TEXT_BRIGHT);
        drawString("]", countX + bracketWidth + countWidth, this.y + 1.0f, GuiStyle.TEXT_DIM);
        
        // Only use scissor if we have content to render
        boolean useScissor = animProgress > 0.01f;
        if (useScissor) {
            ScissorUtil.enable(context, x, 0, x + width, mc.getWindow().getGuiScaledHeight());
        }

        if (animProgress > 0.01f) {
            float y = this.getY() + this.getHeight();
            float maxY = this.getY() + this.getHeight() + animatedHeight;
            
            for (Item item : this.getItems()) {
                if (item.isHidden()) continue;
                item.setLocation(this.x + 1.0f, y);
                item.setWidth(this.getWidth() - 2);
                
                // Only render items that are within the animated area
                if (y < maxY) {
                    // Aggressive fade for widget items too
                    float itemProgress = (y - (this.getY() + this.getHeight())) / Math.max(1.0f, animatedHeight);
                    float itemFade = animProgress * animProgress; // Squared for speed
                    itemFade = Math.max(0.0f, Math.min(1.0f, itemFade * 1.2f));
                    
                    item.renderAlpha = itemFade;
                    
                    if (itemFade > 0.05f) {
                        boolean needsFullScissor = item.isHovering(mouseX, mouseY);
                        if (needsFullScissor) {
                            ScissorUtil.disable(context);
                        }
                        
                        item.drawScreen(context, mouseX, mouseY, partialTicks);

                        if (needsFullScissor) {
                            ScissorUtil.enable(context, x, 0, x + width, mc.getWindow().getGuiScaledHeight());
                        }
                    }
                }
                y += item.getHeight();
            }
        }

        if (useScissor) {
            ScissorUtil.disable(context);
        }
        RenderUtil.rect(context, this.x, this.y, this.x + this.width, bodyBottom, GuiStyle.accentBorder(), 1.0f);
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
            this.x2 = this.x - mouseX;
            this.y2 = this.y - mouseY;
            PagonGui.getClickGui().getComponents().forEach(component -> {
                if (component.drag) {
                    component.drag = false;
                }
            });
            this.drag = true;
            return;
        }
        if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.open = !this.open;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
            return;
        }
        if (!this.open) {
            return;
        }
        this.getItems().forEach(item -> item.mouseClicked(mouseX, mouseY, mouseButton));
    }

    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
        if (releaseButton == 0) {
            this.drag = false;
        }
        if (!this.open) {
            return;
        }
        this.getItems().forEach(item -> item.mouseReleased(mouseX, mouseY, releaseButton));
    }

    public void onKeyTyped(String typedChar, int keyCode) {
        if (!this.open) {
            return;
        }
        this.getItems().forEach(item -> item.onKeyTyped(typedChar, keyCode));
    }

    public void onKeyPressed(int key) {
        if (!open) return;
        this.getItems().forEach(item -> item.onKeyPressed(key));
    }

    public void addButton(Button button) {
        this.items.add(button);
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isOpen() {
        return this.open;
    }

    public final List<Item> getItems() {
        return this.items;
    }

    public boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight();
    }

    private float getTotalItemHeight() {
        float height = 0.0f;
        for (Item item : this.getItems()) {
            if (item.isHidden()) continue;
            height += item.getHeight();
        }
        return height;
    }

    private int getVisibleItemCount() {
        int count = 0;
        for (Item item : this.getItems()) {
            if (!item.isHidden()) {
                count++;
            }
        }
        return count;
    }

    protected void drawString(String text, double x, double y, Color color) {
        drawString(text, x, y, color.hashCode());
    }

    protected void drawString(String text, double x, double y, int color) {
        ClientFont.drawGuiString(context, text, x, y, color);
    }
}


