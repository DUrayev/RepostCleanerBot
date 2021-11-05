package org.telegram.repostcleanerbot;

import it.tdlight.client.APIToken;
import it.tdlight.client.TDLibSettings;
import it.tdlight.common.Init;
import it.tdlight.common.utils.CantLoadLibrary;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RepostCleanerAbilityApplication {
    public static void main(String[] args) {
        try {
            TDLibSettings tdLibSettings = initializeTdLib(System.getenv().get("REPOST_CLEANER_BOT_API_ID"), System.getenv().get("REPOST_CLEANER_BOT_API_HASH"));

            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new RepostCleanerAbilityBot(
                    System.getenv().get("REPOST_CLEANER_BOT_TOKEN"),
                    System.getenv().get("REPOST_CLEANER_BOT_NAME"),
                    System.getenv().get("REPOST_CLEANER_BOT_ADMIN_ID"),
                    tdLibSettings

            ));
        } catch (TelegramApiException | CantLoadLibrary e) {
            e.printStackTrace();
        }
    }

    public static TDLibSettings initializeTdLib(String apiId, String apiHash) throws CantLoadLibrary {
        // Initialize TDLight native libraries
        Init.start();
        // Obtain the API token
        APIToken apiToken = new APIToken(Integer.parseInt(apiId), apiHash);

        // Configure the client
        TDLibSettings settings = TDLibSettings.create(apiToken);

        return settings;
    }
}
