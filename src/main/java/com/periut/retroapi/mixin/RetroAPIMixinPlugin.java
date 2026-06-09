package com.periut.retroapi.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class RetroAPIMixinPlugin implements IMixinConfigPlugin {
	// PURE atlas-coordinate math: these only rewrite vanilla's 256/16 atlas constants to RetroAPI's
	// expanded-atlas size. Under StationAPI the atlas IS StationAPI's, and its own arsenic/items mixins
	// already handle modded sprite UVs on these same vanilla paths, so RetroAPI's versions must NOT run.
	// (Atlas-INDEPENDENT render logic - per-layer tint + the layered item draw loop - was split out to
	// mixin.client.render and is deliberately NOT here, so RetroAPI's custom item rendering stays live
	// under StationAPI; it only selects sprites/tints and delegates UV emission to the StationAPI-patched
	// draws. See mixin/client/render/.)
	private static final Set<String> ATLAS_MIXINS = Set.of(
		"com.periut.retroapi.mixin.client.atlas.AchievementsScreenMixin",
		"com.periut.retroapi.mixin.client.atlas.BlockRendererAtlasMixin",
		"com.periut.retroapi.mixin.client.atlas.BlockParticleMixin",
		"com.periut.retroapi.mixin.client.atlas.EntityRendererAtlasMixin",
		"com.periut.retroapi.mixin.client.atlas.ItemInHandRendererMixin",
		"com.periut.retroapi.mixin.client.atlas.ItemRendererMixin",
		"com.periut.retroapi.mixin.client.atlas.TextureManagerMixin"
		// NOTE: client.PlayerArmorTextureMixin is NOT here. It is a @WrapOperation that swaps the worn-armor
		// texture PATH (RetroArmorTexture.getArmorTexture) on PlayerEntityRenderer.bindTexture(String) - pure
		// path swap, no atlas/UV math - and composes with StationAPI's own station-armor-api texture wrap.
		// Disabling it under StationAPI made custom armor render as vanilla leather, so it must stay enabled.
	);

	private static final Set<String> STATIONAPI_DISABLED_MIXINS = Set.of(
		"com.periut.retroapi.mixin.dimension.DimensionMixin",
		"com.periut.retroapi.mixin.dimension.RegionWorldStorageMixin",
		// NOTE: dimension.PlayerEntityMixin is NOT disabled - it provides RetroAPI's HasTeleportationManager
		// duck interface a portal casts to, and bridges the attached manager onto StationAPI's teleport slot
		// (StationPortalBridge) so RetroAPI portals teleport through StationAPI's dimension system.
		"com.periut.retroapi.mixin.dimension.PlayerDimensionMixin",
		"com.periut.retroapi.mixin.dimension.EntityReadDimensionMixin",
		"com.periut.retroapi.mixin.dimension.server.MinecraftServerMixin",
		"com.periut.retroapi.mixin.dimension.server.PlayerManagerMixin",
		"com.periut.retroapi.mixin.dimension.server.ServerPlayerEntityMixin",
		"com.periut.retroapi.mixin.dimension.client.ClientPlayerEntityMixin",
		"com.periut.retroapi.mixin.client.achievement.AchievementsPageScreenMixin",
		// register.ItemStackMixin is ONLY the numeric-id flattening half (clashes with StationAPI's
		// stationapi:id). The data-component half lives in component.ItemStackComponentMixin, which is
		// deliberately NOT disabled here so components keep working at runtime under StationAPI.
		"com.periut.retroapi.mixin.register.ItemStackMixin",
		"com.periut.retroapi.mixin.register.PlayerInventorySidecarMixin",
		"com.periut.retroapi.mixin.network.BlockUpdatePacketMixin",
		"com.periut.retroapi.mixin.network.BlocksUpdatePacketMixin",
		"com.periut.retroapi.mixin.network.WorldChunkPacketMixin",
		"com.periut.retroapi.mixin.network.ChunkSendMixin",
		// Component network sync rides the slot/inventory packets; StationAPI owns item serialization
		// there (its item packets carry the full stack NBT, including retroapi:components, so component
		// data still syncs without these). The component STORAGE/holder (ItemStackComponentMixin) and
		// pickup carry (PlayerInventoryComponentMixin) stay active.
		"com.periut.retroapi.mixin.component.SlotUpdateComponentMixin",
		"com.periut.retroapi.mixin.component.InventoryComponentMixin",
		"com.periut.retroapi.mixin.component.ItemEntitySpawnComponentMixin",
		// Sound bridge stays RetroAPI-only when StationAPI is present (its packet layer owns sounds there).
		"com.periut.retroapi.mixin.network.ServerWorldSoundMixin",
		"com.periut.retroapi.mixin.client.ClientNetworkHandlerMixin",
		// Both interaction-manager mixins only widen the worldEvent(2001) break-packing constant
		// (256 -> 1<<28). StationAPI's own station-flattening InteractionManagerMixin /
		// ServerPlayerInteractionManagerMixin do the identical 256 -> 268435456 widening, so RetroAPI's
		// are redundant under StationAPI and must stay off to avoid double-@ModifyConstant on the same
		// constant. RetroAPI's WorldRendererMixin decode (left enabled) now uses the matching 28-bit
		// layout, so it agrees with StationAPI's encode. See RetroIds.
		"com.periut.retroapi.mixin.client.ClientPlayerInteractionManagerMixin",
		"com.periut.retroapi.mixin.network.ServerPlayerInteractionManagerMixin",
		// PlayerRendererMixin @ModifyConstant-rewrites the two `256` block-item guards in
		// PlayerEntityRenderer.renderMore (held + helmet) so RetroAPI blocks (id>=256) render as 3D
		// blocks in third person instead of flat sprites. StationAPI's station-flattening
		// PlayerEntityRendererMixin already does this for EVERY BlockItemForm (it @Redirects the same
		// itemId/Item.id reads to 255). With both live, RetroAPI lowers the compare threshold to
		// heldId+1 while flattening raises the value to 255, so 255 >= heldId+1 is true and EVERY held
		// block (vanilla included) wrongly takes the flat-item branch. Defer to flattening under StationAPI.
		"com.periut.retroapi.mixin.client.PlayerRendererMixin",
		"com.periut.retroapi.mixin.recipe.FurnaceBlockEntityMixin",
		"com.periut.retroapi.mixin.client.sound.SoundEngineMixin",
		"com.periut.retroapi.mixin.client.sound.SoundsMixin",
		// Renderer mixin is a @Redirect; to avoid two configs rewriting EntityRenderDispatcher.<init>,
		// it stays disabled under StationAPI and renderers are forwarded through StationAPI's
		// EntityRendererRegisterEvent (see compat/StationAPIRegistryForwarder).
		"com.periut.retroapi.mixin.entity.client.EntityRenderDispatcherMixin",
		// StationAPI's station-items-v0 ContainerScreenMixin cancels and re-draws the vanilla HandledScreen
		// tooltip (posting a cancelable TooltipRenderEvent that CustomTooltipRendererImpl handles), so
		// RetroAPI's over-the-vanilla-draw tooltip mixin never runs / collides. Disable it and feed our
		// extra lines through StationAPI's CustomTooltipProvider instead (see stationapi.ItemTooltipBridgeMixin).
		"com.periut.retroapi.mixin.client.HandledScreenTooltipMixin",
		// Layered/dynamic HELD ITEM render under StationAPI. arsenic @Overwrites the vanilla HeldItemRenderer
		// and routes FIRST-PERSON held items straight into ArsenicOverlayRenderer.renderItem3D (its
		// render(float) -> renderVanilla(f) calls renderItem3D directly), BYPASSING the vanilla
		// renderItem(LivingEntity, ItemStack) that LayeredHeldItemMixin wraps - so the native wrap only caught
		// THIRD-person (which arsenic routes through the vanilla method) and missed first-person. Disable the
		// native wrap here and let stationapi.ArsenicHeldItemLayerMixin wrap renderItem3D instead (the single
		// shared 3D draw both first- and third-person funnel into - one wrap covers both, no double-loop).
		// The GUI wrap + dropped-item wrap (LayeredItemRenderMixin) and per-layer tint (ItemColorMixin) stay
		// LIVE: arsenic does not cancel the vanilla GUI flat-sprite draw for RetroAPI's VanillaBakedModel items
		// and the dropped/ground draw still reaches ItemStack.getSprite, so those layer loops composite fine.
		"com.periut.retroapi.mixin.client.render.LayeredHeldItemMixin"
	);

	// The "apply ONLY when StationAPI is present" mixins (StationSoundBridge, CarverWorld, ItemTooltipBridge,
	// ArsenicHeldItemLayer) no longer live here - they moved to the separate retroapi-stationapi mod, whose
	// own mixin config is only ever loaded when StationAPI is present, so they need no runtime gate at all.

	// NOTE: entity.server.ServerEntityTrackerMixin and entity.server.TrackedEntityMixin are deliberately
	// NOT disabled under StationAPI. They are @Inject (composable) and guarded on isRetroEntity, while
	// StationAPI's equivalents are guarded on CustomSpawnDataProvider - disjoint entity sets, so RetroAPI's
	// OSL-based entity spawn path coexists orthogonally with StationAPI's MessagePacket path (no interference).

	private boolean stationAPIPresent;

	@Override
	public void onLoad(String mixinPackage) {
		stationAPIPresent = FabricLoader.getInstance().isModLoaded("stationapi");
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (ATLAS_MIXINS.contains(mixinClassName) || STATIONAPI_DISABLED_MIXINS.contains(mixinClassName)) {
			return !stationAPIPresent;
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
