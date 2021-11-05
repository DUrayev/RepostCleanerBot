package org.telegram.repostcleanerbot.tdlib.request;

import it.tdlight.jni.TdApi;
import lombok.extern.log4j.Log4j2;
import org.telegram.repostcleanerbot.tdlib.EventManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;

import java.util.List;

@Log4j2
public class DeleteMessagesRequest extends Request {
    public enum EVENTS {
        FINISH
    }

    public DeleteMessagesRequest(BotEmbadedTelegramClient client, EventManager eventManager) {
        super(client, eventManager);
    }

    public void execute(long chatId, List<Long> messageIds, boolean revoke) {
        client.send(new TdApi.DeleteMessages(chatId, messageIds.stream().mapToLong(l -> l).toArray(), revoke), okResult -> {
            boolean isSuccess = !okResult.isError();
            eventManager.fireEvent(EVENTS.FINISH, isSuccess);
        });
    }

}
