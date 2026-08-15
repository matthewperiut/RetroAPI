package com.periut.retrotweaks.mixin.auth.client;

import com.periut.retrotweaks.feature.auth.skin.CapeImageProcessor;
import com.periut.retrotweaks.feature.auth.skin.data.PlayerEntitySkinData;
import com.periut.retrotweaks.feature.auth.skin.data.SkinImageProcessorData;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.ImageProcessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Unique
    private Entity currentEntity; // I hate this but there is no way to get it from @ModifyArg

    @Inject(method = "notifyEntityRemoved", at = @At("HEAD"), cancellable = true)
    private void dontUnloadLocalPlayerSkin(Entity entity, CallbackInfo ci) {
        if (entity instanceof ClientPlayerEntity) {
            ci.cancel();
        }
    }

    @Inject(method = "notifyEntityAdded", at = @At("HEAD"))
    private void getEnttity(Entity entity, CallbackInfo ci) {
        currentEntity = entity;
    }

    @ModifyArg(
            method = "notifyEntityAdded", index = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureManager;downloadImage(Ljava/lang/String;Lnet/minecraft/client/texture/ImageProcessor;)Lnet/minecraft/client/texture/ImageDownload;", ordinal = 0)
    )
    private ImageProcessor redirectSkinProcessor(ImageProcessor def) {
        if (currentEntity instanceof PlayerEntity) {
            PlayerEntitySkinData playerSkinData = (PlayerEntitySkinData) currentEntity;
            SkinImageProcessorData skinImageProcessorData = (SkinImageProcessorData) def;
            skinImageProcessorData.setTextureModel(playerSkinData.getTextureModel());
        }
        return def;
    }

    @ModifyArg(
            method = "notifyEntityAdded", index = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureManager;downloadImage(Ljava/lang/String;Lnet/minecraft/client/texture/ImageProcessor;)Lnet/minecraft/client/texture/ImageDownload;", ordinal = 1)
    )
    private ImageProcessor redirectCapeProcessor(ImageProcessor def) {
        return new CapeImageProcessor();
    }
}
