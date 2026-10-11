package org.pagon.event.impl.gui.hud;

import org.pagon.event.Event;
import net.minecraft.client.gui.GuiGraphics;

public class RenderOverlayEvent extends Event {
    private final GuiGraphics context;

    public RenderOverlayEvent(GuiGraphics context) {
        this.context = context;
    }

    public GuiGraphics getContext() {
        return context;
    }

    public static class Fire extends RenderOverlayEvent {
        public Fire(GuiGraphics context) {
            super(context);
        }
    }

    public static class Portal extends RenderOverlayEvent {
        public Portal(GuiGraphics context) {
            super(context);
        }
    }

    public static class Water extends RenderOverlayEvent {
        public Water(GuiGraphics context) {
            super(context);
        }
    }

    public static class Block extends RenderOverlayEvent {
        public Block(GuiGraphics context) {
            super(context);
        }
    }

    public static class Spyglass extends RenderOverlayEvent {
        public Spyglass(GuiGraphics context) {
            super(context);
        }
    }

    public static class Pumpkin extends RenderOverlayEvent {
        public Pumpkin(GuiGraphics context) {
            super(context);
        }
    }

    public static class BossBar extends RenderOverlayEvent {
        public BossBar(GuiGraphics context) {
            super(context);
        }
    }

    public static class Frostbite extends RenderOverlayEvent {
        public Frostbite(GuiGraphics context) {
            super(context);
        }
    }
}
