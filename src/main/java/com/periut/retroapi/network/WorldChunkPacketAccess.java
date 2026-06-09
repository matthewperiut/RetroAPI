package com.periut.retroapi.network;

import com.periut.retroapi.storage.ChunkExtendedBlocks;

public interface WorldChunkPacketAccess {
	int retroapi$getExtCount();
	int[] retroapi$getExtIndices();
	int[] retroapi$getExtBlockIds();
	int[] retroapi$getExtMeta();
	/** Secondary-meta entries (positions and bits 4-11 values), parallel arrays; may be null. */
	int[] retroapi$getXmetaPositions();
	int[] retroapi$getXmetaValues();
	void retroapi$populateExtended(ChunkExtendedBlocks extended);
}
