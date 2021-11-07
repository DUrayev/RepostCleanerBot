package org.telegram.repostcleanerbot.flow.state;

import it.tdlight.jni.TdApi;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.Flow;
import org.telegram.repostcleanerbot.factory.FlowFactory;
import org.telegram.repostcleanerbot.tdlib.ClientManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;

import javax.inject.Inject;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.abilitybots.api.util.AbilityUtils.getUser;
import static org.telegram.repostcleanerbot.Constants.STATE_DB;

public class PasswordEnteringState implements Flow {

    @Inject
    private BotContext botContext;

    @Inject
    private FlowFactory flowFactory;

    @Inject
    private ClientManager clientManager;

    @Override
    public ReplyFlow getFlow() {
        return ReplyFlow.builder(botContext.bot().db())
                .onlyIf(botContext.isChatInState(STATE_DB.PASSWORD_ENTERING_STATE_DB))
                .onlyIf(botContext.notStartCommand())
                .onlyIf(Flag.MESSAGE)
                .action((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);
                    String password = upd.getMessage().getText();
                    BotEmbadedTelegramClient client = clientManager.getTelegramClientForUser(getUser(upd).getId());

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
                .next(flowFactory.getAllChatsProcessingFlow())
                .next(flowFactory.getSpecificChatProcessingFlow())
                .build();
    }
}
