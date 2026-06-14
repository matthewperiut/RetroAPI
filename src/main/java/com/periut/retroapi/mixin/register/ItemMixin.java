package com.periut.retroapi.mixin.register;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import com.periut.retroapi.register.block.RetroTexture;
import com.periut.retroapi.register.block.RetroTextures;
import com.periut.retroapi.register.item.RetroItemAccess;
import com.periut.retroapi.compat.StationBridges;
import com.periut.retroapi.registry.ItemRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.fabricmc.loader.api.FabricLoader;
import com.periut.retroapi.register.item.RetroFood;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.periut.retroapi.register.item.RetroItemIds;

@Mixin(Item.class)
public abstract class ItemMixin implements RetroItemAccess {

	@Shadow public int maxCount;
	@Shadow protected boolean handheld;
	@Shadow public abstract Item setTextureId(int sprite);

	/**
	 * Resolve the {@link RetroItemAccess#AUTO_ID} sentinel into a real, reserved placeholder slot
	 * before the constructor consumes it (the original body runs {@code this.id = 256 + id} and
	 * {@code Item.ITEMS[256 + id] = this} immediately after this point). Allocating here - rather than
	 * scanning in advance and passing the result in - makes the scan and the store a single atomic
	 * step, so no other item can ever claim the same slot in between. Any non-sentinel id (including
	 * the negative ids vanilla {@code BlockItem}s pass) is left untouched.
	 */
	@ModifyVariable(method = "<init>(I)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private static int retroapi$resolveAutoId(int id) {
		return id == RetroItemAccess.AUTO_ID ? RetroItemIds.allocate() : id;
	}

	@org.spongepowered.asm.mixin.Unique
	private com.periut.retroapi.tag.RetroTool retroapi$toolKind = null;

	@Override
	public RetroItemAccess maxStackSize(int size) {
		this.maxCount = size;
		return this;
	}

	@Override
	public RetroItemAccess tool(com.periut.retroapi.tag.RetroTool tool) {
		this.retroapi$toolKind = tool;
		return this;
	}

	@Override
	public com.periut.retroapi.tag.RetroTool getToolKind() {
		return this.retroapi$toolKind;
	}

	@org.spongepowered.asm.mixin.Unique
	private com.periut.retroapi.tag.RetroToolTier retroapi$toolTier = null;

	@Override
	public RetroItemAccess tier(com.periut.retroapi.tag.RetroToolTier tier) {
		this.retroapi$toolTier = tier;
		return this;
	}

	@Override
	public com.periut.retroapi.tag.RetroToolTier getToolTier() {
		return this.retroapi$toolTier;
	}

	@Override
	public RetroItemAccess handheld() {
		this.handheld = true;
		return this;
	}

	@Override
	public RetroItemAccess food(int health, boolean meat, RetroFood.OnEaten onEaten) {
		RetroFood.register((Item) (Object) this, health, meat, onEaten);
		return this;
	}

	/** Right-clicking a food item eats it (heal + the on-eat effect), the same as vanilla FoodItem. */
	@Inject(
		method = "use(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/item/ItemStack;",
		at = @At("HEAD"), cancellable = true)
	private void retroapi$eatFood(ItemStack stack, World world, PlayerEntity player,
			CallbackInfoReturnable<ItemStack> cir) {
		if (RetroFood.isFood((Item) (Object) this)) {
			cir.setReturnValue(RetroFood.eat(stack, world, player));
		}
	}

	@Override
	public RetroItemAccess texture(NamespacedIdentifier textureId) {
		Item self = (Item) (Object) this;
		RetroTexture tex = RetroTextures.addItemTexture(textureId);
		self.setTextureId(tex.id);
		RetroTextures.trackItem(self, tex);
		return this;
	}

	@Override
	public Item register(NamespacedIdentifier id) {
		Item self = (Item) (Object) this;
		self.setTranslationKey(id.namespace() + "." + id.identifier());

		// Item model JSON (models/item/{id}.json), when present, overrides .texture(...).
		com.periut.retroapi.client.model.ItemModelLoader.tryApply(self, id);

		RetroRegistry.registerItem(new ItemRegistration(id, self));

		if (FabricLoader.getInstance().isModLoaded("stationapi")) {
			StationBridges.get().registerItem(id.namespace(), id.identifier(), self);
		}

		return self;
	}
}
