package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The glued-trapdoor enforcement half: a trapdoor with metadata bit 8 set refuses to toggle, by hand
 * or by redstone. From MiscTweaks. The glue-APPLYING half - consuming the slimeball and setting the
 * bit - is in {@code ItemUseMixin}.
 *
 * <p>Separate from {@link TrapdoorBlockMixin} for the same reason {@code RotatedLogRenderMixin} is
 * separate from {@code BlockRenderManagerMixin}: that class carries the UniTweaks-paired
 * placement-without-support feature and is skipped wholesale when UniTweaks is installed - and the
 * enforcement used to be skipped with it while {@code ItemUseMixin} kept applying, so the player
 * paid the slimeball, heard the confirmation, and the trapdoor opened freely anyway. These two
 * plain {@code @Inject}s consume nothing, and UniTweaks 0.29.0's
 * {@code trapdoorplacement.TrapdoorBlockMixin} touches only the placement methods, so this class is
 * safe to keep applying and is deliberately NOT in {@code UNITWEAKS_SUPERSEDED_MIXINS}.
 */
@Mixin(TrapdoorBlock.class)
public abstract class GluedTrapdoorMixin extends Block {

	protected GluedTrapdoorMixin(int id, Material material) {
		super(id, material);
	}

	/** Metadata bit marking a trapdoor as glued shut. Vanilla uses bits 1-4 only. */
	@Unique
	private static final int RETROTWEAKS_GLUED_BIT = 0x8;

	@Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockMeta(IIII)V"), cancellable = true)
	private void retrotweaks$glueOrRefuse(World world, int x, int y, int z, PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
		if (!Config.BLOCKS.glueTrapdoorsWithSlimeballs) return;

		// Holding a slimeball means the player is glueing it, not toggling it. The slimeball is
		// consumed by ItemUseMixin, which is what actually sets the bit.
		ItemStack held = player == null || player.inventory == null ? null : player.inventory.getSelectedItem();
		if (held != null && held.itemId == Item.SLIMEBALL.id) {
			cir.setReturnValue(false);
			return;
		}
		if ((world.getBlockMeta(x, y, z) & RETROTWEAKS_GLUED_BIT) != 0) {
			retrotweaks$playRefusalSound(world, player, x, y, z);
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "setOpen", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockMeta(IIII)V"), cancellable = true)
	private void retrotweaks$redstoneCannotMoveGlued(World world, int x, int y, int z, boolean open, CallbackInfo ci) {
		if (!Config.BLOCKS.glueTrapdoorsWithSlimeballs) return;
		if ((world.getBlockMeta(x, y, z) & RETROTWEAKS_GLUED_BIT) == 0) return;
		retrotweaks$playRefusalSound(world, null, x, y, z);
		ci.cancel();
	}

	@Unique
	private void retrotweaks$playRefusalSound(World world, PlayerEntity player, int x, int y, int z) {
		if (!Config.SOUNDS.trapdoorRefusalSound) return;
		float pitch = ((world.random.nextFloat() - world.random.nextFloat()) * 0.2F + 1.0F) * 0.8F;
		if (player != null) {
			world.playSound(player, this.soundGroup.getBreakSound(), 1.0F, pitch);
		} else {
			world.playSound(x + 0.5F, y + 0.5F, z + 0.5F, this.soundGroup.getBreakSound(), 1.0F, pitch);
		}
	}
}
