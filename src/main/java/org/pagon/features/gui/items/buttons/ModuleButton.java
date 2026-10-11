package org.pagon.features.gui.items.buttons;

import org.pagon.features.gui.GuiStyle;
import org.pagon.features.gui.items.Item;
import org.pagon.features.modules.Module;
import org.pagon.features.settings.Bind;
import org.pagon.features.settings.Setting;
import org.pagon.util.render.Animation;
import org.pagon.util.render.ClientFont;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.pagon.util.render.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public class ModuleButton
        extends Button {
    private final Module module;
    private List<Item> items = new ArrayList<>();
    private boolean subOpen;
    private final Animation subOpenAnimation = new Animation(0.4f, Animation.Easing.EASE_OUT_CUBIC);

    public ModuleButton(Module module) {
        super(module.getDisplayName());
        this.module = module;
        this.initSettings();
    }

    @SuppressWarnings("unchecked")
    public void initSettings() {
        ArrayList<Item> newItems = new ArrayList<>();
        if (!this.module.getSettings().isEmpty()) {
            for (Setting<?> setting : new ArrayList<>(this.module.getSettings())) {
                if (this.module.getCategory() == Module.Category.HUD
                        && (setting.getName().equalsIgnoreCase("Drawn") || setting.getName().equalsIgnoreCase("Keybind"))) {
                    continue;
                }
                if (this.module.getCategory() == Module.Category.CLIENT
                        && !this.module.getName().equalsIgnoreCase("ClickGui")
                        && (setting.getName().equalsIgnoreCase("Drawn") || setting.getName().equalsIgnoreCase("Keybind"))) {
                    continue;
                }
                if (setting.getValue() instanceof Boolean && !setting.getName().equals("Enabled")) {
                    newItems.add(new BooleanButton((Setting<Boolean>) setting));
                }
                if (setting.getValue() instanceof Bind && !setting.getName().equalsIgnoreCase("Keybind") && !this.module.getName().equalsIgnoreCase("Hud")) {
                    newItems.add(new BindButton((Setting<Bind>) setting));
                }
                if ((setting.getValue() instanceof String || setting.getValue() instanceof Character) && !setting.getName().equalsIgnoreCase("displayName")) {
                    newItems.add(new StringButton((Setting<String>) setting));
                }
                if (setting.isColorSetting()) {
                    newItems.add(new ColorButton((Setting<java.awt.Color>) setting));
                    continue;
                }
                if (setting.isNumberSetting() && setting.hasRestriction()) {
                    newItems.add(new Slider((Setting<Number>) setting));
                    continue;
                }
                if (!setting.isEnumSetting()) continue;
                newItems.add(new EnumButton((Setting<Enum<?>>) setting));
            }
        }
        if (this.module.getCategory() != Module.Category.HUD
                && (this.module.getCategory() != Module.Category.CLIENT || this.module.getName().equalsIgnoreCase("ClickGui"))) {
            newItems.add(new BindButton((Setting<Bind>) this.module.getSettingByName("Keybind")));
        }
        this.items = newItems;
    }

    @Override
    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        // Update animation
        subOpenAnimation.setTarget(this.subOpen ? 1.0f : 0.0f);
        subOpenAnimation.update(partialTicks);
        
        float animProgress = subOpenAnimation.getValue();
        
        if (this.isHovering(mouseX, mouseY)) {
            RenderUtil.rect(context, this.x, this.y, this.x + this.width, this.y + GuiStyle.itemHeight(), GuiStyle.PANEL_HOVER);
        }

        String label = this.module.isTestBuild() ? "test" : null;
        int labelWidth = label == null ? 0 : Math.max(1, ClientFont.guiWidth(label) / 2);
        int nameMaxWidth = Math.max(1, this.width - 6 - labelWidth - (label == null ? 0 : 2));
        String title = ClientFont.trimToWidth(this.getName(), nameMaxWidth);
        float titleX = this.x + 3.0f;
        float titleY = this.y + 2.0f;
        drawString(title, titleX, titleY, GuiStyle.text(this.getState()));

        if (label != null) {
            float labelX = titleX + ClientFont.guiWidth(title) + 2.0f;
            float labelY = titleY - 1.0f;
            context.pose().pushMatrix();
            try {
                context.pose().scale(0.5f, 0.5f);
                ClientFont.drawGuiString(context, label, labelX * 2.0f, labelY * 2.0f, GuiStyle.TEXT_DIM);
            } finally {
                context.pose().popMatrix();
            }
        }
        
        if (!this.items.isEmpty() && animProgress > 0.01f) {
            float totalHeight = 0.0f;
            for (Item item : this.items) {
                if (!item.isHidden()) {
                    totalHeight += item.getHeight();
                }
            }
            
            float animatedHeight = totalHeight * animProgress;
            int settingsBottom = (int) (this.y + GuiStyle.itemHeight() + animatedHeight);
            
            // Background with fade
            int bgAlpha = (int) (animProgress * 224); // 0xE0 = 224
            int bgColor = (bgAlpha << 24) | 0x121111;
            RenderUtil.rect(context, this.x, this.y + GuiStyle.itemHeight(),
                    this.x + this.width, settingsBottom, bgColor);
            
            // Accent line on left with fade
            int accentAlpha = (int) (animProgress * 255);
            int accentColor = (GuiStyle.accent() & 0x00FFFFFF) | (accentAlpha << 24);
            RenderUtil.rect(context, this.x + 2.0f, this.y + GuiStyle.itemHeight() + 1.0f,
                    this.x + 3.0f, settingsBottom - 1.0f, accentColor);
            
            float height = GuiStyle.itemHeight();
            for (Item item : this.items) {
                if (!item.isHidden()) {
                    float itemY = this.y + height;
                    // Only render items within animated bounds
                    if (itemY < this.y + GuiStyle.itemHeight() + animatedHeight) {
                        item.setLocation(this.x, itemY);
                        item.setWidth(this.width);
                        
                        // More aggressive fade - items fade faster to match animation speed
                        // Use squared progress for faster fade-out
                        float itemProgress = (itemY - (this.y + GuiStyle.itemHeight())) / Math.max(1.0f, animatedHeight);
                        float itemFade = animProgress * animProgress; // Squared for faster fade
                        itemFade = Math.max(0.0f, Math.min(1.0f, itemFade * 1.2f)); // Boost and clamp
                        
                        item.renderAlpha = itemFade;
                        
                        // Draw accent line on hover for settings with fade
                        if (item.isHovering(mouseX, mouseY) && itemFade > 0.3f) {
                            int hoverAlpha = (int) (itemFade * 255);
                            int hoverAccent = (GuiStyle.accent() & 0x00FFFFFF) | (hoverAlpha << 24);
                            RenderUtil.rect(context, this.x + 2.0f, itemY,
                                    this.x + 3.0f, itemY + item.getHeight(), hoverAccent);
                        }
                        
                        // Only render if visible enough
                        if (itemFade > 0.05f) {
                            item.drawScreen(context, mouseX, mouseY, partialTicks);
                        }
                    }
                    height += item.getHeight();
                }
                item.update();
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean headerHover = this.isHeaderHovering(mouseX, mouseY);
        if (mouseButton == 0 && headerHover) {
            this.onMouseClick();
            return;
        }

        if (mouseButton == 1 && headerHover && !this.items.isEmpty()) {
            this.subOpen = !this.subOpen;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
            return;
        }

        if (!this.items.isEmpty() && this.subOpen) {
            for (Item item : this.items) {
                if (item.isHidden()) continue;
                item.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
        super.mouseReleased(mouseX, mouseY, releaseButton);
        if (!this.items.isEmpty() && this.subOpen) {
            for (Item item : this.items) {
                if (item.isHidden()) continue;
                item.mouseReleased(mouseX, mouseY, releaseButton);
            }
        }
    }

    @Override
    public void onKeyTyped(String typedChar, int keyCode) {
        super.onKeyTyped(typedChar, keyCode);
        if (!this.items.isEmpty() && this.subOpen) {
            for (Item item : this.items) {
                if (item.isHidden()) continue;
                item.onKeyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    public void onKeyPressed(int key) {
        super.onKeyPressed(key);
        if (!this.items.isEmpty() && this.subOpen) {
            for (Item item : this.items) {
                if (item.isHidden()) continue;
                item.onKeyPressed(key);
            }
        }
    }

    @Override
    public int getHeight() {
        if (!this.items.isEmpty()) {
            subOpenAnimation.setTarget(this.subOpen ? 1.0f : 0.0f);
            float animProgress = subOpenAnimation.getRawValue();
            
            if (animProgress > 0.01f) {
                float totalHeight = GuiStyle.itemHeight();
                for (Item item : this.items) {
                    if (!item.isHidden()) {
                        totalHeight += item.getHeight();
                    }
                }
                return (int) (GuiStyle.itemHeight() + (totalHeight - GuiStyle.itemHeight()) * animProgress);
            }
        }
        return GuiStyle.itemHeight();
    }

    public Module getModule() {
        return this.module;
    }

    @Override
    public void toggle() {
        this.module.toggle();
    }

    @Override
    public boolean getState() {
        return this.module.isEnabled();
    }

    private boolean isHeaderHovering(int mouseX, int mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth()
                && mouseY >= this.getY() && mouseY <= this.getY() + GuiStyle.itemHeight();
    }
}


