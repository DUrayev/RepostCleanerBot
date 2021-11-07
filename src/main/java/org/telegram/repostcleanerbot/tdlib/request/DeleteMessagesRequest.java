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

    public void execute(long chatId, List<Long> messageIds) {
        client.send(new TdApi.DeleteMessages(chatId, messageIds.stream().mapToLong(l -> l).toArray(), true), okResult -> {
            boolean isSuccessWithRevoke = !okResult.isError();
            if(!isSuccessWithRevoke) {
                log.warn("Can't delete messaged with 'revoke'='true' param. Try to delete without revoke");
                client.send(new TdApi.DeleteMessages(chatId, messageIds.stream().mapToLong(l -> l).toArray(), false), okResultWithoutRevoke -> {
                    boolean isSuccessWithoutRevoke = !okResultWithoutRevoke.isError();
                    if(!isSuccessWithoutRevoke) {
                        log.warn(okResult.getError().message);
                    }
                    eventManager.fireEvent(EVENTS.FINISH, isSuccessWithoutRevoke);
                });
            } else {
                eventManager.fireEvent(EVENTS.FINISH, true);
            }
        });
    }

}
