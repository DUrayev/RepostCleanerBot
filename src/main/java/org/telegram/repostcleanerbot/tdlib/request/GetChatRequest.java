package org.telegram.repostcleanerbot.tdlib.request;

import it.tdlight.jni.TdApi;
import lombok.extern.log4j.Log4j2;
import org.telegram.repostcleanerbot.tdlib.EventManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;
import org.telegram.repostcleanerbot.tdlib.entity.Chat;

@Log4j2
public class GetChatRequest extends Request {
    public enum EVENTS {
        FINISH
    }

    public GetChatRequest(BotEmbadedTelegramClient client, EventManager eventManager) {
        super(client, eventManager);
    }

    public void execute(long chatId) {
        client.send(new TdApi.GetChat(chatId), chatResult -> {
            if(!chatResult.isError()) {
                Chat chat = new Chat(chatId, chatResult.get().title, chatResult.get().permissions.canSendMessages);
                this.eventManager.fireEvent(EVENTS.FINISH, chat);
            } else {
                log.warn("Error for chat with id {}: {}", chatId, chatResult.getError().message);
            }
        });
    }
}
