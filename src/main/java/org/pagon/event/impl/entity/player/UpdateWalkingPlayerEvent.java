package org.pagon.event.impl.entity.player;

import org.pagon.event.Event;
import org.pagon.event.Stage;

public class UpdateWalkingPlayerEvent extends Event {
    private final Stage stage;

    public UpdateWalkingPlayerEvent(Stage stage) {
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }
}


