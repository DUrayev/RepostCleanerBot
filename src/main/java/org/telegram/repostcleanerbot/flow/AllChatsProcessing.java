package org.telegram.repostcleanerbot.flow;

import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.Flow;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.repostcleanerbot.repository.UserAllRepostsGroupedByRepostedFromRepository;
import org.telegram.repostcleanerbot.tdlib.ClientManager;
import org.telegram.repostcleanerbot.tdlib.EventManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;
import org.telegram.repostcleanerbot.tdlib.entity.Chat;
import org.telegram.repostcleanerbot.tdlib.entity.Repost;
import org.telegram.repostcleanerbot.tdlib.request.GetChatsRequest;
import org.telegram.repostcleanerbot.tdlib.request.GetRepostsFromChatListRequest;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.abilitybots.api.util.AbilityUtils.getUser;
import static org.telegram.repostcleanerbot.Constants.*;

public class AllChatsProcessing implements Flow {
    @Inject
    private BotContext botContext;
    @Inject
    private ClientManager clientManager;
    @Inject
    private UserAllRepostsGroupedByRepostedFromRepository repostsRepository;

    @Override
    public ReplyFlow getFlow() {
        Reply startAllChatsAnalyzing = Reply.of((bot, upd) -> {
            botContext.hidePreviousReplyMarkup(upd);

            BotEmbadedTelegramClient client = clientManager.getTelegramClientForUser(getUser(upd).getId());
            EventManager getChatsRequestEventManager = new EventManager();
            GetChatsRequest getChatsRequest = new GetChatsRequest(client, getChatsRequestEventManager);

            getChatsRequestEventManager.addEventHandler(GetChatsRequest.EVENTS.CHATS_COUNT_RECEIVED, onChatsCountReceived(upd));
            getChatsRequestEventManager.addEventHandler(GetChatsRequest.EVENTS.FINISH, onAllChatsReceived(upd, client));

            botContext.bot().silent().send("List of chats is requested", getChatId(upd));
            getChatsRequest.execute(ANALYZE_ALL_CHATS_LIMIT);
        }, botContext.hasCallbackQuery(INLINE_BUTTONS.START_ANALYZING_ALL_CHATS));

        Reply cancelAllChatsAnalyzing = Reply.of((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);
                    botContext.bot().silent().send("Ok, process is canceled.\nYou can /start again", getChatId(upd));
                },
                botContext.hasCallbackQuery(INLINE_BUTTONS.CANCEL_ANALYZING_ALL_CHATS));

