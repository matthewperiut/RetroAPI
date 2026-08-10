package com.periut.retroapi.mixin.network;

import com.periut.retroapi.register.block.RetroDisguises;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Dedicated-server half of the disguised breaking speed. See {@code DisguisedHardnessMpMixin}, which is
 * the same redirect on the client's interaction manager.
 *
 * <p>Without this the two sides disagree about how long a disguised block takes, and a dedicated server
 * is the only place that disagreement is visible: the client mines a stone-clad frame at stone speed and
 * a dirt-clad one at dirt speed, while the server times both against the frame's own hardness. Break a
 * frame clad in something soft and it finishes on the client and then sits there, unbroken, until the
 * server's own much longer timer runs out - the block is being mined twice, at two speeds, and the slower
 * one wins.
 *
 * <p>The server asks the same question in three places and all three need the same answer. Two are the
 * shortcuts that skip the timer entirely - the instant break on the first click, and the 0.7 threshold
 * that lets a fast break finish early - and the third is the timer itself, which is the one that decides
 * when the block actually goes. Redirecting only some of them would trade a delay for a block that
 * breaks at the wrong moment, so all three are here.
 *
 * <p>Only the position differs between them: the two click handlers are told where the player is digging,
 * while the per-tick update has to read the dig it already has in progress.
 */
@Mixin(ServerPlayerInteractionManager.class)
public class DisguisedHardnessServerMixin {

	@Shadow private ServerWorld world;
	@Shadow private int miningX;
	@Shadow private int miningY;
	@Shadow private int miningZ;

	/** First click: decides whether the block goes instantly. */
	@Redirect(method = "onBlockBreakingAction",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getHardness(Lnet/minecraft/entity/player/PlayerEntity;)F"))
	private float retroapi$startHardness(Block block, PlayerEntity player, int x, int y, int z, int side) {
		return retroapi$hardness(block, player, x, y, z);
	}

	/** Continued digging: decides whether the break finishes early. */
	@Redirect(method = "continueMining",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getHardness(Lnet/minecraft/entity/player/PlayerEntity;)F"))
	private float retroapi$continueHardness(Block block, PlayerEntity player, int x, int y, int z) {
		return retroapi$hardness(block, player, x, y, z);
	}

	/** The tick timer, which is what actually breaks the block. */
	@Redirect(method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getHardness(Lnet/minecraft/entity/player/PlayerEntity;)F"))
	private float retroapi$updateHardness(Block block, PlayerEntity player) {
		return retroapi$hardness(block, player, this.miningX, this.miningY, this.miningZ);
	}

	@Unique
	private float retroapi$hardness(Block block, PlayerEntity player, int x, int y, int z) {
		Block worn = this.world == null ? null : RetroDisguises.at(this.world, x, y, z);
		return worn == null ? block.getHardness(player) : worn.getHardness(player);
	}
}
