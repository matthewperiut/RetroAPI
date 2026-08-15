package com.periut.retrotweaks.mixin.auth.client;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.auth.skin.ModernPlayerEntityModel;
import com.periut.retrotweaks.feature.auth.skin.data.PlayerEntityRendererSkinData;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer implements PlayerEntityRendererSkinData {
    @Shadow
    private BipedEntityModel bipedModel;

    @Shadow
    private BipedEntityModel armor2;

    public PlayerEntityRendererMixin(EntityModel arg, float f) {
        super(arg, f);
    }

    public void setThinArms(boolean thinArms) {
        this.model = this.bipedModel = new ModernPlayerEntityModel(0.0F, thinArms);
    }

    @Inject(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(F)V"))
    private void fixFirstPerson$1(CallbackInfo ci) {
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Inject(method = "renderHand", at = @At("RETURN"))
    private void fixFirstPerson$2(CallbackInfo ci) {
        ((ModernPlayerEntityModel) bipedModel).rightSleeve.render(0.0625F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    @Inject(method = "render(Lnet/minecraft/entity/player/PlayerEntity;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;render(Lnet/minecraft/entity/LivingEntity;DDDFF)V"))
    private void fixOuterLayer$1(CallbackInfo ci) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Inject(method = "render(Lnet/minecraft/entity/player/PlayerEntity;DDDFF)V", at = @At("RETURN"))
    private void fixOuterLayer$2(CallbackInfo ci) {
        GL11.glDisable(GL11.GL_BLEND);
    }


    @Redirect(method = "renderMore(Lnet/minecraft/entity/player/PlayerEntity;F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;renderCape(F)V"
            )
    )
    protected void mojangFixStationApi_method_827(BipedEntityModel instance, float v) {
        instance.renderCape(v);
    }

    // UniTweaks bugfixes/leggingsridingfix/PlayerEntityRendererMixin.java: leg armor stays in the
    // walking pose while riding because armor2's riding flag is never synced with the base model's.
    @Inject(method = "render(Lnet/minecraft/entity/player/PlayerEntity;DDDFF)V", at = @At("HEAD"))
    private void fixLeggingsWhenRiding(PlayerEntity player, double e, double f, double g, float h, float par6, CallbackInfo ci) {
        if (Config.BUGFIXES.leggingsWhenRidingFix) {
            ItemStack stack = player.inventory.armor[1];
            if (stack != null) {
                if (stack.getItem() instanceof ArmorItem) {
                    this.armor2.riding = this.model.riding;
                }
            }
        }
    }
}
