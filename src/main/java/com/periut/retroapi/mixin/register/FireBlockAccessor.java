package com.periut.retroapi.mixin.register;

import net.minecraft.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FireBlock.class)
public interface FireBlockAccessor {
	@Invoker("registerFlammableBlock")
	void retroapi$registerFlammable(int blockId, int burnChance, int spreadChance);

	@Accessor("burnChances")
	int[] retroapi$getBurnChances();

	@Accessor("spreadChances")
	int[] retroapi$getSpreadChances();
}
