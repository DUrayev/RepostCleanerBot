package org.telegram.repostcleanerbot.flow.state;

import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.State;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.repostcleanerbot.repository.UserChatsRepostedFromRepository;
import org.telegram.repostcleanerbot.tdlib.ClientManager;
import org.telegram.repostcleanerbot.tdlib.EventManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;
import org.telegram.repostcleanerbot.tdlib.entity.RepostsStat;
import org.telegram.repostcleanerbot.tdlib.request.DeleteMessagesRequest;
import org.telegram.repostcleanerbot.utils.I18nService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.abilitybots.api.util.AbilityUtils.getUser;
import static org.telegram.repostcleanerbot.Constants.STATE_DB;

public class CleanRepostsFromSpecificChatState implements State {

    @Inject
    private BotContext botContext;

    @Inject
    private UserChatsRepostedFromRepository repostedFromRepository;

    @Inject
    private ClientManager clientManager;

    @Inject
    private I18nService i18n;

    @Inject
    private KeyboardFactory keyboardFactory;

    @Override
    public Reply getReply() {
        return Reply.of((bot, upd) -> {
            String selectedChatToClean = upd.getMessage().getText();
            botContext.hidePreviousReplyMarkup(upd);

            if(selectedChatToClean.equals(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("cancel_keyboard_btn"))) {
                finishCleaningAndNotify(upd, false);
            } else {
                List<RepostsStat> userRepostsStatList = repostedFromRepository.get(getUser(upd).getId());
                RepostsStat selectedChatRepostedFromToClean = userRepostsStatList.stream().filter(repostsStat -> repostsStat.getRepostedFrom().getTitle().equals(upd.getMessage().getText())).findFirst().get();
                List<Long> repostMessageIdList = selectedChatRepostedFromToClean.getRepostMessageIdList();

                BotEmbadedTelegramClient client = clientManager.getTelegramClientForUser(getUser(upd).getId());
                EventManager deleteMessagesEventManager = new EventManager();
                DeleteMessagesRequest deleteMessagesRequest = new DeleteMessagesRequest(client, deleteMessagesEventManager);

                deleteMessagesEventManager.addEventHandler(DeleteMessagesRequest.EVENTS.FINISH, onMessagesDeleted(upd, selectedChatToClean, userRepostsStatList, selectedChatRepostedFromToClean));

                deleteMessagesRequest.execute(selectedChatRepostedFromToClean.getRepostedIn().getId(), repostMessageIdList);
            }
        },
            botContext.isChatInState(STATE_DB.CLEAN_REPOSTS_FROM_SPECIFIC_CHAT_STATE_DB),
            chatWithRepostsFromReplyKeyboardSelectedOrCancelBtn()
        );
    }

    private Consumer<Object> onMessagesDeleted(Update upd, String selectedChatToClean, List<RepostsStat> userRepostsStatList, RepostsStat selectedChatRepostedFromToClean) {
        return deleteResult -> {
            if ((Boolean) deleteResult) {
                userRepostsStatList.remove(selectedChatRepostedFromToClean);
                if (userRepostsStatList.size() > 0) {
                    repostedFromRepository.save(getUser(upd).getId(), userRepostsStatList);
                    botContext.bot().silent().execute(
                            SendMessage.builder()
                                    .text(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("specific_chat.reposts_from_channel_are_cleaned", selectedChatToClean))
                                    .chatId(getChatId(upd).toString())
                                    .replyMarkup(keyboardFactory.replyKeyboardWithCancelButtons(
                                            userRepostsStatList.stream().map(r -> r.getRepostedFrom().getTitle()).collect(Collectors.toList()),
                                            i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("specific_chat.select_channel_to_clean_keyboard_placeholder"),
                                            getUser(upd).getLanguageCode()
                                    ))
                                    .allowSendingWithoutReply(false)
                                    .build());
                } else {
                    finishCleaningAndNotify(upd, true);
                }
            } else {
                botContext.bot().silent().execute(
                        SendMessage.builder()
                                .text(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("specific_chat.unexpected_error_cleaning_reposts_from_channel", selectedChatToClean))
                                .chatId(getChatId(upd).toString())
                                .replyMarkup(keyboardFactory.replyKeyboardWithCancelButtons(
                                        userRepostsStatList.stream().map(r -> r.getRepostedFrom().getTitle()).collect(Collectors.toList()),
                                        i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("specific_chat.select_channel_to_clean_keyboard_placeholder"),
                                        getUser(upd).getLanguageCode()
                                ))
                                .allowSendingWithoutReply(false)
                                .build());
            }
        };
    }

    private void finishCleaningAndNotify(Update upd, boolean allRepostsAreCleaned) {
        botContext.exitState(upd, STATE_DB.CLEAN_REPOSTS_FROM_SPECIFIC_CHAT_STATE_DB);
        repostedFromRepository.save(getUser(upd).getId(), Collections.emptyList());
        String responseText = allRepostsAreCleaned ? i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("specific_chat.cleaning_is_finished_all_reposts_are_cleaned") : i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("specific_chat.cleaning_is_canceled");
        botContext.bot().silent().execute(
                SendMessage.builder()
                        .text(responseText)
                        .chatId(getChatId(upd).toString())
                        .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                        .allowSendingWithoutReply(false)
                        .build());
    }

    private Predicate<Update> chatWithRepostsFromReplyKeyboardSelectedOrCancelBtn() {
        return upd -> {
            if(botContext.cancelKeyboardButtonSelected().test(upd)) {
                return true;
            }
            if(upd.hasMessage()) {
                List<RepostsStat> userRepostsStatList = repostedFromRepository.get(getUser(upd).getId());
                return userRepostsStatList.stream().anyMatch(repostsStat -> repostsStat.getRepostedFrom().getTitle().equals(upd.getMessage().getText()));
            } else {
                return false;
            }
        };
    }
}
