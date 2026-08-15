package com.periut.retrotweaks.mixin.auth.client;

import com.periut.retrotweaks.feature.auth.skin.data.PlayerEntityRendererSkinData;
import com.periut.retrotweaks.feature.auth.skin.data.PlayerEntitySkinData;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Unique
    private final Map<String, PlayerEntityRenderer> playerRenderers = new HashMap<>();
    @Shadow
    private Map<Class<? extends Entity>, EntityRenderer> renderers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        for (String model : Arrays.asList("default", "slim")) {
            PlayerEntityRenderer renderer = new PlayerEntityRenderer();
            ((PlayerEntityRendererSkinData) renderer).setThinArms(model == "slim");
            renderer.setDispatcher((EntityRenderDispatcher) (Object) this);
            playerRenderers.put(model, renderer);
        }

        this.renderers.put(PlayerEntity.class, playerRenderers.get("default"));
    }

    @Inject(method = "get(Lnet/minecraft/entity/Entity;)Lnet/minecraft/client/render/entity/EntityRenderer;", at = @At("HEAD"), cancellable = true)
    private void onGet(Entity entity, CallbackInfoReturnable<EntityRenderer> cir) {
        if (entity instanceof PlayerEntity player) {
            String textureModel = ((PlayerEntitySkinData) player).getTextureModel();
            cir.setReturnValue(playerRenderers.get(textureModel));
        }
    }
}
