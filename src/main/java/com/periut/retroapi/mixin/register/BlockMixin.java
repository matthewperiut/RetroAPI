package com.periut.retroapi.mixin.register;

import net.minecraft.sound.BlockSoundGroup;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import com.periut.retroapi.register.block.RetroBlockAccess;
import com.periut.retroapi.register.block.RetroTexture;
import com.periut.retroapi.register.block.RetroTextures;
import com.periut.retroapi.register.rendertype.RenderType;
import com.periut.retroapi.compat.StationBridges;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin implements RetroBlockAccess {

	@Shadow public int id;
	@Shadow public int textureId;

	@Shadow protected abstract Block setSoundGroup(BlockSoundGroup sounds);
	@Shadow protected abstract Block setHardness(float strength);
	@Shadow protected abstract Block setResistance(float resistance);
	@Shadow protected abstract Block setLuminance(float light);
	@Shadow protected abstract Block setOpacity(int opacity);
	@Shadow protected abstract Block setUnbreakable();

	/**
	 * Resolve the {@link RetroBlockAccess#AUTO_ID} sentinel into a real, reserved placeholder slot
	 * before the constructor consumes it (the original body runs {@code Block.BLOCKS[id] = this} and
	 * fills the parallel {@code BLOCKS_*} arrays immediately after this point). Allocating here - rather
	 * than scanning in advance and passing the result in - makes the scan and the store a single atomic
	 * step, so no other block can claim the same slot in between. Any real id is left untouched.
	 *
	 * <p>Both constructors are covered; the two-arg form delegates to the three-arg one, and since only
	 * the sentinel is rewritten the second pass is a no-op, so an id is never allocated twice.
	 */
	@org.spongepowered.asm.mixin.injection.ModifyVariable(
		method = "<init>(ILnet/minecraft/block/material/Material;)V",
		at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private static int retroapi$resolveAutoId(int id) {
		return id == RetroBlockAccess.AUTO_ID ? com.periut.retroapi.register.block.RetroBlockIds.allocate() : id;
	}

	@org.spongepowered.asm.mixin.injection.ModifyVariable(
		method = "<init>(IILnet/minecraft/block/material/Material;)V",
		at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private static int retroapi$resolveAutoIdTextured(int id) {
		return id == RetroBlockAccess.AUTO_ID ? com.periut.retroapi.register.block.RetroBlockIds.allocate() : id;
	}

	@Unique private int retroapi$renderType = -1;
	@Unique private boolean retroapi$solidRenderSet = false;
	@Unique private boolean retroapi$solidRender = true;
	@Unique private float[] retroapi$customBounds = null;
	@Unique private boolean retroapi$alwaysDrops = false;
	@Unique private boolean retroapi$alwaysEffectiveTool = false;
	@Unique private Class<?> retroapi$effectiveTool = null;
	@Unique private java.util.List<com.periut.retroapi.state.RetroProperty<?>> retroapi$pendingStates = null;
	@Unique private java.util.function.UnaryOperator<com.periut.retroapi.state.RetroBlockState> retroapi$pendingDefault = null;
	@Unique private boolean retroapi$autoFacing = false;
	@Unique private boolean retroapi$autoFacingVertical = false;
	@Unique private com.periut.retroapi.register.block.RetroFaceTextures retroapi$faceTextures = null;
	@Unique private float retroapi$droppedItemScale = -1.0F;
	@Unique private com.periut.retroapi.client.render.RetroBlockColors.Provider retroapi$tint = null;
	@Unique private java.util.List<com.periut.retroapi.register.block.RetroBlockLayer.Provider> retroapi$layers = null;

	// --- Block property wrappers ---

	@Override
	public RetroBlockAccess sounds(BlockSoundGroup sounds) {
		this.setSoundGroup(sounds);
		return this;
	}

	@Override
	public RetroBlockAccess strength(float strength) {
		this.setHardness(strength);
		this.setResistance(strength);
		return this;
	}

	@Override
	public RetroBlockAccess strength(float strength, float resistance) {
		this.setHardness(strength);
		this.setResistance(resistance);
		return this;
	}

	@Override
	public RetroBlockAccess resistance(float resistance) {
		this.setResistance(resistance);
		return this;
	}

	@Override
	public RetroBlockAccess light(float light) {
		this.setLuminance(light);
		return this;
	}

	@Override
	public RetroBlockAccess opacity(int opacity) {
		this.setOpacity(opacity);
		return this;
	}

	// --- RetroAPI extensions ---

	@Override
	public RetroBlockAccess alwaysDrops() {
		this.retroapi$alwaysDrops = true;
		return this;
	}

	@Override
	public RetroBlockAccess alwaysEffectiveTool() {
		this.retroapi$alwaysEffectiveTool = true;
		return this;
	}

	@Override
	public RetroBlockAccess states(com.periut.retroapi.state.RetroProperty<?>... properties) {
		this.retroapi$pendingStates = new java.util.ArrayList<>(java.util.Arrays.asList(properties));
		// A facing declared by .facing()/.facingAll() survives a later .states(...) call: the two are
		// independent declarations, and losing the facing property here (silently, depending on the order
		// the two were chained) meant the block still oriented itself but had nowhere to store it.
		retroapi$addFacingProperty();
		return this;
	}

	/** Adds the facing property for the declared facing mode, if any, without duplicating it. */
	@Unique
	private void retroapi$addFacingProperty() {
		if (!this.retroapi$autoFacing) {
			return;
		}
		com.periut.retroapi.state.RetroProperty<?> property = this.retroapi$autoFacingVertical
			? com.periut.retroapi.util.RetroDirection.PROPERTY
			: com.periut.retroapi.state.RetroFacing.PROPERTY;
		if (this.retroapi$pendingStates == null) {
			this.retroapi$pendingStates = new java.util.ArrayList<>();
		}
		if (!this.retroapi$pendingStates.contains(property)) {
			this.retroapi$pendingStates.add(property);
		}
	}

	@Override
	public RetroBlockAccess defaultState(java.util.function.UnaryOperator<com.periut.retroapi.state.RetroBlockState> transformer) {
		this.retroapi$pendingDefault = transformer;
		return this;
	}

	@Override
	public RetroBlockAccess facing() {
		this.retroapi$autoFacing = true;
		this.retroapi$autoFacingVertical = false;
		retroapi$addFacingProperty();
		// Aim the inventory icon's default south (front on a visible iso face), unless the caller set one.
		if (this.retroapi$pendingDefault == null) {
			this.retroapi$pendingDefault = s -> s.with(
				com.periut.retroapi.state.RetroFacing.PROPERTY, com.periut.retroapi.state.RetroFacing.SOUTH);
		}
		return this;
	}

	@Override
	public RetroBlockAccess facingAll() {
		this.retroapi$autoFacing = true;
		this.retroapi$autoFacingVertical = true;
		retroapi$addFacingProperty();
		if (this.retroapi$pendingDefault == null) {
			this.retroapi$pendingDefault = s -> s.with(
				com.periut.retroapi.util.RetroDirection.PROPERTY, com.periut.retroapi.util.RetroDirection.SOUTH);
		}
		return this;
	}

	@Override
	public boolean isAutoFacing() {
		return this.retroapi$autoFacing;
	}

	@Override
	public RetroBlockAccess mineable(com.periut.retroapi.tag.RetroTool... tools) {
		for (com.periut.retroapi.tag.RetroTool tool : tools) {
			com.periut.retroapi.tag.RetroTags.addToTag(tool.mineableTag(), (Block) (Object) this);
		}
		return this;
	}

	@Override
	public boolean isAlwaysDrops() {
		return this.retroapi$alwaysDrops;
	}

	@Override
	public boolean isAlwaysEffectiveTool() {
		return this.retroapi$alwaysEffectiveTool;
	}

	@Override
	public RetroBlockAccess effectiveTool(Class<? extends net.minecraft.item.Item> toolClass) {
		this.retroapi$effectiveTool = toolClass;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends net.minecraft.item.Item> getEffectiveTool() {
		return (Class<? extends net.minecraft.item.Item>) this.retroapi$effectiveTool;
	}

	@Override
	public RetroBlockAccess nonOpaque() {
		this.retroapi$solidRenderSet = true;
		this.retroapi$solidRender = false;
		Block.BLOCKS_OPAQUE[this.id] = false;
		Block.BLOCKS_LIGHT_OPACITY[this.id] = 0;
		return this;
	}

	@Override
	public RetroBlockAccess bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		this.retroapi$customBounds = new float[]{minX, minY, minZ, maxX, maxY, maxZ};
		((Block) (Object) this).setBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
		return this;
	}

	@Override
	public RetroBlockAccess droppedItemScale(float scale) {
		this.retroapi$droppedItemScale = scale;
		return this;
	}

	@Override
	public float getDroppedItemScale() {
		return this.retroapi$droppedItemScale;
	}

	@Override
	public RetroBlockAccess renderType(NamespacedIdentifier renderTypeId) {
		this.retroapi$renderType = RenderType.resolve(renderTypeId);
		return this;
	}

	@Override
	public RetroBlockAccess sprite(int spriteId) {
		this.textureId = spriteId;
		return this;
	}

	@Override
	public RetroBlockAccess texture(NamespacedIdentifier textureId) {
		RetroTexture tex = RetroTextures.addBlockTexture(textureId);
		this.textureId = tex.id;
		RetroTextures.trackBlock((Block) (Object) this, tex);
		return this;
	}

	@Override
	public RetroBlockAccess sided(NamespacedIdentifier top, NamespacedIdentifier side, NamespacedIdentifier front) {
		return sided(top, side, front, null);
	}

	@Override
	public RetroBlockAccess sided(NamespacedIdentifier top, NamespacedIdentifier side, NamespacedIdentifier front,
			NamespacedIdentifier bottom) {
		this.retroapi$faceTextures = com.periut.retroapi.register.block.RetroFaceTextures.oriented(
			retroapi$face(top), retroapi$face(side), retroapi$face(front), retroapi$face(bottom));
		retroapi$applyPrimarySprite();
		return this;
	}

	@Override
	public RetroBlockAccess column(NamespacedIdentifier top, NamespacedIdentifier side) {
		return sided(top, side, side, top);
	}

	@Override
	public RetroBlockAccess textures(NamespacedIdentifier bottom, NamespacedIdentifier top,
			NamespacedIdentifier north, NamespacedIdentifier south,
			NamespacedIdentifier west, NamespacedIdentifier east) {
		this.retroapi$faceTextures = com.periut.retroapi.register.block.RetroFaceTextures.absolute(
			new RetroTexture[]{
				retroapi$face(bottom), retroapi$face(top), retroapi$face(north),
				retroapi$face(south), retroapi$face(west), retroapi$face(east)
			});
		retroapi$applyPrimarySprite();
		return this;
	}

	/** Registers one face texture (null-safe), reusing nothing: each identifier gets its own atlas slot. */
	@Unique
	private RetroTexture retroapi$face(NamespacedIdentifier id) {
		return id == null ? null : RetroTextures.addBlockTexture(id);
	}

	/**
	 * Points the block's own sprite (particles, and any render path that ignores per-face textures) at the
	 * face set's primary texture, unless {@code .texture(...)} already chose one.
	 */
	@Unique
	private void retroapi$applyPrimarySprite() {
		RetroTexture primary = this.retroapi$faceTextures.primary();
		if (primary != null) {
			this.textureId = primary.id;
			RetroTextures.trackBlock((Block) (Object) this, primary);
		}
	}

	@Override
	public RetroBlockAccess needsTool(com.periut.retroapi.tag.RetroToolTier tier) {
		com.periut.retroapi.tag.RetroTagKey tag = tier == null ? null : tier.needsTag();
		if (tag != null) {
			com.periut.retroapi.tag.RetroTags.addToTag(tag, (Block) (Object) this);
		}
		return this;
	}

	@Override
	public RetroBlockAccess tag(com.periut.retroapi.tag.RetroTagKey... tags) {
		for (com.periut.retroapi.tag.RetroTagKey tag : tags) {
			com.periut.retroapi.tag.RetroTags.addToTag(tag, (Block) (Object) this);
		}
		return this;
	}

	@Override
	public RetroBlockAccess unbreakable() {
		this.setUnbreakable();
		return this;
	}

	@Override
	public RetroBlockAccess tint(com.periut.retroapi.client.render.RetroBlockColors.Provider provider) {
		this.retroapi$tint = provider;
		com.periut.retroapi.client.render.RetroBlockColors.register((Block) (Object) this, provider);
		return this;
	}

	@Override
	public RetroBlockAccess overlay(NamespacedIdentifier textureId) {
		return overlay(textureId, com.periut.retroapi.register.block.RetroBlockLayer.NO_TINT);
	}

	@Override
	public RetroBlockAccess overlay(NamespacedIdentifier textureId, int tint) {
		com.periut.retroapi.register.block.RetroBlockLayer layer =
			com.periut.retroapi.register.block.RetroBlockLayer.of(RetroTextures.addBlockTexture(textureId), tint);
		return overlay((state, world, x, y, z) -> layer);
	}

	@Override
	public RetroBlockAccess overlay(com.periut.retroapi.register.block.RetroBlockLayer.Provider provider) {
		if (this.retroapi$layers == null) {
			this.retroapi$layers = new java.util.ArrayList<>(2);
		}
		this.retroapi$layers.add(provider);
		return this;
	}

	@Override
	public java.util.List<com.periut.retroapi.register.block.RetroBlockLayer> getOverlayLayers() {
		return getOverlayLayers(
			com.periut.retroapi.state.RetroStates.getDefault((Block) (Object) this), null, 0, 0, 0);
	}

	@Override
	public java.util.List<com.periut.retroapi.register.block.RetroBlockLayer> getOverlayLayers(
			com.periut.retroapi.state.RetroBlockState state, BlockView world, int x, int y, int z) {
		if (this.retroapi$layers == null) {
			return java.util.Collections.emptyList();
		}
		java.util.List<com.periut.retroapi.register.block.RetroBlockLayer> resolved =
			new java.util.ArrayList<>(this.retroapi$layers.size());
		for (com.periut.retroapi.register.block.RetroBlockLayer.Provider provider : this.retroapi$layers) {
			com.periut.retroapi.register.block.RetroBlockLayer layer = provider.layerFor(state, world, x, y, z);
			if (layer != null) {
				resolved.add(layer);
			}
		}
		return resolved;
	}

	@Override
	public boolean hasOverlayLayers() {
		return this.retroapi$layers != null && !this.retroapi$layers.isEmpty();
	}

	@Override
	public Block register(NamespacedIdentifier id) {
		return register(id, BlockItem::new);
	}

	@Override
	public Block register(NamespacedIdentifier id, java.util.function.IntFunction<BlockItem> itemFactory) {
		Block self = (Block) (Object) this;
		self.setTranslationKey(id.namespace() + "." + id.identifier());

		if (this.retroapi$pendingStates != null) {
			com.periut.retroapi.state.RetroStates.define(self, this.retroapi$pendingStates, this.retroapi$pendingDefault);
			this.retroapi$pendingStates = null;
			this.retroapi$pendingDefault = null;
		}

		// Blockstate JSON auto-wiring: data-declared properties, render layer, the model
		// table, the MODEL render type (unless one was set explicitly) and the particle
		// sprite from the model's particle texture.
		com.periut.retroapi.client.model.BlockstateLoader.BlockModelTable table =
			com.periut.retroapi.client.model.BlockstateLoader.tryLoad(self, id);
		if (table != null) {
			if (this.retroapi$renderType == -1) {
				this.retroapi$renderType = RenderType.resolve(com.periut.retroapi.register.rendertype.RenderTypes.MODEL);
			}
			com.periut.retroapi.client.model.RetroModel firstModel = table.firstModel();
			RetroTexture particle = firstModel != null ? firstModel.particle() : null;
			if (particle != null) {
				this.textureId = particle.id;
				RetroTextures.trackBlock(self, particle);
			}
		}

		boolean hasStationAPI = FabricLoader.getInstance().isModLoaded("stationapi");

		BlockItem blockItem = null;
		if (!hasStationAPI) {
			blockItem = itemFactory.apply(this.id - 256);
		}

		RetroRegistry.registerBlock(new BlockRegistration(id, self, blockItem));

		if (hasStationAPI) {
			StationBridges.get().registerBlock(id.namespace(), id.identifier(), self);
		}

		return self;
	}

	// --- Mixin injections ---

	@Inject(method = "isOpaque", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$isSolidRender(CallbackInfoReturnable<Boolean> cir) {
		if (this.retroapi$solidRenderSet) {
			cir.setReturnValue(this.retroapi$solidRender);
		}
	}

	@Inject(method = "isFullCube", at = @At("HEAD"), cancellable = true)
	private void retroapi$isCube(CallbackInfoReturnable<Boolean> cir) {
		if (this.retroapi$solidRenderSet) {
			cir.setReturnValue(this.retroapi$solidRender);
		}
	}

	@Inject(method = "updateBoundingBox", at = @At("HEAD"), cancellable = true)
	private void retroapi$updateShape(BlockView world, int x, int y, int z, CallbackInfo ci) {
		if (this.retroapi$customBounds != null) {
			((Block) (Object) this).setBoundingBox(
				retroapi$customBounds[0], retroapi$customBounds[1], retroapi$customBounds[2],
				retroapi$customBounds[3], retroapi$customBounds[4], retroapi$customBounds[5]
			);
			ci.cancel();
		}
	}

	@Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$getRenderType(CallbackInfoReturnable<Integer> cir) {
		if (this.retroapi$renderType != -1) {
			cir.setReturnValue(this.retroapi$renderType);
		}
	}

	/**
	 * Furnace-like facing for {@code .facing()} blocks: write the placer-facing direction into state on
	 * placement, so the block orients itself with no per-block {@code onPlaced}. Targets the same
	 * {@code onPlaced(World,x,y,z,LivingEntity)} overload the freezer overrides by hand.
	 */
	@Inject(method = "onPlaced(Lnet/minecraft/world/World;IIILnet/minecraft/entity/LivingEntity;)V",
		at = @At("TAIL"), require = 0)
	private void retroapi$autoFace(net.minecraft.world.World world, int x, int y, int z,
			net.minecraft.entity.LivingEntity placer, CallbackInfo ci) {
		if (!this.retroapi$autoFacing) {
			return;
		}
		Block self = (Block) (Object) this;
		// Preserve whatever else placement already wrote into state (a block entity's own onPlaced, a
		// subclass): read the current state and change only the facing.
		com.periut.retroapi.state.RetroBlockState current =
			com.periut.retroapi.state.RetroStates.get(world, x, y, z);
		if (current == null || current.getBlock() != self) {
			current = com.periut.retroapi.state.RetroStates.getDefault(self);
		}
		if (this.retroapi$autoFacingVertical) {
			com.periut.retroapi.state.RetroStates.set(world, x, y, z, current.with(
				com.periut.retroapi.util.RetroDirection.PROPERTY,
				com.periut.retroapi.util.RetroDirection.fromPlacer(placer)));
		} else {
			com.periut.retroapi.state.RetroStates.set(world, x, y, z, current.with(
				com.periut.retroapi.state.RetroFacing.PROPERTY,
				com.periut.retroapi.state.RetroFacing.fromYaw(placer.yaw)));
		}
	}

	/**
	 * Per-face textures in the world: reads the FULL flattened state (including the sidecar bits, which
	 * the metadata nibble alone would lose) so a {@code .sided(...)} block shows its front on the face it
	 * actually points at.
	 */
	@Inject(method = "getTextureId", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$facedTextureInWorld(BlockView world, int x, int y, int z, int side,
			CallbackInfoReturnable<Integer> cir) {
		if (this.retroapi$faceTextures == null) {
			return;
		}
		int sprite = this.retroapi$faceTextures.spriteFor(side,
			com.periut.retroapi.state.RetroStates.get(world, x, y, z));
		if (sprite >= 0) {
			cir.setReturnValue(sprite);
		}
	}

	/** Per-face textures for the inventory/hand form (and any caller that only has metadata). */
	@Inject(method = "getTexture(II)I", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$facedTexture(int side, int meta, CallbackInfoReturnable<Integer> cir) {
		if (this.retroapi$faceTextures == null) {
			return;
		}
		int sprite = this.retroapi$faceTextures.spriteFor(side,
			com.periut.retroapi.state.RetroStates.fromIndex((Block) (Object) this, meta));
		if (sprite >= 0) {
			cir.setReturnValue(sprite);
		}
	}

	/**
	 * Code-declared tinting for blocks with no model JSON: vanilla multiplies a standard block's vertex
	 * colors by this, which is exactly the hook {@code .tint(...)} needs. The inventory form goes through
	 * {@code getColor(meta)} below.
	 */
	@Inject(method = "getColorMultiplier", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$tintInWorld(BlockView world, int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
		// An overlay pass paints in ITS color, not the block's (that is what makes a tinted glyph over an
		// untinted backdrop - or vanilla's green grass edge over plain dirt - a single block).
		if (com.periut.retroapi.register.block.RetroBlockLayerDraw.hasForcedTint()) {
			cir.setReturnValue(com.periut.retroapi.register.block.RetroBlockLayerDraw.forcedTint);
			return;
		}
		if (this.retroapi$tint == null) {
			return;
		}
		cir.setReturnValue(this.retroapi$tint.getColor(
			com.periut.retroapi.state.RetroStates.get(world, x, y, z), world, x, y, z, 0));
	}

	@Inject(method = "getColor", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$tintInInventory(int meta, CallbackInfoReturnable<Integer> cir) {
		if (com.periut.retroapi.register.block.RetroBlockLayerDraw.hasForcedTint()) {
			cir.setReturnValue(com.periut.retroapi.register.block.RetroBlockLayerDraw.forcedTint);
			return;
		}
		if (this.retroapi$tint == null) {
			return;
		}
		cir.setReturnValue(this.retroapi$tint.getColor(
			com.periut.retroapi.state.RetroStates.fromIndex((Block) (Object) this, meta), null, 0, 0, 0, 0));
	}

	@Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$getRenderLayer(CallbackInfoReturnable<Integer> cir) {
		com.periut.retroapi.client.render.RetroRenderLayer layer =
			com.periut.retroapi.client.render.RetroRenderLayers.get((Block) (Object) this);
		if (layer != null) {
			cir.setReturnValue(layer.getPass());
		}
	}

}

