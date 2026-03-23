#if MC_VER >= 160
package com.periut.retroapi.mixin.client.atlas;

import com.periut.retroapi.client.texture.AtlasExpander;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.AchievementsScreen;
import net.minecraft.client.render.vertex.Tesselator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AchievementsScreen.class)
@Environment(EnvType.CLIENT)
public abstract class AchievementsScreenMixin extends Screen {

	@Redirect(
		method = "renderIcons",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/menu/AchievementsScreen;drawTexture(IIIIII)V", ordinal = 0)
	)
	private void retroapi$fixTerrainDrawTexture(AchievementsScreen self, int x, int y, int u, int v, int width, int height) {
		float scale = 1.0F / AtlasExpander.terrainAtlasSize;
		Tesselator tesselator = Tesselator.INSTANCE;
		tesselator.begin();
		tesselator.vertex(x, y + height, this.drawOffset, u * scale, (v + height) * scale);
		tesselator.vertex(x + width, y + height, this.drawOffset, (u + width) * scale, (v + height) * scale);
		tesselator.vertex(x + width, y, this.drawOffset, (u + width) * scale, v * scale);
		tesselator.vertex(x, y, this.drawOffset, u * scale, v * scale);
		tesselator.end();
	}
}
#endif
