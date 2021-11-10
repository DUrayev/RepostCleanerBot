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
import org.telegram.repostcleanerbot.utils.I18nService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

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

    @Inject
    private I18nService i18n;

    @Inject
    private KeyboardFactory keyboardFactory;

    @Override
    public ReplyFlow getFlow() {
        Reply startAllChatsAnalyzing = Reply.of((bot, upd) -> {
            botContext.hidePreviousReplyMarkup(upd);

            BotEmbadedTelegramClient client = clientManager.getTelegramClientForUser(getUser(upd).getId());
            EventManager getChatsRequestEventManager = new EventManager();
            GetChatsRequest getChatsRequest = new GetChatsRequest(client, getChatsRequestEventManager);

            getChatsRequestEventManager.addEventHandler(GetChatsRequest.EVENTS.CHATS_COUNT_RECEIVED, onChatsCountReceived(upd));
            getChatsRequestEventManager.addEventHandler(GetChatsRequest.EVENTS.FINISH, onAllChatsReceived(upd, client));

            botContext.bot().silent().send(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.list_of_chats_requested"), getChatId(upd));
            getChatsRequest.execute(ANALYZE_ALL_CHATS_LIMIT);
        }, botContext.hasCallbackQuery(INLINE_BUTTONS.START_ANALYZING_ALL_CHATS));

        Reply cancelAllChatsAnalyzing = Reply.of((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);
                    botContext.bot().silent().send(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.cancel_all_chats_analyzing"), getChatId(upd));
                },
                botContext.hasCallbackQuery(INLINE_BUTTONS.CANCEL_ANALYZING_ALL_CHATS));

        return ReplyFlow.builder(botContext.bot().db())
                .onlyIf(botContext.hasCallbackQuery(INLINE_BUTTONS.ALL_CHATS))
                .onlyIf(botContext.isChatNotInState(STATE_DB.LOGIN_STATE_DB))
                .action((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);
                    botContext.bot().silent().execute(
                            SendMessage.builder()
                                    .text(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.confirm_all_chats_analyzing"))
                                    .chatId(getChatId(upd).toString())
                                    .replyMarkup(keyboardFactory.withOneLineButtons(
                                            InlineKeyboardButton.builder().text(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.start_analyzing_btn")).callbackData(INLINE_BUTTONS.START_ANALYZING_ALL_CHATS).build(),
                                            InlineKeyboardButton.builder().text(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.cancel_analyzing_btn")).callbackData(INLINE_BUTTONS.CANCEL_ANALYZING_ALL_CHATS).build()
                                    ))
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

            botContext.bot().silent().send(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.all_chats_info_received", chatsWhereYouCanSendMessage.size()), getChatId(upd));

            EventManager getRepostsFromAllChatsEventManager = new EventManager();
            GetRepostsFromChatListRequest getRepostsFromChatListRequest = new GetRepostsFromChatListRequest(client, getRepostsFromAllChatsEventManager);

            getRepostsFromAllChatsEventManager.addEventHandler(GetRepostsFromChatListRequest.EVENTS.FINISH, onAllRepostsReceived(upd));
            addChatsAnalyzingProgressNotifier(upd, chatsWhereYouCanSendMessage.size(), getRepostsFromAllChatsEventManager);

            getRepostsFromChatListRequest.execute(chatsWhereYouCanSendMessage);
        };
    }

    private void addChatsAnalyzingProgressNotifier(Update upd, int totalChatsCountToAnalyze, EventManager getRepostsFromAllChatsEventManager) {
        AtomicInteger analyzedMessagesCount = new AtomicInteger(0);
        Optional<Message> progressNotificationMsgOptional = botContext.bot().silent().execute(
                SendMessage.builder()
                        .text(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.analyzing_progress_notification", 0, totalChatsCountToAnalyze, 0))
                        .chatId(getChatId(upd).toString())
                        .replyMarkup(keyboardFactory.withOneLineButtons()) //without this empty reply markup you're not able to edit message
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
                botContext.bot().silent().execute(EditMessageText.builder()
                        .chatId(getChatId(upd).toString())
                        .messageId(msgWithProgressInfoId)
                        .text(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.analyzing_progress_notification", finishedChatsCount, totalChatsCountToAnalyze, analyzedMessagesCount.get()))
                        .build());
            });
        }
    }

    private Consumer<Object> onChatsCountReceived(Update upd) {
        return chatsCount -> {
            int chatsCountInt = Integer.parseInt(chatsCount.toString());
            botContext.bot().silent().send(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.chats_count_received", chatsCountInt, ANALYZE_ALL_CHATS_LIMIT), getChatId(upd));
        };
    }

    private Consumer<Object> onAllRepostsReceived(Update upd) {
        return allReposts -> {
            Map<String, List<Repost>> repostsGroupedByRepostedFrom = ((List<Repost>) allReposts).stream()
                    .collect(Collectors.groupingBy(r -> String.valueOf(r.getRepostedFrom().getId())));

            Collection<List<Repost>> repostsListGroupedByRepostedFrom = repostsGroupedByRepostedFrom.values();
            List<List<Repost>> repostsListGroupedByRepostedFromSorted = sortByTotalRepostsCountDesc(repostsListGroupedByRepostedFrom);
            addStatisticInfoToChatsTitle(repostsListGroupedByRepostedFromSorted, upd);

            repostsRepository.save(getUser(upd).getId(), repostsListGroupedByRepostedFromSorted);

            botContext.enterState(upd, STATE_DB.CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB);
            botContext.bot().silent().execute(
                    SendMessage.builder()
                            .text(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.select_channel_reposts_from_to_clean"))
                            .chatId(getChatId(upd).toString())
                            .replyMarkup(keyboardFactory.replyKeyboardWithCancelButtons(
                                    repostsListGroupedByRepostedFromSorted.stream().map(groupedList -> groupedList.get(0).getRepostedFrom().getTitle()).collect(Collectors.toList()),
                                    i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.select_channel_to_clean_keyboard_placeholder"),
                                    getUser(upd).getLanguageCode()
                            ))
                            .allowSendingWithoutReply(false)
                            .build());
        };
    }

    private void addStatisticInfoToChatsTitle(List<List<Repost>> groupedRepostsList, Update upd) {
        long ownerUserId = getUser(upd).getId();
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
            String finalRepostedFromTitle = i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.select_channel_to_clean_keyboard_button_with_stat", (i+1), repostedFromTitle, repostedByMe, repostedNotByMe);
            listOfReposts.forEach(r -> r.getRepostedFrom().setTitle(finalRepostedFromTitle));
        }
    }

    private List<List<Repost>> sortByTotalRepostsCountDesc(Collection<List<Repost>> repostsListGroupedByRepostedFrom) {
        List<List<Repost>> repostsListGroupedByRepostedFromSorted = repostsListGroupedByRepostedFrom.stream().sorted(Comparator.comparing(List::size)).collect(Collectors.toList());
        Collections.reverse(repostsListGroupedByRepostedFromSorted);
        return repostsListGroupedByRepostedFromSorted;
    }
}
