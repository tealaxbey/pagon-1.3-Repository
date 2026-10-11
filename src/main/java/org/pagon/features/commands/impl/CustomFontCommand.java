package org.pagon.features.commands.impl;

import org.pagon.Pagon;
import org.pagon.features.commands.Command;
import org.pagon.manager.CommandManager;
import org.pagon.util.font.Fonts;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;

public class CustomFontCommand extends Command {
    public CustomFontCommand() {
        super("customfont");
        setDescription("Sets the client font file");
    }

    @Override
    public void createArgumentBuilder(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandManager> builder) {
        builder.then(argument("font", string()).executes(ctx -> {
            String font = getString(ctx, "font");
            if ("verdana".equalsIgnoreCase(font) || "verdana.ttf".equalsIgnoreCase(font)) {
                Fonts.FONT_FILE_PATH = Fonts.DEFAULT_FONT_FILE_PATH;
                Fonts.loadFonts();
                Pagon.configManager.save();
                return success("Set client font to %s", font);
            }

            String fileName = font.endsWith(".ttf") ? font : font + ".ttf";
            Path path = FabricLoader.getInstance().getGameDir().resolve("pagon").resolve(fileName);
            if (!Files.exists(path)) {
                return fail("Could not find font file: %s", fileName);
            }

            Fonts.FONT_FILE_PATH = path.toAbsolutePath().toString();
            Fonts.loadFonts();
            Pagon.configManager.save();
            return success("Set client font to %s", font);
        }));
    }
}
