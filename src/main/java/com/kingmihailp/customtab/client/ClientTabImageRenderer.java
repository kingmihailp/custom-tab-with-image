package com.kingmihailp.customtab.client;

import com.kingmihailp.customtab.CustomTabMod;
import com.kingmihailp.customtab.config.TabClientConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class ClientTabImageRenderer {

    private static final ResourceLocation TEXTURE_LOC =
            ResourceLocation.fromNamespaceAndPath(CustomTabMod.MODID, "tab_header_image");

    private static String         loadedPath = null;
    private static boolean        loadFailed = false;
    private static int            texWidth   = 0;
    private static int            texHeight  = 0;
    private static boolean        registered = false;
    private static DynamicTexture texture    = null;

    private ClientTabImageRenderer() {}

    public static void invalidate() {
        releaseTexture();
        loadedPath = null;
        loadFailed = false;
        registered = false;
    }

    public static void render(GuiGraphics gui, int screenWidth) {
        if (!TabClientConfig.IMAGE_ENABLED.get()) return;

        String path = TabClientConfig.IMAGE_PATH.get();

        if (!path.equals(loadedPath) || (texture == null && !loadFailed)) {
            loadTexture(path);
        }

        if (texture == null || loadFailed) return;

        // Register once with the texture manager (must be on render thread — we are)
        if (!registered) {
            Minecraft.getInstance().getTextureManager().register(TEXTURE_LOC, texture);
            registered = true;
        }

        int   dispW  = TabClientConfig.IMAGE_DISPLAY_WIDTH.get();
        int   dispH  = TabClientConfig.IMAGE_DISPLAY_HEIGHT.get();
        int   xOff   = TabClientConfig.IMAGE_X_OFFSET.get();
        int   yOff   = TabClientConfig.IMAGE_Y_OFFSET.get();
        float alpha  = TabClientConfig.IMAGE_ALPHA.get().floatValue();

        int x = screenWidth / 2 - dispW / 2 + xOff;
        int y = yOff;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        // blit(atlas, x, y, screenW, screenH, u, v, texRegionW, texRegionH, texTotalW, texTotalH)
        // This samples the full texture (0,0 → texWidth×texHeight) scaled to dispW×dispH on screen.
        gui.blit(TEXTURE_LOC, x, y, dispW, dispH,
                0.0f, 0.0f, texWidth, texHeight, texWidth, texHeight);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    // ------------------------------------------------------------------

    private static void loadTexture(String path) {
        releaseTexture();
        loadedPath = path;
        loadFailed = false;
        registered = false;

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

        // NativeImage.setPixelRGBA expects ABGR (0xAABBGGRR)
        // BufferedImage.getRGB returns ARGB (0xAARRGGBB) — swap R and B
        NativeImage native_ = new NativeImage(texWidth, texHeight, false);
        for (int py = 0; py < texHeight; py++) {
            for (int px = 0; px < texWidth; px++) {
                int argb = img.getRGB(px, py);
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >> 16)  & 0xFF;
                int g = (argb >> 8)   & 0xFF;
                int b =  argb         & 0xFF;
                native_.setPixelRGBA(px, py, (a << 24) | (b << 16) | (g << 8) | r);
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
