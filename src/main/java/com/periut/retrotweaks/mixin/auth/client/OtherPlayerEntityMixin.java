package com.periut.retrotweaks.mixin.auth.client;

import com.periut.retrotweaks.feature.auth.skin.SkinService;
import net.minecraft.client.network.OtherPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OtherPlayerEntity.class)
public abstract class OtherPlayerEntityMixin extends PlayerEntity {
    public OtherPlayerEntityMixin(World arg) {
        super(arg);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        SkinService.getInstance().init(this);
    }
}
