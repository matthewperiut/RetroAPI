package com.periut.retroapi.mixin.gamerule;

import com.periut.retroapi.gamerule.RetroGameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;

/**
 * {@code randomTickSpeed}: how eagerly crops grow, grass spreads and leaves decay.
 *
 * <p>Beta random-ticks 80 blocks per chunk per tick; modern ticks 3 per section and calls that 3.
 * The rule therefore means the same thing in both games - "3 is normal, 6 is twice as fast, 0 is
 * off" - and is scaled onto beta's own number rather than replacing it, so a world that never
 * touches the rule behaves exactly as beta always did.
 */
@Mixin(World.class)
public class RandomTickSpeedMixin {

	/** Beta's own count, which is what a {@code randomTickSpeed} of 3 has to keep meaning. */
	private static final int VANILLA_TICKS_PER_CHUNK = 80;
	private static final int VANILLA_SPEED = 3;

	@ModifyConstant(method = "manageChunkUpdatesAndEvents", constant = @Constant(intValue = VANILLA_TICKS_PER_CHUNK))
	private int retroapi$randomTickSpeed(int original) {
		int speed = RetroGameRules.getInt(RetroGameRules.RANDOM_TICK_SPEED);
		if (speed == VANILLA_SPEED) {
			return original;
		}
		return Math.max(0, original * speed / VANILLA_SPEED);
	}
}
