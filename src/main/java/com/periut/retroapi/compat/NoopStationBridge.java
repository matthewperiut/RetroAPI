package com.periut.retroapi.compat;

import com.periut.retroapi.dimension.TeleportationManager;
import net.minecraft.achievement.Achievement;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Default {@link StationBridge} used when the {@code retroapi-stationapi} mod is absent. Every method is a
 * no-op; in practice none are ever called, because core gates every StationAPI delegation on
 * {@code FabricLoader.isModLoaded("stationapi")} and the bridge resolves to the real implementation whenever
 * that gate passes. This exists only so {@link StationBridges#get()} never returns {@code null}.
 */
final class NoopStationBridge implements StationBridge {
	@Override public void registerBlock(String namespace, String path, Block block) {}
	@Override public void registerItem(String namespace, String path, Item item) {}
	@Override public void registerLangPath() {}
	@Override public void bindAchievement(Achievement achievement, String namespace, String path) {}
	@Override public int addTerrainTexture(String namespace, String path) { return -1; }
	@Override public int addItemTexture(String namespace, String path) { return -1; }
	@Override public void setItemTexture(Item item, String namespace, String path) {}
	@Override public void addShapedRecipe(ItemStack output, Object... recipe) {}
	@Override public void addShapelessRecipe(ItemStack output, Object... ingredients) {}
	@Override public void addSmeltingRecipe(int inputId, ItemStack output) {}
	@Override public void attachPortal(PlayerEntity player, TeleportationManager retroManager) {}
}
