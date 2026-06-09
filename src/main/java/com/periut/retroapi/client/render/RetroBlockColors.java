package com.periut.retroapi.client.render;

import com.periut.retroapi.state.RetroBlockState;
import net.minecraft.block.Block;
import net.minecraft.world.BlockView;

import java.util.HashMap;
import java.util.Map;

/**
 * Tint providers for model faces with {@code tintindex >= 0}: the model renderer
 * multiplies the face color by the provider's 0xRRGGBB result.
 *
 * <pre>
 * RetroBlockColors.register(block, (state, world, x, y, z, tintIndex) -> 0x91BD59);
 * RetroBlockColors.register(block, RetroBlockColors.GRASS);   // biome grass color
 * </pre>
 */
public final class RetroBlockColors {

	@FunctionalInterface
	public interface Provider {
		/** world may be null (item form); return 0xRRGGBB. */
		int getColor(RetroBlockState state, BlockView world, int x, int y, int z, int tintIndex);
	}

	/** Biome grass colormap sampling, the way GrassBlock does it. */
	public static final Provider GRASS = (state, world, x, y, z, tintIndex) ->
		sampleColormap(world, x, z, true);

	/** Biome foliage colormap sampling, the way leaves do it. */
	public static final Provider FOLIAGE = (state, world, x, y, z, tintIndex) ->
		sampleColormap(world, x, z, false);

	private static final Map<Block, Provider> PROVIDERS = new HashMap<>();

	private RetroBlockColors() {}

	public static void register(Block block, Provider provider) {
		PROVIDERS.put(block, provider);
	}

	public static Provider get(Block block) {
		return PROVIDERS.get(block);
	}

	private static int sampleColormap(BlockView world, int x, int z, boolean grass) {
		if (world == null) {
			return grass ? 0x48B518 : 0x48B518;
		}
		net.minecraft.world.biome.source.BiomeSource source = world.getBiomeSource();
		source.getBiomesInArea(x, z, 1, 1);
		double temperature = source.temperatureMap[0];
		double downfall = source.downfallMap[0];
		return grass
			? net.minecraft.client.color.world.GrassColors.getColor(temperature, downfall)
			: net.minecraft.client.color.world.FoliageColors.getColor(temperature, downfall);
	}
}
