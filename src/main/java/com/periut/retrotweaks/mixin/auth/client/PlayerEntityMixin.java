package com.periut.retrotweaks.mixin.auth.client;

import com.periut.retrotweaks.feature.auth.skin.SkinService;
import com.periut.retrotweaks.feature.auth.skin.data.PlayerEntitySkinData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerEntitySkinData {
    @Unique
    private String textureModel;

    public PlayerEntityMixin(World world) {
        super(world);
    }

    @Inject(method = "updateCapeUrl", at = @At("HEAD"), cancellable = true)
    private void cancelUpdateCapeUrl(CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;texture:Ljava/lang/String;"))
    private void redirectTexture(PlayerEntity instance, String value) {
        this.setTextureModel("default");
    }

    @Override
    public String getTextureModel() {
        return textureModel;
    }

    @Unique
    public void setTextureModel(String textureModel) {
        this.textureModel = textureModel;
        this.texture = Objects.equals(textureModel, "default") ? SkinService.STEVE_TEXTURE : SkinService.ALEX_TEXTURE;
    }
}
