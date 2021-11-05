package org.telegram.repostcleanerbot.tdlib.request;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.telegram.repostcleanerbot.tdlib.EventManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;
import org.telegram.repostcleanerbot.tdlib.entity.Chat;
import org.telegram.repostcleanerbot.tdlib.entity.Repost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class GetRepostsFromChatListRequest extends Request {
    private static final int BATCH_SIZE = 5;

    private List<Repost> result = new ArrayList<>();
    private int totalAmountOfChats;
    private AtomicInteger processedChatCount = new AtomicInteger(0);

    public enum EVENTS {
        FINISH
    }

    public GetRepostsFromChatListRequest(BotEmbadedTelegramClient client, EventManager eventManager) {
        super(client, eventManager);
    }

    public void execute(List<Chat> chatList) {
        totalAmountOfChats = chatList.size();
        List<List<Chat>> chatListBatches = ListUtils.partition(chatList, BATCH_SIZE);
        processBatches(chatListBatches);
    }

    private void processBatches(List<List<Chat>> chatListBatches) {
        processBatch(chatListBatches, 0);
    }

    private void processBatch(List<List<Chat>> chatListBatches, int batchNumber) {
        if(batchNumber < chatListBatches.size()) {
            List<Chat> chatListBatch = chatListBatches.get(batchNumber);
            int currentBatchSize = chatListBatch.size();
            AtomicInteger currentBatchProcessedChatCount = new AtomicInteger(0);
            for (Chat chat : chatListBatch) {
                EventManager getRepostsFromChatRequestEventManager = new EventManager();
                getRepostsFromChatRequestEventManager.addEventHandler(GetRepostsFromChatRequest.EVENTS.FINISH, reposts -> {
                    result.addAll((Collection<? extends Repost>) reposts);
                    log.info("'{}' chat analyzed for history. Total progress: {} out of {}", chat.getTitle(), processedChatCount.get() + 1 , totalAmountOfChats);
                    if(processedChatCount.incrementAndGet() == totalAmountOfChats) {
                        eventManager.fireEvent(EVENTS.FINISH, result);
                    } else if(currentBatchProcessedChatCount.incrementAndGet() == currentBatchSize) {
                        processBatch(chatListBatches, batchNumber + 1);
                    }
                });
                GetRepostsFromChatRequest getRepostsFromChatRequest = new GetRepostsFromChatRequest(this.client, getRepostsFromChatRequestEventManager);
                getRepostsFromChatRequest.execute(chat);
            }
        }
    }

}
