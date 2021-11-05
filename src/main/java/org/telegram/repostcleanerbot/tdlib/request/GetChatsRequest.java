package org.telegram.repostcleanerbot.tdlib.request;

import it.tdlight.jni.TdApi;
import org.telegram.repostcleanerbot.tdlib.EventManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;
import org.telegram.repostcleanerbot.tdlib.entity.Chat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GetChatsRequest extends Request {
    public enum EVENTS {
        CHATS_COUNT_RECEIVED,
        FINISH
    }

    public GetChatsRequest(BotEmbadedTelegramClient client, EventManager eventManager) {
        super(client, eventManager);
    }

    public void execute(int chatsLimit) {
        this.client.send(new TdApi.GetChats(null, chatsLimit), chatList -> {
            int totalChatsCount = chatList.get().chatIds.length;
            eventManager.fireEvent(EVENTS.CHATS_COUNT_RECEIVED, totalChatsCount);
            AtomicInteger chatsProcessedCount = new AtomicInteger(0);
            List<Chat> result = new ArrayList<>();
            EventManager getSingleChatInfoRequestEventManager = new EventManager();
            getSingleChatInfoRequestEventManager.addEventHandler(GetChatRequest.EVENTS.FINISH, chat -> {
                result.add((Chat) chat);
                if(chatsProcessedCount.incrementAndGet() == totalChatsCount) {
                    eventManager.fireEvent(EVENTS.FINISH, result);
                }
            });
            GetChatRequest getChatRequest = new GetChatRequest(this.client, getSingleChatInfoRequestEventManager);
            for(long chatId : chatList.get().chatIds) {
                getChatRequest.execute(chatId);
            }
        });
    }

}
