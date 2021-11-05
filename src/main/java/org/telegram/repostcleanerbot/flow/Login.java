package org.telegram.repostcleanerbot.flow;

import com.google.zxing.WriterException;
import it.tdlight.client.AuthenticationData;
import it.tdlight.client.ParameterInfoNotifyLink;
import it.tdlight.client.ParameterInfoPasswordHint;
import it.tdlight.client.TelegramError;
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
import org.telegram.repostcleanerbot.utils.QrCodeUtil;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;
import static org.telegram.abilitybots.api.util.AbilityUtils.getUser;
import static org.telegram.repostcleanerbot.Constants.*;

@Log4j2
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
                    botContext.hidePreviousReplyMarkup(upd);
                    botContext.enterState(upd, STATE_DB.LOGIN_STATE_DB);
                    botContext.exitState(upd, STATE_DB.PASSWORD_ENTERING_STATE_DB);
                    BotEmbadedTelegramClient client = ClientManager.getInstance().getTelegramClientForUser(getUser(upd).getId(), botContext.getTdLibSettings());
                    if(client.isClientInitialized()) {
                        client.send(new TdApi.GetAuthorizationState(), (result) -> {
                            client.handleUpdate(new TdApi.UpdateAuthorizationState(result.get()));
                        });
                    } else {
                        AuthenticationData authenticationData = new BotInteractiveAuthenticationData(botContext, getChatId(upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onWaitTdlibParameters(client, clientUpdate, upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onWaitEncryptionKey(client, clientUpdate, upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onWaitAuthenticationData(client, clientUpdate, upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onWaitOtherDeviceConfirmation(client, clientUpdate, upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onWaitPassword(client, clientUpdate, upd));
                        client.addUpdateHandler(TdApi.UpdateAuthorizationState.class, clientUpdate -> onSuccessAuthorization(client, clientUpdate, upd));
                        client.start(authenticationData);
                    }
                })
                .next(FlowFactory.getAllChatsProcessingFlow(botContext))
                .next(FlowFactory.getSpecificChatProcessingFlow(botContext))
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
            botContext.execute(SendMessage.builder()
                    .text("Do you want to login using a phone number or a qr code?")
                    .chatId(getChatId(botUpdate).toString())
                    .replyMarkup(KeyboardFactory.withOneLineButtons(Constants.INLINE_BUTTONS.PHONE_NUMBER_LOGIN, Constants.INLINE_BUTTONS.QR_CODE_LOGIN))
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
                    BotEmbadedTelegramClient client = ClientManager.getInstance().getTelegramClientForUser(getUser(upd).getId(), botContext.getTdLibSettings());

                    switch (upd.getCallbackQuery().getData()) {
                        case INLINE_BUTTONS.PHONE_NUMBER_LOGIN:
                            break;
                        case INLINE_BUTTONS.QR_CODE_LOGIN:
                            TdApi.RequestQrCodeAuthentication response = new TdApi.RequestQrCodeAuthentication();
                            client.send(response, ok -> {
                                if (ok.isError()) {
                                    throw new TelegramError(ok.getError());
                                }
                            });
                            break;
                    }
                })
                .next(FlowFactory.getAllChatsProcessingFlow(botContext))
                .next(FlowFactory.getSpecificChatProcessingFlow(botContext))
                .build();
    }


    private void onWaitOtherDeviceConfirmation(BotEmbadedTelegramClient client, TdApi.UpdateAuthorizationState clientUpdate, Update botUpdate) {
        if (clientUpdate.authorizationState.getConstructor() == TdApi.AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR) {
            TdApi.AuthorizationStateWaitOtherDeviceConfirmation authorizationState = (TdApi.AuthorizationStateWaitOtherDeviceConfirmation) clientUpdate.authorizationState;
            ParameterInfoNotifyLink parameterInfo = new ParameterInfoNotifyLink(authorizationState.link);
            String confirmationOnOtherDeviceLink = parameterInfo.getLink();
            try {
                InputStream qrCodeImageInputStream = QrCodeUtil.generateImage(confirmationOnOtherDeviceLink, AUTHENTICATION_QR_CODE_IMAGE_SIZE);
                botContext.bot().silent().send("Confirm the following link on other device: " + confirmationOnOtherDeviceLink, getChatId(botUpdate));
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

    private void onWaitPassword(BotEmbadedTelegramClient client, TdApi.UpdateAuthorizationState clientUpdate, Update botUpdate) {
        if (clientUpdate.authorizationState.getConstructor() == TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR) {
            TdApi.AuthorizationStateWaitPassword authorizationState = (TdApi.AuthorizationStateWaitPassword) clientUpdate.authorizationState;
            ParameterInfoPasswordHint parameterInfo = new ParameterInfoPasswordHint(authorizationState.passwordHint,
                    authorizationState.hasRecoveryEmailAddress,
                    authorizationState.recoveryEmailAddressPattern
            );
            botContext.enterState(botUpdate, STATE_DB.PASSWORD_ENTERING_STATE_DB);
            String passwordMessage = "Password authorization:";
            String hint = parameterInfo.getHint();
            if (hint != null && !hint.isEmpty()) {
                passwordMessage += "\n\tHint: " + hint;
            }
            boolean hasRecoveryEmailAddress = parameterInfo.hasRecoveryEmailAddress();
            passwordMessage += "\n\tHas recovery email: " + hasRecoveryEmailAddress;
            String recoveryEmailAddressPattern = parameterInfo.getRecoveryEmailAddressPattern();
            if (recoveryEmailAddressPattern != null && !recoveryEmailAddressPattern.isEmpty()) {
                passwordMessage += "\n\tRecovery email address pattern: " + recoveryEmailAddressPattern;
            }
            passwordMessage += "\nEnter your password";
            botContext.bot().silent().send(passwordMessage, getChatId(botUpdate));
        }
    }

    private void onSuccessAuthorization(BotEmbadedTelegramClient client, TdApi.UpdateAuthorizationState clientUpdate, Update botUpdate) {
        TdApi.AuthorizationState authorizationState = clientUpdate.authorizationState;
        if (authorizationState instanceof TdApi.AuthorizationStateReady) {
            botContext.hidePreviousReplyMarkup(botUpdate);
            botContext.exitState(botUpdate, STATE_DB.LOGIN_STATE_DB);
            botContext.execute(SendMessage.builder()
                            .text("You're successfully logged in. Do you want to analyze all chats or specific chat? ")
                            .chatId(getChatId(botUpdate).toString())
                            .replyMarkup(KeyboardFactory.withOneLineButtons(Constants.INLINE_BUTTONS.SPECIFIC_CHAT, Constants.INLINE_BUTTONS.ALL_CHATS))
                            .allowSendingWithoutReply(false)
                            .build());
        }
    }
}
