package org.pagon.util.font;

import org.pagon.Pagon;
import org.pagon.features.modules.client.FontModule;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class Fonts {
    public static final String DEFAULT_FONT_FILE_PATH = "assets/pagon/font/verdana.ttf";
    private static final float FONT_SIZE = 9.0f;

    public static String FONT_FILE_PATH = DEFAULT_FONT_FILE_PATH;

    public static AWTFontRenderer CLIENT;
    public static AWTFontRenderer CLIENT_UNSCALED;

    private static boolean bootstrapped;
    private static boolean initialized;

    private Fonts() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        Pagon.configManager.addConfig(new FontFile());
        bootstrapped = true;
    }

    public static void init() {
        if (initialized) {
            return;
        }
        loadFonts();
        initialized = true;
    }

    public static void loadFonts() {
        closeFonts();
        try {
            CLIENT = new AWTFontRenderer(openFontStream(FONT_FILE_PATH), FONT_SIZE);
            CLIENT_UNSCALED = new AWTFontRenderer(openFontStream(FONT_FILE_PATH), FONT_SIZE);
        } catch (IOException exception) {
            Pagon.LOGGER.error("Failed to load client fonts from {}", FONT_FILE_PATH, exception);
            try {
                FONT_FILE_PATH = DEFAULT_FONT_FILE_PATH;
                CLIENT = new AWTFontRenderer(openFontStream(FONT_FILE_PATH), FONT_SIZE);
                CLIENT_UNSCALED = new AWTFontRenderer(openFontStream(FONT_FILE_PATH), FONT_SIZE);
            } catch (IOException fallbackException) {
                Pagon.LOGGER.error("Failed to load fallback font", fallbackException);
            }
        }
    }

    public static void closeFonts() {
        if (CLIENT != null) {
            CLIENT.close();
            CLIENT = null;
        }
        if (CLIENT_UNSCALED != null) {
            CLIENT_UNSCALED.close();
            CLIENT_UNSCALED = null;
        }
    }

    public static boolean isInitialized() {
        return initialized && CLIENT != null && CLIENT_UNSCALED != null;
    }

    public static boolean isCustomEnabled() {
        FontModule module = FontModule.getInstance();
        return module != null && module.isEnabled() && isInitialized();
    }

    public static boolean isHudCustomEnabled() {
        FontModule module = FontModule.getInstance();
        return module != null && module.isEnabled() && module.getHud() && isInitialized();
    }

    private static InputStream openFontStream(String path) throws IOException {
        if (path.startsWith("assets/")) {
            InputStream resource = Fonts.class.getClassLoader().getResourceAsStream(path);
            if (resource == null) {
                throw new IOException("Missing bundled font resource: " + path);
            }
            return resource;
        }
        return new FileInputStream(path);
    }
}
