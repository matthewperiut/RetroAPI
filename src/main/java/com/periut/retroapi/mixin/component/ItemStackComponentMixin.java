package com.periut.retroapi.mixin.component;

import com.periut.retroapi.component.ComponentNbt;
import com.periut.retroapi.component.RetroComponentHolder;
import com.periut.retroapi.component.RetroComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * The data-component half of the ItemStack mixin, split out of {@code register.ItemStackMixin} so it can
 * stay ACTIVE under StationAPI. The OTHER half (numeric-id flattening - {@code retroapi:id} clamp/resolve)
 * clashes with StationAPI's own item serialization and stays disabled; data components are independent of
 * it and must keep working when StationAPI is present.
 *
 * <p>Mixes the {@link RetroComponentHolder} duck interface onto every ItemStack (the handle the static
 * {@link com.periut.retroapi.component.RetroComponents} API and the pickup/sync mixins cast to - so those
 * casts no longer crash under StationAPI), deep-copies components on {@code copy()}/{@code split()}, and
 * round-trips them through {@code retroapi:components} NBT alongside whatever id serialization is active
 * (vanilla's numeric id or StationAPI's {@code stationapi:id}). It only touches the additive
 * {@code retroapi:components} sub-tag, so it composes cleanly with StationAPI's writeNbt/readNbt.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackComponentMixin implements RetroComponentHolder {

	/** This stack's data components (see RetroComponents). Lazily created, never on vanilla saves. */
	@Unique
	private Map<RetroComponentType<?>, Object> retroapi$components;

	@Override
	public Map<RetroComponentType<?>, Object> retroapi$components() {
		if (this.retroapi$components == null) {
			this.retroapi$components = new HashMap<>();
		}
		return this.retroapi$components;
	}

	@Override
	public void retroapi$setComponents(Map<RetroComponentType<?>, Object> components) {
		this.retroapi$components = components;
	}

	/** copy() makes a fresh stack; deep-copy the components onto it or they would be shared. */
	@Inject(method = "copy", at = @At("RETURN"))
	private void retroapi$copyComponents(CallbackInfoReturnable<ItemStack> cir) {
		retroapi$copyComponentsTo(cir.getReturnValue());
	}

	/** split() makes a fresh stack taking part of this one; it must carry the same components. */
	@Inject(method = "split", at = @At("RETURN"))
	private void retroapi$splitComponents(int amount, CallbackInfoReturnable<ItemStack> cir) {
		retroapi$copyComponentsTo(cir.getReturnValue());
	}

	@Unique
	private void retroapi$copyComponentsTo(ItemStack target) {
		if (target != null && this.retroapi$components != null && !this.retroapi$components.isEmpty()) {
			((RetroComponentHolder) (Object) target)
				.retroapi$setComponents(new HashMap<>(this.retroapi$components));
		}
	}

	@Inject(method = "writeNbt", at = @At("RETURN"))
	private void retroapi$writeComponents(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
		ComponentNbt.write(this, nbt);
	}

	@Inject(method = "readNbt", at = @At("RETURN"))
	private void retroapi$readComponents(NbtCompound nbt, CallbackInfo ci) {
		ComponentNbt.read(this, nbt);
	}
}
