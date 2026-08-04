package com.periut.retroapi.mixin.register;

import com.periut.retroapi.register.block.RetroDisguises;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes a disguised block sound like what it is wearing when you walk on it. See
 * {@link com.periut.retroapi.register.block.RetroBlockDisguise}.
 *
 * <p>{@code Block.soundGroup} is a field on the block type, so beta has exactly one step sound for every
 * position of a block and no hook that is given both the sound and the place it came from.
 *
 * <p>So the sound call itself is redirected and the position recomputed from the entity, with the same
 * arithmetic vanilla used a few lines earlier to find the block it was standing on, including its habit
 * of looking one block lower when that block is a fence. Recomputing rather than capturing vanilla's
 * locals is the point: the locals are unnamed and their slots are a compiler detail, while the entity's
 * own feet are not going to move between one statement and the next.
 */
@Mixin(Entity.class)
public class DisguisedStepSoundMixin {

	@Redirect(method = "move",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V"),
		require = 0)
	private void retroapi$disguisedStepSound(World world, Entity entity, String sound, float volume, float pitch) {
		BlockSoundGroup worn = retroapi$disguisedGroupUnderfoot(world, entity);
		if (worn == null) {
			world.playSound(entity, sound, volume, pitch);
			return;
		}
		// Vanilla scales a step sound to 0.15 of the group's volume; keep that relationship rather than
		// carrying the old group's number over to a group that may be louder or quieter.
		world.playSound(entity, worn.getSound(), worn.getVolume() * 0.15F, worn.getPitch());
	}

	@Unique
	private BlockSoundGroup retroapi$disguisedGroupUnderfoot(World world, Entity entity) {
		if (world == null || entity == null) {
			return null;
		}
		int x = MathHelper.floor(entity.x);
		int y = MathHelper.floor(entity.y - 0.2F - entity.standingEyeHeight);
		int z = MathHelper.floor(entity.z);
		// Vanilla's own fence rule: standing on top of one, the sound comes from the fence below.
		if (world.getBlockId(x, y, z) == 0 || world.getBlockId(x, y - 1, z) == Block.FENCE.id) {
			if (world.getBlockId(x, y - 1, z) != 0) {
				y--;
			}
		}
		Block worn = RetroDisguises.at(world, x, y, z);
		return worn == null ? null : worn.soundGroup;
	}
}
