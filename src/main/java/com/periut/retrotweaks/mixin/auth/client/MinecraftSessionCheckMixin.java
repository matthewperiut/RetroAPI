package com.periut.retrotweaks.mixin.auth.client;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables {@code Minecraft.startSessionCheck()}. From MojangFix.
 *
 * <p>Five minutes into a session, vanilla b1.7.3 spins up a thread to ping the long-dead
 * login.minecraft.net/session endpoint. Part of the same "Modern Session Authentication" feature
 * group as {@link ClientNetworkHandlerMixin} and {@link SessionMixin}.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftSessionCheckMixin {

    @Inject(method = "startSessionCheck", at = @At("HEAD"), cancellable = true)
    private void retrotweaks$disableSessionCheck(CallbackInfo ci) {
        if (com.periut.retrotweaks.config.Config.MULTIPLAYER.modernAuthentication) {
            ci.cancel();
        }
    }
}
