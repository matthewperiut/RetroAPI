package com.periut.retrotweaks.mixin.item;

import net.minecraft.item.BlockItem;
import net.minecraft.item.LeavesBlockItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Marks leaves as player-placed by setting metadata bit 0x4 on placement. See
 * {@link com.periut.retrotweaks.mixin.block.LeavesBlockMixin}, which reads the bit back to
 * skip decay when {@code Config.BLOCKS.playerPlacedLeafPersistence} is enabled.
 *
 * <p>Vanilla's {@code getPlacementMetadata} already ORs in bit 0x8 here (a decay-scan trigger,
 * unrelated to persistence), so bit 0x4 is otherwise untouched and free to reuse.
 */
@Mixin(LeavesBlockItem.class)
public abstract class LeavesItemMixin extends BlockItem {

	protected LeavesItemMixin(int i) {
		super(i);
		this.setMaxDamage(0);
		this.setHasSubtypes(true);
	}

	@Inject(method = "getPlacementMetadata", at = @At("RETURN"), cancellable = true)
	private void retrotweaks$markPlayerPlaced(int meta, CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(cir.getReturnValueI() | 0x4);
	}
}
