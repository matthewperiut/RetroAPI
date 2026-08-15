package com.periut.retrotweaks.mixin.auth.client;

import com.periut.retrotweaks.feature.auth.skin.data.ModelPartData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.Quad;
import net.minecraft.client.model.Vertex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin implements ModelPartData {
    @Unique
    private int textureWidth = 64;

    @Unique
    private int textureHeight = 32;

    @Redirect(method = "addCuboid(FFFIIIF)V", at = @At(value = "NEW", target = "([Lnet/minecraft/client/model/Vertex;IIII)Lnet/minecraft/client/model/Quad;"))
    private Quad redirectQuad(Vertex[] vertices, int u1, int v1, int u2, int v2) {
        Quad quad = new Quad(vertices);

        vertices[0] = vertices[0].remap((float) u2 / textureWidth, (float) v1 / textureHeight);
        vertices[1] = vertices[1].remap((float) u1 / textureWidth, (float) v1 / textureHeight);
        vertices[2] = vertices[2].remap((float) u1 / textureWidth, (float) v2 / textureHeight);
        vertices[3] = vertices[3].remap((float) u2 / textureWidth, (float) v2 / textureHeight);

        return quad;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public void setTextureWidth(int textureWidth) {
        this.textureWidth = textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public void setTextureHeight(int textureHeight) {
        this.textureHeight = textureHeight;
    }
}
