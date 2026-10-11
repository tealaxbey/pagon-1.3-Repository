package org.pagon.mixin.client;

import org.pagon.event.impl.network.ChatEvent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.pagon.util.traits.Util.EVENT_BUS;

@Mixin(ChatScreen.class)
public class MixinChatScreen {
    @Inject(method = "handleChatInput(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void pagon$hookSendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (chatText == null || chatText.isEmpty()) {
            return;
        }

        if (EVENT_BUS.post(new ChatEvent(chatText))) {
            ci.cancel();
        }
    }
}
