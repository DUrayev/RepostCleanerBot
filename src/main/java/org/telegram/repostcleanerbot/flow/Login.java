package org.telegram.repostcleanerbot.flow;

import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.repostcleanerbot.Constants;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.Flow;
import org.telegram.repostcleanerbot.factory.FlowFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

public class Login implements Flow {

    private final BotContext botContext;

    public Login(BotContext botContext) {
        this.botContext = botContext;
    }

    @Override
    public ReplyFlow getFlow() {
        return ReplyFlow.builder(botContext.bot().db())
                .onlyIf(botContext.hasCallbackQuery(Constants.INLINE_BUTTONS.LOGIN))
                .action((bot, upd) -> {
                    try {
                        botContext.hidePreviousReplyMarkup(upd);
                        botContext.bot().sender().execute(
                                SendMessage.builder()
                                        .text("You're successfully logged in. Do you want to analyze all chats of specific chat? ")
                                        .chatId(getChatId(upd).toString())
                                        .replyMarkup(KeyboardFactory.withOneLineButtons(Constants.INLINE_BUTTONS.ALL_CHATS, Constants.INLINE_BUTTONS.SPECIFIC_CHAT))
                                        .allowSendingWithoutReply(false)
                                        .build());
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .next(FlowFactory.getAllChatsProcessingFlow(botContext))
                .next(FlowFactory.getSpecificChatProcessingFlow(botContext))
                .build();
    }
}
