package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.util.PendingExplosionSource;

import net.minecraft.entity.TntEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks a TNT entity as the source of the explosion it is about to make: {@code explode()}
 * immediately calls {@code World.createExplosion(null, ...)}, so this is the only place that still
 * knows the blast was TNT. {@code ExplosionMixin}'s "Disable TNT Block Breaking" reads the marker;
 * see {@link PendingExplosionSource}.
 *
 * <p>Separate from {@link TntEntityMixin} for the same reason {@code RotatedLogRenderMixin} is
 * separate from {@code BlockRenderManagerMixin}: that class carries the UniTweaks-paired
 * punch-to-defuse feature and is skipped wholesale when UniTweaks is installed, and this marker
 * used to be skipped with it - which silently broke TNT protection while creeper and ghast
 * protection kept working. A plain {@code @Inject} at HEAD consumes nothing, and UniTweaks 0.29.0's
 * {@code punchtntdefuse.TntEntityMixin} touches entirely different methods, so this class is safe
 * to keep applying and is deliberately NOT in {@code UNITWEAKS_SUPERSEDED_MIXINS}.
 */
@Mixin(TntEntity.class)
public abstract class TntExplosionSourceMixin {

	@Inject(method = "explode", at = @At("HEAD"))
	private void retrotweaks$markExplosionSource(CallbackInfo ci) {
		PendingExplosionSource.set((TntEntity) (Object) this);
	}
}
