package org.telegram.repostcleanerbot.tdlib.request;

import it.tdlight.jni.TdApi;
import lombok.extern.log4j.Log4j2;
import org.telegram.repostcleanerbot.tdlib.EventManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;
import org.telegram.repostcleanerbot.tdlib.entity.Chat;
import org.telegram.repostcleanerbot.tdlib.entity.Repost;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class GetRepostsFromChatRequest extends Request {
    private static final int MESSAGE_COUNT_IN_ONE_REQUEST_LIMIT = 100;

    private List<Repost> result = new ArrayList<>();

    public enum EVENTS {
        BATCH_OF_MESSAGES_RECEIVED,
        FINISH
    }

    public GetRepostsFromChatRequest(BotEmbadedTelegramClient client, EventManager eventManager) {
        super(client, eventManager);
    }

    public void execute(Chat chat) {
        getReposts(chat, 0);
    }

    private void getReposts(Chat chat, long fromMessageId) {
        client.send(new TdApi.GetChatHistory(chat.getId(), fromMessageId, 0, MESSAGE_COUNT_IN_ONE_REQUEST_LIMIT, false), messages -> {
            int messagesReceivedCount = messages.get().totalCount;
            this.eventManager.fireEvent(EVENTS.BATCH_OF_MESSAGES_RECEIVED, messagesReceivedCount);
            log.info("{} messages received for chat {}", messagesReceivedCount, chat.getTitle());
            if(messagesReceivedCount != 0) {
                long lastMessageId = messages.get().messages[messagesReceivedCount - 1].id;
                for(TdApi.Message message : messages.get().messages) {
                    TdApi.MessageForwardInfo forwardInfo = message.forwardInfo;
                    if(forwardInfo != null) {
                        TdApi.MessageForwardOrigin originRepostedFrom = forwardInfo.origin;
                        if(originRepostedFrom instanceof TdApi.MessageForwardOriginChannel) {
                            Date repostedDate = new Date(message.date * 1000L);
                            long repostedByUserId = -1;
                            if(message.sender instanceof TdApi.MessageSenderUser) {
                                repostedByUserId = ((TdApi.MessageSenderUser)message.sender).userId;
                            }
                            long repostedFromChatId = ((TdApi.MessageForwardOriginChannel) originRepostedFrom).chatId;
                            Repost repost = Repost.builder()
                                        .id(message.id)
                                        .repostedAt(repostedDate)
                                        .repostedBy(repostedByUserId)
                                        .repostedFrom(Chat.builder().id(repostedFromChatId).build())
                                        .repostedIn(chat)
                                        .build();
                            result.add(repost);
                        }
                    }
                }
                getReposts(chat, lastMessageId);
            } else {
                fillRepostedFromChatsInfo(result);
            }
        });
    }

    private void fillRepostedFromChatsInfo(List<Repost> repostList) {
        AtomicInteger repostProcessedCount = new AtomicInteger(0);
        if(repostList.size() > 0) {
            for(Repost repost : repostList) {
                EventManager getSingleChatInfoRequestEventManager = new EventManager();
                getSingleChatInfoRequestEventManager.addEventHandler(GetChatRequest.EVENTS.FINISH, repostedFromChat -> {
                    repost.getRepostedFrom().setTitle(((Chat)repostedFromChat).getTitle());
                    repost.getRepostedFrom().setCanSendMessage(((Chat)repostedFromChat).isCanSendMessage());
                    if(repostProcessedCount.incrementAndGet() == repostList.size()) {
                        this.eventManager.fireEvent(EVENTS.FINISH, result);
                    }
                });
                GetChatRequest getChatRequest = new GetChatRequest(this.client, getSingleChatInfoRequestEventManager);
                getChatRequest.execute(repost.getRepostedFrom().getId());
            }
        } else {
            this.eventManager.fireEvent(EVENTS.FINISH, result);
        }
    }

}
