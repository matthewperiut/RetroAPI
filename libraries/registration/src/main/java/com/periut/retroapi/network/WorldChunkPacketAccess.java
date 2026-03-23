package com.periut.retroapi.network;

import com.periut.retroapi.storage.ChunkExtendedBlocks;

public interface WorldChunkPacketAccess {
	int retroapi$getExtCount();
	int[] retroapi$getExtIndices();
	int[] retroapi$getExtBlockIds();
	int[] retroapi$getExtMeta();
	void retroapi$populateExtended(ChunkExtendedBlocks extended);
}
