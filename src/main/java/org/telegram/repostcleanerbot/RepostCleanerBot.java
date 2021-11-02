package org.telegram.repostcleanerbot;

import org.telegram.repostcleanerbot.command.StartCommand;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class RepostCleanerBot extends TelegramLongPollingCommandBot {
    private final String botUsername;
    private final String botToken;

    public RepostCleanerBot(String botToken, String botUsername) {
        super();
        this.botUsername = botUsername;
        this.botToken = botToken;

        register(new StartCommand("start", "Старт"));
    }

    @Override
    public String getBotUsername() {
        return this.botUsername;
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        SendMessage answer = new SendMessage();
        boolean sendResponse = false;

        Message msg = update.getMessage();
        if (msg != null) {
            sendResponse = true;
            User user = msg.getFrom();
            String userName = user.getUserName();

            answer.setChatId(msg.getChatId().toString());
            answer.setText(userName);
        } else if(update.getChatMember() != null) {
            sendResponse = true;
            answer.setChatId(update.getChatMember().getChat().getId().toString());
            answer.setText(update.getChatMember().getFrom().getUserName());
        }

        if(sendResponse) {
            try {
                execute(answer);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotToken() {
        return this.botToken;
    }
}
