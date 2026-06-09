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

	@Unique private int retroapi$renderType = -1;
	@Unique private boolean retroapi$solidRenderSet = false;
	@Unique private boolean retroapi$solidRender = true;
	@Unique private float[] retroapi$customBounds = null;
	@Unique private boolean retroapi$alwaysDrops = false;
	@Unique private boolean retroapi$alwaysEffectiveTool = false;
	@Unique private Class<?> retroapi$effectiveTool = null;
	@Unique private java.util.List<com.periut.retroapi.state.RetroProperty<?>> retroapi$pendingStates = null;
	@Unique private java.util.function.UnaryOperator<com.periut.retroapi.state.RetroBlockState> retroapi$pendingDefault = null;

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
		return this;
	}

	@Override
	public RetroBlockAccess defaultState(java.util.function.UnaryOperator<com.periut.retroapi.state.RetroBlockState> transformer) {
		this.retroapi$pendingDefault = transformer;
		return this;
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

	@Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$getRenderLayer(CallbackInfoReturnable<Integer> cir) {
		com.periut.retroapi.client.render.RetroRenderLayer layer =
			com.periut.retroapi.client.render.RetroRenderLayers.get((Block) (Object) this);
		if (layer != null) {
			cir.setReturnValue(layer.getPass());
		}
	}

}

