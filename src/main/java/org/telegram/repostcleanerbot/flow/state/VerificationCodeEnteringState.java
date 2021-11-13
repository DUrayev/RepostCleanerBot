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

public class VerificationCodeEnteringState implements Flow {

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
                .onlyIf(botContext.isChatInState(STATE_DB.VERIFICATION_CODE_ENTERING_STATE_DB))
                .onlyIf(botContext.notStartCommand())
                .onlyIf(Flag.MESSAGE)
                .action((bot, upd) -> {
                    String verificationCode = upd.getMessage().getText();
                    if(verificationCode.length() > 1) {
                        verificationCode = verificationCode.substring(1); //skip first fake digit from code which is supposed to be added by user, to avoid telegram security of expiring shared codes
                    }
                    BotEmbadedTelegramClient client = clientManager.getTelegramClientForUser(getUser(upd).getId());

                    TdApi.CheckAuthenticationCode checkAuthenticationCodeRequest = new TdApi.CheckAuthenticationCode(verificationCode);
                    client.send(checkAuthenticationCodeRequest, response -> {
                        if(response.isError()) {
                            botContext.bot().silent().send(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("login.verification_code_server_error", response.getError().message), getChatId(upd));
                        } else {
                            botContext.exitState(upd, STATE_DB.VERIFICATION_CODE_ENTERING_STATE_DB);
                            //Nothing to send in chat.
                            //The next step is handled by TdApi.AuthorizationStateReady handler
                            //or by TdApi.AuthorizationStateWaitPassword handler in case of two step verification enabled
                        }
                    });
                })
                .next(flowFactory.getAllChatsProcessingFlow())
                .next(flowFactory.getSpecificChatProcessingFlow())
                .build();
    }
}
