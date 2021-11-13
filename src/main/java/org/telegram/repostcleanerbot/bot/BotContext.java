package org.telegram.repostcleanerbot.bot;

import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.repostcleanerbot.Constants;
import org.telegram.repostcleanerbot.repository.UserAllRepostsGroupedByRepostedFromRepository;
import org.telegram.repostcleanerbot.repository.UserChatsRepository;
import org.telegram.repostcleanerbot.repository.UserChatsRepostedFromRepository;
import org.telegram.repostcleanerbot.tdlib.ClientManager;
import org.telegram.repostcleanerbot.utils.I18nService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.abilitybots.api.util.AbilityUtils.getUser;

public class BotContext {

    @Inject
    private AbilityBot bot;

    @Inject
    private I18nService i18n;

    @Inject
    private ClientManager clientManager;

    public Predicate<Update> isChatInState(String stateDbName) {
        return upd -> {
            Map<String, Boolean> chatCleaningStateDb = bot.db().getMap(stateDbName);
            return chatCleaningStateDb.get(getUser(upd).getId().toString());
        };
    }

    public Predicate<Update> notStartCommand() {
        return upd -> upd.hasMessage() && !upd.getMessage().getText().equalsIgnoreCase("/start");
    }

    public Predicate<Update> isChatNotInState(String stateDbName) {
        return isChatInState(stateDbName).negate();
    }

    public void enterState(Update upd, String stateDbName) {
        enterState(getUser(upd).getId(), stateDbName);
    }

    public void enterState(Long userId, String stateDbName) {
        Map<String, Boolean> chatCleaningStateDb = bot.db().getMap(stateDbName);
        chatCleaningStateDb.put(userId.toString(), true);
    }

    public void exitState(Update upd, String stateDbName) {
        exitState(getUser(upd).getId(), stateDbName);
    }

    public void exitState(Long userId, String stateDbName) {
        Map<String, Boolean> chatCleaningStateDb = bot.db().getMap(stateDbName);
        chatCleaningStateDb.put(userId.toString(), false);
    }

    public void hidePreviousReplyMarkup(Update upd) {
        if(upd.hasCallbackQuery()) {
            try {
                bot.sender().execute(EditMessageReplyMarkup.builder()
                        .chatId(getChatId(upd).toString())
                        .messageId(upd.getCallbackQuery().getMessage().getMessageId())
                        .replyMarkup(null)
                        .build());
            } catch (TelegramApiException ignore) {
            }
        }
    }

    public Predicate<Update> hasCallbackQuery(String... possibleCallbackData) {
        return upd -> upd.hasCallbackQuery() && Arrays.stream(possibleCallbackData).anyMatch(data -> upd.getCallbackQuery().getData().equalsIgnoreCase(data)) ;
    }

    public Predicate<Update> cancelKeyboardButtonSelected() {
        return upd -> upd.hasMessage() && upd.getMessage().getText().equals(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("cancel_keyboard_btn")) ;
    }

    public BaseAbilityBot bot() {
        return bot;
    }

    public Predicate<Update> itemFromReplyKeyboardSelected(List<String> replyItems) {
        return upd -> upd.hasMessage() && replyItems.contains(upd.getMessage().getText());
    }

    public boolean isAdmin(long userId) {
        return bot().isAdmin(userId) || bot().isCreator(userId);
    }

    public void handleLogOut(User user, Long chatId) {
        this.exitState(user.getId(), Constants.STATE_DB.LOGIN_STATE_DB);
        this.exitState(user.getId(), Constants.STATE_DB.TWO_STEP_VERIFICATION_PASSWORD_ENTERING_STATE_DB);
        this.exitState(user.getId(), Constants.STATE_DB.CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB);
        this.exitState(user.getId(), Constants.STATE_DB.CLEAN_REPOSTS_FROM_SPECIFIC_CHAT_STATE_DB);
        this.exitState(user.getId(), Constants.STATE_DB.PHONE_NUMBER_ENTERING_STATE_DB);
        this.exitState(user.getId(), Constants.STATE_DB.VERIFICATION_CODE_ENTERING_STATE_DB);

        clientManager.removeClientForUser(user.getId());
        this.bot().silent().execute(SendMessage.builder()
                .text(i18n.forLanguage(user.getLanguageCode()).getMsg("start.logout"))
                .chatId(chatId.toString())
                .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                .allowSendingWithoutReply(false)
                .build());
    }
}
