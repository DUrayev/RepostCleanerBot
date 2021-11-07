package org.telegram.repostcleanerbot;

import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Locality;
import org.telegram.abilitybots.api.objects.Privacy;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.factory.FlowFactory;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import javax.inject.Inject;
import javax.inject.Named;

import static org.telegram.repostcleanerbot.Constants.INLINE_BUTTONS;

public class RepostCleanerAbilityBot extends AbilityBot {

    @Inject
    private BotContext botContext;

    @Inject
    private FlowFactory flowFactory;

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
                            botContext.exitState(ctx.user().getId(), Constants.STATE_DB.PASSWORD_ENTERING_STATE_DB);
                            botContext.exitState(ctx.user().getId(), Constants.STATE_DB.CLEAN_REPOSTS_FROM_ALL_CHATS_STATE_DB);
                            botContext.exitState(ctx.user().getId(), Constants.STATE_DB.CLEAN_REPOSTS_FROM_SPECIFIC_CHAT_STATE_DB);
                            botContext.execute(SendMessage.builder()
                                    .text("You need to login")
                                    .chatId(ctx.chatId().toString())
                                    .replyMarkup(KeyboardFactory.withOneLineButtons(INLINE_BUTTONS.LOGIN))
                                    .allowSendingWithoutReply(false)
                                    .build());
                        }
                )
                .reply(flowFactory.getLoginFlow())
                .reply(flowFactory.getCleanRepostsFromAllChatsState())
                .reply(flowFactory.getCleanRepostsFromSpecificChatState())
                .reply(flowFactory.getPasswordEnteringState())
                .build();
    }

    @Override
    public long creatorId() {
        return adminId;
    }
}
