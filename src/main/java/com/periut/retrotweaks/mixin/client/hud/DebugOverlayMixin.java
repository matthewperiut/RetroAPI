package com.periut.retrotweaks.mixin.client.hud;

import com.periut.retrotweaks.config.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Duration;

/**
 * Extra lines on the F3 debug overlay: play time, light level, biome, day count, slime chunk.
 * From UniTweaksTelsAddons.
 *
 * <p>Each line costs a little work, so the shared lookups (chunk, world properties, biome) are only
 * done when at least one of the options that needs them is on.
 */
@Mixin(InGameHud.class)
public class DebugOverlayMixin {

	@Shadow private Minecraft minecraft;

	/** Brightness thresholds for each light level, so a float can be shown as vanilla's 0-15. */
	@Unique
	private static final float[] RETROTWEAKS_LIGHT_STEPS = {
		0.06F, 0.07F, 0.09F, 0.125F, 0.14F, 0.16F, 0.2F, 0.25F,
		0.3F, 0.325F, 0.4F, 0.5F, 0.625F, 0.75F, 0.875F
	};

	@Inject(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/font/TextRenderer;drawWithShadow(Ljava/lang/String;III)V"))
	private void retrotweaks$extraDebugLines(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
		Config.DebugOverlay debug = Config.HUD.debug;
		TextRenderer text = this.minecraft.textRenderer;
		int y = debug.additionsYOffset;
		int colour = 14737632;

		if (debug.showHoursPlayed) {
			long hours = Duration.ofSeconds(this.minecraft.stats.get(Stats.PLAY_ONE_MINUTE) / 20).toHours();
			text.drawWithShadow("Play Time: " + hours + (hours == 1 ? " hour" : " hours"), 2, 96 + y, colour);
		}

		boolean needsWorld = debug.showLightLevel || debug.showBiome || debug.showDayCounter || debug.showSlimeChunk;
		if (!needsWorld) return;

		PlayerEntity player = this.minecraft.player;
		if (player == null || player.world == null) return;

		if (debug.showLightLevel) {
			text.drawWithShadow("Light Level: " + retrotweaks$lightLevel(player), 2, 112 + y, colour);
		}
		if (debug.showBiome) {
			text.drawWithShadow("Biome: " + retrotweaks$biomeName(player), 2, 120 + y, colour);
		}
		if (debug.showDayCounter && player.world.getProperties() != null) {
			long day = player.world.getProperties().getTime() / 24000L;
			text.drawWithShadow("Day: " + day, 2, 128 + y, colour);
		}
		if (debug.showSlimeChunk) {
			Chunk chunk = player.world.getChunkFromPos(MathHelper.floor(player.x), MathHelper.floor(player.z));
			// 987234911 is vanilla's slime-chunk salt; the same roll the spawner makes.
			boolean slime = chunk.getSlimeRandom(987234911L).nextInt(10) == 0;
			text.drawWithShadow("Slime Chunk: " + slime, 2, 136 + y, colour);
		}
	}

	@Unique
	private static int retrotweaks$lightLevel(PlayerEntity player) {
		float brightness = player.getBrightnessAtEyes(1.0F);
		for (int level = 0; level < RETROTWEAKS_LIGHT_STEPS.length; level++) {
			if (brightness < RETROTWEAKS_LIGHT_STEPS[level]) return level;
		}
		return 15;
	}

	@Unique
	private static String retrotweaks$biomeName(PlayerEntity player) {
		if (player.world.getBiomeSource() == null) return "Unknown";
		Biome biome = player.world.getBiomeSource().getBiome(MathHelper.floor(player.x), MathHelper.floor(player.z));
		return biome == null ? "Unknown" : biome.name;
	}
}
