package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.DeadBushBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Random;

/** Dead bushes: shears collect them, they can drop sticks, and they can be hidden. From MiscTweaks and BetaTweaks. */
@Mixin(DeadBushBlock.class)
public abstract class DeadBushBlockMixin extends PlantBlock {

	protected DeadBushBlockMixin(int id, int textureId) {
		super(id, textureId);
	}

	@Unique
	private boolean retrotweaks$brokenByShears;

	@Override
	public void afterBreak(World world, PlayerEntity player, int x, int y, int z, int meta) {
		retrotweaks$brokenByShears = Config.WORLD.flora.shearsCollectDeadBush && retrotweaks$usedShears(player);
		if (retrotweaks$brokenByShears) player.inventory.getSelectedItem().damage(1, player);
		super.afterBreak(world, player, x, y, z, meta);
	}

	@Inject(method = "getDroppedItemId", at = @At("RETURN"), cancellable = true)
	private void retrotweaks$deadBushDrops(int blockMeta, Random random, CallbackInfoReturnable<Integer> cir) {
		if (retrotweaks$brokenByShears) {
			retrotweaks$brokenByShears = false;
			cir.setReturnValue(this.id);
			return;
		}
		if (Config.WORLD.flora.hideDeadShrubs) {
			cir.setReturnValue(-1);
			return;
		}
		if (Config.WORLD.flora.deadBushesDropSticks) {
			// Same one-in-eight chance as seeds from tall grass.
			cir.setReturnValue(random.nextInt(8) == 0 ? Item.STICK.id : -1);
		}
	}

	@Environment(EnvType.CLIENT)
	@Override
	public int getRenderType() {
		return Config.WORLD.flora.hideDeadShrubs ? 0 : super.getRenderType();
	}

	@Override
	public void updateBoundingBox(BlockView blockView, int x, int y, int z) {
		if (!Config.WORLD.flora.hideDeadShrubs) {
			super.updateBoundingBox(blockView, x, y, z);
			return;
		}
		this.setBoundingBox(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
	}

	@Unique
	private static boolean retrotweaks$usedShears(PlayerEntity player) {
		if (player == null || player.inventory == null) return false;
		ItemStack held = player.inventory.getSelectedItem();
		return held != null && held.itemId == Item.SHEARS.id;
	}
}
