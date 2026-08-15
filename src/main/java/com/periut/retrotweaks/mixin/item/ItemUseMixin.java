package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Item interactions that vanilla routes through the generic {@code Item} class: defusing TNT with
 * shears, glueing a trapdoor with a slimeball, and colouring a sign with dye. From MiscTweaks.
 *
 * <p>They share a mixin because they share a hook, not because they are related - splitting them
 * would mean three mixins on the same two methods of the same class.
 */
@Mixin(Item.class)
public class ItemUseMixin {

	/** Metadata bit marking a trapdoor as glued shut; see {@code TrapdoorBlockMixin}. */
	@Unique
	private static final int RETROTWEAKS_GLUED_BIT = 0x8;

	/**
	 * Colour codes indexed by dye metadata, so a dye's own colour is what lands on the sign.
	 * Dye metadata runs black-first while the codes do not, hence the explicit table.
	 */
	@Unique
	private static final char[] RETROTWEAKS_SIGN_COLOURS =
		{'0', '4', '2', '9', '1', '5', '3', '7', '8', 'c', 'a', 'e', 'b', 'd', '6', 'f'};

	@Inject(method = "getAttackDamage", at = @At("HEAD"))
	private void retrotweaks$defuseTntWithShears(Entity target, CallbackInfoReturnable<Integer> cir) {
		if (!Config.WORLD.explosions.defuseTntWithShears) return;
		if (((Item) (Object) this).id != Item.SHEARS.id) return;
		if (!(target instanceof TntEntity tnt)) return;

		// Server side only: the client half would spawn a ghost item that vanishes next tick.
		if (!tnt.world.isRemote) {
			ItemEntity dropped = new ItemEntity(tnt.world, tnt.x, tnt.y, tnt.z, new ItemStack(Block.TNT));
			dropped.velocityY = 0.2;
			tnt.world.spawnEntity(dropped);
		}
		tnt.markDead();
	}

	@Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$slimeballAndDye(ItemStack stack, PlayerEntity user, World world,
			int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {
		if (stack == null) return;
		int itemId = ((Item) (Object) this).id;

		if (itemId == Item.SLIMEBALL.id && Config.BLOCKS.glueTrapdoorsWithSlimeballs) {
			if (world.getBlockId(x, y, z) == Block.TRAPDOOR.id) {
				int meta = world.getBlockMeta(x, y, z);
				boolean wasGlued = (meta & RETROTWEAKS_GLUED_BIT) != 0;
				// Toggling means one slimeball glues it and a second click ungluess it.
				world.setBlockMeta(x, y, z, meta ^ RETROTWEAKS_GLUED_BIT);
				if (!wasGlued) {
					stack.count--;
					// From MiscTweaks: a confirmation cue for the glue taking, distinct from the
					// break-sound refusal TrapdoorBlockMixin plays when trying to open a glued one.
					if (Config.SOUNDS.trapdoorGlueSound) world.playSound(user, "mob.slime", 1.0F, 0.9F);
				}
				cir.setReturnValue(true);
				return;
			}
		}

		if (itemId == Item.DYE.id && Config.BLOCKS.colorSignsWithDye) {
			retrotweaks$dyeSign(stack, world, x, y, z, cir);
		}
	}

	@Unique
	private void retrotweaks$dyeSign(ItemStack stack, World world, int x, int y, int z,
			CallbackInfoReturnable<Boolean> cir) {
		int blockId = world.getBlockId(x, y, z);
		if (blockId != Block.SIGN.id && blockId != Block.WALL_SIGN.id) return;
		if (!(world.getBlockEntity(x, y, z) instanceof SignBlockEntity sign)) return;

		char colour = RETROTWEAKS_SIGN_COLOURS[stack.getDamage() & 0xF];
		boolean changed = false;
		for (int line = 0; line < sign.texts.length; line++) {
			String text = sign.texts[line];
			if (!text.contains("§")) {
				sign.texts[line] = "§" + colour + text;
				changed = true;
			} else if (!text.startsWith("§" + colour)) {
				sign.texts[line] = "§" + colour + text.substring(2);
				changed = true;
			}
		}
		if (!changed) {
			// Already this colour, so nothing was used and nothing should be consumed.
			cir.setReturnValue(false);
			return;
		}
		stack.count--;
		cir.setReturnValue(true);
	}
}
