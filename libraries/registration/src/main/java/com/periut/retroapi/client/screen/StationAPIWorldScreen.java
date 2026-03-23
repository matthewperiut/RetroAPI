package com.periut.retroapi.client.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.ArrayList;
import java.util.List;

public class StationAPIWorldScreen extends Screen {
	private static final String MESSAGE =
		"This is a StationAPI World. It cannot be loaded without StationAPI, " +
		"but it can be turned back into a Vanilla world by loading the game with StationAPI, " +
		"clicking edit, and pressing convert to McRegion. " +
		"RetroAPI mods only will transfer to Vanilla world format, and " +
		"StationAPI-dependent items, blocks, etc will disappear. " +
		"RetroAPI mods will work in StationAPI seamlessly alongside other StationAPI mods.";

	private List<String> messageLines;

	@Override
	public void init() {
		this.buttons.clear();
		this.messageLines = wrapText(MESSAGE, this.width - 60);

		int textEndY = this.height / 4 + 10 + this.messageLines.size() * 12;
		int buttonY = Math.min(textEndY + 20, this.height - 30);

		this.buttons.add(new ButtonWidget(
			0, this.width / 2 - 100, buttonY, "Back to Title Screen"
		));
	}

	private List<String> wrapText(String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		String[] words = text.split(" ");
		StringBuilder currentLine = new StringBuilder();

		for (String word : words) {
			String test = currentLine.length() == 0 ? word : currentLine + " " + word;
			if (this.textRenderer.getWidth(test) > maxWidth && currentLine.length() > 0) {
				lines.add(currentLine.toString());
				currentLine = new StringBuilder(word);
			} else {
				if (currentLine.length() > 0) currentLine.append(" ");
				currentLine.append(word);
			}
		}
		if (currentLine.length() > 0) {
			lines.add(currentLine.toString());
		}
		return lines;
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == 0) {
			this.minecraft.world = null;
			this.minecraft.openScreen(new TitleScreen());
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTick) {
		this.renderBackground();

		this.drawCenteredString(this.textRenderer, "StationAPI World Detected",
			this.width / 2, this.height / 4 - 10, 0xFF5555);

		int y = this.height / 4 + 10;
		for (String line : this.messageLines) {
			this.drawCenteredString(this.textRenderer, line,
				this.width / 2, y, 0xAAAAAA);
			y += 12;
		}

		super.render(mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}
