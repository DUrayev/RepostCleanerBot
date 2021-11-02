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

public class SpecificChatProcessing implements Flow {
    private final BotContext botContext;

    public SpecificChatProcessing(BotContext botContext) {
        this.botContext = botContext;
    }

    @Override
    public ReplyFlow getFlow() {
        ReplyFlow chatToCleanSelectionFlow = ReplyFlow.builder(botContext.bot().db())
                .onlyIf(botContext.itemFromReplyKeyboardSelected(CommonMap.myChats))
                .action((bot, upd) -> {
                    try {
                        botContext.hidePreviousReplyMarkup(upd);
                        String selectedChatToClean = upd.getMessage().getText();
                        botContext.enterState(upd, Constants.STATE_DB.CLEAN_REPOSTS_FROM_SPECIFIC_CHAT_STATE_DB);
                        botContext.bot().sender().execute(
                                SendMessage.builder()
                                        .text("The following statistic for chat " + selectedChatToClean + ":\nReposts from which channel you want to clean in " + selectedChatToClean + "?")
                                        .chatId(getChatId(upd).toString())
                                        .replyMarkup(KeyboardFactory.replyKeyboardButtons(CommonMap.repostsFromSpecificChatList, "Select channel name"))
                                        .allowSendingWithoutReply(false)
                                        .build());
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .build();

        Reply cancel = Reply.of((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);
                    botContext.bot().silent().send("Ok, process is canceled.\nYou can /start again", getChatId(upd));
                },
                botContext.hasCallbackQuery(Constants.INLINE_BUTTONS.CANCEL));

        return ReplyFlow.builder(botContext.bot().db())
                .onlyIf(botContext.hasCallbackQuery(Constants.INLINE_BUTTONS.SPECIFIC_CHAT))
                .action((bot, upd) -> {
                    try {
                        botContext.hidePreviousReplyMarkup(upd);
                        botContext.bot().sender().execute(
                                SendMessage.builder()
                                        .text("Select the chat where you can send messages.\nWhich one do you want to analyze and clean?")
                                        .chatId(getChatId(upd).toString())
                                        .replyMarkup(KeyboardFactory.replyKeyboardButtons(CommonMap.myChats, "Select channel name"))
                                        .allowSendingWithoutReply(false)
                                        .build());
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .next(chatToCleanSelectionFlow)
                .next(cancel)
                .build();
    }
}