        return ReplyFlow.builder(botContext.bot().db())
                .onlyIf(botContext.hasCallbackQuery(INLINE_BUTTONS.ALL_CHATS))
                .onlyIf(botContext.isChatNotInState(STATE_DB.LOGIN_STATE_DB))
                .action((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);
                    botContext.execute(
                            SendMessage.builder()
                                    .text("All chats analyzing is not a quick process. Do you want to continue?")
                                    .chatId(getChatId(upd).toString())
                                    .replyMarkup(KeyboardFactory.withOneLineButtons(INLINE_BUTTONS.START_ANALYZING_ALL_CHATS, INLINE_BUTTONS.CANCEL_ANALYZING_ALL_CHATS))
                                    .allowSendingWithoutReply(false)
                                    .build());
                })
                .next(startAllChatsAnalyzing)
                .next(cancelAllChatsAnalyzing)
                .build();
    }

    private Consumer<Object> onAllChatsReceived(Update upd, BotEmbadedTelegramClient client) {
        return chatList -> {
            List<Chat> chatsWhereYouCanSendMessage = ((List<Chat>)chatList).stream().filter(Chat::isCanSendMessage).collect(Collectors.toList());

            botContext.bot().silent().send("There are " + chatsWhereYouCanSendMessage.size() + " chats where you can send messages\nAnalyzing is started", getChatId(upd));

            EventManager getRepostsFromAllChatsEventManager = new EventManager();
            GetRepostsFromChatListRequest getRepostsFromChatListRequest = new GetRepostsFromChatListRequest(client, getRepostsFromAllChatsEventManager);

            getRepostsFromAllChatsEventManager.addEventHandler(GetRepostsFromChatListRequest.EVENTS.FINISH, onAllRepostsReceived(upd));
            addChatsAnalyzingProgressNotifier(upd, chatsWhereYouCanSendMessage.size(), getRepostsFromAllChatsEventManager);

            getRepostsFromChatListRequest.execute(chatsWhereYouCanSendMessage);
        };
    }

    private void addChatsAnalyzingProgressNotifier(Update upd, int totalChatsCountToAnalyze, EventManager getRepostsFromAllChatsEventManager) {
        String chatsProgressNotification = "Total progress:\n%s out of " + totalChatsCountToAnalyze + " chats were analyzed\n%s messages were analyzed. Please, wait...";
        AtomicInteger analyzedMessagesCount = new AtomicInteger(0);
        Optional<Message> progressNotificationMsgOptional = botContext.bot().silent().execute(
                SendMessage.builder()
                        .text(String.format(chatsProgressNotification, "0", "0"))
                        .chatId(getChatId(upd).toString())
                        .replyMarkup(KeyboardFactory.withOneLineButtons()) //without this empty reply markup you're not able to edit message
                        .allowSendingWithoutReply(false)
                        .build());

        if(progressNotificationMsgOptional.isPresent()) {
            Integer msgWithProgressInfoId = progressNotificationMsgOptional.get().getMessageId();
            getRepostsFromAllChatsEventManager.addEventHandler(GetRepostsFromChatListRequest.EVENTS.BATCH_OF_MESSAGES_RECEIVED, messagesBatchSize -> {
                if((Integer)messagesBatchSize > 0) {
                    analyzedMessagesCount.addAndGet((Integer) messagesBatchSize);
                }
            });
            getRepostsFromAllChatsEventManager.addEventHandler(GetRepostsFromChatListRequest.EVENTS.CHAT_PROCESSING_FINISHED, finishedChatsCount -> {
                botContext.execute(EditMessageText.builder()
                        .chatId(getChatId(upd).toString())
                        .messageId(msgWithProgressInfoId)
                        .text(String.format(chatsProgressNotification, finishedChatsCount.toString(), analyzedMessagesCount.get()))
                        .build());
            });
        }
    }

    private Consumer<Object> onChatsCountReceived(Update upd) {
        return chatsCount -> {
            int chatsCountInt = Integer.parseInt(chatsCount.toString());
            botContext.bot().silent().send("You have " + chatsCountInt + " chats (limit is " + ANALYZE_ALL_CHATS_LIMIT + " )\nReceiving chat details...", getChatId(upd));
        };
    }

    private Consumer<Object> onAllRepostsReceived(Update upd) {
        return allReposts -> {
            Map<String, List<Repost>> repostsGroupedByRepostedFrom = ((List<Repost>) allReposts).stream()
                    .collect(Collectors.groupingBy(r -> String.valueOf(r.getRepostedFrom().getId())));

            Collection<List<Repost>> repostsListGroupedByRepostedFrom = repostsGroupedByRepostedFrom.values();
            List<List<Repost>> repostsListGroupedByRepostedFromSorted = sortByTotalRepostsCountDesc(repostsListGroupedByRepostedFrom);
            addStatisticInfoToChatsTitle(repostsListGroupedByRepostedFromSorted, getUser(upd).getId());

            repostsRepository.save(getUser(upd).getId(), repostsListGroupedByRepostedFromSorted);

            botContext.enterState(upd, STATE_DB.CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB);
            botContext.execute(
                    SendMessage.builder()
                            .text("Reposts from which channel you want to clean everywhere?")
                            .chatId(getChatId(upd).toString())
                            .replyMarkup(KeyboardFactory.replyKeyboardWithCancelButtons(repostsListGroupedByRepostedFromSorted.stream().map(groupedList -> groupedList.get(0).getRepostedFrom().getTitle()).collect(Collectors.toList()), "Select channel name"))
                            .allowSendingWithoutReply(false)
                            .build());
        };
    }

    private void addStatisticInfoToChatsTitle(List<List<Repost>> groupedRepostsList, long ownerUserId) {
        for (int i = 0; i < groupedRepostsList.size(); i++) {
            List<Repost> listOfReposts = groupedRepostsList.get(i);
            String repostedFromTitle = listOfReposts.get(0).getRepostedFrom().getTitle();

            int repostedByMe = 0;
            int repostedNotByMe = 0;

            for (Repost repost : listOfReposts) {
                if (repost.getRepostedBy() == ownerUserId) {
                    repostedByMe++;
                } else {
                    repostedNotByMe++;
                }
            }
            String finalRepostedFromTitle = (i+1) + ". " + repostedFromTitle + " [You: " + repostedByMe + ", Not you: " + repostedNotByMe + "]";
            listOfReposts.forEach(r -> r.getRepostedFrom().setTitle(finalRepostedFromTitle));
        }
    }

    private List<List<Repost>> sortByTotalRepostsCountDesc(Collection<List<Repost>> repostsListGroupedByRepostedFrom) {
        List<List<Repost>> repostsListGroupedByRepostedFromSorted = repostsListGroupedByRepostedFrom.stream().sorted(Comparator.comparing(List::size)).collect(Collectors.toList());
        Collections.reverse(repostsListGroupedByRepostedFromSorted);
        return repostsListGroupedByRepostedFromSorted;
    }
}
