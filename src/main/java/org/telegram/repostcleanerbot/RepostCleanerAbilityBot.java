package org.telegram.repostcleanerbot;

import it.tdlight.jni.TdApi;
import lombok.extern.log4j.Log4j2;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Locality;
import org.telegram.abilitybots.api.objects.Privacy;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.factory.FlowFactory;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.repostcleanerbot.tdlib.ClientManager;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;
import org.telegram.repostcleanerbot.utils.I18nService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import javax.inject.Inject;
import javax.inject.Named;

import static org.telegram.repostcleanerbot.Constants.INLINE_BUTTONS;

@Log4j2
public class RepostCleanerAbilityBot extends AbilityBot {

    @Inject
    private BotContext botContext;

    @Inject
    private FlowFactory flowFactory;

    @Inject
    private I18nService i18n;

    @Inject
    private KeyboardFactory keyboardFactory;

    @Inject
    private ClientManager clientManager;

    private final long adminId;

    @Inject
    public RepostCleanerAbilityBot(@Named(Constants.BOT_TOKEN) String botToken, @Named(Constants.BOT_NAME) String botUsername, @Named(Constants.BOT_ADMIN_ID) String adminId) {
        super(botToken, botUsername);
        this.adminId = Long.parseLong(adminId);
    }

    public Ability processStart() {
        return Ability
                .builder()
                .name("start")
                .info("Start repost cleaner bot")
                .enableStats()
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> {
                            botContext.exitState(ctx.user().getId(), Constants.STATE_DB.TWO_STEP_VERIFICATION_PASSWORD_ENTERING_STATE_DB);
                            botContext.exitState(ctx.user().getId(), Constants.STATE_DB.CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB);
                            botContext.exitState(ctx.user().getId(), Constants.STATE_DB.CLEAN_REPOSTS_FROM_SPECIFIC_CHAT_STATE_DB);
                            botContext.exitState(ctx.user().getId(), Constants.STATE_DB.PHONE_NUMBER_ENTERING_STATE_DB);
                            botContext.exitState(ctx.user().getId(), Constants.STATE_DB.VERIFICATION_CODE_ENTERING_STATE_DB);
                            botContext.bot().silent().execute(SendMessage.builder()
                                    .text(i18n.forLanguage(ctx.user().getLanguageCode()).getMsg("start.greeting"))
                                    .chatId(ctx.chatId().toString())
                                    .replyMarkup(keyboardFactory.withOneLineButtons(
                                            InlineKeyboardButton.builder().text(i18n.forLanguage(ctx.user().getLanguageCode()).getMsg("start.login_btn")).callbackData(INLINE_BUTTONS.LOGIN).build()
                                    ))
                                    .allowSendingWithoutReply(false)
                                    .build());
                        }
                )
                .reply(flowFactory.getLoginFlow())
                .reply(flowFactory.getCleanRepostsFromAllChatsState())
                .reply(flowFactory.getCleanRepostsFromSpecificChatState())
                .reply(flowFactory.getTwoStepVerificationPasswordEnteringState())
                .reply(flowFactory.getPhoneNumberEnteringState())
                .reply(flowFactory.getVerificationCodeEnteringState())
                .build();
    }

    public Ability logoutAction() {
        return Ability
                .builder()
                .name("logout")
                .info("Clean authorization info from bot")
                .enableStats()
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(this::handleLogout)
                .build();
    }

    public Ability stopAction() {
        return Ability
                .builder()
                .name("stop")
                .info("Clean authorization info from bot")
                .enableStats()
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(this::handleLogout)
                .build();
    }

    private void handleLogout(org.telegram.abilitybots.api.objects.MessageContext ctx) {
        BotEmbadedTelegramClient client = clientManager.getTelegramClientForUser(ctx.user().getId());
        try {
            client.send(new TdApi.LogOut(), okResult -> {
                if(okResult.isError()) { //handler only any exception result. Success logging out is handled by "onLoggingOut" handler of Login.java
                    botContext.handleLogOut(ctx.user(), ctx.chatId());
                }
            });
        } catch (Exception e) {
            botContext.handleLogOut(ctx.user(), ctx.chatId());
        }
    }

    @Override
    public long creatorId() {
        return adminId;
    }
}
