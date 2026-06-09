package com.periut.retroapi.mixin.dimension;

import com.periut.retroapi.dimension.RetroDimensionRegistry;
import com.periut.retroapi.storage.PlayerDimensionSidecar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Safe-reset of the player's saved dimension id. Vanilla {@code PlayerEntity.writeNbt} writes
 * {@code Dimension = dimensionId}; for a modded dimension that is a poison value that crashes vanilla
 * on load.
 *
 * <p><b>Save</b> ({@code writeNbt} TAIL): capture the EXACT {@code Pos}/{@code Rotation} values vanilla
 * just wrote into the compound, record them (plus the real dimension id) in the
 * {@link PlayerDimensionSidecar}, then clamp the compound to a vanilla-safe state (Dimension 0,
 * position = overworld spawn).
 *
 * <p><b>Load</b> ({@code readNbt} HEAD): inject the recorded values back INTO the compound before
 * vanilla parses it, so the normal loading path reads them as if they had always been in the file.
 * No post-load {@code setPositionAndAngles} - position, bounding box, prev-coords and rotation all
 * come out of vanilla's own code, byte-exact with what was saved.
 *
 * <p>Disabled under StationAPI (its world dir is never set, so the sidecar no-ops anyway, but
 * disabling keeps the path clean).
 */
@Mixin(PlayerEntity.class)
public class PlayerDimensionMixin {
	@Inject(method = "writeNbt", at = @At("TAIL"))
	private void retroapi$clampDimension(NbtCompound nbt, CallbackInfo ci) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		int dim = self.dimensionId;
		if (!RetroDimensionRegistry.isVanillaId(dim) && RetroDimensionRegistry.getBySerialId(dim) != null) {
			// Record the exact doubles/floats vanilla wrote - not the live entity fields - so the
			// round trip through the sidecar is bit-identical to a vanilla save/load.
			double px = self.x, py = self.y, pz = self.z;
			NbtList posList = nbt.getList("Pos");
			if (posList != null && posList.size() >= 3) {
				px = ((NbtDouble) posList.get(0)).value;
				py = ((NbtDouble) posList.get(1)).value;
				pz = ((NbtDouble) posList.get(2)).value;
			}
			float yaw = self.yaw, pitch = self.pitch;
			NbtList rotList = nbt.getList("Rotation");
			if (rotList != null && rotList.size() >= 2) {
				yaw = ((NbtFloat) rotList.get(0)).value;
				pitch = ((NbtFloat) rotList.get(1)).value;
			}
			PlayerDimensionSidecar.recordPlayer(self.name, dim, px, py, pz, yaw, pitch);

			nbt.putInt("Dimension", 0);
			// Clamp the vanilla position to the (shared) overworld spawn so a vanilla open puts the
			// player on solid ground at spawn, not in the void at the modded-dimension coordinates.
			if (self.world != null) {
				Vec3i spawn = self.world.getSpawnPos();
				NbtList pos = new NbtList();
				pos.add(new NbtDouble(spawn.x + 0.5));
				pos.add(new NbtDouble(spawn.y + 1.0));
				pos.add(new NbtDouble(spawn.z + 0.5));
				nbt.put("Pos", pos);
			}
		} else {
			PlayerDimensionSidecar.removePlayer(self.name);
		}
	}

	// NOTE: the LOAD-side injection lives in EntityReadDimensionMixin, NOT here. Vanilla's NBT
	// entry point is Entity.read(NbtCompound): it parses Pos/Rotation FIRST and only then calls
	// the per-class readNbt(nbt) hook - so an injection at PlayerEntity.readNbt HEAD is early
	// enough for "Dimension" (read inside readNbt) but TOO LATE for "Pos"/"Rotation" (already
	// parsed). That asymmetry shipped once: dimension restored, position reset to the clamp.
}
