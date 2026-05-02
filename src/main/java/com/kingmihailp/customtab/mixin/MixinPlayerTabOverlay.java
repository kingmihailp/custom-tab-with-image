package com.kingmihailp.customtab.mixin;

import com.kingmihailp.customtab.client.ClientTabImageRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Injects into the tab overlay render method to shift the tab down and draw
 * the client-side image above it.
 */
@Mixin(PlayerTabOverlay.class)
public abstract class MixinPlayerTabOverlay {

    @Unique
    private boolean customtab$didShift = false;

    @Inject(
            method = "render",
            at = @At("HEAD")
    )
    private void customtab$shiftTabDown(
            GuiGraphics guiGraphics,
            int screenWidth,
            Scoreboard scoreboard,
            @Nullable Objective objective,
            CallbackInfo ci
    ) {
        customtab$didShift = ClientTabImageRenderer.isImageReady();
        if (customtab$didShift) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, ClientTabImageRenderer.getTabYShift(), 0);
        }
    }

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void customtab$renderHeaderImage(
            GuiGraphics guiGraphics,
            int screenWidth,
            Scoreboard scoreboard,
            @Nullable Objective objective,
            CallbackInfo ci
    ) {
        if (customtab$didShift) {
            guiGraphics.pose().popPose();
            customtab$didShift = false;
        }
        ClientTabImageRenderer.render(guiGraphics, screenWidth);
    }
}
