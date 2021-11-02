package org.telegram.repostcleanerbot;

import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.repostcleanerbot.bot.BotContext;
import org.telegram.repostcleanerbot.factory.FlowFactory;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.telegram.repostcleanerbot.Constants.INLINE_BUTTONS;

public class RepostCleanerAbilityBot extends AbilityBot {
    private final BotContext botContext;

    private final long adminId;
    private final String apiId;
    private final String apiHash;

    public RepostCleanerAbilityBot(String botToken, String botUsername, String adminId, String apiId, String apiHash) {
        super(botToken, botUsername);
        this.adminId = Long.parseLong(adminId);
        this.apiId = apiId;
        this.apiHash = apiHash;
        botContext = new BotContext(this);
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
                            try {
                                sender.execute(
                                        SendMessage.builder()
                                                .text("You need to login")
                                                .chatId(ctx.chatId().toString())
                                                .replyMarkup(KeyboardFactory.withOneLineButtons(INLINE_BUTTONS.LOGIN))
                                                .allowSendingWithoutReply(false)
                                                .build());
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                )
                .reply(FlowFactory.getLoginFlow(botContext))
                .reply(FlowFactory.getCleanRepostsFromAllChatsState(botContext))
                .reply(FlowFactory.getCleanRepostsFromSpecificChatState(botContext))
                .build();
    }

    @Override
    public long creatorId() {
        return adminId;
    }
}
