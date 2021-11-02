package org.telegram.repostcleanerbot.bot;

import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.abilitybots.api.util.AbilityUtils.getUser;

public class BotContext {

    private final BaseAbilityBot bot;

    public BotContext(BaseAbilityBot bot) {
       this.bot = bot;
    }

    public Predicate<Update> isChatInState(String stateDbName) {
        return upd -> {
            Map<String, Boolean> chatCleaningStateDb = bot.db().getMap(stateDbName);
            return chatCleaningStateDb.get(getUser(upd).getId().toString());
        };
    }

    public void enterState(Update upd, String stateDbName) {
        Map<String, Boolean> chatCleaningStateDb = bot.db().getMap(stateDbName);
        chatCleaningStateDb.put(getUser(upd).getId().toString(), true);
    }

    public void exitState(Update upd, String stateDbName) {
        Map<String, Boolean> chatCleaningStateDb = bot.db().getMap(stateDbName);
        chatCleaningStateDb.put(getUser(upd).getId().toString(), false);
    }

    public void hidePreviousReplyMarkup(Update upd) {
        if(upd.hasCallbackQuery()) {
            try {
                bot.sender().execute(EditMessageReplyMarkup.builder()
                        .chatId(getChatId(upd).toString())
                        .messageId(upd.getCallbackQuery().getMessage().getMessageId())
                        .replyMarkup(null)
                        .build());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }


    @NotNull
    public Predicate<Update> hasCallbackQuery(String msg) {
        return upd -> upd.hasCallbackQuery() && upd.getCallbackQuery().getData().equalsIgnoreCase(msg);
    }

    public BaseAbilityBot bot() {
        return bot;
    }

    public Predicate<Update> itemFromReplyKeyboardSelected(List<String> replyItems) {
        return upd -> upd.hasMessage() && replyItems.contains(upd.getMessage().getText());
    }
}
