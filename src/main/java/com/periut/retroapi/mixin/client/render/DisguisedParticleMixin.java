package com.periut.retroapi.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.periut.retroapi.register.block.RetroDisguises;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.particle.BlockParticle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes a disguised block break into the right coloured dust. See
 * {@link com.periut.retroapi.register.block.RetroBlockDisguise}.
 *
 * <p>{@code BlockParticle} takes a block and reads {@code getTexture(0, meta)} off it, which is the block's
 * static sprite and has no idea where the block was. Both particle calls do know the position, so the
 * block handed to the particle is swapped for the one that position presents as.
 *
 * <p>The two calls need different sources for it. While a block is being chipped away it is still there,
 * so the disguise can simply be read. The cloud thrown out when it finally breaks is produced by a world
 * event that arrives after the block is gone, so that one reads what {@code RetroDisguises} wrote down on
 * the way out.
 */
@Mixin(ParticleManager.class)
@Environment(EnvType.CLIENT)
public class DisguisedParticleMixin {

	@WrapOperation(method = "addBlockBreakingParticles",
		at = @At(value = "NEW", target = "(Lnet/minecraft/world/World;DDDDDDLnet/minecraft/block/Block;II)Lnet/minecraft/client/particle/BlockParticle;"),
		require = 1)
	private BlockParticle retroapi$disguiseBreakingParticle(World world, double px, double py, double pz,
			double vx, double vy, double vz, Block block, int side, int meta,
			Operation<BlockParticle> original, int x, int y, int z, int face) {
		Block worn = RetroDisguises.at(world, x, y, z);
		if (worn != null) {
			return original.call(world, px, py, pz, vx, vy, vz, worn, side,
				RetroDisguises.metaAt(world, x, y, z));
		}
		return original.call(world, px, py, pz, vx, vy, vz, block, side, meta);
	}

	@WrapOperation(method = "addBlockBreakParticles",
		at = @At(value = "NEW", target = "(Lnet/minecraft/world/World;DDDDDDLnet/minecraft/block/Block;II)Lnet/minecraft/client/particle/BlockParticle;"),
		require = 1)
	private BlockParticle retroapi$disguiseBreakParticle(World world, double px, double py, double pz,
			double vx, double vy, double vz, Block block, int side, int meta,
			Operation<BlockParticle> original, int x, int y, int z, int blockId, int blockMeta) {
		Block worn = RetroDisguises.remembered(x, y, z);
		if (worn != null) {
			return original.call(world, px, py, pz, vx, vy, vz, worn, side,
				RetroDisguises.rememberedMeta(x, y, z));
		}
		return original.call(world, px, py, pz, vx, vy, vz, block, side, meta);
	}
}
