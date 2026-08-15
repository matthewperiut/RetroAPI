package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.block.LogBlock;
import net.minecraft.block.material.Material;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Logs remember which way they were placed, as they do from beta 1.8 onwards. From MiscTweaks.
 *
 * <p>Metadata bits 1-2 are the wood type (vanilla) and bits 4-8 are the axis (added here). The
 * drop metadata is masked back down to the type so a sideways log still stacks with an upright one.
 *
 * <p>Turning this off leaves already-placed sideways logs looking like oak until they are replaced,
 * which is why the option says restart and warns about it.
 */
@Mixin(LogBlock.class)
public abstract class LogBlockMixin extends Block {

	protected LogBlockMixin(int id, Material material) {
		super(id, material);
	}

	/** Vanilla's render type for logs is 31 (a normal cube); 18 is claimed here for rotated ones. */
	@Unique
	private static final int RETROTWEAKS_ROTATED_LOG_RENDER_TYPE = 18;

	@Unique private static final int RETROTWEAKS_TYPE_MASK = 0x3;
	@Unique private static final int RETROTWEAKS_AXIS_MASK = 0xC;
	@Unique private static final int RETROTWEAKS_AXIS_NORTH_SOUTH = 0x4;
	@Unique private static final int RETROTWEAKS_AXIS_EAST_WEST = 0x8;

	@Unique private static final int RETROTWEAKS_CUT_TEXTURE = 21;
	@Unique private static final int RETROTWEAKS_OAK_TEXTURE = 20;
	@Unique private static final int RETROTWEAKS_SPRUCE_TEXTURE = 116;
	@Unique private static final int RETROTWEAKS_BIRCH_TEXTURE = 117;

	@Environment(EnvType.CLIENT)
	@Override
	public int getRenderType() {
		// An override, not an inject: LogBlock inherits getRenderType rather than declaring it.
		return Config.BLOCKS.logRotation ? RETROTWEAKS_ROTATED_LOG_RENDER_TYPE : super.getRenderType();
	}

	@Inject(method = "getTexture(II)I", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$rotatedTexture(int side, int meta, CallbackInfoReturnable<Integer> cir) {
		if (!Config.BLOCKS.logRotation) return;

		int bark = switch (meta & RETROTWEAKS_TYPE_MASK) {
			case 1 -> RETROTWEAKS_SPRUCE_TEXTURE;
			case 2 -> RETROTWEAKS_BIRCH_TEXTURE;
			default -> RETROTWEAKS_OAK_TEXTURE;
		};

		// The cut face is whichever pair of sides the log's axis points through.
		boolean cutFace = switch (meta & RETROTWEAKS_AXIS_MASK) {
			case RETROTWEAKS_AXIS_NORTH_SOUTH -> side == 2 || side == 3;
			case RETROTWEAKS_AXIS_EAST_WEST -> side == 4 || side == 5;
			default -> side == 0 || side == 1;
		};
		cir.setReturnValue(cutFace ? RETROTWEAKS_CUT_TEXTURE : bark);
	}

	@Inject(method = "getDroppedItemMeta", at = @At("RETURN"), cancellable = true)
	private void retrotweaks$dropWithoutRotation(int blockMeta, CallbackInfoReturnable<Integer> cir) {
		if (Config.BLOCKS.logRotation) cir.setReturnValue(blockMeta & RETROTWEAKS_TYPE_MASK);
	}
}
