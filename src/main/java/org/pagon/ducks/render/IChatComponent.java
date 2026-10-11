package org.pagon.ducks.render;

import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.MessageSignature;

public interface IChatComponent {
    void pagon$addMessage(GuiMessage message);
    void pagon$removeMessage(MessageSignature signature);
}


