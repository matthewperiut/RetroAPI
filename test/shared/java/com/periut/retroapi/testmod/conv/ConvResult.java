package com.periut.retroapi.testmod.conv;

import com.periut.retroapi.testmod.TestMod;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Writes a machine-readable PASS/FAIL line into the world directory after a scenario runs, so the
 * {@code conversionRoundTripTest} Gradle task can gate on it (it greps for {@code FAIL}). Appends, so
 * the multi-stage round-trip leaves a full trail of every stage's verdict.
 */
public final class ConvResult {
	private ConvResult() {}

	public static void write(File worldDir, String stage, boolean pass, String detail) {
		String line = stage + ": " + (pass ? "PASS" : "FAIL") + (detail == null || detail.isEmpty() ? "" : " - " + detail);
		TestMod.LOGGER.info("[conv-result] {}", line);
		try (FileWriter w = new FileWriter(new File(worldDir, Scenario.RESULT_FILE), true)) {
			w.write(line + System.lineSeparator());
		} catch (IOException e) {
			TestMod.LOGGER.error("[conv-result] failed to write result file", e);
		}
	}
}
