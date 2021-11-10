package org.telegram.repostcleanerbot.flow.state;

import it.tdlight.jni.TdApi;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.Flow;
import org.telegram.repostcleanerbot.factory.FlowFactory;
import org.telegram.repostcleanerbot.tdlib.ClientManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;
import org.telegram.repostcleanerbot.utils.I18nService;

import javax.inject.Inject;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.abilitybots.api.util.AbilityUtils.getUser;
import static org.telegram.repostcleanerbot.Constants.STATE_DB;

public class TwoStepVerificationPasswordEnteringState implements Flow {

    @Inject
    private BotContext botContext;

    @Inject
    private FlowFactory flowFactory;

    @Inject
    private ClientManager clientManager;

    @Inject
    private I18nService i18n;

    @Override
    public ReplyFlow getFlow() {
        return ReplyFlow.builder(botContext.bot().db())
                .onlyIf(botContext.isChatInState(STATE_DB.TWO_STEP_VERIFICATION_PASSWORD_ENTERING_STATE_DB))
                .onlyIf(botContext.notStartCommand())
                .onlyIf(Flag.MESSAGE)
                .action((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);
                    String password = upd.getMessage().getText();
                    BotEmbadedTelegramClient client = clientManager.getTelegramClientForUser(getUser(upd).getId());

                    TdApi.CheckAuthenticationPassword response = new TdApi.CheckAuthenticationPassword(password);
                    client.send(response, ok -> {
                        if (ok.isError()) {
                            botContext.bot().silent().send(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("login.incorrect_two_step_verification_password"), getChatId(upd));
                        } else {
                            botContext.exitState(upd, STATE_DB.TWO_STEP_VERIFICATION_PASSWORD_ENTERING_STATE_DB);
                            //Nothing to send in chat. The next step is handled by TdApi.AuthorizationStateReady handler
                        }
                    });
                })
                .next(flowFactory.getAllChatsProcessingFlow())
                .next(flowFactory.getSpecificChatProcessingFlow())
                .build();
    }
}
