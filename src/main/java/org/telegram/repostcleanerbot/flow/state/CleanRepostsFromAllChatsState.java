package org.telegram.repostcleanerbot.flow.state;

import org.jetbrains.annotations.NotNull;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.State;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.repostcleanerbot.repository.UserAllRepostsGroupedByRepostedFromRepository;
import org.telegram.repostcleanerbot.tdlib.ClientManager;
import org.telegram.repostcleanerbot.tdlib.EventManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;
import org.telegram.repostcleanerbot.tdlib.entity.Repost;
import org.telegram.repostcleanerbot.tdlib.request.DeleteMessagesRequest;
import org.telegram.repostcleanerbot.utils.I18nService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.abilitybots.api.util.AbilityUtils.getUser;
import static org.telegram.repostcleanerbot.Constants.STATE_DB;

public class CleanRepostsFromAllChatsState implements State {

    @Inject
    private BotContext botContext;

    @Inject
    private UserAllRepostsGroupedByRepostedFromRepository repostsRepository;

    @Inject
    private ClientManager clientManager;

    @Inject
    private I18nService i18n;

    @Inject
    private KeyboardFactory keyboardFactory;

    @Override
    public Reply getReply() {
        return Reply.of((bot, upd) -> {
            botContext.hidePreviousReplyMarkup(upd);
            String selectedChatToClean = upd.getMessage().getText();
            if(selectedChatToClean.equals(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("cancel_keyboard_btn"))) {
                finishCleaningAndNotify(upd, false);
            } else {
                List<List<Repost>> repostsListGroupedByRepostedFrom = repostsRepository.get(getUser(upd).getId());

                List<Repost> repostsToClean = repostsListGroupedByRepostedFrom.stream().filter(groupedRepostsList -> groupedRepostsList.get(0).getRepostedFrom().getTitle().equals(selectedChatToClean)).findFirst().get();

                Map<Long, List<Repost>> repostsToCleanGroupedByRepostedInId = (repostsToClean).stream()
                        .collect(Collectors.groupingBy(r -> r.getRepostedIn().getId()));

                BotEmbadedTelegramClient client = clientManager.getTelegramClientForUser(getUser(upd).getId());
                EventManager deleteMessagesEventManager = new EventManager();
                DeleteMessagesRequest deleteMessagesRequest = new DeleteMessagesRequest(client, deleteMessagesEventManager);

                int totalChatsToCleanIn = repostsToCleanGroupedByRepostedInId.keySet().size();
                AtomicInteger cleanedChatsCount = new AtomicInteger(0);
                repostsToCleanGroupedByRepostedInId.forEach((repostedInChatId, listOfReposts) -> {
                    deleteMessagesEventManager.addEventHandler(DeleteMessagesRequest.EVENTS.FINISH, onMessagesDeletedFromChat(upd, selectedChatToClean, repostsListGroupedByRepostedFrom, totalChatsToCleanIn, cleanedChatsCount, listOfReposts));
                    deleteMessagesRequest.execute(repostedInChatId, listOfReposts.stream().map(Repost::getId).collect(Collectors.toList()));
                });
            }
        }, botContext.isChatInState(STATE_DB.CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB), repostedFromChatFromReplyKeyboardSelectedOrCancelBtn());
    }

    @NotNull
    private Consumer<Object> onMessagesDeletedFromChat(Update upd, String selectedChatToClean, List<List<Repost>> repostsListGroupedByRepostedFrom, int totalChatsToCleanIn, AtomicInteger cleanedChatsCount, List<Repost> listOfReposts) {
        return deleteResult -> {
            if ((Boolean) deleteResult) {
                if (cleanedChatsCount.incrementAndGet() == totalChatsToCleanIn) {
                    repostsListGroupedByRepostedFrom.removeIf(groupedRepostsList -> groupedRepostsList.get(0).getRepostedFrom().getTitle().equals(selectedChatToClean));
                    if (repostsListGroupedByRepostedFrom.size() > 0) {
                        repostsRepository.save(getUser(upd).getId(), repostsListGroupedByRepostedFrom);
                        botContext.bot().silent().execute(
                                SendMessage.builder()
                                        .text(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.reposts_from_channel_are_cleaned_everywhere", selectedChatToClean))
                                        .chatId(getChatId(upd).toString())
                                        .replyMarkup(keyboardFactory.replyKeyboardWithCancelButtons(
                                                repostsListGroupedByRepostedFrom.stream().map(groupedList -> groupedList.get(0).getRepostedFrom().getTitle()).collect(Collectors.toList()),
                                                i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.select_channel_to_clean_keyboard_placeholder"),
                                                getUser(upd).getLanguageCode()
                                        ))
                                        .allowSendingWithoutReply(false)
                                        .build());
                    } else {
                        finishCleaningAndNotify(upd, true);
                    }
                }
            } else {
                botContext.bot().silent().execute(
                        SendMessage.builder()
                                .text(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.unexpected_error_cleaning_reposts_from_channel", selectedChatToClean, listOfReposts.get(0).getRepostedIn().getTitle()))
                                .chatId(getChatId(upd).toString())
                                .replyMarkup(keyboardFactory.replyKeyboardWithCancelButtons(
                                        repostsListGroupedByRepostedFrom.stream().map(groupedList -> groupedList.get(0).getRepostedFrom().getTitle()).collect(Collectors.toList()),
                                        i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.select_channel_to_clean_keyboard_placeholder"),
                                        getUser(upd).getLanguageCode()
                                ))
                                .allowSendingWithoutReply(false)
                                .build());
            }
        };
    }

    private void finishCleaningAndNotify(Update upd, boolean allRepostsAreCleaned) {
        botContext.exitState(upd, STATE_DB.CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB);
        repostsRepository.save(getUser(upd).getId(), Collections.emptyList());
        String responseText = allRepostsAreCleaned ? i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.cleaning_is_finished_all_reposts_are_cleaned") : i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("all_chats.cleaning_is_canceled");
        botContext.bot().silent().execute(
                SendMessage.builder()
                        .text(responseText)
                        .chatId(getChatId(upd).toString())
                        .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                        .allowSendingWithoutReply(false)
                        .build());
    }

    private Predicate<Update> repostedFromChatFromReplyKeyboardSelectedOrCancelBtn() {
        return upd -> {
            if(botContext.cancelKeyboardButtonSelected().test(upd)) {
                return true;
            }
            if(upd.hasMessage()) {
                List<List<Repost>> repostsListGroupedByRepostedFrom = repostsRepository.get(getUser(upd).getId());
                return repostsListGroupedByRepostedFrom.stream().anyMatch(groupedRepostsList -> groupedRepostsList.get(0).getRepostedFrom().getTitle().equals(upd.getMessage().getText()));
            } else {
                return false;
            }
        };
    }
}
