package org.telegram.repostcleanerbot;

import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.repostcleanerbot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.validation.constraints.NotNull;
import java.util.function.Predicate;

import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

public class RepostCleanerAbilityBotBackup extends AbilityBot {

    private final long adminId;

    public RepostCleanerAbilityBotBackup(String botToken, String botUsername, String adminId) {
        super(botToken, botUsername);
        this.adminId = Long.parseLong(adminId);
    }

    public Ability sayHelloWorld() {
        return Ability
                .builder()
                .name("hello")
                .info("hello info")
                .enableStats()
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> silent.send("Hello world!", ctx.chatId()))
                .build();
    }

    public Ability processStart() {
        return Ability
                .builder()
                .name("start")
                .info("start info")
                .enableStats()
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> {
                            try {
                                sender.execute(
                                        SendMessage.builder()
                                                .text("Select wake up or hello")
                                                .chatId(ctx.chatId().toString())
                                                .replyMarkup(KeyboardFactory.withOneLineButtons("wake up", "/hello"))
                                                .build());
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                )
                .build();
    }

    public ReplyFlow directionFlow() {
        Reply saidLeft = Reply.of((bot, upd) -> silent.send("Sir, I have gone left.", getChatId(upd)),
                hasCallbackQuery("go left or else"), Flag.CALLBACK_QUERY);

        ReplyFlow leftflow = ReplyFlow.builder(db)
                .action((bot, upd) -> {
                    try {
                        sender.execute(
                                SendMessage.builder()
                                        .text("I don't know how to go left.")
                                        .chatId(getChatId(upd).toString())
                                        .replyMarkup(KeyboardFactory.withOneLineButtons("go left or else", "..."))
                                        .build());
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .onlyIf(hasCallbackQuery("left"))
                .onlyIf(Flag.CALLBACK_QUERY)
                .next(saidLeft).build();

        Reply saidRight = Reply.of((bot, upd) -> silent.send("Sir, I have gone right.", getChatId(upd)),
                hasCallbackQuery("right"), Flag.CALLBACK_QUERY);

        return ReplyFlow.builder(db)
                .action((bot, upd) -> {
                    try {
                        sender.execute(
                                SendMessage.builder()
                                        .text("Command me to go left or right!")
                                        .chatId(getChatId(upd).toString())
                                        .replyMarkup(KeyboardFactory.withOneLineButtons("left", "right"))
                                        .build());
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .onlyIf(hasCallbackQuery("wake up"))
                .onlyIf(Flag.CALLBACK_QUERY)
                .next(leftflow)
                .next(saidRight)
                .build();
    }

    @NotNull
    private Predicate<Update> hasMessageWith(String msg) {
        return upd -> upd.getMessage().getText().equalsIgnoreCase(msg);
    }

    @NotNull
    private Predicate<Update> hasCallbackQuery(String msg) {
        return upd -> upd.getCallbackQuery().getData().equalsIgnoreCase(msg);
    }

    @Override
    public long creatorId() {
        return adminId;
    }
}
