package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.material.Material;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Gives the grass block item its proper faces. From UniTweaks.
 *
 * <p>Vanilla's {@code getTexture(side, meta)} - the one used when a block is drawn as an item -
 * returns the side texture for every face, so a grass block in the inventory has dirt on top.
 * {@code GrassBlock} only overrides the in-world variant, so this is an override rather than an
 * inject: there is no {@code getTexture(II)} on the class to inject into.
 */
@Mixin(GrassBlock.class)
public abstract class GrassBlockMixin extends Block {

	protected GrassBlockMixin(int id, Material material) {
		super(id, material);
	}

	@Override
	public int getTexture(int side, int meta) {
		if (!Config.BUGFIXES.grassBlockItemFix) return super.getTexture(side, meta);
		return switch (side) {
			case 0 -> 2;  // bottom: dirt
			case 1 -> 0;  // top: grass
			default -> 3; // sides: the grass-edged dirt texture
		};
	}
}
