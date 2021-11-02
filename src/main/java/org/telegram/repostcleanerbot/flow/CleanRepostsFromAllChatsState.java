package org.telegram.repostcleanerbot.flow;

import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.repostcleanerbot.CommonMap;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.State;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.repostcleanerbot.Constants.*;

public class CleanRepostsFromAllChatsState implements State {
    private final BotContext botContext;

    public CleanRepostsFromAllChatsState(BotContext botContext) {
        this.botContext = botContext;
    }

    @Override
    public Reply getReply() {
        return Reply.of((bot, upd) -> {
            String selectedChatToClean = upd.getMessage().getText();
            botContext.hidePreviousReplyMarkup(upd);
            try {
                if(selectedChatToClean.equals(INLINE_BUTTONS.FINISH_CLEANING)) {
                    botContext.exitState(upd, STATE_DB.CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB);
                    botContext.bot().sender().execute(
                            SendMessage.builder()
                                    .text("Ok, cleaning finished. You can /start again")
                                    .chatId(getChatId(upd).toString())
                                    .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                                    .allowSendingWithoutReply(false)
                                    .build());

                } else {
                    CommonMap.repostsFromAllChatsList.remove(selectedChatToClean);
                    botContext.bot().sender().execute(
                            SendMessage.builder()
                                    .text("Reposts from chat '" + selectedChatToClean + "' are cleaned.\nWould you like to clean more reposts?")
                                    .chatId(getChatId(upd).toString())
                                    .replyMarkup(KeyboardFactory.replyKeyboardButtons(CommonMap.repostsFromAllChatsList, "Select channel name"))
                                    .allowSendingWithoutReply(false)
                                    .build());
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }, botContext.itemFromReplyKeyboardSelected(CommonMap.repostsFromAllChatsList), botContext.isChatInState(STATE_DB.CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB));
    }
}
