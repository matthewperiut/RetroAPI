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
	@Override public void registerBlock(String namespace, String path, Block block,
		java.util.function.IntFunction<net.minecraft.item.BlockItem> itemFactory) {}
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
	@Override public java.util.List<String> dimensionIds() { return java.util.List.of(); }
	@Override public boolean switchDimension(PlayerEntity player, String identifier) { return false; }
	@Override public String dimensionIdentifier(int serialId) { return null; }
	@Override public java.util.List<String> itemIdentifiers() { return java.util.List.of(); }
	@Override public int itemId(String identifier) { return -1; }
	@Override public String itemIdentifier(Item item) { return null; }
	@Override public int sprintKeyCode() { return -1; }
}
