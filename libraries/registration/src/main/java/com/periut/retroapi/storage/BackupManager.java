package com.periut.retroapi.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupManager {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/BackupManager");
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	public static void backupRetroApiData(File worldDir) {
		File retroapiDir = new File(worldDir, "retroapi");
		if (!retroapiDir.exists() || !retroapiDir.isDirectory()) {
			LOGGER.info("No retroapi data to backup in {}", worldDir);
			return;
		}

		String timestamp = LocalDateTime.now().format(FORMATTER);
		File backupDir = new File(retroapiDir, "backups/" + timestamp);
		backupDir.mkdirs();

		File[] files = retroapiDir.listFiles();
		if (files == null) return;

		for (File file : files) {
			if (file.getName().equals("backups")) continue;
			try {
				copyRecursive(file, new File(backupDir, file.getName()));
			} catch (IOException e) {
				LOGGER.error("Failed to backup {}", file, e);
			}
		}

		LOGGER.info("Backed up retroapi data to {}", backupDir);
	}

	private static void copyRecursive(File src, File dest) throws IOException {
		if (src.isDirectory()) {
			dest.mkdirs();
			File[] children = src.listFiles();
			if (children == null) return;
			for (File child : children) {
				copyRecursive(child, new File(dest, child.getName()));
			}
		} else {
			try (InputStream in = new FileInputStream(src);
				 OutputStream out = new FileOutputStream(dest)) {
				byte[] buf = new byte[8192];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			}
		}
	}
}
