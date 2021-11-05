package org.telegram.repostcleanerbot;

import it.tdlight.client.TDLibSettings;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.factory.FlowFactory;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.telegram.repostcleanerbot.Constants.INLINE_BUTTONS;

public class RepostCleanerAbilityBot extends AbilityBot {

    private final BotContext botContext;
    private final long adminId;
    private final TDLibSettings tdLibSettings;

    public RepostCleanerAbilityBot(String botToken, String botUsername, String adminId, TDLibSettings tdLibSettings) {
        super(botToken, botUsername);
        this.adminId = Long.parseLong(adminId);
        this.tdLibSettings = tdLibSettings;
        botContext = new BotContext(this, tdLibSettings);
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
                            botContext.execute(SendMessage.builder()
                                    .text("You need to login")
                                    .chatId(ctx.chatId().toString())
                                    .replyMarkup(KeyboardFactory.withOneLineButtons(INLINE_BUTTONS.LOGIN))
                                    .allowSendingWithoutReply(false)
                                    .build());
                        }
                )
                .reply(FlowFactory.getLoginFlow(botContext))
                .reply(FlowFactory.getCleanRepostsFromAllChatsState(botContext))
                .reply(FlowFactory.getCleanRepostsFromSpecificChatState(botContext))
                .reply(FlowFactory.getPasswordEnteringState(botContext))
                .build();
    }

    @Override
    public long creatorId() {
        return adminId;
    }
}
