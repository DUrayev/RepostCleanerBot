package org.telegram.repostcleanerbot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class RepostCleanerApplication {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new RepostCleanerBot(
                    System.getenv().get("REPOST_CLEANER_BOT_TOKEN"),
                    System.getenv().get("REPOST_CLEANER_BOT_NAME")
            ));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
