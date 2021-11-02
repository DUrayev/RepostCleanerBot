package org.telegram.repostcleanerbot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class RepostCleanerAbilityApplication {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new RepostCleanerAbilityBot(
                    System.getenv().get("REPOST_CLEANER_BOT_TOKEN"),
                    System.getenv().get("REPOST_CLEANER_BOT_NAME"),
                    System.getenv().get("REPOST_CLEANER_BOT_ADMIN_ID"),
                    System.getenv().get("REPOST_CLEANER_BOT_API_ID"),
                    System.getenv().get("REPOST_CLEANER_BOT_API_HASH")
            ));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
