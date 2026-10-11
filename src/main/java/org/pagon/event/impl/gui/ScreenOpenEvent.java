package org.pagon.event.impl.gui;

import org.pagon.event.Event;
import net.minecraft.client.gui.screens.Screen;

public class ScreenOpenEvent extends Event {
    private final Screen screen;

    public ScreenOpenEvent(Screen screen) {
        this.screen = screen;
    }

    public Screen getScreen() {
        return screen;
    }
}
