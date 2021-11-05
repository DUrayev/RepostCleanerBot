package org.telegram.repostcleanerbot.flow;

import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.repostcleanerbot.CommonMap;
import org.telegram.repostcleanerbot.Constants;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.Flow;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

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
            botContext.hidePreviousReplyMarkup(upd);
            botContext.enterState(upd, STATE_DB.CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB);
            botContext.execute(
                    SendMessage.builder()
                            .text("Reposts from which channel you want to clean everywhere?")
                            .chatId(getChatId(upd).toString())
                            .replyMarkup(KeyboardFactory.replyKeyboardButtons(CommonMap.repostsFromAllChatsList, "Select channel name"))
                            .allowSendingWithoutReply(false)
                            .build());
        }, botContext.hasCallbackQuery(INLINE_BUTTONS.START_CLEANING));

        Reply cancel = Reply.of((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);
                    botContext.bot().silent().send("Ok, process is canceled.\nYou can /start again", getChatId(upd));
                },
                botContext.hasCallbackQuery(INLINE_BUTTONS.CANCEL));

        return ReplyFlow.builder(botContext.bot().db())
                .onlyIf(botContext.hasCallbackQuery(INLINE_BUTTONS.ALL_CHATS))
                .onlyIf(botContext.isChatNotInState(STATE_DB.LOGIN_STATE_DB))
                .action((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);
                    botContext.execute(
                            SendMessage.builder()
                                    .text("See the following statistic:\nDo you want to start reposts cleaning?")
                                    .chatId(getChatId(upd).toString())
                                    .replyMarkup(KeyboardFactory.withOneLineButtons(Constants.INLINE_BUTTONS.START_CLEANING, Constants.INLINE_BUTTONS.CANCEL))
                                    .allowSendingWithoutReply(false)
                                    .build());
                })
                .next(enterCleaningState)
                .next(cancel)
                .build();
    }
}
