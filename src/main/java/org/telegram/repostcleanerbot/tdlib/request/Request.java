package org.telegram.repostcleanerbot.tdlib.request;

import org.telegram.repostcleanerbot.tdlib.EventManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;

public abstract class Request extends EventProvider {

    protected BotEmbadedTelegramClient client;

    protected Request(BotEmbadedTelegramClient client, EventManager eventManager) {
        super(eventManager);
        this.client = client;
    }
}
