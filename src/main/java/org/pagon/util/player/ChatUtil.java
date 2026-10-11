package org.pagon.util.player;

import org.pagon.Pagon;
import org.pagon.ducks.render.IChatComponent;
import org.pagon.features.commands.Command;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;

import java.nio.charset.StandardCharsets;

import static org.pagon.util.traits.Util.mc;

public class ChatUtil {
    public static void sendMessage(Component message, String identifier) {
        sendClientSideMessage(Component.empty()
                .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
                .append(getClientNameComponent())
                .append(" ")
                .append(message), identifier);
    }

    public static void sendNotificationMessage(Component message, String identifier) {
        sendClientSideMessage(Component.empty()
                .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
                .append(getNotificationPrefixComponent())
                .append(" ")
                .append(message), identifier);
    }

    public static void sendClientSideMessage(Component message, String identifier) {
        if (Command.nullCheck()) return;

        IChatComponent chat = (IChatComponent) mc.gui.getChat();
        MessageSignature signature = new MessageSignature(get256Bytes(identifier));
        chat.pagon$removeMessage(signature);
        chat.pagon$addMessage(new GuiMessage(mc.gui.getGuiTicks(), message, signature, getMessageTag()));
    }

    public static Component getClientNameComponent() {
        return Component.empty()
                .withColor(Pagon.colorManager.getColorAsInt())
                .append("[")
                .append("Legacy")
                .append("]");
    }

    private static Component getNotificationPrefixComponent() {
        return getClientNameComponent();
    }

    private static GuiMessageTag getMessageTag() {
        return new GuiMessageTag(Pagon.colorManager.getColorAsInt(), null, null, null);
    }

    private static byte[] get256Bytes(String identifier) {
        byte[] bytes = new byte[256];
        byte[] identifierBytes = identifier.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(identifierBytes, 0, bytes, 0, Math.min(bytes.length, identifierBytes.length));
        return bytes;
    }
}


