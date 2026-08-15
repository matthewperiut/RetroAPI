package com.periut.retroapi.commandblock;

import com.periut.retroapi.commands.FeedbackSink;
import com.periut.retroapi.text.Text;

/**
 * Catches what a command block's command said, so the edit screen can show it as "Previous Output"
 * the way modern does.
 */
public final class CommandBlockFeedback implements FeedbackSink {

    private final StringBuilder text = new StringBuilder();

    @Override
    public void send(final Text message) {
        if (message == null) {
            return;
        }
        if (text.length() > 0) {
            text.append('\n');
        }
        text.append(message.getString());
    }

    public String getText() {
        return text.toString();
    }
}
