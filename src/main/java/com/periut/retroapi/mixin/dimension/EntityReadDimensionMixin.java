package com.periut.retroapi.mixin.dimension;

import com.periut.retroapi.dimension.RetroDimensionRegistry;
import com.periut.retroapi.storage.PlayerDimensionSidecar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Load side of the player dimension sidecar ({@code PlayerDimensionMixin} is the save side).
 *
 * <p>This MUST hook {@code Entity.read(NbtCompound)} - the public NBT entry point - and not
 * {@code PlayerEntity.readNbt}: {@code read} parses {@code Pos}/{@code Rotation} FIRST and only
 * then calls the per-class {@code readNbt} hook (where {@code Dimension} is read). Injecting the
 * sidecar values into the compound here, before ANY parsing, means vanilla's own loading path
 * places the player - position, bounding box, prev-coords, rotation, dimension - exactly as if
 * the values had always been in the file. No post-load repositioning.
 */
@Mixin(Entity.class)
public class EntityReadDimensionMixin {
	@Inject(method = "read", at = @At("HEAD"))
	private void retroapi$injectRealDimension(NbtCompound nbt, CallbackInfo ci) {
		if (!((Object) this instanceof PlayerEntity self)) return;
		PlayerDimensionSidecar.Entry entry = PlayerDimensionSidecar.getPlayer(self.name);
		if (entry == null) return;
		// Only inject while the dimension is actually registered; if its mod was removed, the
		// clamped vanilla state (overworld spawn) is the correct, safe fallback and the sidecar
		// entry survives untouched for when the mod returns.
		if (RetroDimensionRegistry.getBySerialId(entry.dimensionId) == null) return;

		nbt.putInt("Dimension", entry.dimensionId);
		NbtList pos = new NbtList();
		pos.add(new NbtDouble(entry.x));
		pos.add(new NbtDouble(entry.y));
		pos.add(new NbtDouble(entry.z));
		nbt.put("Pos", pos);
		if (entry.hasRotation) {
			NbtList rot = new NbtList();
			rot.add(new NbtFloat(entry.yaw));
			rot.add(new NbtFloat(entry.pitch));
			nbt.put("Rotation", rot);
		}
	}
}
