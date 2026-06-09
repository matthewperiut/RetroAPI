package com.periut.retroapi.network;

import net.minecraft.world.chunk.Chunk;

public interface BlocksUpdatePacketAccess {
	short[] retroapi$getFullBlockIds();
	void retroapi$populateFullIds(Chunk chunk);
}
