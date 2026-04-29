package com.kingmihailp.customtab.client;

import com.kingmihailp.customtab.CustomTabMod;
import com.kingmihailp.customtab.config.TabClientConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Manages the lifecycle of the client-side tab header image texture and renders it.
 *
 * The texture is loaded lazily and cached until the configured path changes.
 * Call {@link #invalidate()} to force a reload on the next render.
 */
public final class ClientTabImageRenderer {

    private static final ResourceLocation TEXTURE_LOC =
            ResourceLocation.fromNamespaceAndPath(CustomTabMod.MODID, "tab_header_image");

    private static String  loadedPath   = null;
    private static boolean loadFailed   = false;
    private static int     texWidth     = 0;
    private static int     texHeight    = 0;
    private static DynamicTexture texture = null;

    private ClientTabImageRenderer() {}

    /** Force the texture to be reloaded on the next render call. */
    public static void invalidate() {
        releaseTexture();
        loadedPath  = null;
        loadFailed  = false;
    }

    /**
     * Called every frame while the tab list is open.
     * Handles lazy loading, then renders the image centered at the top of the screen.
     */
    public static void render(GuiGraphics gui, int screenWidth) {
        if (!TabClientConfig.IMAGE_ENABLED.get()) return;

        String path = TabClientConfig.IMAGE_PATH.get();

        // Reload if path changed or texture was never loaded
        if (!path.equals(loadedPath) || (texture == null && !loadFailed)) {
            loadTexture(path);
        }

        if (texture == null || loadFailed) return;

        int dispW  = TabClientConfig.IMAGE_DISPLAY_WIDTH.get();
        int dispH  = TabClientConfig.IMAGE_DISPLAY_HEIGHT.get();
        int xOff   = TabClientConfig.IMAGE_X_OFFSET.get();
        int yOff   = TabClientConfig.IMAGE_Y_OFFSET.get();
        float alpha = (float) TabClientConfig.IMAGE_ALPHA.get().doubleValue();

        int x = screenWidth / 2 - dispW / 2 + xOff;
        int y = yOff;

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        // Register texture on the texture manager if not yet registered
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        tm.register(TEXTURE_LOC, texture);

        gui.blit(TEXTURE_LOC, x, y, 0, 0, dispW, dispH, texWidth, texHeight);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    // ------------------------------------------------------------------

    private static void loadTexture(String path) {
        releaseTexture();
        loadedPath = path;
        loadFailed = false;

        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(Minecraft.getInstance().gameDirectory, path);
        }

        if (!file.exists()) {
            CustomTabMod.LOGGER.warn("[ClientTab] Image not found: {}", file.getAbsolutePath());
            loadFailed = true;
            return;
        }

        BufferedImage img;
        try {
            img = ImageIO.read(file);
        } catch (IOException e) {
            CustomTabMod.LOGGER.error("[ClientTab] Failed to read image {}: {}", file.getAbsolutePath(), e.getMessage());
            loadFailed = true;
            return;
        }

        if (img == null) {
            CustomTabMod.LOGGER.error("[ClientTab] Unsupported image format: {}", file.getAbsolutePath());
            loadFailed = true;
            return;
        }

        texWidth  = img.getWidth();
        texHeight = img.getHeight();

        // NativeImage.setPixelRGBA expects ABGR (little-endian RGBA)
        NativeImage native_ = new NativeImage(texWidth, texHeight, false);

        for (int py = 0; py < texHeight; py++) {
            for (int px = 0; px < texWidth; px++) {
                int argb = img.getRGB(px, py);
                // Java ARGB → NativeImage ABGR
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >> 16)  & 0xFF;
                int g = (argb >> 8)   & 0xFF;
                int b = argb          & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                native_.setPixelRGBA(px, py, abgr);
            }
        }

        texture = new DynamicTexture(native_);
        CustomTabMod.LOGGER.info("[ClientTab] Loaded image {} ({}×{})", file.getName(), texWidth, texHeight);
    }

    private static void releaseTexture() {
        if (texture != null) {
            texture.close();
            texture = null;
        }
        texWidth  = 0;
        texHeight = 0;
    }
}
