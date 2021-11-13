package org.telegram.repostcleanerbot.flow;

import com.google.zxing.WriterException;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import lombok.extern.log4j.Log4j2;
import org.telegram.abilitybots.api.objects.ReplyFlow;
import org.telegram.repostcleanerbot.Constants;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.bot.Flow;
import org.telegram.repostcleanerbot.factory.FlowFactory;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.repostcleanerbot.tdlib.ClientManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;
import org.telegram.repostcleanerbot.tdlib.client.BotInteractiveAuthenticationData;
import org.telegram.repostcleanerbot.utils.I18nService;
import org.telegram.repostcleanerbot.utils.QrCodeUtil;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.abilitybots.api.util.AbilityUtils.getUser;
import static org.telegram.repostcleanerbot.Constants.*;

@Log4j2
public class Login implements Flow {

    @Inject
    private BotContext botContext;

    @Inject
    private FlowFactory flowFactory;

    @Inject
    private ClientManager clientManager;

    @Inject
    private I18nService i18n;

    @Inject
    private KeyboardFactory keyboardFactory;

    @Override
    public ReplyFlow getFlow() {
        return ReplyFlow.builder(botContext.bot().db())
                .onlyIf(botContext.hasCallbackQuery(Constants.INLINE_BUTTONS.LOGIN))
                .action((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);
                    botContext.enterState(upd, STATE_DB.LOGIN_STATE_DB);
                    botContext.exitState(upd, STATE_DB.TWO_STEP_VERIFICATION_PASSWORD_ENTERING_STATE_DB);
                    BotEmbadedTelegramClient client = clientManager.getTelegramClientForUser(getUser(upd).getId());
                    if(client.isClientInitialized()) {
                        client.send(new TdApi.GetAuthorizationState(), (result) -> {
                            client.handleUpdate(new TdApi.UpdateAuthorizationState(result.get()));
                        });
                    } else {
                        AuthenticationData authenticationData = new BotInteractiveAuthenticationData(botContext, getChatId(upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onWaitTdlibParameters(client, clientUpdate, upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onWaitEncryptionKey(client, clientUpdate, upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onWaitAuthenticationData(client, clientUpdate, upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onWaitVerificationCode(client, clientUpdate, upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onWaitOtherDeviceConfirmation(client, clientUpdate, upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onWaitTwoStepVerificationPassword(client, clientUpdate, upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onSuccessAuthorization(client, clientUpdate, upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onLoggingOut(client, clientUpdate, upd));
                        client.start(authenticationData);
                    }
                })
                .next(flowFactory.getAllChatsProcessingFlow())
                .next(flowFactory.getSpecificChatProcessingFlow())
                .next(phoneNumberOfQrCodeSelectionFlow())
                .build();
    }

    private void onWaitTdlibParameters(BotEmbadedTelegramClient client, TdApi.UpdateAuthorizationState clientUpdate, Update botUpdate) {
        if (clientUpdate.authorizationState.getConstructor() == TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR) {
            TdApi.TdlibParameters params = new TdApi.TdlibParameters();
            params.useTestDc = client.getSettings().isUsingTestDatacenter();
            params.databaseDirectory = client.getSettings().getDatabaseDirectoryPath().toString();
            params.filesDirectory = client.getSettings().getDownloadedFilesDirectoryPath().toString();
            params.useFileDatabase = client.getSettings().isFileDatabaseEnabled();
            params.useChatInfoDatabase = client.getSettings().isChatInfoDatabaseEnabled();
            params.useMessageDatabase = client.getSettings().isMessageDatabaseEnabled();
            params.useSecretChats = false;
            params.apiId = client.getSettings().getApiToken().getApiID();
            params.apiHash = client.getSettings().getApiToken().getApiHash();
            params.systemLanguageCode = client.getSettings().getSystemLanguageCode();
            params.deviceModel = client.getSettings().getDeviceModel();
            params.systemVersion = client.getSettings().getSystemVersion();
            params.applicationVersion = client.getSettings().getApplicationVersion();
            params.enableStorageOptimizer = client.getSettings().isStorageOptimizerEnabled();
            params.ignoreFileNames = client.getSettings().isIgnoreFileNames();
            client.send(new TdApi.SetTdlibParameters(params), ok -> {
                if (ok.isError()) {
                    throw new TelegramError(ok.getError());
                }
            });
        }
    }

    private void onWaitEncryptionKey(BotEmbadedTelegramClient client, TdApi.UpdateAuthorizationState clientUpdate, Update botUpdate) {
        if (clientUpdate.authorizationState.getConstructor() == TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR) {
            client.send(new TdApi.CheckDatabaseEncryptionKey(), ok -> {
                if (ok.isError()) {
                    throw new TelegramError(ok.getError());
                }
            });
        }
    }

    private void onWaitAuthenticationData(BotEmbadedTelegramClient client, TdApi.UpdateAuthorizationState clientUpdate, Update botUpdate) {
        if (clientUpdate.authorizationState.getConstructor() == TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR) {

            List<InlineKeyboardButton> loginMethods = new ArrayList<>();
            loginMethods.add(InlineKeyboardButton.builder().text(i18n.forLanguage(getUser(botUpdate).getLanguageCode()).getMsg("login.qr_code_btn")).callbackData(INLINE_BUTTONS.QR_CODE_LOGIN).build());
            if(botContext.isAdmin(getUser(botUpdate).getId())) {
                loginMethods.add(InlineKeyboardButton.builder().text(i18n.forLanguage(getUser(botUpdate).getLanguageCode()).getMsg("login.phone_number_btn")).callbackData(INLINE_BUTTONS.PHONE_NUMBER_LOGIN).build());
            }

            botContext.bot().silent().execute(SendMessage.builder()
                    .text(i18n.forLanguage(getUser(botUpdate).getLanguageCode()).getMsg("login.login_method_type_request"))
                    .chatId(getChatId(botUpdate).toString())
                    .replyMarkup(keyboardFactory.withOneLineButtons(
                            loginMethods.toArray(new InlineKeyboardButton[0])
                    ))
                    .allowSendingWithoutReply(false)
                    .build());
        }
    }

    private ReplyFlow phoneNumberOfQrCodeSelectionFlow() {
        return ReplyFlow.builder(botContext.bot().db())
                .onlyIf(botContext.hasCallbackQuery(INLINE_BUTTONS.PHONE_NUMBER_LOGIN, INLINE_BUTTONS.QR_CODE_LOGIN))
                .onlyIf(botContext.isChatInState(STATE_DB.LOGIN_STATE_DB))
                .action((bot, upd) -> {
                    botContext.hidePreviousReplyMarkup(upd);
                    BotEmbadedTelegramClient client = clientManager.getTelegramClientForUser(getUser(upd).getId());

                    switch (upd.getCallbackQuery().getData()) {
                        case INLINE_BUTTONS.PHONE_NUMBER_LOGIN:
                            botContext.enterState(upd, STATE_DB.PHONE_NUMBER_ENTERING_STATE_DB);
                            botContext.bot().silent().send(i18n.forLanguage(getUser(upd).getLanguageCode()).getMsg("login.enter_phone_number_request"), getChatId(upd));
                            break;
                        case INLINE_BUTTONS.QR_CODE_LOGIN:
                            TdApi.RequestQrCodeAuthentication requestQrCodeAuthentication = new TdApi.RequestQrCodeAuthentication();
                            client.send(requestQrCodeAuthentication, ok -> {
                                if (ok.isError()) {
                                    throw new TelegramError(ok.getError());
                                }
                            });
                            break;
                    }
                })
                .next(flowFactory.getPhoneNumberEnteringState())
                .next(flowFactory.getAllChatsProcessingFlow())
                .next(flowFactory.getSpecificChatProcessingFlow())
                .build();
    }

    private void onWaitVerificationCode(BotEmbadedTelegramClient client, TdApi.UpdateAuthorizationState clientUpdate, Update botUpdate) {
        if (clientUpdate.authorizationState.getConstructor() == TdApi.AuthorizationStateWaitCode.CONSTRUCTOR) {
            TdApi.AuthorizationStateWaitCode authorizationState = (TdApi.AuthorizationStateWaitCode) clientUpdate.authorizationState;
            ParameterInfoCode codeInfo = new ParameterInfoCode(
                    authorizationState.codeInfo.phoneNumber,
                    authorizationState.codeInfo.nextType,
                    authorizationState.codeInfo.timeout,
                    authorizationState.codeInfo.type
            );
            botContext.enterState(botUpdate, STATE_DB.VERIFICATION_CODE_ENTERING_STATE_DB);

            String askVerificationCodeMessage = i18n.forLanguage(getUser(botUpdate).getLanguageCode()).getMsg("login.verification_code_request",
                    codeInfo.getPhoneNumber(),
                    codeInfo.getTimeout(),
                    codeInfo.getType().getClass().getSimpleName().replace("AuthenticationCodeType", ""),
                    codeInfo.getNextType().getClass() != null ? codeInfo.getNextType().getClass().getSimpleName().replace("AuthenticationCodeType", "") : "");

            botContext.bot().silent().send(askVerificationCodeMessage, getChatId(botUpdate));
        }
    }

    private void onWaitOtherDeviceConfirmation(BotEmbadedTelegramClient client, TdApi.UpdateAuthorizationState clientUpdate, Update botUpdate) {
        if (clientUpdate.authorizationState.getConstructor() == TdApi.AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR) {
            TdApi.AuthorizationStateWaitOtherDeviceConfirmation authorizationState = (TdApi.AuthorizationStateWaitOtherDeviceConfirmation) clientUpdate.authorizationState;
            ParameterInfoNotifyLink parameterInfo = new ParameterInfoNotifyLink(authorizationState.link);
            String confirmationOnOtherDeviceLink = parameterInfo.getLink();
            try {
                InputStream qrCodeImageInputStream = QrCodeUtil.generateImage(confirmationOnOtherDeviceLink, AUTHENTICATION_QR_CODE_IMAGE_SIZE);
                botContext.bot().silent().send(i18n.forLanguage(getUser(botUpdate).getLanguageCode()).getMsg("login.confirm_other_device_link", confirmationOnOtherDeviceLink), getChatId(botUpdate));
                botContext.bot().sender().sendPhoto(SendPhoto.builder()
                        .chatId(getChatId(botUpdate).toString())
                        .photo(new InputFile(qrCodeImageInputStream, AUTHENTICATION_QR_CODE_IMAGE_NAME))
                        .build()
                );
            } catch (WriterException | IOException | TelegramApiException ex) {
                throw new IllegalStateException("Can't encode QR code", ex);
            }
        }
    }

    private void onWaitTwoStepVerificationPassword(BotEmbadedTelegramClient client, TdApi.UpdateAuthorizationState clientUpdate, Update botUpdate) {
        if (clientUpdate.authorizationState.getConstructor() == TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR) {
            TdApi.AuthorizationStateWaitPassword authorizationState = (TdApi.AuthorizationStateWaitPassword) clientUpdate.authorizationState;
            ParameterInfoPasswordHint parameterInfo = new ParameterInfoPasswordHint(authorizationState.passwordHint,
                    authorizationState.hasRecoveryEmailAddress,
                    authorizationState.recoveryEmailAddressPattern
            );
            botContext.enterState(botUpdate, STATE_DB.TWO_STEP_VERIFICATION_PASSWORD_ENTERING_STATE_DB);
            String hint = parameterInfo.getHint();
            boolean hasRecoveryEmailAddress = parameterInfo.hasRecoveryEmailAddress();
            String recoveryEmailAddressPattern = parameterInfo.getRecoveryEmailAddressPattern();

            String passwordMessage = i18n.forLanguage(getUser(botUpdate).getLanguageCode()).getMsg("login.two_step_verification_password_request",
                    hint != null && !hint.isEmpty() ? hint : "",
                    hasRecoveryEmailAddress,
                    recoveryEmailAddressPattern != null && !recoveryEmailAddressPattern.isEmpty() ? recoveryEmailAddressPattern : "");
            botContext.bot().silent().send(passwordMessage, getChatId(botUpdate));
        }
    }

    private void onSuccessAuthorization(BotEmbadedTelegramClient client, TdApi.UpdateAuthorizationState clientUpdate, Update botUpdate) {
        TdApi.AuthorizationState authorizationState = clientUpdate.authorizationState;
        if (authorizationState instanceof TdApi.AuthorizationStateReady) {

            List<InlineKeyboardButton> processingFlowTypes = new ArrayList<>();
            processingFlowTypes.add(InlineKeyboardButton.builder().text(i18n.forLanguage(getUser(botUpdate).getLanguageCode()).getMsg("login.specific_chat_flow_btn")).callbackData(INLINE_BUTTONS.SPECIFIC_CHAT).build());
            if(botContext.isAdmin(getUser(botUpdate).getId())) {
                processingFlowTypes.add(InlineKeyboardButton.builder().text(i18n.forLanguage(getUser(botUpdate).getLanguageCode()).getMsg("login.all_chats_flow_btn")).callbackData(INLINE_BUTTONS.ALL_CHATS).build());
            }

            botContext.hidePreviousReplyMarkup(botUpdate);
            botContext.exitState(botUpdate, STATE_DB.LOGIN_STATE_DB);
            botContext.bot().silent().execute(SendMessage.builder()
                            .text(i18n.forLanguage(getUser(botUpdate).getLanguageCode()).getMsg("login.successful_login_ask_next_flow"))
                            .chatId(getChatId(botUpdate).toString())
                            .replyMarkup(keyboardFactory.withOneLineButtons(
                                processingFlowTypes.toArray(new InlineKeyboardButton[0])
                            ))
                            .allowSendingWithoutReply(false)
                            .build());
        }
    }

    private void onLoggingOut(BotEmbadedTelegramClient client, TdApi.UpdateAuthorizationState clientUpdate, Update botUpdate) {
        if (clientUpdate.authorizationState.getConstructor() == TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR) {
            botContext.handleLogOut(getUser(botUpdate), getChatId(botUpdate));
        }
    }
}
