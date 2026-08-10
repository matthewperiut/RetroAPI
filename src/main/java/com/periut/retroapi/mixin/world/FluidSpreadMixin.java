package com.periut.retroapi.mixin.world;

import com.periut.retroapi.register.block.RetroBlockAccess;
import net.minecraft.block.Block;
import net.minecraft.block.FlowingLiquidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes {@link RetroBlockAccess#waterTolerant()} mean something: flowing water treats those blocks as wall
 * rather than as something to dissolve.
 *
 * <p>{@code isLiquidBreaking} is the one question the whole spreading algorithm asks about a neighbour, and
 * its name reads backwards: it returns <b>true</b> when the position <i>stops</i> the liquid. Vanilla answers
 * it from the material - {@code material.blocksMovement()}, plus a short hardcoded list of doors, signs,
 * ladders and sugar cane that water is allowed to wash away. Three call sites consume it:
 *
 * <ul>
 *   <li>{@code canSpreadTo}, which decides whether water may occupy a position at all - and {@code spreadTo}
 *       drops the occupant's items and overwrites it when it may;</li>
 *   <li>{@code getSpread}, which picks which of the four horizontal directions water prefers;</li>
 *   <li>{@code getDistanceToGap}, which looks ahead for a hole to fall into.</li>
 * </ul>
 *
 * <p>Answering "yes, this stops the liquid" therefore covers all three at once, which is the point of
 * injecting here rather than into {@code canSpreadTo}: water neither enters a tolerant block, nor flows
 * towards one preferentially, nor mistakes one for a hole in the floor.
 *
 * <p>Guarded on the liquid being water. Lava reaching a plant still destroys it.
 */
@Mixin(FlowingLiquidBlock.class)
public abstract class FluidSpreadMixin {

	@Inject(method = "isLiquidBreaking", at = @At("HEAD"), cancellable = true)
	private void retroapi$waterTolerantStopsWater(World world, int x, int y, int z,
			CallbackInfoReturnable<Boolean> cir) {
		if (((Block) (Object) this).material != Material.WATER) {
			return;
		}
		int id = world.getBlockId(x, y, z);
		if (id <= 0) {
			return;
		}
		Block block = Block.BLOCKS[id];
		if (block != null && ((RetroBlockAccess) block).isWaterTolerant()) {
			cir.setReturnValue(true);
		}
	}
}
