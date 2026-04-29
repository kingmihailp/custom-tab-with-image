package com.kingmihailp.customtab.mixin;

import com.kingmihailp.customtab.client.ClientTabImageRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Injects into the tab overlay render method to draw the client-side image
 * on top of the tab header, after all vanilla rendering is done.
 */
@Mixin(PlayerTabOverlay.class)
public abstract class MixinPlayerTabOverlay {

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
        ClientTabImageRenderer.render(guiGraphics, screenWidth);
    }
}
