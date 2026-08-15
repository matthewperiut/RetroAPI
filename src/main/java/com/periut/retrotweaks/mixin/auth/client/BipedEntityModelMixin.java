package com.periut.retrotweaks.mixin.auth.client;

import com.periut.retrotweaks.feature.auth.skin.ModernPlayerEntityModel;
import com.periut.retrotweaks.feature.auth.skin.data.ModelPartData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(BipedEntityModel.class)
public class BipedEntityModelMixin {
    @Redirect(
            method = "<init>(FF)V",
            at = @At(value = "NEW", target = "(II)Lnet/minecraft/client/model/ModelPart;"),
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;ears:Lnet/minecraft/client/model/ModelPart;", shift = Shift.AFTER))
    )
    private ModelPart onTexturedQuad(int u, int v) {
        ModelPart modelPart = new ModelPart(u, v);

        BipedEntityModel self = (BipedEntityModel) (Object) this;
        if (self instanceof ModernPlayerEntityModel) {
            ((ModelPartData) modelPart).setTextureHeight(64);
        }

        return modelPart;
    }
}
