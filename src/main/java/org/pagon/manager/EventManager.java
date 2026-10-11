package org.pagon.manager;

import org.pagon.Pagon;
import org.pagon.event.Stage;
import org.pagon.event.impl.entity.DeathEvent;
import org.pagon.event.impl.entity.player.TickEvent;
import org.pagon.event.impl.entity.player.UpdateWalkingPlayerEvent;
import org.pagon.event.impl.input.KeyInputEvent;
import org.pagon.event.impl.input.MouseInputEvent;
import org.pagon.event.impl.network.ChatEvent;
import org.pagon.event.impl.network.PacketEvent;
import org.pagon.event.impl.render.Render2DEvent;
import org.pagon.event.impl.render.Render3DEvent;
import org.pagon.event.system.Subscribe;
import org.pagon.features.Feature;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.entity.player.Player;

public class EventManager extends Feature {
    public void init() {
        EVENT_BUS.register(this);
    }

    public void onUnload() {
        EVENT_BUS.unregister(this);
    }

    @Subscribe
    public void onTick(TickEvent event) {
        var level = mc.level;
        if (mc.player == null || level == null)
            return;
        Pagon.rotationManager.beginRotationTick();
        Pagon.moduleManager.onTick();
        Pagon.rotationManager.endRotationTick();
        Pagon.teleportManager.tick();
        for (Player player : level.players()) {
            if (player == null || player.getHealth() > 0.0F)
                continue;
            EVENT_BUS.post(new DeathEvent(player));
        }
    }

    @Subscribe
    public void onUpdateWalkingPlayer(UpdateWalkingPlayerEvent event) {
        if (nullCheck())
            return;
        if (event.getStage() == Stage.PRE) {
            Pagon.speedManager.update();
            Pagon.rotationManager.updateRotations();
            Pagon.positionManager.updatePosition();
            Pagon.rotationManager.applyRotations();
        }
        if (event.getStage() == Stage.POST) {
            Pagon.rotationManager.restoreRotations();
            Pagon.positionManager.restorePosition();
        }
    }

    @Subscribe
    public void onPacketReceive(PacketEvent.Receive event) {
        Pagon.serverManager.onPacketReceived();
        if (event.getPacket() instanceof ClientboundSetTimePacket)
            Pagon.serverManager.update();
        if (event.getPacket() instanceof ClientboundCustomPayloadPacket(CustomPacketPayload payload)
                && payload instanceof BrandPayload(String brand)) {
            Pagon.serverManager.setServerBrand(brand);
        }
    }

    @Subscribe
    public void onWorldRender(Render3DEvent event) {
        Pagon.moduleManager.onRender3D(event);
    }

    @Subscribe
    public void onRenderGameOverlayEvent(Render2DEvent event) {
        Pagon.moduleManager.onRender2D(event);
    }

    @Subscribe
    public void onKeyInput(KeyInputEvent event) {
        Pagon.moduleManager.onKeyPressed(event.getKey());
    }

    @Subscribe
    public void onMouseInput(MouseInputEvent event) {
        if (event.getAction() == 1) {
            Pagon.moduleManager.onMouseClicked(event.getButton());
        }
    }

    @Subscribe
    public void onChatSent(ChatEvent event) {
        String message = event.getMessage();
        if (!message.startsWith(Pagon.commandManager.getCommandPrefix())) {
            return;
        }
        event.cancel();
        Pagon.commandManager.onChatSent(message);
    }
}


