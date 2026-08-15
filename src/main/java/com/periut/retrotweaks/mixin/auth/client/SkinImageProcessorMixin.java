package com.periut.retrotweaks.mixin.auth.client;

import com.periut.retrotweaks.feature.auth.skin.data.SkinImageProcessorData;
import net.minecraft.client.texture.SkinImageProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.Objects;

@Mixin(SkinImageProcessor.class)
public class SkinImageProcessorMixin implements SkinImageProcessorData {
    @Unique
    private String textureModel;

    @ModifyConstant(method = "process", constant = @Constant(intValue = 32, ordinal = 0), require = 0)
    private int getImageHeight(int def) {
        return 64;
    }

    @Inject(method = "process", at = @At(value = "INVOKE", target = "Ljava/awt/Graphics;dispose()V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onSkinGraphics(BufferedImage bufferedImage, CallbackInfoReturnable<BufferedImage> cir, BufferedImage parsedImage, Graphics g) {
        if (g instanceof Graphics2D graphics) {
            if (bufferedImage.getHeight() == 32) {
                graphics.drawImage(bufferedImage.getSubimage(0, 16, 16, 16), 16, 48, 16, 16, null);
                graphics.drawImage(bufferedImage.getSubimage(40, 16, 16, 16), 32, 48, 16, 16, null);
            }

            for (int i = 0; i < 2; ++i) {
                this.flipArea(graphics, parsedImage, 16 + i * 32, 0, 8, 8);
            }

            boolean isSlim = Objects.equals(textureModel, "slim");

            for (int i = 1; i <= 2; ++i) {
                this.flipArea(graphics, parsedImage, 8, i * 16, 4, 4);
                this.flipArea(graphics, parsedImage, 28, i * 16, 8, 4);
                this.flipArea(graphics, parsedImage, isSlim ? 47 : 48, i * 16, isSlim ? 3 : 4, 4);
            }

            for (int i = 0; i < 4; ++i) {
                boolean isSlimArm = isSlim && i >= 2;
                this.flipArea(graphics, parsedImage, (isSlimArm ? 7 : 8) + i * 16, 48, isSlimArm ? 3 : 4, 4);
            }
        }

    }

    public BufferedImage deepCopy(BufferedImage image) {
        ColorModel colorModel = image.getColorModel();
        boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();
        WritableRaster raster = image.copyData(image.getRaster().createCompatibleWritableRaster());
        return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
    }

    private void flipArea(Graphics2D graphics, BufferedImage bufferedImage, int x, int y, int width, int height) {
        BufferedImage image = this.deepCopy(bufferedImage.getSubimage(x, y, width, height));
        graphics.setBackground(new Color(0, true));
        graphics.clearRect(x, y, width, height);
        graphics.drawImage(image, x, y + height, width, -height, null);
    }

    @Override
    public void setTextureModel(String textureModel) {
        this.textureModel = textureModel;
    }
}
