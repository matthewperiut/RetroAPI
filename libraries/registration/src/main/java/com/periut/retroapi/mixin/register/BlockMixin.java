package com.periut.retroapi.mixin.register;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import com.periut.retroapi.register.block.BlockActivatedHandler;
import com.periut.retroapi.register.block.RetroBlockAccess;
import com.periut.retroapi.register.block.RetroTexture;
import com.periut.retroapi.register.block.RetroTextures;
import com.periut.retroapi.register.blockentity.RetroBlockEntityType;
import com.periut.retroapi.register.rendertype.RenderType;
#if MC_B1_6_OR_LATER
import com.periut.retroapi.compat.StationAPICompat;
#endif
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
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
	@Shadow public int sprite;

	@Shadow protected abstract Block setSounds(Block.Sounds sounds);
	@Shadow protected abstract Block setStrength(float strength);
	@Shadow protected abstract Block setBlastResistance(float resistance);
	@Shadow protected abstract Block setLight(float light);
	@Shadow protected abstract Block setOpacity(int opacity);

	@Unique private int retroapi$renderType = -1;
	@Unique private boolean retroapi$solidRenderSet = false;
	@Unique private boolean retroapi$solidRender = true;
	@Unique private float[] retroapi$customBounds = null;
	@Unique private RetroBlockEntityType<?> retroapi$blockEntityType = null;
	@Unique private BlockActivatedHandler retroapi$activatedHandler = null;

	// --- Block property wrappers ---

	@Override
	public RetroBlockAccess sounds(Block.Sounds sounds) {
		this.setSounds(sounds);
		return this;
	}

	@Override
	public RetroBlockAccess strength(float strength) {
		this.setStrength(strength);
		this.setBlastResistance(strength);
		return this;
	}

	@Override
	public RetroBlockAccess strength(float strength, float resistance) {
		this.setStrength(strength);
		this.setBlastResistance(resistance);
		return this;
	}

	@Override
	public RetroBlockAccess resistance(float resistance) {
		this.setBlastResistance(resistance);
		return this;
	}

	@Override
	public RetroBlockAccess light(float light) {
		this.setLight(light);
		return this;
	}

	@Override
	public RetroBlockAccess opacity(int opacity) {
		this.setOpacity(opacity);
		return this;
	}

	// --- RetroAPI extensions ---

	@Override
	public RetroBlockAccess nonOpaque() {
		this.retroapi$solidRenderSet = true;
		this.retroapi$solidRender = false;
#if MC_B1_6_OR_LATER
		Block.IS_SOLID_RENDER[this.id] = false;
#else
		Block.IS_SOLID[this.id] = false;
#endif
		Block.OPACITIES[this.id] = 0;
		return this;
	}

	@Override
	public RetroBlockAccess bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		this.retroapi$customBounds = new float[]{minX, minY, minZ, maxX, maxY, maxZ};
		((Block) (Object) this).setShape(minX, minY, minZ, maxX, maxY, maxZ);
		return this;
	}

	@Override
	public RetroBlockAccess renderType(NamespacedIdentifier renderTypeId) {
		this.retroapi$renderType = RenderType.resolve(renderTypeId);
		return this;
	}

	@Override
	public RetroBlockAccess sprite(int spriteId) {
		this.sprite = spriteId;
		return this;
	}

	@Override
	public RetroBlockAccess texture(NamespacedIdentifier textureId) {
		RetroTexture tex = RetroTextures.addBlockTexture(textureId);
		this.sprite = tex.id;
		RetroTextures.trackBlock((Block) (Object) this, tex);
		return this;
	}

	@Override
	public RetroBlockAccess blockEntity(RetroBlockEntityType<?> type) {
		this.retroapi$blockEntityType = type;
		Block.HAS_BLOCK_ENTITY[this.id] = true;
		return this;
	}

	@Override
	public RetroBlockAccess activated(BlockActivatedHandler handler) {
		this.retroapi$activatedHandler = handler;
		return this;
	}

	@Override
	public Block register(NamespacedIdentifier id) {
		Block self = (Block) (Object) this;
		self.setKey(id.namespace() + "." + id.identifier());

#if MC_B1_6_OR_LATER
		boolean hasStationAPI = FabricLoader.getInstance().isModLoaded("stationapi");

		BlockItem blockItem = null;
		if (!hasStationAPI) {
			blockItem = new BlockItem(this.id - 256);
		}

		RetroRegistry.registerBlock(new BlockRegistration(id, self, blockItem));

		if (hasStationAPI) {
			StationAPICompat.registerBlock(id.namespace(), id.identifier(), self);
		}
#else
		BlockItem blockItem = new BlockItem(this.id - 256);
		RetroRegistry.registerBlock(new BlockRegistration(id, self, blockItem));
#endif

		return self;
	}

	// --- Mixin injections ---

#if MC_B1_6_OR_LATER
	@Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true, require = 0)
#else
	@Inject(method = "isSolid", at = @At("HEAD"), cancellable = true)
#endif
	private void retroapi$isSolidRender(CallbackInfoReturnable<Boolean> cir) {
		if (this.retroapi$solidRenderSet) {
			cir.setReturnValue(this.retroapi$solidRender);
		}
	}

	@Inject(method = "isCube", at = @At("HEAD"), cancellable = true)
	private void retroapi$isCube(CallbackInfoReturnable<Boolean> cir) {
		if (this.retroapi$solidRenderSet) {
			cir.setReturnValue(this.retroapi$solidRender);
		}
	}

	@Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
	private void retroapi$updateShape(WorldView world, int x, int y, int z, CallbackInfo ci) {
		if (this.retroapi$customBounds != null) {
			((Block) (Object) this).setShape(
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

	@Inject(method = "onAdded", at = @At("HEAD"))
	private void retroapi$onAdded(World world, int x, int y, int z, CallbackInfo ci) {
		if (this.retroapi$blockEntityType != null) {
			world.setBlockEntity(x, y, z, this.retroapi$blockEntityType.create());
		}
	}

	@Inject(method = "onRemoved", at = @At("HEAD"))
	private void retroapi$onRemoved(World world, int x, int y, int z, CallbackInfo ci) {
		if (this.retroapi$blockEntityType != null) {
			world.removeBlockEntity(x, y, z);
		}
	}

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void retroapi$use(World world, int x, int y, int z, PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
		if (this.retroapi$activatedHandler != null) {
			if (world.isMultiplayer) {
				cir.setReturnValue(true);
				return;
			}
			cir.setReturnValue(this.retroapi$activatedHandler.onActivated(world, x, y, z, player));
		}
	}
}
