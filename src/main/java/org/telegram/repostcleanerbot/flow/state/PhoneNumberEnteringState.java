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

public class PhoneNumberEnteringState implements Flow {

    private static final int MINIMUM_PHONE_NUMBER_DIGITS_LENGTH = 4;

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
                .onlyIf(botContext.isChatInState(STATE_DB.PHONE_NUMBER_ENTERING_STATE_DB))
                .onlyIf(botContext.notStartCommand())
                .onlyIf(Flag.MESSAGE)
                .action((bot, upd) -> {
                    String phoneNumber = upd.getMessage().getText();
                    String formattedPhoneNumber = formatPhoneNumber(phoneNumber);
                    if(formattedPhoneNumber == null) {
                        botContext.bot().silent().send(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("login.invalid_phone_number"), getChatId(upd));
                    } else {
                        BotEmbadedTelegramClient client = clientManager.getTelegramClientForUser(getUser(upd).getId());

                        TdApi.PhoneNumberAuthenticationSettings phoneSettings = new TdApi.PhoneNumberAuthenticationSettings(false, false, false);
                        TdApi.SetAuthenticationPhoneNumber authenticationPhoneNumberRequest = new TdApi.SetAuthenticationPhoneNumber(formattedPhoneNumber, phoneSettings);
                        client.send(authenticationPhoneNumberRequest, response -> {
                            if(response.isError()) {
                                botContext.bot().silent().send(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("login.phone_number_server_error", response.getError().message), getChatId(upd));
                            } else {
                                botContext.exitState(upd, STATE_DB.PHONE_NUMBER_ENTERING_STATE_DB);
                                botContext.enterState(upd, STATE_DB.VERIFICATION_CODE_ENTERING_STATE_DB);
                                //Nothing to send in chat. The next step is handled by TdApi.AuthorizationStateWaitCode handler
                            }
                        });
                    }
                })
                .next(flowFactory.getVerificationCodeEnteringState())
                .build();
    }

    private String formatPhoneNumber(String phoneNumber) {
        String onlyDigitsPhoneNumber = phoneNumber.replaceAll("[\\D]", "");
        if(onlyDigitsPhoneNumber.length() < MINIMUM_PHONE_NUMBER_DIGITS_LENGTH) {
            return null;
        } else {
            try {
                return String.valueOf(Long.parseLong(onlyDigitsPhoneNumber));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
