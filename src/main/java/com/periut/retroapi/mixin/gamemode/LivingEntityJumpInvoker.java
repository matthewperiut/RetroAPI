package com.periut.retroapi.mixin.gamemode;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Vanilla's own jump impulse, reachable from outside the class that declares it.
 *
 * <p>{@code jump()} is protected on {@code LivingEntity}, and a mixin can only {@code @Shadow} what its
 * target class <em>declares</em> - so a mixin on {@code ClientPlayerEntity} cannot shadow it, however
 * plainly the player inherits it. Same reason {@code EntityAccessor} exists for {@code fallDistance}.
 *
 * <p>Calling the real method rather than setting a velocity by hand keeps the sprint-jump boost and
 * anything else a mod has done to jumping, which is what modern's flight toggle gets from
 * {@code jumpFromGround()}.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityJumpInvoker {

	@Invoker("jump")
	void retroapi$jump();
}
