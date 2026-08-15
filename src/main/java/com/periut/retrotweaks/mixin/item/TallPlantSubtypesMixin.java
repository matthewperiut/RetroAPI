package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the three variants of the tall-plant block behave as three real subtypes of one item, the way
 * wool, dye and saplings already do.
 *
 * <p>Three vanilla gaps, all of which only became visible once the sheared drop stopped flattening
 * every plant to meta 0:
 *
 * <ul>
 *   <li><b>{@code hasSubtypes} is false.</b> {@code ItemStack} consults it to decide whether damage is
 *       part of an item's identity, so with it false a fern and a short grass are the same stack: they
 *       merge on pickup and the survivor's meta decides the name, the sprite and the tint for all of
 *       them. That is the "they all join the same stack and gain or lose colour" behaviour - the drop
 *       was carrying the right meta, and merging threw it away.
 *   <li><b>{@code Item.getTextureId(int)} ignores its meta</b> and returns the item's one texture, and
 *       {@code BlockItem} never overrides it. So every variant drew with the same sprite whatever meta
 *       it held. The block's own {@code getTexture} has the correct per-meta mapping already, so this
 *       just asks it.
 *   <li><b>{@code Item.getPlacementMetadata(int)} returns 0 for everything.</b> It is what
 *       {@code BlockItem.useOnBlock} hands to {@code World.setBlock}, and only the handful of vanilla
 *       subclasses that need it override it - {@code BlockItem} itself does not. So placing ANY of the
 *       three variants built a dead shrub, meta 0 being the shrub, however carefully the item had kept
 *       its variant up to that point. Wool and saplings dodge this by being {@code SecondaryBlockItem},
 *       whose entire body is {@code return meta;} - which is what this returns too.
 * </ul>
 *
 * <p>Deliberately NOT new registered items. Three subtypes of one item is what vanilla itself does for
 * this exact problem; it needs no registry entry, no identifier, no data fixer and no model, so it
 * behaves identically on a bare install, under RetroAPI and under StationAPI - none of which can
 * disagree about core item semantics the way they disagree about renderers. The
 * {@code minecraft:short_grass}/{@code fern}/{@code dead_shrub} identifiers that DO get registered when
 * an API is present are aliases ONTO these subtypes rather than a second representation of them - see
 * {@link com.periut.retrotweaks.feature.flora.TallPlantVariants}.
 *
 * <p>Targets {@code Item} rather than {@code BlockItem} because all three methods are declared there,
 * and is gated on the block's own id so no other item is affected.
 */
@Mixin(Item.class)
public class TallPlantSubtypesMixin {

	@Unique
	private boolean retrotweaks$isTallPlantItem() {
		return Config.WORLD.flora.tallGrassItems && ((Item) (Object) this).id == Block.GRASS.id;
	}

	@Inject(method = "hasSubtypes", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$tallPlantHasSubtypes(CallbackInfoReturnable<Boolean> cir) {
		if (retrotweaks$isTallPlantItem()) cir.setReturnValue(true);
	}

	// Item.getTextureId is @Environment(CLIENT): the loader strips it from Item on a dedicated
	// server, and an injector left pointing at it is a hard crash during mixin apply, not a warning.
	// Marking the handler itself CLIENT makes the loader strip the injector alongside its target.
	@Environment(EnvType.CLIENT)
	@Inject(method = "getTextureId(I)I", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$tallPlantTexture(int meta, CallbackInfoReturnable<Integer> cir) {
		if (!retrotweaks$isTallPlantItem()) return;
		// The block already maps meta to the right sprite - grass, fern or dead bush. Side is ignored
		// by TallPlantBlock.getTexture, so 0 is as good as any.
		cir.setReturnValue(Block.GRASS.getTexture(0, meta));
	}

	@Inject(method = "getPlacementMetadata", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$tallPlantPlacementMeta(int meta, CallbackInfoReturnable<Integer> cir) {
		if (!retrotweaks$isTallPlantItem()) return;
		// Clamped rather than passed straight through: a hand-made stack (a /give with a silly meta, a
		// save from a mod that used the spare bits) must not write a state the block has no texture for.
		cir.setReturnValue(meta >= 0 && meta <= 2 ? meta : 0);
	}
}
