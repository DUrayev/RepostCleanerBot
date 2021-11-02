package org.telegram.repostcleanerbot.flow;

import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.repostcleanerbot.CommonMap;
import org.telegram.repostcleanerbot.Constants;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.Flow;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.repostcleanerbot.Constants.*;

public class AllChatsProcessing implements Flow {
    private final BotContext botContext;

    public AllChatsProcessing(BotContext botContext) {
        this.botContext = botContext;
    }

    @Override
    public ReplyFlow getFlow() {
        Reply enterCleaningState = Reply.of((bot, upd) -> {
            try {
                botContext.hidePreviousReplyMarkup(upd);
                botContext.enterState(upd, STATE_DB.CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB);
                botContext.bot().sender().execute(
                        SendMessage.builder()
                                .text("Reposts from which channel you want to clean everywhere?")
                                .chatId(getChatId(upd).toString())
                                .replyMarkup(KeyboardFactory.replyKeyboardButtons(CommonMap.repostsFromAllChatsList, "Select channel name"))
                                .allowSendingWithoutReply(false)
                                .build());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }, botContext.hasCallbackQuery(Constants.INLINE_BUTTONS.START_CLEANING));

        Reply cancel = Reply.of((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);
                    botContext.bot().silent().send("Ok, process is canceled.\nYou can /start again", getChatId(upd));
                },
                botContext.hasCallbackQuery(Constants.INLINE_BUTTONS.CANCEL));

        return ReplyFlow.builder(botContext.bot().db())
                .onlyIf(botContext.hasCallbackQuery(Constants.INLINE_BUTTONS.ALL_CHATS))
                .action((bot, upd) -> {
                    try {
                        botContext.hidePreviousReplyMarkup(upd);
                        botContext.bot().sender().execute(
                                SendMessage.builder()
                                        .text("See the following statistic:\nDo you want to start reposts cleaning?")
                                        .chatId(getChatId(upd).toString())
                                        .replyMarkup(KeyboardFactory.withOneLineButtons(Constants.INLINE_BUTTONS.START_CLEANING, Constants.INLINE_BUTTONS.CANCEL))
                                        .allowSendingWithoutReply(false)
                                        .build());
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .next(enterCleaningState)
                .next(cancel)
                .build();
    }
}
