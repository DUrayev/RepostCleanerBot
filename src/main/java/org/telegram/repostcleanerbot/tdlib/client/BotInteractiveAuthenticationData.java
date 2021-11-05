package org.telegram.repostcleanerbot.tdlib.client;

import it.tdlight.client.AuthenticationData;
import org.telegram.repostcleanerbot.bot.BotContext;

public class BotInteractiveAuthenticationData implements AuthenticationData {

    private final BotContext botContext;
    private final long chatId;

    public BotInteractiveAuthenticationData(BotContext botContext, long chatId) {
        this.botContext = botContext;
        this.chatId = chatId;
    }

    @Override
    public boolean isQrCode() {
        return false;
    }

    @Override
    public boolean isBot() {
        return false;
    }

    @Override
    public long getUserPhoneNumber() {
        return 0;
    }

    @Override
    public String getBotToken() {
        return null;
    }
}
