package com.periut.retroapi.mixin.world;

import com.periut.retroapi.world.height.RetroExtendedSections;
import com.periut.retroapi.world.height.RetroWorldHeight;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes an extended dimension's extra Y range visible to <em>the game</em> rather than only to
 * {@link RetroWorldHeight}'s own accessors.
 *
 * <h2>Why this is the piece everything else waits on</h2>
 * {@link RetroWorldHeight} has always been able to store, save and reload a block below y0 - but nothing else
 * in beta could see one. {@code World.getBlockId} answers {@code 0} for any {@code y < 0} before it ever
 * reaches a chunk, and every consumer in the game funnels through it: the renderer, the light engine,
 * collision, block updates, mob spawning, {@code isAir}, {@code getMaterial}. So a mod could put a block at
 * y-1 and then find that no part of the game agreed it was there. Routing these five methods is what turns
 * the extension from a private data layer into world space; the renderer and the light engine are the next
 * two steps, and neither is useful before this one.
 *
 * <h2>Cost</h2>
 * {@code getBlockId} is one of the hottest methods in the game, so the guard is ordered to leave as early as
 * possible: a Y inside vanilla's 0-127 returns after one range test, and a Y outside it returns after one
 * more static boolean read unless a dimension has actually been extended. Only then does anything look at a
 * map. A game with no extended dimension pays two comparisons per block read and nothing else.
 *
 * <h2>What is deliberately not here</h2>
 * Lighting. An extension block is currently unlit as far as beta's light arrays are concerned, because those
 * are sized {@code 16*128*16} and have nowhere to put it - see the class doc on {@link RetroWorldHeight}.
 * Making blocks <em>visible</em> and making them <em>lit</em> are separate pieces of work and this is the
 * first one.
 */
@Mixin(World.class)
public abstract class WorldHeightMixin {

	@Shadow public abstract Chunk getChunk(int chunkX, int chunkZ);

	// hasChunk is private in World, so it takes the shadow-with-a-body form rather than an abstract one.
	@Shadow
	private boolean hasChunk(int chunkX, int chunkZ) {
		throw new AssertionError("mixin shadow");
	}

	/**
	 * True when this Y belongs to this world's extension - the one branch that sends a read or a write to
	 * {@link RetroExtendedSections} instead of to the chunk's byte array.
	 *
	 * <p>The order of the three tests is the whole performance story; see the class doc.
	 */
	@Unique
	private boolean retroapi$isExtension(int y) {
		if (y >= RetroWorldHeight.VANILLA_BOTTOM_Y && y <= RetroWorldHeight.VANILLA_TOP_Y) {
			return false;
		}
		if (!RetroWorldHeight.anyExtended()) {
			return false;
		}
		return RetroWorldHeight.isInWorld(((World) (Object) this).dimension.id, y);
	}

	/** The extension storage of the chunk at a position, or null when there is no chunk to ask. */
	@Unique
	private RetroExtendedSections retroapi$sections(int x, int z) {
		Chunk chunk = this.getChunk(x >> 4, z >> 4);
		return chunk == null ? null : RetroExtendedSections.of(chunk);
	}

	@Inject(method = "getBlockId(III)I", at = @At("HEAD"), cancellable = true)
	private void retroapi$getBlockId(int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
		if (!retroapi$isExtension(y)) {
			return;
		}
		RetroExtendedSections sections = retroapi$sections(x, z);
		cir.setReturnValue(sections == null ? 0 : sections.getBlock(x & 15, y, z & 15));
	}

	@Inject(method = "getBlockMeta(III)I", at = @At("HEAD"), cancellable = true)
	private void retroapi$getBlockMeta(int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
		if (!retroapi$isExtension(y)) {
			return;
		}
		RetroExtendedSections sections = retroapi$sections(x, z);
		cir.setReturnValue(sections == null ? 0 : sections.getMeta(x & 15, y, z & 15));
	}

	@Inject(method = "setBlockWithoutNotifyingNeighbors(IIII)Z", at = @At("HEAD"), cancellable = true)
	private void retroapi$setBlock(int x, int y, int z, int blockId, CallbackInfoReturnable<Boolean> cir) {
		if (!retroapi$isExtension(y)) {
			return;
		}
		cir.setReturnValue(retroapi$write(x, y, z, blockId, 0));
	}

	@Inject(method = "setBlockWithoutNotifyingNeighbors(IIIII)Z", at = @At("HEAD"), cancellable = true)
	private void retroapi$setBlockMeta(int x, int y, int z, int blockId, int meta,
			CallbackInfoReturnable<Boolean> cir) {
		if (!retroapi$isExtension(y)) {
			return;
		}
		cir.setReturnValue(retroapi$write(x, y, z, blockId, meta));
	}

	@Unique
	private boolean retroapi$write(int x, int y, int z, int blockId, int meta) {
		Chunk chunk = this.getChunk(x >> 4, z >> 4);
		if (chunk == null) {
			return false;
		}
		RetroExtendedSections.of(chunk).setBlock(x & 15, y, z & 15, blockId, meta);
		// The sidecar rides the chunk's own save, so the chunk has to know it changed.
		chunk.dirty = true;
		return true;
	}

	/**
	 * Beta answers "is this position loaded" with {@code y >= 0 && y < 128 && hasChunk(...)}, and the Y half
	 * of that is not really a question about loading - a beta chunk is a whole column, always. Inside the
	 * extension the answer is therefore whatever the chunk test says.
	 */
	@Inject(method = "isPosLoaded(III)Z", at = @At("HEAD"), cancellable = true)
	private void retroapi$isPosLoaded(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (!retroapi$isExtension(y)) {
			return;
		}
		cir.setReturnValue(this.hasChunk(x >> 4, z >> 4));
	}
}
