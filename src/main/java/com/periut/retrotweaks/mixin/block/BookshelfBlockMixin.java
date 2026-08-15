package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.block.BookshelfBlock;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Bookshelves drop their three books instead of nothing. From AnnoyanceFix and UniTweaks.
 *
 * <p>Both the count and the item have to change: vanilla returns a drop count of zero, so changing
 * only the item id would still drop nothing. The id comes from an override rather than an inject
 * because {@code BookshelfBlock} does not declare that method at all - it inherits it.
 */
@Mixin(BookshelfBlock.class)
public abstract class BookshelfBlockMixin extends Block {

	protected BookshelfBlockMixin(int id, Material material) {
		super(id, material);
	}

	@Inject(method = "getDroppedItemCount", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$dropThreeBooks(Random random, CallbackInfoReturnable<Integer> cir) {
		if (Config.BLOCKS.bookshelvesDropBooks) cir.setReturnValue(3);
	}

	@Override
	public int getDroppedItemId(int blockMeta, Random random) {
		return Config.BLOCKS.bookshelvesDropBooks ? Item.BOOK.id : super.getDroppedItemId(blockMeta, random);
	}
}
