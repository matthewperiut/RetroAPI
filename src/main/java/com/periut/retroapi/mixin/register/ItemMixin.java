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
	private java.util.Set<com.periut.retroapi.tag.RetroTool> retroapi$toolKinds = java.util.Collections.emptySet();

	@Override
	public RetroItemAccess maxStackSize(int size) {
		this.maxCount = size;
		return this;
	}

	@Override
	public RetroItemAccess tool(com.periut.retroapi.tag.RetroTool... tools) {
		this.retroapi$toolKinds = tools.length == 0
			? java.util.Collections.emptySet()
			: new java.util.LinkedHashSet<>(java.util.Arrays.asList(tools));
		return this;
	}

	@Override
	public java.util.Set<com.periut.retroapi.tag.RetroTool> getToolKinds() {
		return this.retroapi$toolKinds;
	}

	@org.spongepowered.asm.mixin.Unique
	private com.periut.retroapi.tag.RetroToolTier retroapi$toolTier = null;
	@org.spongepowered.asm.mixin.Unique
	private com.periut.retroapi.tag.RetroToolTier.Dynamic retroapi$toolTierDynamic = null;

	@Override
	public RetroItemAccess tier(com.periut.retroapi.tag.RetroToolTier tier) {
		this.retroapi$toolTier = tier;
		return this;
	}

	@Override
	public RetroItemAccess tier(com.periut.retroapi.tag.RetroToolTier.Dynamic tier) {
		this.retroapi$toolTierDynamic = tier;
		return this;
	}

	@org.spongepowered.asm.mixin.Unique
	private com.periut.retroapi.tag.RetroToolTier.Contextual retroapi$toolTierContextual = null;
	@org.spongepowered.asm.mixin.Unique
	private com.periut.retroapi.tag.RetroToolTier.Positional retroapi$toolTierPositional = null;
	@org.spongepowered.asm.mixin.Unique
	private float retroapi$miningSpeed = 0.0F;
	@org.spongepowered.asm.mixin.Unique
	private int retroapi$attackDamage = -1;
	@org.spongepowered.asm.mixin.Unique
	private boolean retroapi$damageOnMine = true;

	@Override
	public RetroItemAccess tier(com.periut.retroapi.tag.RetroToolTier.Contextual tier) {
		this.retroapi$toolTierContextual = tier;
		return this;
	}

	@Override
	public com.periut.retroapi.tag.RetroToolTier getToolTier() {
		return this.retroapi$toolTier;
	}

	@Override
	public com.periut.retroapi.tag.RetroToolTier.Dynamic getToolTierDynamic() {
		return this.retroapi$toolTierDynamic;
	}

	@Override
	public com.periut.retroapi.tag.RetroToolTier.Contextual getToolTierContextual() {
		return this.retroapi$toolTierContextual;
	}

	@Override
	public RetroItemAccess tier(com.periut.retroapi.tag.RetroToolTier.Positional tier) {
		this.retroapi$toolTierPositional = tier;
		return this;
	}

	@Override
	public com.periut.retroapi.tag.RetroToolTier.Positional getToolTierPositional() {
		return this.retroapi$toolTierPositional;
	}

	@Override
	public RetroItemAccess miningSpeed(float speed) {
		this.retroapi$miningSpeed = speed;
		return this;
	}

	@Override
	public float getMiningSpeed() {
		return this.retroapi$miningSpeed;
	}

	@Override
	public RetroItemAccess attackDamage(int damage) {
		this.retroapi$attackDamage = damage;
		return this;
	}

	@Override
	public int getAttackDamage() {
		return this.retroapi$attackDamage;
	}

	@Override
	public RetroItemAccess durability(int uses) {
		((Item) (Object) this).setMaxDamage(uses);
		return this;
	}

	@Override
	public RetroItemAccess damageOnMine(boolean enabled) {
		this.retroapi$damageOnMine = enabled;
		return this;
	}

	/**
	 * Teaches VANILLA that a declared tool is the right tool for a block. Beta answers "is this item
	 * effective here?" from hardcoded block lists inside each tool item, which no modded item is in and
	 * no modded block is in either - the reason a custom drill could not harvest a modded (or even a
	 * vanilla) stone block. Answer it from the {@code mineable/<tool>} tags plus the tool's tier instead,
	 * so every vanilla path that asks - drops, breaking speed, the "correct tool" check - agrees.
	 */
	@Inject(method = "isSuitableFor", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$isSuitableFor(net.minecraft.block.Block block, CallbackInfoReturnable<Boolean> cir) {
		if (this.retroapi$toolKinds.isEmpty() || block == null) {
			return;
		}
		java.util.Set<com.periut.retroapi.tag.RetroTool> mineable =
			com.periut.retroapi.tag.RetroTags.mineableTools(block);
		if (mineable.isEmpty() || java.util.Collections.disjoint(this.retroapi$toolKinds, mineable)) {
			return; // not our kind of block: let vanilla answer
		}
		com.periut.retroapi.tag.RetroToolTier required = com.periut.retroapi.tag.RetroTags.requiredTier(block);
		com.periut.retroapi.tag.RetroToolTier held = this.retroapi$toolTier != null
			? this.retroapi$toolTier : com.periut.retroapi.tag.RetroToolTier.WOOD;
		if (held.isAtLeast(required)) {
			cir.setReturnValue(true);
		}
	}

	/** The declared mining speed, for tools built from parameters instead of a {@code ToolMaterial}. */
	@Inject(method = "getMiningSpeedMultiplier", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$miningSpeed(ItemStack stack, net.minecraft.block.Block block,
			CallbackInfoReturnable<Float> cir) {
		if (this.retroapi$miningSpeed <= 0.0F || this.retroapi$toolKinds.isEmpty() || block == null) {
			return;
		}
		java.util.Set<com.periut.retroapi.tag.RetroTool> mineable =
			com.periut.retroapi.tag.RetroTags.mineableTools(block);
		if (!mineable.isEmpty() && !java.util.Collections.disjoint(this.retroapi$toolKinds, mineable)) {
			cir.setReturnValue(this.retroapi$miningSpeed);
		}
	}

	/** The declared attack damage, for tools built from parameters instead of a {@code ToolMaterial}. */
	@Inject(method = "getAttackDamage", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$attackDamage(net.minecraft.entity.Entity target, CallbackInfoReturnable<Integer> cir) {
		if (this.retroapi$attackDamage >= 0) {
			cir.setReturnValue(this.retroapi$attackDamage);
		}
	}

	/**
	 * Wears a declared tool down as it mines, the way a vanilla {@code ToolItem} does - otherwise a
	 * parameter-built tool with {@code .durability(...)} would be indestructible in practice.
	 */
	@Inject(method = "postMine", at = @At("HEAD"), require = 0)
	private void retroapi$damageOnMine(ItemStack stack, int blockId, int x, int y, int z,
			net.minecraft.entity.LivingEntity miner, CallbackInfoReturnable<Boolean> cir) {
		if (!this.retroapi$damageOnMine || this.retroapi$toolKinds.isEmpty()) {
			return;
		}
		Item self = (Item) (Object) this;
		if (self.getMaxDamage() <= 0) {
			return;
		}
		net.minecraft.block.Block block = blockId > 0 && blockId < net.minecraft.block.Block.BLOCKS.length
			? net.minecraft.block.Block.BLOCKS[blockId] : null;
		// Vanilla only wears a tool on blocks that actually take effort (hardness > 0).
		if (block != null && block.getHardness() > 0.0F) {
			stack.damage(1, miner);
		}
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

	@org.spongepowered.asm.mixin.Unique
	private RetroTexture retroapi$baseTexture = null;

	@Override
	public RetroItemAccess texture(NamespacedIdentifier textureId) {
		Item self = (Item) (Object) this;
		RetroTexture tex = RetroTextures.addItemTexture(textureId);
		self.setTextureId(tex.id);
		RetroTextures.trackItem(self, tex);
		this.retroapi$baseTexture = tex;
		return this;
	}

	@Override
	public RetroItemAccess layers(NamespacedIdentifier base, NamespacedIdentifier... overlays) {
		Item self = (Item) (Object) this;
		// Remember the base slot so a later .overlay(...) stacks onto the same one.
		this.retroapi$baseTexture = com.periut.retroapi.client.model.ItemModelLoader.applyLayers(
			self, base, java.util.Arrays.asList(overlays));
		return this;
	}

	@Override
	public RetroItemAccess overlay(NamespacedIdentifier overlayTextureId) {
		if (this.retroapi$baseTexture == null) {
			com.periut.retroapi.RetroAPI.LOGGER.warn(
				"overlay({}) called with no base texture; call .texture(...) or .layers(...) first", overlayTextureId);
			return this;
		}
		com.periut.retroapi.client.model.ItemModelLoader.addOverlay(this.retroapi$baseTexture, overlayTextureId);
		return this;
	}

	/** Statically declared render-time layers (see RetroItemAccess.overlay(id, tint)). Null until used. */
	@org.spongepowered.asm.mixin.Unique
	private java.util.List<com.periut.retroapi.component.RetroTextureLayer> retroapi$declaredLayers = null;

	@Override
	public RetroItemAccess overlay(NamespacedIdentifier overlayTextureId, int tint) {
		retroapi$seedBaseLayer();
		this.retroapi$declaredLayers.add(com.periut.retroapi.component.RetroTextureLayer.tinted(
			// get-or-add, not add: the same overlay sprite declared across twenty items is one atlas slot,
			// and the handle resolves its index at draw time, so this is safe before the atlas is built.
			RetroTextures.getOrAddItemTexture(overlayTextureId), tint));
		return this;
	}

	@Override
	public RetroItemAccess layer(com.periut.retroapi.component.RetroTextureLayer layer) {
		if (layer == null) {
			return this;
		}
		if (this.retroapi$declaredLayers == null) {
			this.retroapi$declaredLayers = new java.util.ArrayList<>();
		}
		this.retroapi$declaredLayers.add(layer);
		return this;
	}

	@Override
	public java.util.List<com.periut.retroapi.component.RetroTextureLayer> getDeclaredLayers() {
		return this.retroapi$declaredLayers == null
			? java.util.Collections.emptyList()
			: java.util.Collections.unmodifiableList(this.retroapi$declaredLayers);
	}

	/**
	 * Makes sure layer 0 is the item's own sprite before the first overlay stacks onto it. Prefers the
	 * tracked {@link RetroTexture} handle so the base resolves at draw time too; falls back to whatever
	 * sprite index the item currently reports (a vanilla item, or one whose texture came from elsewhere).
	 */
	@org.spongepowered.asm.mixin.Unique
	private void retroapi$seedBaseLayer() {
		if (this.retroapi$declaredLayers != null) {
			return;
		}
		this.retroapi$declaredLayers = new java.util.ArrayList<>();
		Item self = (Item) (Object) this;
		RetroTexture base = this.retroapi$baseTexture != null
			? this.retroapi$baseTexture
			: RetroTextures.getTrackedTexture(self);
		this.retroapi$declaredLayers.add(base != null
			? com.periut.retroapi.component.RetroTextureLayer.plain(base)
			: com.periut.retroapi.component.RetroTextureLayer.plain(self.getTextureId(0)));
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
