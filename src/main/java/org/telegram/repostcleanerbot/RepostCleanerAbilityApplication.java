package org.telegram.repostcleanerbot;

import it.tdlight.common.Init;
import it.tdlight.common.utils.CantLoadLibrary;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.repostcleanerbot.utils.InstantiationUtils;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class RepostCleanerAbilityApplication {
    public static void main(String[] args) throws CantLoadLibrary {
        try {
            Init.start(); // Initialize TDLight native libraries
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(InstantiationUtils.getInstance(AbilityBot.class));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
