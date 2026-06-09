package com.periut.retroapi.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * Asynchronous, crash-safe persistence for all sidecar files.
 *
 * <p><b>Why async:</b> b1.7.3 single-player autosaves every few seconds on the main thread; gzipping
 * a multi-megabyte region sidecar inline froze the game on every autosave. The NBT tree is not
 * thread-safe (it keeps being mutated by chunk saves), so {@link #snapshot} serializes it to a raw
 * byte[] on the caller's thread - cheap, no compression - and the expensive gzip + disk write runs
 * on a single background thread. One thread keeps writes to the same file ordered (FIFO).
 *
 * <p><b>Why atomic:</b> writing straight to the target file truncates it first; a crash mid-write
 * left a torn gzip that failed to load, resetting the region to empty - entire regions of modded
 * blocks "became air". Every write here goes to a {@code .tmp} sibling and is atomically renamed
 * over the target, so on disk there is only ever the old complete file or the new complete file.
 *
 * <p>{@link #drain()} blocks until the queue is empty - called on world switch
 * ({@code SidecarManager.flush}) and from a JVM shutdown hook so a normal quit never loses
 * queued writes. A hard crash loses at most the writes queued since the last completed autosave,
 * never the integrity of files already on disk.
 */
public final class SidecarIo {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/SidecarIo");

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "RetroAPI-Sidecar-IO");
		t.setDaemon(true);
		return t;
	});

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(SidecarIo::drain, "RetroAPI-Sidecar-Drain"));
	}

	private SidecarIo() {
	}

	/**
	 * Serialize the NBT tree to raw (uncompressed) bytes on the CALLER's thread - the tree is not
	 * thread-safe, so this is the hand-off point; the returned snapshot is immutable.
	 */
	public static byte[] snapshot(NbtCompound root) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(8192);
		NbtIo.write(root, new DataOutputStream(bytes));
		return bytes.toByteArray();
	}

	/** Queue an atomic gzip write of a snapshot. Returns immediately. */
	public static void writeAsync(File file, byte[] payload) {
		EXECUTOR.execute(() -> writeNow(file, payload));
	}

	/**
	 * Gzip the snapshot to {@code <file>.tmp} and atomically rename it over {@code file}.
	 * Output is byte-identical to {@code NbtIo.writeCompressed}, so files stay interchangeable
	 * with the old synchronous writers and with {@code NbtIo.readCompressed}.
	 */
	public static void writeNow(File file, byte[] payload) {
		try {
			File dir = file.getParentFile();
			if (dir != null) {
				dir.mkdirs();
			}
			File tmp = new File(dir, file.getName() + ".tmp");
			try (GZIPOutputStream gz = new GZIPOutputStream(new FileOutputStream(tmp))) {
				gz.write(payload);
			}
			try {
				Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to write sidecar {}", file, e);
		}
	}

	/** Block until every queued write has reached disk (bounded; logs instead of hanging forever). */
	public static void drain() {
		try {
			EXECUTOR.submit(() -> {
			}).get(60, TimeUnit.SECONDS);
		} catch (Exception e) {
			LOGGER.warn("Sidecar IO drain did not complete cleanly", e);
		}
	}
}
