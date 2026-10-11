package org.pagon.mixin.network;

import org.pagon.Pagon;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.pagon.event.impl.network.DisconnectEvent;
import org.pagon.event.impl.network.PacketEvent;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(Connection.class)
public class MixinConnection {

    @Shadow
    private Channel channel;
    @Shadow
    @Final
    private PacketFlow receiving;

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    public void channelRead0(ChannelHandlerContext chc, Packet<?> packet, CallbackInfo ci) {
        if (this.channel.isOpen() && packet != null) {
            try {
                if (EVENT_BUS.post(new PacketEvent.Receive(packet))) {
                    ci.cancel();
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void sendPacket(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        if (this.receiving != PacketFlow.CLIENTBOUND) return;
        try {
            if (EVENT_BUS.post(new PacketEvent.Send(packet))) {
                ci.cancel();
            }
        } catch (Exception ignored) {
        }
    }

    @ModifyVariable(method = "sendPacket", at = @At("HEAD"), argsOnly = true)
    private Packet<?> rewriteMovementPacket(Packet<?> packet) {
        if (this.receiving != PacketFlow.CLIENTBOUND || Pagon.rotationManager == null) return packet;
        return Pagon.rotationManager.rewriteMovementPacket(packet);
    }

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void disconnect(Component reason, CallbackInfo ci) {
        EVENT_BUS.post(new DisconnectEvent());
    }

}


