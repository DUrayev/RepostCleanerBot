package org.telegram.repostcleanerbot;

import lombok.extern.log4j.Log4j2;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Locality;
import org.telegram.abilitybots.api.objects.Privacy;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.factory.FlowFactory;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
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

    @Override
    public long creatorId() {
        return adminId;
    }
}
