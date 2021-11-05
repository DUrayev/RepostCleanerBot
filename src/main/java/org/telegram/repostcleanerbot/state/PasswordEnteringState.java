package org.telegram.repostcleanerbot.state;

import it.tdlight.jni.TdApi;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.Flow;
import org.telegram.repostcleanerbot.factory.FlowFactory;
import org.telegram.repostcleanerbot.tdlib.ClientManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.abilitybots.api.util.AbilityUtils.getUser;
import static org.telegram.repostcleanerbot.Constants.STATE_DB;

public class PasswordEnteringState implements Flow {
    private final BotContext botContext;

    public PasswordEnteringState(BotContext botContext) {
        this.botContext = botContext;
    }

    @Override
    public ReplyFlow getFlow() {
        return ReplyFlow.builder(botContext.bot().db())
                .onlyIf(botContext.isChatInState(STATE_DB.PASSWORD_ENTERING_STATE_DB))
                .onlyIf(botContext.notStartCommand())
                .onlyIf(Flag.MESSAGE)
                .action((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);
                    String password = upd.getMessage().getText();
                    BotEmbadedTelegramClient client = ClientManager.getInstance().getTelegramClientForUser(getUser(upd).getId(), botContext.getTdLibSettings());

                    TdApi.CheckAuthenticationPassword response = new TdApi.CheckAuthenticationPassword(password);
                    client.send(response, ok -> {
                        if (ok.isError()) {
                            botContext.bot().silent().send("Incorrect password. Please, try again", getChatId(upd));
                        } else {
                            botContext.exitState(upd, STATE_DB.PASSWORD_ENTERING_STATE_DB);
                            //Nothing to send in chat. The next step is handled by TdApi.AuthorizationStateReady handler
                        }
                    });
                })
                .next(FlowFactory.getAllChatsProcessingFlow(botContext))
                .next(FlowFactory.getSpecificChatProcessingFlow(botContext))
                .build();
    }
}
