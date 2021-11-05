package org.telegram.repostcleanerbot.tdlib;

import it.tdlight.client.TDLibSettings;
import org.telegram.repostcleanerbot.tdlib.client.BotEmbadedTelegramClient;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ClientManager {
    private static ClientManager instance;

    private Map<Long, BotEmbadedTelegramClient> clientPerUser = new HashMap<>();

    private ClientManager() {}

    public static ClientManager getInstance() {
        if(instance == null) {
            instance = new ClientManager();
        }
        return instance;
    }

    public BotEmbadedTelegramClient getTelegramClientForUser(long userId, TDLibSettings settings) {
        BotEmbadedTelegramClient client = clientPerUser.get(userId);
        if(client == null) {
            // Configure the session directory
            Path sessionPath = Paths.get("tdlight-session\\" + userId);
            settings.setDatabaseDirectoryPath(sessionPath.resolve("data"));
            settings.setDownloadedFilesDirectoryPath(sessionPath.resolve("downloads"));

            client = new BotEmbadedTelegramClient(settings);
            clientPerUser.put(userId, client);
        }
        return client;
    }
}
