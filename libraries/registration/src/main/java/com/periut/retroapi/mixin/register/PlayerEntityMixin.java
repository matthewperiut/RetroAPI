package com.periut.retroapi.mixin.register;

import com.periut.retroapi.register.block.RetroBlockAccess;
import net.minecraft.block.Block;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.mob.player.PlayerInventory;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

	@Shadow public PlayerInventory inventory;

	@Inject(method = "canMineBlock", at = @At("HEAD"), cancellable = true)
	private void retroapi$alwaysDrops(Block block, CallbackInfoReturnable<Boolean> cir) {
		if (((RetroBlockAccess) block).isAlwaysDrops()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getMiningSpeed", at = @At("RETURN"), cancellable = true)
	private void retroapi$effectiveTool(Block block, CallbackInfoReturnable<Float> cir) {
		RetroBlockAccess access = (RetroBlockAccess) block;
		if (cir.getReturnValue() > 1.0f) return;

		ItemStack held = this.inventory.getSelectedItem();
		if (held == null) return;
		Item item = held.getItem();

		if (access.isAlwaysEffectiveTool()) {
			Float speed = retroapi$getToolSpeed(item);
			if (speed != null) cir.setReturnValue(speed);
		} else {
			Class<? extends Item> effectiveTool = access.getEffectiveTool();
			if (effectiveTool != null && effectiveTool.isInstance(item)) {
				Float speed = retroapi$getToolSpeed(item);
				if (speed != null) cir.setReturnValue(speed);
			}
		}
	}

	@Unique
	private Float retroapi$getToolSpeed(Item item) {
		if (item instanceof PickaxeItem) return this.inventory.getMiningSpeed(Block.STONE);
		if (item instanceof AxeItem) return this.inventory.getMiningSpeed(Block.PLANKS);
		if (item instanceof ShovelItem) return this.inventory.getMiningSpeed(Block.DIRT);
		if (item instanceof SwordItem) return 1.5f;
		return null;
	}
}
