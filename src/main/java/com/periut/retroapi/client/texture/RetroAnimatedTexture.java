package com.periut.retroapi.client.texture;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

/**
 * One animated sprite on an expanded atlas: holds the extracted frames (RGBA bytes at the
 * pack's sprite size) and a frame clock, and uploads the current frame into the atlas
 * texture with {@code glTexSubImage2D} when it changes.
 *
 * <p>Design note: vanilla's {@code DynamicTexture} pipeline hardcodes 16px sprites and the
 * 256px atlas in {@code TextureManager.tick} (a soup of literal 16s with three different
 * meanings, which {@code @ModifyConstant} cannot tell apart). Rather than patch that, RetroAPI
 * drives its animators itself from a {@code tick()} tail hook in {@code TextureManagerMixin},
 * computing exact offsets from the slot and sprite size. Same upload call, same timing,
 * correct on any atlas size and any HD pack.</p>
 */
public class RetroAnimatedTexture {

	private static ByteBuffer uploadBuffer = BufferUtils.createByteBuffer(16 * 16 * 4);

	private final int slot;
	private final int spriteSize;
	/** Frame pixels in PNG order, RGBA, spriteSize * spriteSize * 4 bytes each. */
	private final byte[][] frames;
	/** Play order (indices into frames) and per-step times, never null after construction. */
	private final int[] order;
	private final int[] times;
	private final boolean interpolate;

	private int step = 0;
	private int clock = 0;
	private final byte[] pixels;
	private boolean needsUpload = true;

	public RetroAnimatedTexture(int slot, int spriteSize, byte[][] frames, AnimationMetadata meta) {
		this.slot = slot;
		this.spriteSize = spriteSize;
		this.frames = frames;
		if (meta.frameOrder != null) {
			this.order = meta.frameOrder;
			this.times = meta.frameTimes;
		} else {
			this.order = new int[frames.length];
			this.times = new int[frames.length];
			for (int i = 0; i < frames.length; i++) {
				this.order[i] = i;
				this.times[i] = meta.frametime;
			}
		}
		this.interpolate = meta.interpolate;
		this.pixels = new byte[spriteSize * spriteSize * 4];
		System.arraycopy(frame(this.order[0]), 0, this.pixels, 0, this.pixels.length);
	}

	private byte[] frame(int index) {
		return frames[Math.floorMod(index, frames.length)];
	}

	/** Advances the frame clock one game tick. */
	public void tick() {
		clock++;
		int time = times[step];
		if (clock >= time) {
			clock = 0;
			step = (step + 1) % order.length;
			if (!interpolate) {
				System.arraycopy(frame(order[step]), 0, pixels, 0, pixels.length);
				needsUpload = true;
			}
		}
		if (interpolate) {
			byte[] current = frame(order[step]);
			byte[] next = frame(order[(step + 1) % order.length]);
			float t = (float) clock / (float) times[step];
			for (int i = 0; i < pixels.length; i++) {
				int a = current[i] & 0xFF;
				int b = next[i] & 0xFF;
				pixels[i] = (byte) (a + (int) ((b - a) * t));
			}
			needsUpload = true;
		}
	}

	/** Uploads the current frame into the bound atlas texture if it changed this tick. */
	public void upload() {
		if (!needsUpload) {
			return;
		}
		needsUpload = false;
		if (uploadBuffer.capacity() < pixels.length) {
			uploadBuffer = BufferUtils.createByteBuffer(pixels.length);
		}
		uploadBuffer.clear();
		uploadBuffer.put(pixels);
		uploadBuffer.flip();
		int x = (slot % 16) * spriteSize;
		int y = (slot / 16) * spriteSize;
		GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, spriteSize, spriteSize,
			GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, uploadBuffer);
	}
}
